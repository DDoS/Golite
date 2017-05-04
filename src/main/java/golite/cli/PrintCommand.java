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

import golite.Golite;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 *
 */
public class PrintCommand extends Command {
    private final ParseCommand parse = new ParseCommand();
    private File output;

    public PrintCommand() {
        super("print");
        setParent(parse);
        parse.setOutputEnabled(false);
    }

    @Override
    public String getHelp() {
        return "Pretty-prints the Golite program";
    }

    @Override
    public void addCommandLineOptions(Options options) {
    }

    @CommandOutput(extension = "pretty.go")
    public void setOutput(File output) {
        this.output = output;
    }

    @Override
    public void execute(CommandLine commandLine) {
    }

    @Override
    public void output(CommandLine commandLine) {
        try (Writer writer = new BufferedWriter(new FileWriter(output))) {
            Golite.prettyPrint(parse.getAst(), writer);
        } catch (Exception exception) {
            throw new CommandException("Error when printing", exception);
        }
    }
}
