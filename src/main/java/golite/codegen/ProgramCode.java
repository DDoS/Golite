/*
 * This file is part of GoLite, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2017 Aleksi Sapon, Rohit Verma, Ayesha Krishnamurthy <https://github.com/DDoS/Golite>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package golite.codegen;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.LLVM;
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

    public ProgramCode(File irFile) {
        // This is to ensure that the native libraries get loaded (sort of a bug fix)
        try {
            Class.forName(LLVM.class.getCanonicalName());
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException(exception);
        }
        // Read the contents of the file to a byte buffer
        final ByteBuffer buffer;
        try (FileChannel channel = FileChannel.open(irFile.toPath(), StandardOpenOption.READ)) {
            buffer = ByteBuffer.allocateDirect((int) channel.size());
            channel.read(buffer);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to read module file: " + exception.getMessage());
        }
        buffer.flip();
        // Convert this to an LLVM buffer via copying (to prevent issues with the garbage collector)
        final BytePointer range = new BytePointer(buffer);
        final LLVMMemoryBufferRef llvmBuffer =
                LLVMCreateMemoryBufferWithMemoryRangeCopy(range, range.limit(), new BytePointer("module"));
        // Now parse the the IR: check for "BC" in ASCII as the first two bytes for bitcode, otherwise it's text IR
        final PointerPointer<LLVMModuleRef> modulePtr = new PointerPointer<>(new LLVMModuleRef[1]);
        final BytePointer errorMsgPtr = new BytePointer((Pointer) null);
        final int success;
        buffer.rewind();
        if (buffer.get() == 0x42 && buffer.get() == 0x43) {
            success = LLVMParseBitcodeInContext(LLVMGetGlobalContext(), llvmBuffer, modulePtr, errorMsgPtr);
        } else {
            success = LLVMParseIRInContext(LLVMGetGlobalContext(), llvmBuffer, modulePtr, errorMsgPtr);
        }
        String errorMsg = null;
        if (success != 0) {
            errorMsg = errorMsgPtr.getString();
            LLVMDisposeMessage(errorMsgPtr);
        }
        if (errorMsg != null) {
            throw new RuntimeException("Failed to decode module code: " + errorMsg);
        }
        program = modulePtr.get(LLVMModuleRef.class);
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
