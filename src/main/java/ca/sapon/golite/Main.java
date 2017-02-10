package ca.sapon.golite;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PushbackReader;

import tiny.lexer.Lexer;
import tiny.parser.Parser;
import tiny.node.Start;

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
