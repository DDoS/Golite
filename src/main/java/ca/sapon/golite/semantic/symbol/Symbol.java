package ca.sapon.golite.semantic.symbol;

import ca.sapon.golite.util.SourcePositioned;

/**
 *
 */
public abstract class Symbol implements SourcePositioned {
    private final int startLine, endLine;
    private final int startPos, endPos;
    private final String name;

    protected Symbol(String name) {
        this(0, 0, 0, 0, name);
    }

    public Symbol(int startLine, int endLine, int startPos, int endPos, String name) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.startPos = startPos;
        this.endPos = endPos;
        this.name = name;
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

    public String getName() {
        return name;
    }
}