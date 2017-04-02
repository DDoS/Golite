package golite;

import java.io.File;

import golite.cli.Cli;
import golite.cli.CompileCommand;
import golite.codegen.ProgramCode;

/**
 *
 */
public class Compile {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Expected one file");
            System.exit(1);
        }
        final File inputFile = new File(args[0]);
        final ProgramCode code = new ProgramCode(inputFile);
        final File outputFile = Cli.setFileExtension(inputFile, "out");
        CompileCommand.compileAndLink(code.asNativeCode(), CompileCommand.DEFAULT_RUNTIME_PATH, outputFile);
    }
}
