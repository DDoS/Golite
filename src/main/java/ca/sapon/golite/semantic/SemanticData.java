package ca.sapon.golite.semantic;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import ca.sapon.golite.semantic.context.Context;
import ca.sapon.golite.semantic.type.Type;
import ca.sapon.golite.util.NodePosition;
import ca.sapon.golite.util.SourcePrinter;
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

    public void printContexts(SourcePrinter printer, boolean all) {
        // The contexts are serially ID'd, so we can sort them by ID to get the order in which they were entered
        final TreeMap<Context, Node> sortedContextNodes = new TreeMap<>(Comparator.comparingInt(Context::getID));
        sortedContextNodes.putAll(contextNodes);
        // Iterate the context, and print them with the node information
        for (Entry<Context, Node> contextNode : sortedContextNodes.entrySet()) {
            printer.print("########################################").newLine();
            if (all) {
                contextNode.getKey().printAll(printer);
            } else {
                contextNode.getKey().print(printer);
            }
            printer.print("@").print(new NodePosition(contextNode.getValue()).positionToString()).newLine();
        }
    }
}
