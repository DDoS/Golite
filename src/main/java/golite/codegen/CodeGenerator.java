package golite.codegen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import golite.ir.IrVisitor;
import golite.ir.node.Append;
import golite.ir.node.Assignment;
import golite.ir.node.BinArFloat64;
import golite.ir.node.BinArInt;
import golite.ir.node.BoolLit;
import golite.ir.node.Call;
import golite.ir.node.Cast;
import golite.ir.node.CmpBool;
import golite.ir.node.CmpFloat64;
import golite.ir.node.CmpInt;
import golite.ir.node.CmpString;
import golite.ir.node.ConcatString;
import golite.ir.node.Expr;
import golite.ir.node.Float64Lit;
import golite.ir.node.FunctionDecl;
import golite.ir.node.Identifier;
import golite.ir.node.Indexing;
import golite.ir.node.IntLit;
import golite.ir.node.IrNode;
import golite.ir.node.Jump;
import golite.ir.node.JumpCond;
import golite.ir.node.Label;
import golite.ir.node.LogicAnd;
import golite.ir.node.LogicNot;
import golite.ir.node.LogicOr;
import golite.ir.node.MemsetZero;
import golite.ir.node.PrintBool;
import golite.ir.node.PrintFloat64;
import golite.ir.node.PrintInt;
import golite.ir.node.PrintRune;
import golite.ir.node.PrintString;
import golite.ir.node.Program;
import golite.ir.node.Select;
import golite.ir.node.Stmt;
import golite.ir.node.StringLit;
import golite.ir.node.UnaArFloat64;
import golite.ir.node.UnaArInt;
import golite.ir.node.ValueReturn;
import golite.ir.node.VariableDecl;
import golite.ir.node.VoidReturn;
import golite.semantic.symbol.Function;
import golite.semantic.symbol.Variable;
import golite.semantic.type.ArrayType;
import golite.semantic.type.BasicType;
import golite.semantic.type.FunctionType;
import golite.semantic.type.FunctionType.Parameter;
import golite.semantic.type.IndexableType;
import golite.semantic.type.SliceType;
import golite.semantic.type.StructType;
import golite.semantic.type.StructType.Field;
import golite.semantic.type.Type;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;

import static org.bytedeco.javacpp.LLVM.*;

/**
 *
 */
public class CodeGenerator implements IrVisitor {
    private LLVMModuleRef module;
    private LLVMTypeRef sliceRuntimeType;
    private LLVMValueRef memsetFunction;
    private LLVMValueRef printBoolFunction;
    private LLVMValueRef printIntFunction;
    private LLVMValueRef printRuneFunction;
    private LLVMValueRef printFloat64Function;
    private LLVMValueRef printStringFunction;
    private LLVMValueRef checkBoundsFunction;
    private LLVMValueRef sliceAppendFunction;
    private LLVMValueRef sliceConcatFunction;
    private LLVMValueRef compareStringFunction;
    private final Map<StructType, LLVMTypeRef> structs = new HashMap<>();
    private final Map<String, LLVMValueRef> stringConstants = new HashMap<>();
    private final Map<Function, LLVMValueRef> functions = new HashMap<>();
    private LLVMValueRef currentFunction;
    private LLVMBuilderRef builder;
    private final Map<Expr<?>, LLVMValueRef> exprValues = new HashMap<>();
    private final Map<Expr<?>, LLVMValueRef> exprPtrs = new HashMap<>();
    private final Map<Variable<?>, LLVMValueRef> globalVariables = new HashMap<>();
    private final Map<Variable<?>, LLVMValueRef> functionVariables = new HashMap<>();
    private final Map<Label, LLVMBasicBlockRef> functionBlocks = new HashMap<>();
    private LLVMBasicBlockRef currentBlock;

    public ProgramCode getCode() {
        if (module == null) {
            throw new IllegalStateException("The visitor hasn't been applied to a program yet");
        }
        return new ProgramCode(module);
    }

    @Override
    public void visitProgram(Program program) {
        // Create the module
        module = LLVMModuleCreateWithName("golite." + program.getPackageName());
        // Declare the external function from the C stdlib and the Golite runtime
        declareExternalSymbols();
        // Codegen it
        program.getGlobals().forEach(this::codeGenGlobal);
        program.getFunctions().forEach(function -> function.visit(this));
        // Validate it
        final BytePointer errorMsgPtr = new BytePointer((Pointer) null);
        String errorMsg = null;
        if (LLVMVerifyModule(module, LLVMReturnStatusAction, errorMsgPtr) != 0) {
            errorMsg = errorMsgPtr.getString();
            LLVMDisposeMessage(errorMsgPtr);
        }
        if (errorMsg != null) {
            final BytePointer moduleString = LLVMPrintModuleToString(module);
            String detailedErrorMsg = "Failed to verify module: " + errorMsg +
                    "\n==================\n" + moduleString.getString();
            LLVMDisposeMessage(moduleString);
            throw new RuntimeException(detailedErrorMsg);
        }
    }

    private void codeGenGlobal(VariableDecl<?> variableDecl) {
        final Variable<?> variable = variableDecl.getVariable();
        final LLVMTypeRef type = createType(variable.getType());
        final LLVMValueRef global = LLVMAddGlobal(module, type, variableDecl.getUniqueName());
        LLVMSetLinkage(global, LLVMPrivateLinkage);
        LLVMSetInitializer(global, LLVMConstNull(type));
        globalVariables.put(variable, global);
    }

    @Override
    public void visitFunctionDecl(FunctionDecl functionDecl) {
        final Function symbol = functionDecl.getFunction();
        // The only external functions are the static initializer and golite main
        final boolean external = functionDecl.isStaticInit() || functionDecl.isMain();
        // Build the LLVM function type
        final FunctionType functionType = symbol.getType();
        final List<Parameter> params = functionType.getParameters();
        final LLVMTypeRef[] llvmParams = new LLVMTypeRef[params.size()];
        for (int i = 0; i < llvmParams.length; i++) {
            llvmParams[i] = createType(params.get(i).getType());
        }
        final LLVMTypeRef llvmReturn = functionType.getReturnType().map(this::createType).orElse(LLVMVoidType());
        // Prevent user defined functions from having the same name as the static initializer or golite main
        final String name;
        if (functionDecl.isStaticInit()) {
            name = STATIC_INIT_FUNCTION;
        } else if (functionDecl.isMain()) {
            name = GOLITE_MAIN_FUNCTION;
        } else {
            final String funcName = symbol.getName();
            switch (funcName) {
                case STATIC_INIT_FUNCTION:
                    name = STATIC_INIT_FUNCTION + "1";
                    break;
                case GOLITE_MAIN_FUNCTION:
                    name = GOLITE_MAIN_FUNCTION + "1";
                    break;
                default:
                    name = funcName;
                    break;
            }
        }
        // Create the LLVM function
        final LLVMValueRef function = createFunction(external, name, llvmReturn, llvmParams);
        functions.put(symbol, function);
        currentFunction = function;
        // Create the builder for the function
        builder = LLVMCreateBuilder();
        // Start the function body
        final LLVMBasicBlockRef entry = LLVMAppendBasicBlock(function, "entry");
        LLVMPositionBuilderAtEnd(builder, entry);
        currentBlock = entry;
        // Place the variables values for the parameters on the stack
        final List<Variable<?>> paramVariables = symbol.getParameters();
        final List<String> paramUniqueNames = functionDecl.getParamUniqueNames();
        for (int i = 0; i < paramVariables.size(); i++) {
            final Variable<?> variable = paramVariables.get(i);
            final LLVMValueRef varPtr = allocateStackVariable(variable, paramUniqueNames.get(i));
            functionVariables.put(variable, varPtr);
            // Store the parameter in the stack variable
            LLVMBuildStore(builder, LLVMGetParam(function, i), varPtr);
        }
        // Generate all the basic blocks the the labels in advance
        final List<Stmt> statements = functionDecl.getStatements();
        functionBlocks.clear();
        statements.stream()
                .filter(stmt -> stmt instanceof Label)
                .map(stmt -> (Label) stmt)
                .forEach(label -> functionBlocks.put(label, LLVMAppendBasicBlock(function, label.getName())));
        // Codegen the body
        statements.forEach(stmt -> stmt.visit(this));
        // Dispose of the builder
        LLVMDisposeBuilder(builder);
        // Clear the function's variables as we exit it
        functionVariables.clear();
    }

    @Override
    public void visitVariableDecl(VariableDecl<?> variableDecl) {
        final Variable<?> variable = variableDecl.getVariable();
        final LLVMValueRef varPtr = allocateStackVariable(variable, variableDecl.getUniqueName());
        functionVariables.put(variable, varPtr);
    }

    @Override
    public void visitVoidReturn(VoidReturn voidReturn) {
        LLVMBuildRetVoid(builder);
    }

    @Override
    public void visitValueReturn(ValueReturn valueReturn) {
        valueReturn.getValue().visit(this);
        LLVMBuildRet(builder, getExprValue(valueReturn.getValue()));
    }

    @Override
    public void visitPrintBool(PrintBool printBool) {
        generatePrintStmt(printBool.getValue(), printBoolFunction);
    }

    @Override
    public void visitPrintInt(PrintInt printInt) {
        generatePrintStmt(printInt.getValue(), printIntFunction);
    }

    @Override
    public void visitPrintRune(PrintRune printRune) {
        generatePrintStmt(printRune.getValue(), printRuneFunction);
    }

    @Override
    public void visitPrintFloat64(PrintFloat64 printFloat64) {
        generatePrintStmt(printFloat64.getValue(), printFloat64Function);
    }

    @Override
    public void visitPrintString(PrintString printString) {
        generatePrintStmt(printString.getValue(), printStringFunction);
    }

    private void generatePrintStmt(Expr<BasicType> value, LLVMValueRef printFunction) {
        value.visit(this);
        LLVMValueRef arg = getExprValue(value);
        if (value.getType() == BasicType.BOOL) {
            // Must zero-extent bool (i1) to char (i8)
            arg = LLVMBuildZExt(builder, arg, LLVMInt8Type(), "boolToChar");
        }
        final LLVMValueRef[] args = {arg};
        LLVMBuildCall(builder, printFunction, new PointerPointer<>(args), 1, "");
    }

    @Override
    public void visitMemsetZero(MemsetZero memsetZero) {
        final Expr<?> value = memsetZero.getValue();
        value.visit(this);
        // Get the size of the of the memory to clear using an LLVM trick
        // (pointer to second element starting at null, then convert to int)
        final LLVMValueRef size = calculateSizeOfType(value.getType());
        // Call the memset intrinsic on a pointer to the value
        final LLVMTypeRef i8Ptr = LLVMPointerType(LLVMInt8Type(), 0);
        final LLVMValueRef bytePtr = LLVMBuildPointerCast(builder, getExprPtr(value), i8Ptr, value + "Ptr");
        final LLVMValueRef[] memsetArgs = {
                bytePtr, LLVMConstInt(LLVMInt8Type(), 0, 0), size,
                LLVMConstInt(LLVMInt32Type(), 0, 0), LLVMConstInt(LLVMInt1Type(), 0, 0)
        };
        LLVMBuildCall(builder, memsetFunction, new PointerPointer<>(memsetArgs), memsetArgs.length, "");
    }

    @Override
    public void visitAssignment(Assignment assignment) {
        assignment.getLeft().visit(this);
        assignment.getRight().visit(this);
        final LLVMValueRef ptr = getExprPtr(assignment.getLeft());
        final LLVMValueRef value = getExprValue(assignment.getRight());
        LLVMBuildStore(builder, value, ptr);
    }

    @Override
    public void visitJump(Jump jump) {
        final LLVMBasicBlockRef target = functionBlocks.get(jump.getLabel());
        LLVMBuildBr(builder, target);
    }

    @Override
    public void visitJumpCond(JumpCond jumpCond) {
        final Label label = jumpCond.getLabel();
        final LLVMBasicBlockRef trueTarget = functionBlocks.get(label);
        // Add a new basic block after the current one, which is where flow will resume if the condition is false
        final LLVMBasicBlockRef falseTarget = LLVMAppendBasicBlock(currentFunction, label.getName() + "False");
        LLVMMoveBasicBlockAfter(falseTarget, currentBlock);
        // The build the conditional break instruction, to the true label, or to the block just after the jump
        jumpCond.getCondition().visit(this);
        final LLVMValueRef condition = getExprValue(jumpCond.getCondition());
        LLVMBuildCondBr(builder, condition, trueTarget, falseTarget);
        // Move to build to the new false block
        LLVMPositionBuilderAtEnd(builder, falseTarget);
        currentBlock = falseTarget;
    }

    @Override
    public void visitLabel(Label label) {
        final LLVMBasicBlockRef block = functionBlocks.get(label);
        LLVMPositionBuilderAtEnd(builder, block);
        currentBlock = block;
    }

    @Override
    public void visitIntLit(IntLit intLit) {
        exprValues.put(intLit, LLVMConstInt(LLVMInt32Type(), intLit.getValue(), 1));
    }

    @Override
    public void visitFloatLit(Float64Lit float64Lit) {
        exprValues.put(float64Lit, LLVMConstReal(LLVMDoubleType(), float64Lit.getValue()));
    }

    @Override
    public void visitBoolLit(BoolLit boolLit) {
        exprValues.put(boolLit, LLVMConstInt(LLVMInt1Type(), boolLit.getValue() ? 1 : 0, 0));
    }

    @Override
    public void visitStringLit(StringLit stringLit) {
        // Declare the string constant, then get a pointer to the first character
        final LLVMValueRef value = declareStringConstant(stringLit);
        final LLVMValueRef stringPtr = getAggregateMemberPtr(value, LLVMConstInt(LLVMInt64Type(), 0, 0),
                "strPtr");
        // Create the string struct with the length and character array
        final LLVMValueRef[] stringData = {
                LLVMConstInt(LLVMInt32Type(), stringLit.getUtf8Data().limit(), 1), stringPtr
        };
        final LLVMValueRef stringStruct = LLVMConstNamedStruct(sliceRuntimeType, new PointerPointer<>(stringData),
                stringData.length);
        exprValues.put(stringLit, stringStruct);
    }

    @Override
    public void visitIdentifier(Identifier<?> identifier) {
        // Only put a pointer to the variable, it will be loaded when necessary
        // Start by checking the function variables
        LLVMValueRef varPtr = functionVariables.get(identifier.getVariable());
        if (varPtr != null) {
            exprPtrs.put(identifier, varPtr);
            return;
        }
        // Otherwise go for the globals
        varPtr = globalVariables.get(identifier.getVariable());
        if (varPtr == null) {
            throw new IllegalStateException("Should have found the variable " + identifier.toString());
        }
        exprPtrs.put(identifier, varPtr);
    }

    @Override
    public void visitSelect(Select select) {
        // Get a pointer to the struct value
        final Expr<StructType> value = select.getValue();
        value.visit(this);
        final LLVMValueRef valuePtr = getExprPtr(value);
        // Get the constant index into that struct
        final int index = value.getType().fieldIndex(select.getField());
        final LLVMValueRef indexValue = LLVMConstInt(LLVMInt32Type(), index, 1);
        // The resulting value is a pointer to the field in the struct
        final LLVMValueRef fieldPtr = getAggregateMemberPtr(valuePtr, indexValue, select.getField());
        exprPtrs.put(select, fieldPtr);
    }

    @Override
    public void visitIndexing(Indexing<?> indexing) {
        final Expr<? extends IndexableType> value = indexing.getValue();
        value.visit(this);
        final LLVMValueRef valuePtr = getExprPtr(value);
        indexing.getIndex().visit(this);
        final LLVMValueRef index = getExprValue(indexing.getIndex());
        // Find the length of the slice or array
        final Type componentType = value.getType().getComponent();
        final LLVMValueRef length;
        if (value.getType() instanceof SliceType) {
            // Get a pointer to the length field (first in the struct)
            final LLVMValueRef lengthIndex = LLVMConstInt(LLVMInt32Type(), 0, 0);
            final LLVMValueRef lengthPtr = getAggregateMemberPtr(valuePtr, lengthIndex, "i8Length");
            // Then load the value at the pointer
            final LLVMValueRef i8Length = LLVMBuildLoad(builder, lengthPtr, "i8Length");
            // Finally divide it by the length of the components
            final LLVMValueRef sizeOfComponent = calculateSizeOfType(componentType);
            length = LLVMBuildSDiv(builder, i8Length, sizeOfComponent, "length");
        } else {
            // This is just a constant int
            length = LLVMConstInt(LLVMInt32Type(), ((ArrayType) value.getType()).getLength(), 1);
        }
        // Call the bounds check function with the length and index
        final LLVMValueRef[] lengthArg = {index, length};
        LLVMBuildCall(builder, checkBoundsFunction, new PointerPointer<>(lengthArg), lengthArg.length, "");
        // Get a pointer to the start of the indexable data
        final LLVMValueRef dataPtr;
        if (value.getType() instanceof SliceType) {
            // For a slice, we first get a pointer to the raw data (int8_t*, second field in the struct)
            final LLVMValueRef dataFieldIndex = LLVMConstInt(LLVMInt32Type(), 1, 0);
            final LLVMValueRef rawDataPtrPtr = getAggregateMemberPtr(valuePtr, dataFieldIndex, "rawDataPtr");
            final LLVMValueRef rawDataPtr = LLVMBuildLoad(builder, rawDataPtrPtr, "rawDataPtr");
            // Then we cast this pointer to the array component type
            final LLVMTypeRef componentPtrType = LLVMPointerType(createType(componentType), 0);
            dataPtr = LLVMBuildPointerCast(builder, rawDataPtr, componentPtrType, "dataPtr");
        } else {
            // For arrays we just have to get a pointer to the first element
            final LLVMValueRef firstIndex = LLVMConstInt(LLVMInt64Type(), 0, 0);
            dataPtr = getAggregateMemberPtr(valuePtr, firstIndex, "rawData");
        }
        // Then we can address the pointer at the index into the array
        final LLVMValueRef[] indices = {index};
        final LLVMValueRef componentPtr = LLVMBuildGEP(builder, dataPtr, new PointerPointer<>(indices), indices.length,
                "componentPtr");
        exprPtrs.put(indexing, componentPtr);
    }

    @Override
    public void visitCall(Call call) {
        final Function function = call.getFunction();
        final LLVMValueRef llvmFunction = functions.get(function);
        // Codegen the arguments
        final List<Expr<?>> args = call.getArguments();
        args.forEach(arg -> arg.visit(this));
        final LLVMValueRef[] argValues = args.stream().map(this::getExprValue).toArray(LLVMValueRef[]::new);
        // Build the call
        final String name = function.getType().getReturnType().isPresent() ? function.getName() : "";
        final LLVMValueRef value = LLVMBuildCall(builder, llvmFunction,
                new PointerPointer<>(argValues), argValues.length, name);
        exprValues.put(call, value);
    }

    @Override
    public void visitCast(Cast cast) {
        final Expr<BasicType> arg = cast.getArgument();
        arg.visit(this);
        final LLVMValueRef argValue = getExprValue(arg);
        final Type argType = arg.getType();
        final Type castType = cast.getType();
        final LLVMValueRef value;
        if (argType.isInteger() && castType == BasicType.FLOAT64) {
            // Integer to float
            value = LLVMBuildSIToFP(builder, argValue, LLVMDoubleType(), "intToFloat64");
        } else if (argType == BasicType.FLOAT64 && castType.isInteger()) {
            // Float to integer
            value = LLVMBuildFPToSI(builder, argValue, LLVMInt32Type(), "float64ToInt");
        } else {
            // Anything else is an identity cast, so ignore it
            value = argValue;
        }
        exprValues.put(cast, value);
    }

    @Override
    public void visitAppend(Append append) {
        append.getLeft().visit(this);
        append.getRight().visit(this);
        // First get the slice value
        final LLVMValueRef slice = getExprValue(append.getLeft());
        // Then get a pointer to the data to append, cast to int_t*
        final LLVMValueRef rightPtr = getExprPtr(append.getRight());
        final LLVMTypeRef i8Ptr = LLVMPointerType(LLVMInt8Type(), 0);
        final LLVMValueRef appendDataPtr = LLVMBuildPointerCast(builder, rightPtr, i8Ptr, "appendDataPtr");
        // Next we calculate the size of the data to append
        final LLVMValueRef appendLength = calculateSizeOfType(append.getRight().getType());
        // Finally we can call the append function with the values obtained previously
        final LLVMValueRef[] appendCallArgs = {slice, appendDataPtr, appendLength};
        final LLVMValueRef appendCall = LLVMBuildCall(builder, sliceAppendFunction, new PointerPointer<>(appendCallArgs),
                appendCallArgs.length, "append");
        exprValues.put(append, appendCall);
    }

    @Override
    public void visitLogicNot(LogicNot logicNot) {
        logicNot.getInner().visit(this);
        final LLVMValueRef inner = getExprValue(logicNot.getInner());
        final LLVMValueRef result = LLVMBuildNot(builder, inner, "logicNot");
        exprValues.put(logicNot, result);
    }

    @Override
    public void visitUnaArInt(UnaArInt unaArInt) {
        unaArInt.getInner().visit(this);
        final LLVMValueRef result;
        final LLVMValueRef inner = getExprValue(unaArInt.getInner());
        if (unaArInt.getOp() == UnaArInt.Op.NEG) {
            result = LLVMBuildNeg(builder, inner, "intNeg");
            exprValues.put(unaArInt, result);
        } else if (unaArInt.getOp() == UnaArInt.Op.BIT_NEG) {
            result = LLVMBuildNot(builder, inner, "intBitNeg");
            exprValues.put(unaArInt, result);
        } else {
            exprValues.put(unaArInt, getExprValue(unaArInt.getInner()));
        }
    }

    @Override
    public void visitUnaArFloat64(UnaArFloat64 unaArFloat64) {
        unaArFloat64.getInner().visit(this);
        final LLVMValueRef exp = getExprValue(unaArFloat64.getInner());
        if (unaArFloat64.getOp() == UnaArFloat64.Op.NEG) {
            final LLVMValueRef negExp = LLVMBuildFNeg(builder, exp, "float64Neg");
            exprValues.put(unaArFloat64, negExp);
        } else {
            exprValues.put(unaArFloat64, getExprValue(unaArFloat64.getInner()));
        }
    }

    @Override
    public void visitBinArInt(BinArInt binArInt) {
        final Expr<BasicType> left = binArInt.getLeft();
        final Expr<BasicType> right = binArInt.getRight();
        left.visit(this);
        right.visit(this);
        final LLVMValueRef leftValue = getExprValue(left);
        final LLVMValueRef rightValue = getExprValue(right);
        final LLVMValueRef result;
        switch (binArInt.getOp()) {
            case ADD:
                result = LLVMBuildAdd(builder, leftValue, rightValue, "intAdd");
                break;
            case SUB:
                result = LLVMBuildSub(builder, leftValue, rightValue, "intSub");
                break;
            case MUL:
                result = LLVMBuildMul(builder, leftValue, rightValue, "intMul");
                break;
            case DIV:
                result = LLVMBuildSDiv(builder, leftValue, rightValue, "intDiv");
                break;
            case REM:
                result = LLVMBuildSRem(builder, leftValue, rightValue, "intRem");
                break;
            case BIT_OR:
                result = LLVMBuildOr(builder, leftValue, rightValue, "intBitOr");
                break;
            case BIT_AND:
                result = LLVMBuildAnd(builder, leftValue, rightValue, "intBitAnd");
                break;
            case LSHIFT:
                result = LLVMBuildShl(builder, leftValue, rightValue, "intLshift");
                break;
            case RSHIFT:
                result = LLVMBuildAShr(builder, leftValue, rightValue, "intRshift");
                break;
            case BIT_XOR:
                result = LLVMBuildXor(builder, leftValue, rightValue, "intXor");
                break;
            case BIT_AND_NOT:
                final LLVMValueRef intBitNot = LLVMBuildNot(builder, rightValue, "intBitNot");
                result = LLVMBuildAnd(builder, leftValue, intBitNot, "intAndNot");
                break;
            default:
                throw new UnsupportedOperationException(binArInt.getOp().name());
        }
        exprValues.put(binArInt, result);
    }

    @Override
    public void visitConcatString(ConcatString concatString) {
        concatString.getLeft().visit(this);
        concatString.getRight().visit(this);
        final LLVMValueRef[] args = {getExprValue(concatString.getLeft()), getExprValue(concatString.getRight())};
        final LLVMValueRef concat = LLVMBuildCall(builder, sliceConcatFunction, new PointerPointer<>(args), args.length,
                "concat");
        exprValues.put(concatString, concat);
    }

    @Override
    public void visitBinArFloat64(BinArFloat64 binArFloat64) {
        final Expr<BasicType> left = binArFloat64.getLeft();
        final Expr<BasicType> right = binArFloat64.getRight();
        left.visit(this);
        right.visit(this);
        final LLVMValueRef leftValue = getExprValue(left);
        final LLVMValueRef rightValue = getExprValue(right);
        final LLVMValueRef exp;
        switch (binArFloat64.getOp()) {
            case ADD:
                exp = LLVMBuildFAdd(builder, leftValue, rightValue, "float64Add");
                break;
            case SUB:
                exp = LLVMBuildFSub(builder, leftValue, rightValue, "float64Sub");
                break;
            case MUL:
                exp = LLVMBuildFMul(builder, leftValue, rightValue, "float64Mul");
                break;
            case DIV:
                exp = LLVMBuildFDiv(builder, leftValue, rightValue, "float64Div");
                break;
            default:
                throw new UnsupportedOperationException(binArFloat64.getOp().name());
        }
        exprValues.put(binArFloat64, exp);
    }

    @Override
    public void visitCmpBool(CmpBool cmpBool) {
        final Expr<BasicType> left = cmpBool.getLeft();
        final Expr<BasicType> right = cmpBool.getRight();
        left.visit(this);
        right.visit(this);
        final LLVMValueRef leftValue = getExprValue(left);
        final LLVMValueRef rightValue = getExprValue(right);
        final LLVMValueRef result;
        switch (cmpBool.getOp()) {
            case EQ:
                result = LLVMBuildICmp(builder, LLVMIntEQ, leftValue, rightValue, "boolEq");
                break;
            case NEQ:
                result = LLVMBuildICmp(builder, LLVMIntNE, leftValue, rightValue, "boolNeq");
                break;
            default:
                throw new UnsupportedOperationException(cmpBool.getOp().name());
        }
        exprValues.put(cmpBool, result);
    }

    @Override
    public void visitCmpInt(CmpInt cmpInt) {
        final Expr<BasicType> left = cmpInt.getLeft();
        final Expr<BasicType> right = cmpInt.getRight();
        left.visit(this);
        right.visit(this);
        final LLVMValueRef leftValue = getExprValue(left);
        final LLVMValueRef rightValue = getExprValue(right);
        final LLVMValueRef result;
        switch (cmpInt.getOp()) {
            case EQ:
                result = LLVMBuildICmp(builder, LLVMIntEQ, leftValue, rightValue, "intEq");
                break;
            case NEQ:
                result = LLVMBuildICmp(builder, LLVMIntNE, leftValue, rightValue, "intNeq");
                break;
            case LESS:
                result = LLVMBuildICmp(builder, LLVMIntSLT, leftValue, rightValue, "intLess");
                break;
            case LESS_EQ:
                result = LLVMBuildICmp(builder, LLVMIntSLE, leftValue, rightValue, "intLessEq");
                break;
            case GREAT:
                result = LLVMBuildICmp(builder, LLVMIntSGT, leftValue, rightValue, "intGreat");
                break;
            case GREAT_EQ:
                result = LLVMBuildICmp(builder, LLVMIntSGE, leftValue, rightValue, "intGreatEq");
                break;
            default:
                throw new UnsupportedOperationException(cmpInt.getOp().name());
        }
        exprValues.put(cmpInt, result);
    }

    @Override
    public void visitCmpFloat64(CmpFloat64 cmpFloat64) {
        final Expr<BasicType> left = cmpFloat64.getLeft();
        final Expr<BasicType> right = cmpFloat64.getRight();
        left.visit(this);
        right.visit(this);
        final LLVMValueRef leftValue = getExprValue(left);
        final LLVMValueRef rightValue = getExprValue(right);
        final LLVMValueRef result;
        switch (cmpFloat64.getOp()) {
            case EQ:
                result = LLVMBuildICmp(builder, LLVMRealOEQ, leftValue, rightValue, "float64Eq");
                break;
            case NEQ:
                result = LLVMBuildICmp(builder, LLVMRealONE, leftValue, rightValue, "float64Neq");
                break;
            case LESS:
                result = LLVMBuildICmp(builder, LLVMRealOLT, leftValue, rightValue, "float64Less");
                break;
            case LESS_EQ:
                result = LLVMBuildICmp(builder, LLVMRealOLE, leftValue, rightValue, "float64LessEq");
                break;
            case GREAT:
                result = LLVMBuildICmp(builder, LLVMRealOGT, leftValue, rightValue, "float64Great");
                break;
            case GREAT_EQ:
                result = LLVMBuildICmp(builder, LLVMRealOGE, leftValue, rightValue, "float64GreatEq");
                break;
            default:
                throw new UnsupportedOperationException(cmpFloat64.getOp().name());
        }
        exprValues.put(cmpFloat64, result);
    }

    @Override
    public void visitCmpString(CmpString cmpString) {
        cmpString.getLeft().visit(this);
        cmpString.getRight().visit(this);
        final LLVMValueRef kind = LLVMConstInt(LLVMInt32Type(), cmpString.getOp().getRuntimeID(), 0);
        final LLVMValueRef[] args = {kind, getExprValue(cmpString.getLeft()), getExprValue(cmpString.getRight())};
        final LLVMValueRef i8Compare = LLVMBuildCall(builder, compareStringFunction, new PointerPointer<>(args), args.length,
                "i8String" + cmpString.getOp().getCamelCase());
        final LLVMValueRef compare = LLVMBuildTrunc(builder, i8Compare, LLVMInt1Type(),
                "string" + cmpString.getOp().getCamelCase());
        exprValues.put(cmpString, compare);
    }

    @Override
    public void visitLogicAnd(LogicAnd logicAnd) {
        // Add two new basic blocks: short-circuiting, and evaluating the right side
        final LLVMBasicBlockRef andFull = LLVMAppendBasicBlock(currentFunction, "andFull");
        LLVMMoveBasicBlockAfter(andFull, currentBlock);
        final LLVMBasicBlockRef andEnd = LLVMAppendBasicBlock(currentFunction, "andEnd");
        LLVMMoveBasicBlockAfter(andEnd, andFull);
        // Allocate a variable to store the result, and initialize it to false
        final LLVMValueRef andResultPtr = LLVMBuildAlloca(builder, LLVMInt1Type(), "andResultPtr");
        LLVMBuildStore(builder, LLVMConstInt(LLVMInt1Type(), 0, 0), andResultPtr);
        // Evaluate the left side in the current block, if it's true, then jump to the block for the right side
        logicAnd.getLeft().visit(this);
        final LLVMValueRef left = getExprValue(logicAnd.getLeft());
        LLVMBuildCondBr(builder, left, andFull, andEnd);
        // Start the block for the right side
        LLVMPositionBuilderAtEnd(builder, andFull);
        currentBlock = andFull;
        // Evaluate the right and store its value as the result, then jump to the end
        logicAnd.getRight().visit(this);
        final LLVMValueRef right = getExprValue(logicAnd.getRight());
        LLVMBuildStore(builder, right, andResultPtr);
        LLVMBuildBr(builder, andEnd);
        // Start the end block
        LLVMPositionBuilderAtEnd(builder, andEnd);
        currentBlock = andEnd;
        // Return the value in the result variable
        final LLVMValueRef andResult = LLVMBuildLoad(builder, andResultPtr, "andResult");
        exprValues.put(logicAnd, andResult);
    }

    @Override
    public void visitLogicOr(LogicOr logicOr) {
        // Add two new basic blocks: short-circuiting, and evaluating the right side
        final LLVMBasicBlockRef orFull = LLVMAppendBasicBlock(currentFunction, "orFull");
        LLVMMoveBasicBlockAfter(orFull, currentBlock);
        final LLVMBasicBlockRef orEnd = LLVMAppendBasicBlock(currentFunction, "orEnd");
        LLVMMoveBasicBlockAfter(orEnd, orFull);
        // Allocate a variable to store the result, and initialize it to true
        final LLVMValueRef orResultPtr = LLVMBuildAlloca(builder, LLVMInt1Type(), "orResultPtr");
        LLVMBuildStore(builder, LLVMConstInt(LLVMInt1Type(), 1, 0), orResultPtr);
        // Evaluate the left side in the current block, if it's false, then jump to the block for the right side
        logicOr.getLeft().visit(this);
        final LLVMValueRef left = getExprValue(logicOr.getLeft());
        LLVMBuildCondBr(builder, left, orEnd, orFull);
        // Start the block for the right side
        LLVMPositionBuilderAtEnd(builder, orFull);
        currentBlock = orFull;
        // Evaluate the right and store its value as the result, then jump to the end
        logicOr.getRight().visit(this);
        final LLVMValueRef right = getExprValue(logicOr.getRight());
        LLVMBuildStore(builder, right, orResultPtr);
        LLVMBuildBr(builder, orEnd);
        // Start the end block
        LLVMPositionBuilderAtEnd(builder, orEnd);
        currentBlock = orEnd;
        // Return the value in the result variable
        final LLVMValueRef orResult = LLVMBuildLoad(builder, orResultPtr, "orResult");
        exprValues.put(logicOr, orResult);
    }

    private LLVMValueRef getExprValue(Expr<?> expr) {
        // This might be directly a value
        final LLVMValueRef value = exprValues.get(expr);
        if (value != null) {
            return value;
        }
        // Otherwise we need to load the pointer
        final LLVMValueRef ptr = exprPtrs.get(expr);
        return LLVMBuildLoad(builder, ptr, IrNode.toString(expr));
    }

    private LLVMValueRef getExprPtr(Expr<?> expr) {
        // This might already be a pointer value
        final LLVMValueRef ptr = exprPtrs.get(expr);
        if (ptr != null) {
            return ptr;
        }
        // Otherwise we need to store the value and return a pointer to the memory
        final LLVMValueRef value = exprValues.get(expr);
        final LLVMValueRef memory = LLVMBuildAlloca(builder, createType(expr.getType()),
                IrNode.toString(expr) + "Ptr");
        LLVMBuildStore(builder, value, memory);
        return memory;
    }

    private LLVMValueRef getAggregateMemberPtr(LLVMValueRef valuePtr, LLVMValueRef index, String memberName) {
        final LLVMValueRef[] indices = {LLVMConstInt(LLVMInt64Type(), 0, 0), index};
        return LLVMBuildInBoundsGEP(builder, valuePtr, new PointerPointer<>(indices), indices.length,
                memberName + "Ptr");
    }

    private LLVMValueRef allocateStackVariable(Variable<?> variable, String uniqueName) {
        final LLVMTypeRef type = createType(variable.getType());
        return LLVMBuildAlloca(builder, type, uniqueName);
    }

    private LLVMValueRef calculateSizeOfType(Type type) {
        // LLVM trick: get pointer to second element starting at null
        final LLVMValueRef nullPtr = LLVMConstPointerNull(LLVMPointerType(createType(type), 0));
        final LLVMValueRef[] secondIndex = {LLVMConstInt(LLVMInt64Type(), 1, 0)};
        final LLVMValueRef secondElementPtr = LLVMBuildGEP(builder, nullPtr, new PointerPointer<>(secondIndex),
                secondIndex.length, "secondElementPtr");
        // Then convert it to int, which will be the size of the first element of the array (also the size of the type)
        return LLVMBuildPtrToInt(builder, secondElementPtr, LLVMInt32Type(), "size");
    }

    private LLVMValueRef createFunction(boolean external, String name, LLVMTypeRef returnType, LLVMTypeRef... parameters) {
        final LLVMTypeRef functionType = LLVMFunctionType(returnType, new PointerPointer<>(parameters), parameters.length,
                0);
        final LLVMValueRef function = LLVMAddFunction(module, name, functionType);
        LLVMSetFunctionCallConv(function, LLVMCCallConv);
        LLVMSetLinkage(function, external ? LLVMExternalLinkage : LLVMPrivateLinkage);
        return function;
    }

    private LLVMTypeRef createType(Type type) {
        if (type == BasicType.BOOL) {
            return LLVMInt1Type();
        }
        if (type == BasicType.INT || type == BasicType.RUNE) {
            return LLVMInt32Type();
        }
        if (type == BasicType.FLOAT64) {
            return LLVMDoubleType();
        }
        if (type == BasicType.STRING) {
            return sliceRuntimeType;
        }
        if (type instanceof ArrayType) {
            final ArrayType arrayType = (ArrayType) type;
            return LLVMArrayType(createType(arrayType.getComponent()), arrayType.getLength());
        }
        if (type instanceof SliceType) {
            return sliceRuntimeType;
        }
        if (type instanceof StructType) {
            final StructType structType = (StructType) type;
            final LLVMTypeRef llvmType = structs.get(structType);
            if (llvmType != null) {
                return llvmType;
            }
            final LLVMTypeRef[] fieldTypes = createFieldTypes((structType).getFields());
            final LLVMTypeRef namedStruct = LLVMStructCreateNamed(LLVMGetGlobalContext(), "struct");
            LLVMStructSetBody(namedStruct, new PointerPointer<>(fieldTypes), fieldTypes.length, 0);
            structs.put(structType, namedStruct);
            return namedStruct;
        }
        throw new IllegalArgumentException("Unknown type class: " + type);
    }

    private LLVMTypeRef[] createFieldTypes(List<Field> fields) {
        return fields.stream().map(field -> createType(field.getType())).toArray(LLVMTypeRef[]::new);
    }

    private LLVMValueRef declareStringConstant(StringLit stringLit) {
        // Don't declare it if it already exists
        LLVMValueRef constant = stringConstants.get(stringLit.getValue());
        if (constant != null) {
            return constant;
        }
        // Otherwise create it and add it to the pool
        final BytePointer data = new BytePointer(stringLit.getUtf8Data());
        final int stringLength = (int) data.limit();
        constant = LLVMAddGlobal(module, LLVMArrayType(LLVMInt8Type(), stringLength), "strLit");
        LLVMSetLinkage(constant, LLVMInternalLinkage);
        LLVMSetGlobalConstant(constant, 1);
        LLVMSetInitializer(constant, LLVMConstString(data, stringLength, 1));
        stringConstants.put(stringLit.getValue(), constant);
        return constant;
    }

    private void declareExternalSymbols() {
        final LLVMTypeRef i8Pointer = LLVMPointerType(LLVMInt8Type(), 0);
        // Structure types
        sliceRuntimeType = LLVMStructCreateNamed(LLVMGetGlobalContext(), RUNTIME_SLICE);
        final LLVMTypeRef[] stringStructElements = {LLVMInt32Type(), i8Pointer};
        LLVMStructSetBody(sliceRuntimeType, new PointerPointer<>(stringStructElements), stringStructElements.length,
                0);
        // Intrinsics
        memsetFunction = createFunction(true, "llvm.memset.p0i8.i32", LLVMVoidType(), i8Pointer,
                LLVMInt8Type(), LLVMInt32Type(), LLVMInt32Type(), LLVMInt1Type());
        // Runtime functions
        printBoolFunction = createFunction(true, RUNTIME_PRINT_BOOL, LLVMVoidType(), LLVMInt8Type());
        printIntFunction = createFunction(true, RUNTIME_PRINT_INT, LLVMVoidType(), LLVMInt32Type());
        printRuneFunction = createFunction(true, RUNTIME_PRINT_RUNE, LLVMVoidType(), LLVMInt32Type());
        printFloat64Function = createFunction(true, RUNTIME_PRINT_FLOAT64, LLVMVoidType(), LLVMDoubleType());
        printStringFunction = createFunction(true, RUNTIME_PRINT_STRING, LLVMVoidType(), sliceRuntimeType);
        checkBoundsFunction = createFunction(true, RUNTIME_CHECK_BOUNDS, LLVMVoidType(), LLVMInt32Type(),
                LLVMInt32Type());
        sliceAppendFunction = createFunction(true, RUNTIME_SLICE_APPEND, sliceRuntimeType,
                sliceRuntimeType, i8Pointer, LLVMInt32Type());
        sliceConcatFunction = createFunction(true, RUNTIME_SLICE_CONCAT, sliceRuntimeType, sliceRuntimeType,
                sliceRuntimeType);
        compareStringFunction = createFunction(true, RUNTIME_COMPARE_STRING, LLVMInt8Type(), LLVMInt32Type(),
                sliceRuntimeType, sliceRuntimeType);
    }

    private static final String RUNTIME_SLICE = "goliteRtSlice";
    private static final String RUNTIME_PRINT_BOOL = "goliteRtPrintBool";
    private static final String RUNTIME_PRINT_INT = "goliteRtPrintInt";
    private static final String RUNTIME_PRINT_RUNE = "goliteRtPrintRune";
    private static final String RUNTIME_PRINT_FLOAT64 = "goliteRtPrintFloat64";
    private static final String RUNTIME_PRINT_STRING = "goliteRtPrintString";
    private static final String RUNTIME_CHECK_BOUNDS = "goliteRtCheckBounds";
    private static final String RUNTIME_SLICE_APPEND = "goliteRtSliceAppend";
    private static final String RUNTIME_SLICE_CONCAT = "goliteRtSliceConcat";
    private static final String RUNTIME_COMPARE_STRING = "goliteRtCompareString";
    private static final String STATIC_INIT_FUNCTION = "staticInit";
    private static final String GOLITE_MAIN_FUNCTION = "goliteMain";
}
