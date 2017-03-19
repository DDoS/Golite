package golite.codegen;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import golite.ir.node.Assignment;
import golite.ir.node.BoolLit;
import golite.ir.node.Expr;
import golite.ir.node.FloatLit;
import golite.ir.node.FunctionDecl;
import golite.ir.node.Identifier;
import golite.ir.node.IntLit;
import golite.ir.node.IrVisitor;
import golite.ir.node.PrintBool;
import golite.ir.node.PrintInt;
import golite.ir.node.PrintString;
import golite.ir.node.Program;
import golite.ir.node.StringLit;
import golite.ir.node.VariableDecl;
import golite.ir.node.VoidReturn;
import golite.semantic.symbol.Function;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;

import static org.bytedeco.javacpp.LLVM.*;

/**
 *
 */
public class CodeGenerator implements IrVisitor {
    public static final String RUNTIME_PRINT_INT = "__golite_runtime_printInt";
    public static final String RUNTIME_PRINT_STRING = "__golite_runtime_printString";
    public static final String MAIN_FUNCTION = "main";
    private LLVMModuleRef module;
    private final Deque<LLVMBuilderRef> builders = new ArrayDeque<>();
    private final Map<Expr, LLVMValueRef> exprValues = new HashMap<>();
    private final Map<String, LLVMValueRef> stringConstants = new HashMap<>();
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
        module = LLVMModuleCreateWithName("golite_" + program.getPackageName());
        // Declare the external function from the C stdlib and the Golite runtime
        declareExternalFunctions();
        // Codegen it
        program.getFunctions().forEach(function -> function.visit(this));
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
        // TODO: remove this debug printing
        System.out.println(LLVMPrintModuleToString(module).getString());
    }

    @Override
    public void visitFunctionDecl(FunctionDecl functionDecl) {
        // Create the function symbol
        final Function symbol = functionDecl.getSymbol();
        // The only external function is the main
        final boolean external = symbol.getName().equals(MAIN_FUNCTION);
        final LLVMTypeRef[] params = {};
        final LLVMValueRef function = declareFunction(external, symbol.getName(), LLVMVoidType(), params);
        // Create the builder for the function
        final LLVMBuilderRef builder = LLVMCreateBuilder();
        builders.push(builder);
        // Start the function body
        final LLVMBasicBlockRef entry = LLVMAppendBasicBlock(function, "entry");
        LLVMPositionBuilderAtEnd(builder, entry);
        // Codegen the body
        functionDecl.getStatements().forEach(stmt -> stmt.visit(this));
        // Termination is handled implicitly by the last return statement
        // Dispose of the builder
        LLVMDisposeBuilder(builders.pop());
    }

    @Override
    public void visitVariableDecl(VariableDecl function) {

    }

    @Override
    public void visitVoidReturn(VoidReturn voidReturn) {
        LLVMBuildRetVoid(builders.peek());
    }

    @Override
    public void visitPrintBool(PrintBool printBool) {

    }

    @Override
    public void visitPrintInt(PrintInt printInt) {
        printInt.getValue().visit(this);
        final LLVMValueRef[] args = {exprValues.get(printInt.getValue())};
        final LLVMValueRef printIntFunction = LLVMGetNamedFunction(module, RUNTIME_PRINT_INT);
        LLVMBuildCall(builders.peek(), printIntFunction, new PointerPointer<>(args), 1, "");
    }

    @Override
    public void visitIntLit(IntLit intLit) {
        exprValues.put(intLit, LLVMConstInt(LLVMInt32Type(), intLit.getValue(), 0));
    }

    @Override
    public void visitFloatLit(FloatLit floatLit) {

    }

    @Override
    public void visitPrintString(PrintString printString) {
        printString.getValue().visit(this);
        final LLVMValueRef[] args = {exprValues.get(printString.getValue())};
        final LLVMValueRef printStringFunction = LLVMGetNamedFunction(module, RUNTIME_PRINT_STRING);
        LLVMBuildCall(builders.peek(), printStringFunction, new PointerPointer<>(args), 1, "");

    }

    @Override
    public void visitAssignment(Assignment assignment) {

    }

    @Override
    public void visitBoolLit(BoolLit boolLit) {

    }

    @Override
    public void visitStringLit(StringLit stringLit) {
        final LLVMValueRef value = declareStringConstant(stringLit.getValue());
        // Index the string at zero, then get a pointer at that index + 0 (weird, I know)
        final LLVMValueRef[] indices = {LLVMConstInt(LLVMInt32Type(), 0, 0), LLVMConstInt(LLVMInt32Type(), 0, 0)};
        final LLVMValueRef stringPtr = LLVMBuildInBoundsGEP(builders.peek(), value, new PointerPointer<>(indices), 2, "");
        exprValues.put(stringLit, stringPtr);
    }

    @Override
    public void visitIdentifier(Identifier identifier) {

    }

    private void declareExternalFunctions() {
        // Runtime print functions
        declareFunction(true, RUNTIME_PRINT_INT, LLVMVoidType(), LLVMInt32Type());
        declareFunction(true, RUNTIME_PRINT_STRING, LLVMVoidType(), LLVMPointerType(LLVMInt8Type(), 0));
    }

    private LLVMValueRef declareFunction(boolean external, String name, LLVMTypeRef returnType, LLVMTypeRef... parameters) {
        final LLVMTypeRef functionType = LLVMFunctionType(returnType, new PointerPointer<>(parameters), parameters.length, 0);
        final LLVMValueRef function = LLVMAddFunction(module, name, functionType);
        LLVMSetFunctionCallConv(function, LLVMCCallConv);
        LLVMSetLinkage(function, external ? LLVMExternalLinkage : LLVMPrivateLinkage);
        return function;
    }

    private LLVMValueRef declareStringConstant(String string) {
        // Don't declare it if it already exists
        LLVMValueRef constant = stringConstants.get(string);
        if (constant != null) {
            return constant;
        }
        // Otherwise create it and add it to the pool
        final BytePointer data;
        try {
            data = new BytePointer(string, "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            throw new RuntimeException(exception);
        }
        final int originalLength = (int) data.limit();
        final int nullTerminatedLength = originalLength + 1;
        constant = LLVMAddGlobal(module, LLVMArrayType(LLVMInt8Type(), nullTerminatedLength), "str_lit");
        LLVMSetLinkage(constant, LLVMInternalLinkage);
        LLVMSetGlobalConstant(constant, 1);
        LLVMSetInitializer(constant, LLVMConstString(data, originalLength, 0));
        stringConstants.put(string, constant);
        return constant;
    }
}
