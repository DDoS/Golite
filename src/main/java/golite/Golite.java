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
package golite;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;

import golite.lexer.Lexer;
import golite.lexer.LexerException;
import golite.node.Start;
import golite.parser.Parser;
import golite.parser.ParserException;
import golite.semantic.SemanticData;
import golite.semantic.SemanticException;
import golite.semantic.check.TypeChecker;
import golite.semantic.check.TypeCheckerException;
import golite.syntax.EnclosedExprRemover;
import golite.syntax.GoliteLexer;
import golite.syntax.SyntaxException;
import golite.syntax.print.PrettyPrinter;
import golite.syntax.weed.Weeder;
import golite.syntax.weed.WeederException;

/**
 * Methods for processing Golite code.
 */
public final class Golite {
    private Golite() throws Exception {
        throw new Exception("No");
    }

    public static Start parse(Reader source) throws IOException, SyntaxException {
        final Lexer lexer = new GoliteLexer(new PushbackReader(source, 4096));
        final Parser parser = new Parser(lexer);
        try {
            final Start ast = parser.parse();
            ast.apply(new Weeder());
            EnclosedExprRemover.removedEnclosed(ast);
            return ast;
        } catch (LexerException exception) {
            throw new SyntaxException(exception);
        } catch (ParserException exception) {
            throw new SyntaxException(exception);
        } catch (WeederException exception) {
            throw new SyntaxException(exception);
        }
    }

    public static void prettyPrint(Reader source, Writer pretty) throws IOException, SyntaxException {
        prettyPrint(parse(source), pretty);
    }

    public static void prettyPrint(Start ast, Writer pretty) {
        prettyPrint(ast, null, pretty);
    }

    public static void prettyPrint(Start ast, SemanticData semantics, Writer pretty) {
        ast.apply(new PrettyPrinter(semantics, pretty));
    }

    public static SemanticData typeCheck(Reader source) throws IOException, SyntaxException, SemanticException {
        return typeCheck(parse(source));
    }

    public static SemanticData typeCheck(Start ast) throws SemanticException {
        try {
            final TypeChecker typeChecker = new TypeChecker();
            ast.apply(typeChecker);
            return typeChecker.getGeneratedData();
        } catch (TypeCheckerException exception) {
            throw new SemanticException(exception);
        }
    }
}
