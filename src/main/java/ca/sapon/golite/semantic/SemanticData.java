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
    private final Map<Context, Node> contextNodes;

    public SemanticData(Map<PExpr, Type> exprNodeTypes, Map<Context, Node> contextNodes) {
        this.exprNodeTypes = exprNodeTypes;
        this.contextNodes = contextNodes;
    }

    public Optional<Type> getExprType(PExpr expr) {
        return Optional.ofNullable(exprNodeTypes.get(expr));
    }
}
