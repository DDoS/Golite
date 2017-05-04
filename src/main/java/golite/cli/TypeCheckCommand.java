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
import golite.node.Start;
import golite.semantic.SemanticData;
import golite.semantic.check.TypeChecker;
import golite.semantic.check.TypeCheckerException;
import golite.util.SourcePrinter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 *
 */
public class TypeCheckCommand extends Command {
    private static final String PRINT_TYPES_OPTION = "pptype";
    private static final String DUMP_SYMBOL_TABLE_OPTION = "dumpsymtab";
    private static final String DUMP_ALL_SYMBOL_TABLES_OPTION = "dumpsymtaball";
    private final ParseCommand parse = new ParseCommand();
    private SemanticData semantics;
    private File typeOutput;
    private File symbolOutput;

    public TypeCheckCommand() {
        super("typecheck");
        setParent(parse);
        parse.setOutputEnabled(false);
    }

    public Start getAst() {
        return parse.getAst();
    }

    public SemanticData getSemantics() {
        return semantics;
    }

    @Override
    public String getHelp() {
        return "Checks that the input is semantically valid";
    }

    @Override
    public void addCommandLineOptions(Options options) {
        options
                .addOption(PRINT_TYPES_OPTION, "Pretty-print the program with type annotations")
                .addOption(DUMP_SYMBOL_TABLE_OPTION, "Write the top of the symbol table at the end of each scope")
                .addOption(DUMP_ALL_SYMBOL_TABLES_OPTION, "Write the full symbol table at the end of each scope");
    }

    @CommandOutput(extension = "pptype.go")
    public void setTypeOutput(File output) {
        this.typeOutput = output;
    }

    @CommandOutput(extension = "symtab", auxiliary = true)
    public void setSymbolOutput(File output) {
        this.symbolOutput = output;
    }

    @Override
    public void execute(CommandLine commandLine) {
        final TypeChecker typeChecker = new TypeChecker();
        String typeError = null;
        try {
            parse.getAst().apply(typeChecker);
        } catch (TypeCheckerException exception) {
            typeError = exception.getMessage();
        }
        semantics = typeChecker.getGeneratedData();
        // Throw the type-checker exception if it failed
        if (typeError != null) {
            throw new CommandException(typeError);
        }
    }

    @Override
    public void output(CommandLine commandLine) {
        // Print the contexts if enabled, even if type checking failed
        final boolean printAllContexts = commandLine.hasOption(DUMP_ALL_SYMBOL_TABLES_OPTION);
        if (printAllContexts || commandLine.hasOption(DUMP_SYMBOL_TABLE_OPTION)) {
            try (Writer writer = new BufferedWriter(new FileWriter(symbolOutput))) {
                semantics.printContexts(new SourcePrinter(writer), printAllContexts);
            } catch (Exception exception) {
                throw new CommandException("Error when dumping symbols", exception);
            }
        }
        // Finally print the type-annotated program if necessary
        if (commandLine.hasOption(PRINT_TYPES_OPTION)) {
            try (Writer writer = new BufferedWriter(new FileWriter(typeOutput))) {
                Golite.prettyPrint(parse.getAst(), semantics, writer);
            } catch (Exception exception) {
                throw new CommandException("Error when printing types", exception);
            }
        }
    }
}
