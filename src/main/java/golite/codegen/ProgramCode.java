package golite.codegen;

import java.nio.ByteBuffer;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;

import static org.bytedeco.javacpp.LLVM.*;

/**
 *
 */
public class ProgramCode {
    private final LLVMModuleRef program;
    private boolean optimized = false;
    private String textCode = null;
    private ByteBuffer bitCode = null;
    private LLVMTargetMachineRef nativeMachine = null;
    private ByteBuffer nativeCode = null;

    public ProgramCode(LLVMModuleRef program) {
        this.program = program;
    }

    public void optimize() {
        if (optimized) {
            return;
        }
        LLVMPassManagerRef passManager = LLVMCreatePassManager();
        LLVMAddConstantPropagationPass(passManager);
        LLVMAddInstructionCombiningPass(passManager);
        LLVMAddPromoteMemoryToRegisterPass(passManager);
        LLVMAddGVNPass(passManager);
        LLVMAddCFGSimplificationPass(passManager);
        LLVMRunPassManager(passManager, program);
        LLVMDisposePassManager(passManager);
        optimized = true;
    }

    public String asText() {
        if (textCode != null) {
            return textCode;
        }
        final BytePointer moduleString = LLVMPrintModuleToString(program);
        final String text = moduleString.getString();
        LLVMDisposeMessage(moduleString);
        return textCode = text;
    }

    public ByteBuffer asBitCode() {
        if (bitCode != null) {
            bitCode.rewind();
            return bitCode;
        }
        final LLVMMemoryBufferRef memoryBuffer = LLVMWriteBitcodeToMemoryBuffer(program);
        final BytePointer bufferStart = LLVMGetBufferStart(memoryBuffer);
        final long bufferSize = LLVMGetBufferSize(memoryBuffer);
        final ByteBuffer code = bufferStart.limit(bufferSize).asByteBuffer();
        return bitCode = code;
    }

    public ByteBuffer asNativeCode() {
        if (nativeCode != null) {
            nativeCode.rewind();
            return nativeCode;
        }
        // Start by initializing the current machine as the target
        initializeNativeMachine();
        // Then we can emit the code
        final PointerPointer<LLVMMemoryBufferRef> memoryBufferPtr = new PointerPointer<>(new LLVMMemoryBufferRef[1]);
        final BytePointer errorMsgPtr = new BytePointer((Pointer) null);
        if (LLVMTargetMachineEmitToMemoryBuffer(nativeMachine, program, LLVMObjectFile, errorMsgPtr, memoryBufferPtr) != 0) {
            final String errorMsg = errorMsgPtr.getString();
            LLVMDisposeMessage(errorMsgPtr);
            if (errorMsg != null) {
                throw new RuntimeException("Failed emit the machine code: " + errorMsg);
            }
        }
        // Finally we just need to read the LLVM buffer to a Java one
        final LLVMMemoryBufferRef memoryBuffer = memoryBufferPtr.get(LLVMMemoryBufferRef.class);
        final BytePointer bufferStart = LLVMGetBufferStart(memoryBuffer);
        final long bufferSize = LLVMGetBufferSize(memoryBuffer);
        final ByteBuffer code = bufferStart.limit(bufferSize).asByteBuffer();
        return nativeCode = code;
    }

    private void initializeNativeMachine() {
        if (nativeMachine != null) {
            return;
        }
        // Initialize the target machine for the current one
        LLVMInitializeNativeTarget();
        LLVMInitializeNativeAsmPrinter();
        // Then get the target triple string
        final BytePointer targetTriple = LLVMGetDefaultTargetTriple();
        final String targetTripleString = targetTriple.getString();
        LLVMDisposeMessage(targetTriple);
        // Now get the target from the triple string
        final PointerPointer<LLVMTargetRef> targetPtr = new PointerPointer<>(new LLVMTargetRef[1]);
        final BytePointer errorMsgPtr = new BytePointer((Pointer) null);
        String errorMsg = null;
        if (LLVMGetTargetFromTriple(targetTripleString, targetPtr, errorMsgPtr) != 0) {
            errorMsg = errorMsgPtr.getString();
            LLVMDisposeMessage(errorMsgPtr);
        }
        if (errorMsg != null) {
            throw new RuntimeException("Failed to get target machine: " + errorMsg);
        }
        final LLVMTargetRef target = targetPtr.get(LLVMTargetRef.class);
        // Now we can create a machine for that target
        nativeMachine = LLVMCreateTargetMachine(target, targetTripleString, "generic", "",
                LLVMRelocDefault, LLVMCodeModelDefault, LLVMCodeGenLevelDefault);
        // It's recommended that we set the module data layout and target to the machine and triple
        LLVMSetModuleDataLayout(program, LLVMCreateTargetDataLayout(nativeMachine));
        LLVMSetTarget(program, targetTriple);
    }

    private void dispose() {
        if (nativeMachine != null) {
            LLVMDisposeTargetMachine(nativeMachine);
        }
        LLVMDisposeModule(program);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        dispose();
    }
}
