package ca.sapon.golite;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PushbackReader;

import tiny.lexer.Lexer;
import tiny.parser.Parser;
import tiny.node.Start;

public class Main {
    public static void main(String[] args) throws Exception {
        final BufferedReader in = new BufferedReader(new FileReader("/Users/aleksi/Desktop/test_valid_2.tiny"));
        final Lexer lexer = new Lexer(new PushbackReader(in));
        final Parser parser = new Parser(lexer);
        final Start ast = parser.parse();
        System.out.println(ast);
    }
}
