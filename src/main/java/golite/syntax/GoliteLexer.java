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
package golite.syntax;

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
