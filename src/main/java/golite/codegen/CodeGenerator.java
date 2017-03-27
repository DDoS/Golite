package golite.codegen;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
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
    private final Deque<LLVMBuilderRef> builders = new ArrayDeque<>();
    private final Map<StructType, LLVMTypeRef> structs = new HashMap<>();
    private final Map<Function, LLVMValueRef> functions = new HashMap<>();
    private final Map<Expr<?>, LLVMValueRef> exprValues = new HashMap<>();
    private final Map<Expr<?>, LLVMValueRef> exprPtrs = new HashMap<>();
    private final Map<String, LLVMValueRef> stringConstants = new HashMap<>();
    private final Map<Variable<?>, LLVMValueRef> globalVariables = new HashMap<>();
    private final Map<Variable<?>, LLVMValueRef> functionVariables = new HashMap<>();
    private ByteBuffer machineCode;

    public ByteBuffer getMachineCode() {
        if (machineCode == null) {
            throw new IllegalStateException("The generator hasn't been applied yet");
        }
        return machineCode;
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
        // Apply some important optimization passes
        LLVMPassManagerRef pass = LLVMCreatePassManager();
        LLVMAddConstantPropagationPass(pass);
        LLVMAddInstructionCombiningPass(pass);
        LLVMAddPromoteMemoryToRegisterPass(pass);
        LLVMAddGVNPass(pass);
        LLVMAddCFGSimplificationPass(pass);
        LLVMRunPassManager(pass, module);
        // TODO: remove debug printing
        final BytePointer moduleString = LLVMPrintModuleToString(module);
        System.out.println(moduleString.getString());
        LLVMDisposeMessage(moduleString);
        // Generate the machine code: start by initializing the target machine for the current one
        LLVMInitializeNativeTarget();
        LLVMInitializeNativeAsmPrinter();
        // Then get the target triple string
        final BytePointer targetTriple = LLVMGetDefaultTargetTriple();
        final String targetTripleString = targetTriple.getString();
        LLVMDisposeMessage(targetTriple);
        // Now get the target from the triple string
        final PointerPointer<LLVMTargetRef> targetPtr = new PointerPointer<>(new LLVMTargetRef[1]);
        if (LLVMGetTargetFromTriple(targetTripleString, targetPtr, errorMsgPtr) != 0) {
            errorMsg = errorMsgPtr.getString();
            LLVMDisposeMessage(errorMsgPtr);
        }
        if (errorMsg != null) {
            throw new RuntimeException("Failed to get target machine: " + errorMsg);
        }
        final LLVMTargetRef target = targetPtr.get(LLVMTargetRef.class);
        // Now we can create a machine for that target
        final LLVMTargetMachineRef machine = LLVMCreateTargetMachine(target, targetTripleString, "generic", "",
                LLVMRelocDefault, LLVMCodeModelDefault, LLVMCodeGenLevelDefault);
        // It's recommended that we set the module data layout and target to the machine and triple
        LLVMSetModuleDataLayout(module, LLVMCreateTargetDataLayout(machine));
        LLVMSetTarget(module, targetTriple);
        // Finally we can actually emit the machine code (as an object file)
        final PointerPointer<LLVMMemoryBufferRef> memoryBufferPtr = new PointerPointer<>(new LLVMMemoryBufferRef[1]);
        if (LLVMTargetMachineEmitToMemoryBuffer(machine, module, LLVMObjectFile, errorMsgPtr, memoryBufferPtr) != 0) {
            errorMsg = errorMsgPtr.getString();
            LLVMDisposeMessage(errorMsgPtr);
            if (errorMsg != null) {
                throw new RuntimeException("Failed emit the machine code: " + errorMsg);
            }
        }
        // Now we just transfer that code to a Java byte buffer
        final LLVMMemoryBufferRef memoryBuffer = memoryBufferPtr.get(LLVMMemoryBufferRef.class);
        final BytePointer bufferStart = LLVMGetBufferStart(memoryBuffer);
        final long bufferSize = LLVMGetBufferSize(memoryBuffer);
        machineCode = bufferStart.limit(bufferSize).asByteBuffer();
        // Delete the machine and module now that we have the code
        LLVMDisposeTargetMachine(machine);
        LLVMDisposeModule(module);
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
        // Create the function symbol
        final Function symbol = functionDecl.getFunction();
        // The only external functions are the main and static initializer
        final boolean external = functionDecl.isMain() || functionDecl.isStaticInit();
        // Build the LLVM function type
        final FunctionType functionType = symbol.getType();
        final List<Parameter> params = functionType.getParameters();
        final LLVMTypeRef[] llvmParams = new LLVMTypeRef[params.size()];
        for (int i = 0; i < llvmParams.length; i++) {
            llvmParams[i] = createType(params.get(i).getType());
        }
        final LLVMTypeRef llvmReturn = functionType.getReturnType().map(this::createType).orElse(LLVMVoidType());
        // Prevent user defined functions from having the same name as the static initializer
        final String name;
        if (functionDecl.isStaticInit()) {
            name = STATIC_INIT_FUNCTION;
        } else {
            final String funcName = symbol.getName();
            name = funcName.equals(STATIC_INIT_FUNCTION) ? STATIC_INIT_FUNCTION + "1" : funcName;
        }
        // Create the LLVM function
        final LLVMValueRef function = createFunction(external, name, llvmReturn, llvmParams);
        functions.put(symbol, function);
        // Create the builder for the function
        final LLVMBuilderRef builder = LLVMCreateBuilder();
        builders.push(builder);
        // Start the function body
        final LLVMBasicBlockRef entry = LLVMAppendBasicBlock(function, "entry");
        LLVMPositionBuilderAtEnd(builder, entry);
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
        // Codegen the body
        functionDecl.getStatements().forEach(stmt -> stmt.visit(this));
        // Termination is handled implicitly by the last return statement
        // Dispose of the builder
        LLVMDisposeBuilder(builders.pop());
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
        LLVMBuildRetVoid(builders.peek());
    }

    @Override
    public void visitValueReturn(ValueReturn valueReturn) {
        valueReturn.getValue().visit(this);
        LLVMBuildRet(builders.peek(), getExprValue(valueReturn.getValue()));
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
            arg = LLVMBuildZExt(builders.peek(), arg, LLVMInt8Type(), "bool_to_char");
        }
        final LLVMValueRef[] args = {arg};
        LLVMBuildCall(builders.peek(), printFunction, new PointerPointer<>(args), 1, "");
    }

    @Override
    public void visitMemsetZero(MemsetZero memsetZero) {
        final Expr<?> value = memsetZero.getValue();
        value.visit(this);
        // Get the size of the of the memory to clear using an LLVM trick
        // (pointer to second element starting at null, then convert to int)
        final LLVMValueRef size = calculateSizeOfType(value.getType());
        // Call the memset intrinsic on a pointer to the value
        final LLVMBuilderRef builder = builders.peek();
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
        LLVMBuildStore(builders.peek(), value, ptr);
    }

    @Override
    public void visitJump(Jump jump) {

    }

    @Override
    public void visitLabel(Label label) {

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
        final LLVMValueRef[] stringData = {LLVMConstInt(LLVMInt32Type(), stringLit.getUtf8Data().limit(), 1), stringPtr};
        final LLVMValueRef stringStruct = LLVMConstNamedStruct(sliceRuntimeType, new PointerPointer<>(stringData), stringData.length);
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
        final LLVMBuilderRef builder = builders.peek();
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
        final LLVMValueRef value = LLVMBuildCall(builders.peek(), llvmFunction,
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
            value = LLVMBuildSIToFP(builders.peek(), argValue, LLVMDoubleType(), "int_to_float64");
        } else if (argType == BasicType.FLOAT64 && castType.isInteger()) {
            // Float to integer
            value = LLVMBuildFPToSI(builders.peek(), argValue, LLVMInt32Type(), "float64_to_int");
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
        final LLVMBuilderRef builder = builders.peek();
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
        // LLVMBuildNot()
    }

    @Override
    public void visitUnaArInt(UnaArInt unaArInt) {
        // LLVMBuildNeg()
        // LLVMBuildNot()
    }

    @Override
    public void visitUnaArFloat64(UnaArFloat64 unaArFloat64) {
        // LLVMBuildFNeg()
    }

    @Override
    public void visitBinArInt(BinArInt binArInt) {
        // LLVMBuildAdd()
        // LLVMBuildSub()
        // LLVMBuildMul()
        // LLVMBuildSDiv()
        // LLVMBuildSRem()
        // LLVMBuildOr()
        // LLVMBuildAnd()
        // LLVMBuildShl()
        // LLVMBuildAShr()
        // LLVMBuildAnd(, LLVMBuildNot())
        // LLVMBuildXor()
    }

    @Override
    public void visitConcatString(ConcatString concatString) {
        // Runtime call
    }

    @Override
    public void visitBinArFloat64(BinArFloat64 binArFloat64) {
        // LLVMBuildFAdd()
        // LLVMBuildFSub()
        // LLVMBuildFMul()
        // LLVMBuildFDiv()
    }

    @Override
    public void visitCmpBool(CmpBool cmpBool) {
        // LLVMBuildICmp()
    }

    @Override
    public void visitCmpInt(CmpInt cmpInt) {
        // LLVMBuildICmp()
    }

    @Override
    public void visitCmpFloat64(CmpFloat64 cmpFloat64) {
        // LLVMBuildFCmp()
        // Use ordered comparisons
    }

    @Override
    public void visitCmpString(CmpString cmpString) {
        // Runtime call
    }

    @Override
    public void visitLogicAnd(LogicAnd logicAnd) {

    }

    @Override
    public void visitLogicOr(LogicOr logicOr) {

    }

    private LLVMValueRef getExprValue(Expr<?> expr) {
        // This might be directly a value
        final LLVMValueRef value = exprValues.get(expr);
        if (value != null) {
            return value;
        }
        // Otherwise we need to load the pointer
        final LLVMValueRef ptr = exprPtrs.get(expr);
        return LLVMBuildLoad(builders.peek(), ptr, IrNode.toString(expr));
    }

    private LLVMValueRef getExprPtr(Expr<?> expr) {
        // This might already be a pointer value
        final LLVMValueRef ptr = exprPtrs.get(expr);
        if (ptr != null) {
            return ptr;
        }
        // Otherwise we need to store the value and return a pointer to the memory
        final LLVMValueRef value = exprValues.get(expr);
        final LLVMValueRef memory = LLVMBuildAlloca(builders.peek(), createType(expr.getType()), IrNode.toString(expr) + "Ptr");
        LLVMBuildStore(builders.peek(), value, memory);
        return memory;
    }

    private LLVMValueRef getAggregateMemberPtr(LLVMValueRef valuePtr, LLVMValueRef index, String memberName) {
        final LLVMValueRef[] indices = {LLVMConstInt(LLVMInt64Type(), 0, 0), index};
        return LLVMBuildInBoundsGEP(builders.peek(), valuePtr, new PointerPointer<>(indices), indices.length, memberName + "Ptr");
    }

    private LLVMValueRef allocateStackVariable(Variable<?> variable, String uniqueName) {
        final LLVMTypeRef type = createType(variable.getType());
        return LLVMBuildAlloca(builders.peek(), type, uniqueName);
    }

    private LLVMValueRef calculateSizeOfType(Type type) {
        // LLVM trick: get pointer to second element starting at null
        final LLVMBuilderRef builder = builders.peek();
        final LLVMValueRef nullPtr = LLVMConstPointerNull(LLVMPointerType(createType(type), 0));
        final LLVMValueRef[] secondIndex = {LLVMConstInt(LLVMInt64Type(), 1, 0)};
        final LLVMValueRef secondElementPtr = LLVMBuildGEP(builder, nullPtr, new PointerPointer<>(secondIndex), secondIndex.length,
                "secondElementPtr");
        // Then convert it to int, which will be the size of the first element of the array (also the size of the type)
        return LLVMBuildPtrToInt(builder, secondElementPtr, LLVMInt32Type(), "size");
    }

    private LLVMValueRef createFunction(boolean external, String name, LLVMTypeRef returnType, LLVMTypeRef... parameters) {
        final LLVMTypeRef functionType = LLVMFunctionType(returnType, new PointerPointer<>(parameters), parameters.length, 0);
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
        constant = LLVMAddGlobal(module, LLVMArrayType(LLVMInt8Type(), stringLength), "str_lit");
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
        LLVMStructSetBody(sliceRuntimeType, new PointerPointer<>(stringStructElements), stringStructElements.length, 0);
        // Intrinsics
        memsetFunction = createFunction(true, "llvm.memset.p0i8.i32", LLVMVoidType(), i8Pointer, LLVMInt8Type(),
                LLVMInt32Type(), LLVMInt32Type(), LLVMInt1Type());
        // Runtime functions
        printBoolFunction = createFunction(true, RUNTIME_PRINT_BOOL, LLVMVoidType(), LLVMInt8Type());
        printIntFunction = createFunction(true, RUNTIME_PRINT_INT, LLVMVoidType(), LLVMInt32Type());
        printRuneFunction = createFunction(true, RUNTIME_PRINT_RUNE, LLVMVoidType(), LLVMInt32Type());
        printFloat64Function = createFunction(true, RUNTIME_PRINT_FLOAT64, LLVMVoidType(), LLVMDoubleType());
        printStringFunction = createFunction(true, RUNTIME_PRINT_STRING, LLVMVoidType(), sliceRuntimeType);
        checkBoundsFunction = createFunction(true, RUNTIME_CHECK_BOUNDS, LLVMVoidType(), LLVMInt32Type(), LLVMInt32Type());
        sliceAppendFunction = createFunction(true, RUNTIME_SLICE_APPEND, sliceRuntimeType,
                sliceRuntimeType, i8Pointer, LLVMInt32Type());
    }

    private static final String RUNTIME_SLICE = "goliteRtSlice";
    private static final String RUNTIME_PRINT_BOOL = "goliteRtPrintBool";
    private static final String RUNTIME_PRINT_INT = "goliteRtPrintInt";
    private static final String RUNTIME_PRINT_RUNE = "goliteRtPrintRune";
    private static final String RUNTIME_PRINT_FLOAT64 = "goliteRtPrintFloat64";
    private static final String RUNTIME_PRINT_STRING = "goliteRtPrintString";
    private static final String RUNTIME_CHECK_BOUNDS = "goliteRtCheckBounds";
    private static final String RUNTIME_SLICE_APPEND = "goliteRtSliceAppend";
    private static final String STATIC_INIT_FUNCTION = "staticInit";
}
