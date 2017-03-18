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
