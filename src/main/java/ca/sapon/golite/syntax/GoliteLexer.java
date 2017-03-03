package ca.sapon.golite.syntax;

import java.io.IOException;
import java.io.PushbackReader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import golite.lexer.Lexer;
import golite.lexer.LexerException;
import golite.node.EOF;
import golite.node.TCommentMultiln;
import golite.node.TCommentSingleln;
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
import golite.node.TSpace;
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
        final Class<? extends Token> tokenClass = token.getClass();
        // Ignore spaces before a new line
        if (tokenClass == TSpace.class) {
            return;
        }
        // Ignore a multi-line comment if it doesn't have a new line, in which case it's just like a really long space
        if (tokenClass == TCommentMultiln.class && !hasNewLine(token.getText())) {
            return;
        }
        // If the token ends a line and the previous token matches certain rules, replace it by a semicolon
        if (previousToken != null && END_LINE_TOKENS.contains(tokenClass)
                && FINAL_LINE_TOKENS.contains(previousToken.getClass())) {
            token = new TSemi(token.getLine(), token.getPos());
        }
        // Either way, set the current token as the previous one
        previousToken = token;
    }

    private static boolean hasNewLine(String string) {
        return string.indexOf('\n') >= 0 || string.indexOf('\r') >= 0;
    }

    private static final Set<Class<? extends Token>> END_LINE_TOKENS = Stream.of(
            TEndln.class, TCommentSingleln.class, TCommentMultiln.class, EOF.class
    ).collect(Collectors.toSet());
    private static final Set<Class<? extends Token>> FINAL_LINE_TOKENS = Stream.of(
            TIdenf.class, TIntLit.class, THexLit.class, TOctLit.class, TFloatLit.class,
            TInterpretedStringLit.class, TRawStringLit.class, TRuneLit.class,
            TKwBreak.class, TKwContinue.class, TKwFallthrough.class, TKwReturn.class,
            TDblPlus.class, TDblMinus.class, TRhtParen.class, TRhtSqParen.class, TRhtBrace.class
    ).collect(Collectors.toSet());
}
