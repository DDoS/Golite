package ca.sapon.golite.semantic;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import ca.sapon.golite.semantic.context.Context;
import ca.sapon.golite.semantic.type.Type;
import golite.node.Node;
import golite.node.PExpr;

/**
 *
 */
public class SemanticData {
    public static SemanticData EMPTY = new SemanticData(Collections.emptyMap(), Collections.emptyMap());
    private final Map<PExpr, Type> exprNodeTypes;
    private final Map<Node, Context> nodeContexts;

    public SemanticData(Map<PExpr, Type> exprNodeTypes, Map<Node, Context> nodeContexts) {
        this.exprNodeTypes = exprNodeTypes;
        this.nodeContexts = nodeContexts;
    }

    public Optional<Type> getExprType(PExpr expr) {
        return Optional.ofNullable(exprNodeTypes.get(expr));
    }

    public Optional<Context> getNodeContext(Node node) {
        Context context;
        do {
            context = nodeContexts.get(node);
            node = node.parent();
        } while (context == null && node != null);
        return Optional.ofNullable(context);
    }
}
