package golite.codegen;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import golite.ir.node.Assignment;
import golite.ir.node.BoolLit;
import golite.ir.node.Call;
import golite.ir.node.Cast;
import golite.ir.node.Expr;
import golite.ir.node.Float64Lit;
import golite.ir.node.FunctionDecl;
import golite.ir.node.Identifier;
import golite.ir.node.IntLit;
import golite.ir.node.IrNode;
import golite.ir.node.IrVisitor;
import golite.ir.node.MemsetZero;
import golite.ir.node.PrintBool;
import golite.ir.node.PrintFloat64;
import golite.ir.node.PrintInt;
import golite.ir.node.PrintRune;
import golite.ir.node.PrintString;
import golite.ir.node.Program;
import golite.ir.node.StringLit;
import golite.ir.node.ValueReturn;
import golite.ir.node.VariableDecl;
import golite.ir.node.VoidReturn;
import golite.semantic.symbol.Function;
import golite.semantic.symbol.Variable;
import golite.semantic.type.ArrayType;
import golite.semantic.type.BasicType;
import golite.semantic.type.FunctionType;
import golite.semantic.type.FunctionType.Parameter;
import golite.semantic.type.SliceType;
import golite.semantic.type.StructType;
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
    private LLVMTypeRef stringType;
    private LLVMValueRef printBoolFunction;
    private LLVMValueRef printIntFunction;
    private LLVMValueRef printRuneFunction;
    private LLVMValueRef printFloat64Function;
    private LLVMValueRef printStringFunction;
    private final Deque<LLVMBuilderRef> builders = new ArrayDeque<>();
    private final Map<Function, LLVMValueRef> functions = new HashMap<>();
    private final Map<Expr, LLVMValueRef> exprValues = new HashMap<>();
    private final Map<Expr, LLVMValueRef> exprPtrs = new HashMap<>();
    private final Map<String, LLVMValueRef> stringConstants = new HashMap<>();
    private final Map<Variable, LLVMValueRef> functionVariables = new HashMap<>();
    private ByteBuffer bitCode;

    public ByteBuffer getBitCode() {
        if (bitCode == null) {
            throw new IllegalStateException("The generator hasn't been applied yet");
        }
        return bitCode;
    }

    @Override
    public void visitProgram(Program program) {
        // Create the module
        module = LLVMModuleCreateWithName("golite." + program.getPackageName());
        // Declare the external function from the C stdlib and the Golite runtime
        declareExternalSymbols();
        // Codegen it
        program.getFunctions().forEach(function -> function.visit(this));
        // TODO: remove this debug printing
        System.out.println(LLVMPrintModuleToString(module).getString());
        // Validate it
        final BytePointer errorMessagePtr = new BytePointer((Pointer) null);
        final String errorMessage;
        if (LLVMVerifyModule(module, LLVMReturnStatusAction, errorMessagePtr) != 0) {
            errorMessage = errorMessagePtr.getString();
        } else {
            errorMessage = null;
        }
        LLVMDisposeMessage(errorMessagePtr);
        if (errorMessage != null) {
            throw new RuntimeException("Failed to verify module: " + errorMessage);
        }
        // Generate the bit code
        final LLVMMemoryBufferRef bufferRef = LLVMWriteBitcodeToMemoryBuffer(module);
        final BytePointer bufferStart = LLVMGetBufferStart(bufferRef);
        final long bufferSize = LLVMGetBufferSize(bufferRef);
        bitCode = bufferStart.limit(bufferSize).asByteBuffer();
        // Delete the module now that we have the code
        LLVMDisposeModule(module);
    }

    @Override
    public void visitFunctionDecl(FunctionDecl functionDecl) {
        // Create the function symbol
        final Function symbol = functionDecl.getFunction();
        // The only external function is the main
        final boolean external = symbol.getName().equals("main");
        // Build the LLVM function type
        final FunctionType functionType = symbol.getType();
        final List<Parameter> params = functionType.getParameters();
        final LLVMTypeRef[] llvmParams = new LLVMTypeRef[params.size()];
        for (int i = 0; i < llvmParams.length; i++) {
            llvmParams[i] = createType(params.get(i).getType());
        }
        final LLVMTypeRef llvmReturn = functionType.getReturnType().map(this::createType).orElse(LLVMVoidType());
        // Create the LLVM function
        final LLVMValueRef function = createFunction(external, symbol.getName(), llvmReturn, llvmParams);
        functions.put(symbol, function);
        // Create the builder for the function
        final LLVMBuilderRef builder = LLVMCreateBuilder();
        builders.push(builder);
        // Start the function body
        final LLVMBasicBlockRef entry = LLVMAppendBasicBlock(function, "entry");
        LLVMPositionBuilderAtEnd(builder, entry);
        // Place the variables values for the parameters on the stack
        final List<Variable> paramVariables = symbol.getParameters();
        final List<String> paramUniqueNames = functionDecl.getParamUniqueNames();
        for (int i = 0; i < paramVariables.size(); i++) {
            final Variable variable = paramVariables.get(i);
            final LLVMValueRef varPtr = placeVariableOnStack(variable, paramUniqueNames.get(i));
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
    public void visitVariableDecl(VariableDecl variableDecl) {
        final Variable variable = variableDecl.getVariable();
        final LLVMValueRef varPtr = placeVariableOnStack(variable, variableDecl.getUniqueName());
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

    private void generatePrintStmt(Expr value, LLVMValueRef printFunction) {
        value.visit(this);
        LLVMValueRef arg = getExprValue(value);
        if (value.getType() == BasicType.BOOL) {
            // Must zero-extent bools (i1) to char (i8)
            arg = LLVMBuildZExt(builders.peek(), arg, LLVMInt8Type(), "bool_to_char");
        }
        final LLVMValueRef[] args = {arg};
        LLVMBuildCall(builders.peek(), printFunction, new PointerPointer<>(args), 1, "");
    }

    @Override
    public void visitMemsetZero(MemsetZero memsetZero) {

    }

    @Override
    public void visitAssignment(Assignment assignment) {
        assignment.getLeft().visit(this);
        assignment.getRight().visit(this);
        final LLVMValueRef ptr = exprPtrs.get(assignment.getLeft());
        final LLVMValueRef value = getExprValue(assignment.getRight());
        LLVMBuildStore(builders.peek(), value, ptr);
    }

    @Override
    public void visitIntLit(IntLit intLit) {
        exprValues.put(intLit, LLVMConstInt(LLVMInt32Type(), intLit.getValue(), 0));
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
        final LLVMValueRef value = declareStringConstant(stringLit);
        // Get a pointer to the character array plus 0, then get a pointer to the first character plus 0
        final LLVMValueRef[] indices = {LLVMConstInt(LLVMInt32Type(), 0, 0), LLVMConstInt(LLVMInt32Type(), 0, 0)};
        final LLVMValueRef stringPtr = LLVMBuildInBoundsGEP(builders.peek(), value, new PointerPointer<>(indices), 2, "");
        // Create the string struct with the length and character array
        final LLVMValueRef[] stringData = {LLVMConstInt(LLVMInt32Type(), stringLit.getUtf8Data().limit(), 0), stringPtr};
        final LLVMValueRef stringStruct = LLVMConstNamedStruct(stringType, new PointerPointer<>(stringData), stringData.length);
        exprValues.put(stringLit, stringStruct);
    }

    @Override
    public void visitIdentifier(Identifier identifier) {
        // Only put a pointer to the variable, it will be loaded when necessary
        final LLVMValueRef varPtr = functionVariables.get(identifier.getVariable());
        exprPtrs.put(identifier, varPtr);
    }

    @Override
    public void visitCall(Call call) {
        final Function function = call.getFunction();
        final LLVMValueRef llvmFunction = functions.get(function);
        // Codegen the arguments
        final List<Expr> args = call.getArguments();
        args.forEach(arg -> arg.visit(this));
        final LLVMValueRef[] argValues = args.stream().map(this::getExprValue).toArray(LLVMValueRef[]::new);
        // Build the call
        final LLVMValueRef value = LLVMBuildCall(builders.peek(), llvmFunction, new PointerPointer<>(argValues), argValues.length,
                function.getName());
        exprValues.put(call, value);
    }

    @Override
    public void visitCast(Cast cast) {

    }

    private LLVMValueRef placeVariableOnStack(Variable variable, String uniqueName) {
        final LLVMTypeRef type = createType(variable.getType());
        return LLVMBuildAlloca(builders.peek(), type, uniqueName);
    }

    private LLVMValueRef getExprValue(Expr expr) {
        // This might be directly a value
        final LLVMValueRef value = exprValues.get(expr);
        if (value != null) {
            return value;
        }
        // Otherwise we need to load the pointer
        final LLVMValueRef ptr = exprPtrs.get(expr);
        return LLVMBuildLoad(builders.peek(), ptr, IrNode.toString(expr));
    }

    private LLVMValueRef createFunction(boolean external, String name, LLVMTypeRef returnType, LLVMTypeRef... parameters) {
        final LLVMTypeRef functionType = LLVMFunctionType(returnType, new PointerPointer<>(parameters), parameters.length, 0);
        final LLVMValueRef function = LLVMAddFunction(module, name, functionType);
        LLVMSetFunctionCallConv(function, LLVMCCallConv);
        LLVMSetLinkage(function, external ? LLVMExternalLinkage : LLVMPrivateLinkage);
        return function;
    }

    private LLVMTypeRef createType(Type type) {
        // TODO: cache these maybe?
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
            return stringType;
        }
        if (type instanceof ArrayType) {
            throw new UnsupportedOperationException("TODO");
        }
        if (type instanceof SliceType) {
            throw new UnsupportedOperationException("TODO");
        }
        if (type instanceof StructType) {
            final LLVMTypeRef[] fieldTypes = ((StructType) type).getFields().stream()
                    .map(field -> createType(field.getType()))
                    .toArray(LLVMTypeRef[]::new);
            return LLVMStructType(new PointerPointer<>(fieldTypes), fieldTypes.length, 0);
        }
        throw new IllegalArgumentException("Unknown type class: " + type);
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
        // Structure types
        stringType = LLVMStructCreateNamed(LLVMGetGlobalContext(), RUNTIME_STRING);
        final LLVMTypeRef[] stringStructElements = {LLVMInt32Type(), LLVMPointerType(LLVMInt8Type(), 0)};
        LLVMStructSetBody(stringType, new PointerPointer<>(stringStructElements), stringStructElements.length, 0);
        // Runtime print functions
        printBoolFunction = createFunction(true, RUNTIME_PRINT_BOOL, LLVMVoidType(), LLVMInt8Type());
        printIntFunction = createFunction(true, RUNTIME_PRINT_INT, LLVMVoidType(), LLVMInt32Type());
        printRuneFunction = createFunction(true, RUNTIME_PRINT_RUNE, LLVMVoidType(), LLVMInt32Type());
        printFloat64Function = createFunction(true, RUNTIME_PRINT_FLOAT64, LLVMVoidType(), LLVMDoubleType());
        printStringFunction = createFunction(true, RUNTIME_PRINT_STRING, LLVMVoidType(), stringType);
    }

    private static final String RUNTIME_STRING = "goliteRtString";
    private static final String RUNTIME_PRINT_BOOL = "goliteRtPrintBool";
    private static final String RUNTIME_PRINT_INT = "goliteRtPrintInt";
    private static final String RUNTIME_PRINT_RUNE = "goliteRtPrintRune";
    private static final String RUNTIME_PRINT_FLOAT64 = "goliteRtPrintFloat64";
    private static final String RUNTIME_PRINT_STRING = "goliteRtPrintString";
}
