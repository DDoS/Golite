package ca.sapon.golite.semantic.check;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ca.sapon.golite.semantic.context.Context;
import ca.sapon.golite.semantic.context.TopLevelContext;
import ca.sapon.golite.semantic.symbol.NamedType;
import ca.sapon.golite.semantic.type.ArrayType;
import ca.sapon.golite.semantic.type.SliceType;
import ca.sapon.golite.semantic.type.StructType;
import ca.sapon.golite.semantic.type.StructType.Field;
import ca.sapon.golite.semantic.type.Type;
import golite.analysis.AnalysisAdapter;
import golite.node.AArrayType;
import golite.node.AIntDecExpr;
import golite.node.AIntHexExpr;
import golite.node.AIntOctExpr;
import golite.node.ANameType;
import golite.node.AProg;
import golite.node.ASliceType;
import golite.node.AStructField;
import golite.node.AStructType;
import golite.node.PExpr;
import golite.node.PStructField;
import golite.node.PType;

/**
 *
 */
public class TypeChecker extends AnalysisAdapter {
    private final Map<PExpr, Type> exprNodeTypes = new HashMap<>();
    private final Map<PType, Type> typeNodeTypes = new HashMap<>();
    private Context context;

    @Override
    public void caseAProg(AProg node) {
        context = new TopLevelContext();
    }

    @Override
    public void caseANameType(ANameType node) {
        final String name = node.getIdenf().getText();
        final Optional<NamedType> namedType = context.resolveType(name);
        if (!namedType.isPresent()) {
            throw new TypeCheckerException(node, "No type for name: " + name);
        }
        typeNodeTypes.put(node, namedType.get().getType());
    }

    @Override
    public void caseASliceType(ASliceType node) {
        node.getType().apply(this);
        final Type component = typeNodeTypes.get(node.getType());
        typeNodeTypes.put(node, new SliceType(component));
    }

    @Override
    public void caseAArrayType(AArrayType node) {
        final int length = parseInt(node.getExpr());
        node.getType().apply(this);
        final Type component = typeNodeTypes.get(node.getType());
        typeNodeTypes.put(node, new ArrayType(component, length));
    }

    @Override
    public void caseAStructType(AStructType node) {
        final List<Field> fields = new ArrayList<>();
        for (PStructField pField : node.getFields()) {
            final AStructField fieldNode = (AStructField) pField;
            fieldNode.getType().apply(this);
            final Type fieldType = typeNodeTypes.get(fieldNode.getType());
            fieldNode.getNames().forEach(idenf -> fields.add(new Field(idenf.getText(), fieldType)));
        }
        typeNodeTypes.put(node, new StructType(fields));
    }

    private static int parseInt(PExpr intLit) {
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
        // The literal should be valid integer thanks to the grammar, but could be too big
        try {
            return Integer.decode(text);
        } catch (NumberFormatException exception) {
            throw new TypeCheckerException(intLit, "Signed integer overflow");
        }
    }
}
