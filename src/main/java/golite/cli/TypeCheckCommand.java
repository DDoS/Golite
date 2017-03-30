package golite.cli;

import java.io.Writer;

import golite.Golite;
import golite.node.Start;
import golite.semantic.SemanticData;
import golite.semantic.check.TypeChecker;
import golite.semantic.check.TypeCheckerException;
import golite.syntax.print.PrinterException;
import golite.util.SourcePrinter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 *
 */
public class TypeCheckCommand extends Command {
    private final ParseCommand parse = new ParseCommand();
    private SemanticData semantics;
    private Writer typeOutput;
    private Writer symbolOutput;

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
                .addOption(new Option("pptype", "Add type annotations to the pretty-printed output"))
                .addOption(new Option("dumpsymtab", "Write the top of the symbol table at the end of each scope"))
                .addOption(new Option("dumpsymtaball", "Write the full symbol table at the end of each scope"));
    }

    @CommandOutput(extension = "pptype.go")
    public void setTypeOutput(Writer output) {
        this.typeOutput = output;
    }

    @CommandOutput(extension = "symtab", auxiliary = true)
    public void setSymbolOutput(Writer output) {
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
        final boolean printAllContexts = commandLine.hasOption("dumpsymtaball");
        if (printAllContexts || commandLine.hasOption("dumpsymtab")) {
            try {
                semantics.printContexts(new SourcePrinter(symbolOutput), printAllContexts);
            } catch (PrinterException exception) {
                throw new CommandException("Error when printing: " + exception.getMessage());
            }
        }
        // Finally print the type-annotated program if necessary
        if (commandLine.hasOption("pptype")) {
            try {
                Golite.prettyPrint(parse.getAst(), semantics, typeOutput);
            } catch (PrinterException exception) {
                throw new CommandException("Error when printing: " + exception.getMessage());
            }
        }
    }
}
