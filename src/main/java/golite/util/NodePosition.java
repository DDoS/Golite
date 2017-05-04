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
package golite.util;

import golite.analysis.DepthFirstAdapter;
import golite.node.Node;
import golite.node.Token;

/**
 * Traverse the AST to extract source position information for the node.
 */
public class NodePosition extends DepthFirstAdapter implements SourcePositioned {
    private int startLine = Integer.MAX_VALUE;
    private int endLine = Integer.MIN_VALUE;
    private int startPos = Integer.MAX_VALUE;
    private int endPos = Integer.MIN_VALUE;

    public NodePosition(Node node) {
        node.apply(this);
    }

    @Override
    public int getStartLine() {
        return startLine;
    }

    @Override
    public int getEndLine() {
        return endLine;
    }

    @Override
    public int getStartPos() {
        return startPos;
    }

    @Override
    public int getEndPos() {
        return endPos;
    }

    @Override
    public void defaultCase(Node node) {
        if (!(node instanceof Token)) {
            return;
        }
        final Token token = (Token) node;
        final int line = token.getLine();
        final int pos = token.getPos();
        startLine = Math.min(startLine, line);
        endLine = Math.max(endLine, line);
        startPos = Math.min(startPos, pos);
        endPos = Math.max(endPos, pos);
    }
}
