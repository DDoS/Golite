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
                .append('[').append(posToString(position.getStartLine()))
                .append(',').append(posToString(position.getStartPos())).append(']');
        if (position.getStartLine() != position.getEndLine() || position.getStartPos() != position.getEndPos()) {
            detailed.append(" to ").append('[').append(posToString(position.getEndLine()))
                    .append(',').append(posToString(position.getEndPos())).append(']');
        }
        return detailed.append(": ").append(message).toString();
    }

    static String posToString(int pos) {
        if (pos <= 0) {
            return "?";
        }
        return Integer.toString(pos);
    }
}
