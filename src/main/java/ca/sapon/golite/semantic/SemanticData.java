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
    private final Map<PExpr, Type> exprNodeTypes;
    private final Map<Node, Context> nodeContexts;
    private boolean printTypes = false;
    private boolean printContexts = false;
    private boolean printAllContexts = false;

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

    public boolean printTypes() {
        return printTypes;
    }

    public boolean printContexts() {
        return printContexts;
    }

    public boolean printAllContexts() {
        return printAllContexts;
    }

    public void printTypes(boolean printTypes) {
        this.printTypes = printTypes;
    }

    public void printContexts(boolean printContexts) {
        this.printContexts = printContexts;
    }

    public void printAllContexts(boolean printAllContexts) {
        this.printAllContexts = printAllContexts;
        if (printAllContexts) {
            printContexts(true);
        }
    }

    public static SemanticData EMPTY = new SemanticData(Collections.emptyMap(), Collections.emptyMap()) {
        @Override
        public void printTypes(boolean printTypes) {
            throw new IllegalArgumentException("Can't modify the empty semantic data singleton");
        }

        @Override
        public void printContexts(boolean printContexts) {
            throw new IllegalArgumentException("Can't modify the empty semantic data singleton");
        }

        @Override
        public void printAllContexts(boolean printAllContexts) {
            throw new IllegalArgumentException("Can't modify the empty semantic data singleton");
        }
    };
}
