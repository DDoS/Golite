package golite.syntax;

import golite.analysis.DepthFirstAdapter;
import golite.node.AEnclosedExpr;
import golite.node.Start;

/**
 * Replaces expressions of the form {@code (expr)} with just {@code expr},
 * since the parenthesis are no longer necessary after weeding.
 */
public class EnclosedExprRemover extends DepthFirstAdapter {
    private static final EnclosedExprRemover INSTANCE = new EnclosedExprRemover();

    private EnclosedExprRemover() {
    }

    @Override
    public void outAEnclosedExpr(AEnclosedExpr node) {
        node.replaceBy(node.getExpr());
    }

    public static void removedEnclosed(Start ast) {
        ast.apply(INSTANCE);
    }
}
