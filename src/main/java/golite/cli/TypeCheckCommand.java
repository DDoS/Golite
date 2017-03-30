package golite.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import golite.Golite;
import golite.node.Start;
import golite.semantic.SemanticData;
import golite.semantic.check.TypeChecker;
import golite.semantic.check.TypeCheckerException;
import golite.syntax.print.PrinterException;
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
    public void addCommandLineOptions(Options options) {
        options
                .addOption(PRINT_TYPES_OPTION, "Add type annotations to the pretty-printed output")
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
            } catch (PrinterException | IOException exception) {
                throw new CommandException("Error when printing: " + exception.getMessage());
            }
        }
        // Finally print the type-annotated program if necessary
        if (commandLine.hasOption(PRINT_TYPES_OPTION)) {
            try (Writer writer = new BufferedWriter(new FileWriter(typeOutput))) {
                Golite.prettyPrint(parse.getAst(), semantics, writer);
            } catch (PrinterException | IOException exception) {
                throw new CommandException("Error when printing: " + exception.getMessage());
            }
        }
    }
}
