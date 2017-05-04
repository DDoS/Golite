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

import java.io.Reader;

import golite.Golite;
import golite.node.Start;
import golite.syntax.SyntaxException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 *
 */
public class ParseCommand extends Command {
    private Reader input;
    private Start ast;

    public ParseCommand() {
        super("parse");
    }

    public Start getAst() {
        return ast;
    }

    @Override
    public String getHelp() {
        return "Checks that the input is syntactically valid";
    }

    @Override
    public void addCommandLineOptions(Options options) {
    }

    @CommandInput
    public void setInput(Reader input) {
        this.input = input;
    }

    @Override
    public void execute(CommandLine commandLine) {
        try {
            ast = Golite.parse(input);
        } catch (SyntaxException exception) {
            throw new CommandException(exception.getMessage());
        } catch (Exception exception) {
            throw new CommandException("Error when parsing", exception);
        }
    }

    @Override
    public void output(CommandLine commandLine) {
    }
}
