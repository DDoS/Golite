package ca.sapon.golite;

import java.io.IOException;
import java.io.PushbackReader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import golite.lexer.Lexer;
import golite.lexer.LexerException;
import golite.node.TDblMinus;
import golite.node.TDblPlus;
import golite.node.TEndln;
import golite.node.TFloatLit;
import golite.node.THexLit;
import golite.node.TIdenf;
import golite.node.TIntLit;
import golite.node.TInterpretedStringLit;
import golite.node.TKwBreak;
import golite.node.TKwContinue;
import golite.node.TKwFallthrough;
import golite.node.TKwReturn;
import golite.node.TOctLit;
import golite.node.TRawStringLit;
import golite.node.TRhtBrace;
import golite.node.TRhtParen;
import golite.node.TRhtSqParen;
import golite.node.TRuneLit;
import golite.node.TSemi;
import golite.node.Token;

/**
 * A modified lexer that supports automatic semicolon insertion, according to the Go rules.
 * <p>Based off the example shown in class.</p>
 */
public class GoliteLexer extends Lexer {
    private Token previousToken = null;

    public GoliteLexer(PushbackReader in) {
        super(in);
    }

    @Override
    protected void filter() throws LexerException, IOException {
        if (previousToken != null && token.getClass() == TEndln.class && FINAL_LINE_TOKENS.contains(previousToken.getClass())) {
            token = new TSemi(token.getLine(), token.getPos());
        }
        previousToken = token;
    }

    private static final Set<Class<? extends Token>> FINAL_LINE_TOKENS = Stream.of(
            TIdenf.class, TIntLit.class, THexLit.class, TOctLit.class, TFloatLit.class,
            TInterpretedStringLit.class, TRawStringLit.class, TRuneLit.class,
            TKwBreak.class, TKwContinue.class, TKwFallthrough.class, TKwReturn.class,
            TDblPlus.class, TDblMinus.class, TRhtParen.class, TRhtSqParen.class, TRhtBrace.class
    ).collect(Collectors.toSet());
}
