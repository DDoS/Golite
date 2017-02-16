package ca.sapon.golite;

import golite.lexer.Lexer;
import golite.node.Start;
import golite.parser.Parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PushbackReader;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new RuntimeException("Expected the source file path as an argument");
        }
        final BufferedReader in = new BufferedReader(new FileReader(args[0]));
        final Lexer lexer = new Lexer(new PushbackReader(in));
        final Parser parser = new Parser(lexer);
        final Start ast = parser.parse();
        System.out.println(ast);
    }
}
