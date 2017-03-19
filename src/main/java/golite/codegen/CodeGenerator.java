package golite.codegen;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import golite.ir.node.FunctionDecl;
import golite.ir.node.IntLit;
import golite.ir.node.IrVisitor;
import golite.ir.node.PrintInt;
import golite.ir.node.Program;
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
    private LLVMModuleRef module;
    private final Deque<LLVMBuilderRef> builders = new ArrayDeque<>();
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
    public void visitFunctionDecl(FunctionDecl function) {
        // Create the function symbol
        final Function symbol = function.getSymbol();
        final LLVMTypeRef[] params = {};
        final LLVMTypeRef functionType = LLVMFunctionType(LLVMVoidType(), new PointerPointer<>(params), params.length, 0);
        final LLVMValueRef llvmFunction = LLVMAddFunction(module, symbol.getName(), functionType);
        LLVMSetFunctionCallConv(llvmFunction, LLVMCCallConv);
        // Create the builder for the function
        final LLVMBuilderRef builder = LLVMCreateBuilder();
        builders.push(builder);
        // Start the function body
        final LLVMBasicBlockRef entry = LLVMAppendBasicBlock(llvmFunction, "entry");
        LLVMPositionBuilderAtEnd(builder, entry);
        // Codegen the body
        function.getStatements().forEach(stmt -> stmt.visit(this));
        // Termination is handled implicitly by the last return statement
        // Dispose of the builder
        LLVMDisposeBuilder(builders.pop());
    }

    @Override
    public void visitVoidReturn(VoidReturn voidReturn) {
        LLVMBuildRetVoid(builders.peek());
    }

    @Override
    public void visitPrintInt(PrintInt printInt) {

    }

    @Override
    public void visitIntLit(IntLit intLit) {

    }
}
