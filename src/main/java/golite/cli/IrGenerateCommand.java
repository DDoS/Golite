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

import golite.ir.IrConverter;
import golite.ir.node.Program;
import golite.util.SourcePrinter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 *
 */
public class IrGenerateCommand extends Command {
    private final TypeCheckCommand typeCheck = new TypeCheckCommand();
    private Program program;
    private File output;

    public IrGenerateCommand() {
        super("irgen");
        setParent(typeCheck);
        typeCheck.setOutputEnabled(true);
    }

    public Program getProgram() {
        return program;
    }

    @Override
    public String getHelp() {
        return "Generates a custom IR used for code generation";
    }

    @Override
    public void addCommandLineOptions(Options options) {
    }

    @CommandOutput(extension = "ir")
    public void setOutput(File output) {
        this.output = output;
    }

    @Override
    public void execute(CommandLine commandLine) {
        final IrConverter irConverter = new IrConverter(typeCheck.getSemantics());
        try {
            typeCheck.getAst().apply(irConverter);
        } catch (Exception exception) {
            throw new CommandException("Error when generating IR", exception);
        }
        program = irConverter.getProgram();
    }

    @Override
    public void output(CommandLine commandLine) {
        try (Writer writer = new BufferedWriter(new FileWriter(output))) {
            program.print(new SourcePrinter(writer));
        } catch (Exception exception) {
            throw new CommandException("Error when printing IR", exception);
        }
    }
}
