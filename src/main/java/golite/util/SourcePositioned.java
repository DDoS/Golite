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

/**
 * A object that has source position information attached to it.
 */
public interface SourcePositioned {
    int getStartLine();

    int getEndLine();

    int getStartPos();

    int getEndPos();

    default String positionToString() {
        final StringBuilder positionString = new StringBuilder()
                .append('[').append(posToString(getStartLine()))
                .append(',').append(posToString(getStartPos())).append(']');
        if (getStartLine() != getEndLine() || getStartPos() != getEndPos()) {
            positionString.append(" to ").append('[').append(posToString(getEndLine()))
                    .append(',').append(posToString(getEndPos())).append(']');
        }
        return positionString.toString();
    }

    static String appendPosition(SourcePositioned position, String message) {
        return position.positionToString() + ": " + message;
    }

    static String posToString(int pos) {
        if (pos <= 0) {
            return "?";
        }
        return Integer.toString(pos);
    }
}
