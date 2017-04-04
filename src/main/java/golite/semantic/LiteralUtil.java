package golite.semantic;

import golite.node.AIntDecExpr;
import golite.node.AIntHexExpr;
import golite.node.AIntOctExpr;
import golite.node.PExpr;

/**
 *
 */
public class LiteralUtil {
    public static int parseInt(PExpr intLit) {
        final String text;
        if (intLit instanceof AIntDecExpr) {
            text = ((AIntDecExpr) intLit).getIntLit().getText();
        } else if (intLit instanceof AIntOctExpr) {
            text = ((AIntOctExpr) intLit).getOctLit().getText();
        } else if (intLit instanceof AIntHexExpr) {
            text = ((AIntHexExpr) intLit).getHexLit().getText();
        } else {
            throw new IllegalArgumentException(intLit.getClass() + " is not an int literal node");
        }
        return Long.decode(text).intValue();
    }
}
