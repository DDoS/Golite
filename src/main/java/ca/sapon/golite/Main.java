package ca.sapon.golite;

import java.io.BufferedReader;
import java.io.FileReader;

public final class Main {
    private Main() throws Exception {
        throw new Exception("No");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new RuntimeException("Expected the source file path as an argument");
        }
        final BufferedReader source = new BufferedReader(new FileReader(args[0]));
        System.out.println(Golite.prettyPrint(source));
    }
}
