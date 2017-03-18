package golite.semantic.check;

import golite.util.NodePosition;
import golite.util.SourcePositioned;
import golite.node.Node;

/**
 * An exception throw by the type checker.
 */
public class TypeCheckerException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public TypeCheckerException(Node node, String message) {
        this(new NodePosition(node), message);
    }

    public TypeCheckerException(SourcePositioned source, String message) {
        super(SourcePositioned.appendPosition(source, message));
    }
}
