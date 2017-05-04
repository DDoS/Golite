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
package golite.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import golite.codegen.CodeGenerator;
import golite.codegen.ProgramCode;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 *
 */
public class CodeGenerateCommand extends Command {
    private static final String NO_OPTIMIZATION_OPTION = "nopt";
    private static final String OUTPUT_BIT_CODE_OPTION = "bc";
    private final IrGenerateCommand irGenerate = new IrGenerateCommand();
    private ProgramCode code;
    private File textOutput;
    private File bitCodeOutput;

    public CodeGenerateCommand() {
        super("codegen");
        setParent(irGenerate);
        irGenerate.setOutputEnabled(false);
    }

    public ProgramCode getCode() {
        return code;
    }

    @Override
    public String getHelp() {
        return "Generates textual LLVM IR";
    }

    @Override
    public void addCommandLineOptions(Options options) {
        if (isOutputEnabled()) {
            options.addOption(OUTPUT_BIT_CODE_OPTION, "Output as LLVM bit code instead of text");
        }
        options.addOption(NO_OPTIMIZATION_OPTION, "Disable LLVM optimization passes");
    }

    @CommandOutput(extension = "ll")
    public void setOutputText(File output) {
        this.textOutput = output;
    }

    @CommandOutput(extension = "bc")
    public void setOutputBitCode(File output) {
        this.bitCodeOutput = output;
    }

    @Override
    public void execute(CommandLine commandLine) {
        final CodeGenerator codeGenerator = new CodeGenerator();
        try {
            irGenerate.getProgram().visit(codeGenerator);
        } catch (Exception exception) {
            throw new CommandException("Error when generating code", exception);
        }
        code = codeGenerator.getCode();
        if (!commandLine.hasOption(NO_OPTIMIZATION_OPTION)) {
            code.optimize();
        }
    }

    @Override
    public void output(CommandLine commandLine) {
        if (commandLine.hasOption(OUTPUT_BIT_CODE_OPTION)) {
            try (FileChannel channel = FileChannel.open(bitCodeOutput.toPath(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                channel.write(code.asBitCode());
            } catch (Exception exception) {
                throw new CommandException("Error when writing bit code", exception);
            }
        } else {
            try (Writer writer = new BufferedWriter(new FileWriter(textOutput))) {
                writer.write(code.asText());
            } catch (Exception exception) {
                throw new CommandException("Error when writing text code", exception);
            }
        }
    }
}
