package ca.sapon.golite.util;

/**
 *
 */
public interface SourcePositioned {
    int getStartLine();

    int getEndLine();

    int getStartPos();

    int getEndPos();

    static String appendPosition(SourcePositioned position, String message) {
        final StringBuilder detailed = new StringBuilder()
                .append('[').append(position.getStartLine()).append(',').append(position.getStartPos()).append(']');
        if (position.getStartLine() != position.getEndLine() || position.getStartPos() != position.getEndPos()) {
            detailed.append(" to ").append('[').append(position.getEndLine()).append(',').append(position.getEndPos()).append(']');
        }
        return detailed.append(": ").append(message).toString();
    }
}
