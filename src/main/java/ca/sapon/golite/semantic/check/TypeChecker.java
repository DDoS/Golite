package ca.sapon.golite.semantic.check;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ca.sapon.golite.semantic.context.Context;
import ca.sapon.golite.semantic.context.FunctionContext;
import ca.sapon.golite.semantic.context.TopLevelContext;
import ca.sapon.golite.semantic.symbol.Function;
import ca.sapon.golite.semantic.symbol.NamedType;
import ca.sapon.golite.semantic.symbol.Variable;
import ca.sapon.golite.semantic.type.ArrayType;
import ca.sapon.golite.semantic.type.BasicType;
import ca.sapon.golite.semantic.type.SliceType;
import ca.sapon.golite.semantic.type.StructType;
import ca.sapon.golite.semantic.type.StructType.Field;
import ca.sapon.golite.semantic.type.Type;
import ca.sapon.golite.util.NodePosition;
import golite.analysis.AnalysisAdapter;
import golite.node.AArrayType;
import golite.node.AFloatExpr;
import golite.node.AFuncDecl;
import golite.node.AIdentExpr;
import golite.node.AIntDecExpr;
import golite.node.AIntHexExpr;
import golite.node.AIntOctExpr;
import golite.node.ANameType;
import golite.node.AParam;
import golite.node.AProg;
import golite.node.ARuneExpr;
import golite.node.ASliceType;
import golite.node.AStringIntrExpr;
import golite.node.AStringRawExpr;
import golite.node.AStructField;
import golite.node.AStructType;
import golite.node.ATypeDecl;
import golite.node.AVarDecl;
import golite.node.Node;
import golite.node.PExpr;
import golite.node.PParam;
import golite.node.PStructField;
import golite.node.PType;
import golite.node.Start;
import golite.node.TIdenf;

/**
 *
 */
public class TypeChecker extends AnalysisAdapter {
    private final Map<PExpr, Type> exprNodeTypes = new HashMap<>();
    private final Map<PType, Type> typeNodeTypes = new HashMap<>();
    private final Map<Node, Context> nodeContexts = new HashMap<>();
    private Context context;

    @Override
    public void caseStart(Start node) {
        node.getPProg().apply(this);
    }

    @Override
    public void caseAProg(AProg node) {
        context = new TopLevelContext();
        nodeContexts.put(node, context);
        node.getDecl().forEach(decl -> decl.apply(this));
    }

    @Override
    public void caseAVarDecl(AVarDecl node) {
        final NodePosition position = new NodePosition(node);
        // Type-check the values
        final List<Type> valueTypes = new ArrayList<>();
        for (PExpr exprNode : node.getExpr()) {
            exprNode.apply(this);
            valueTypes.add(exprNodeTypes.get(exprNode));
        }
        // Declare the variables
        if (node.getType() != null) {
            // If we have the type, then declare a variable for each identifier, all with the same type
            node.getType().apply(this);
            final Type type = typeNodeTypes.get(node.getType());
            // Check that the values have the same type as the variable (this is skipped if there are no values)
            for (int i = 0; i < valueTypes.size(); i++) {
                final Type valueType = valueTypes.get(i);
                if (!valueType.equals(type)) {
                    throw new TypeCheckerException(node.getExpr().get(i), String.format("Cannot assign type %s to %s", valueType, type));
                }
            }
            // Declare the variables
            node.getIdenf().stream()
                    .map(idenf -> new Variable(position, idenf.getText(), type, false))
                    .forEach(context::declareVariable);
        } else {
            // Otherwise declare the variable for each identifier using the value types
            final List<TIdenf> idenfs = node.getIdenf();
            for (int i = 0; i < idenfs.size(); i++) {
                context.declareVariable(new Variable(position, idenfs.get(i).getText(), valueTypes.get(i), false));
            }
        }
    }

    @Override
    public void caseATypeDecl(ATypeDecl node) {
        node.getType().apply(this);
        context.declareType(new NamedType(new NodePosition(node), node.getIdenf().getText(), typeNodeTypes.get(node.getType())));
    }

    @Override
    public void caseAFuncDecl(AFuncDecl node) {
        // Create variables for the parameters, which will be declared into the function context
        final List<Variable> params = new ArrayList<>();
        for (PParam pParam : node.getParam()) {
            final NodePosition paramPos = new NodePosition(pParam);
            final AParam param = (AParam) pParam;
            param.getType().apply(this);
            final Type paramType = typeNodeTypes.get(param.getType());
            param.getIdenf().forEach(idenf -> params.add(new Variable(paramPos, idenf.getText(), paramType, false)));
        }
        // Now check the return type (if it exists)
        final Type returnType;
        if (node.getType() != null) {
            node.getType().apply(this);
            returnType = typeNodeTypes.get(node.getType());
        } else {
            returnType = null;
        }
        // Now declare the function
        final Function function = new Function(new NodePosition(node), node.getIdenf().getText(), params, returnType);
        context.declareFunction(function);
        // Enter the function body
        context = new FunctionContext((TopLevelContext) context, function);
        nodeContexts.put(node, context);
        // Declare the parameters as variables
        params.forEach(context::declareVariable);
        // TODO: type check the statements
        // TODO: check that the function returns on each path
        // Exit the function body
        context = context.getParent();
    }

    @Override
    public void caseAIdentExpr(AIdentExpr node) {
        final String name = node.getIdenf().getText();
        final Optional<Variable> variable = context.resolveVariable(name);
        if (!variable.isPresent()) {
            throw new TypeCheckerException(node, "Undeclared variable " + name);
        }
        exprNodeTypes.put(node, variable.get().getType());
    }

    @Override
    public void caseAIntDecExpr(AIntDecExpr node) {
        exprNodeTypes.put(node, BasicType.INT);
    }

    @Override
    public void caseAIntOctExpr(AIntOctExpr node) {
        exprNodeTypes.put(node, BasicType.INT);
    }

    @Override
    public void caseAIntHexExpr(AIntHexExpr node) {
        exprNodeTypes.put(node, BasicType.INT);
    }

    @Override
    public void caseAFloatExpr(AFloatExpr node) {
        exprNodeTypes.put(node, BasicType.FLOAT64);
    }

    @Override
    public void caseARuneExpr(ARuneExpr node) {
        exprNodeTypes.put(node, BasicType.RUNE);
    }

    @Override
    public void caseAStringIntrExpr(AStringIntrExpr node) {
        exprNodeTypes.put(node, BasicType.STRING);
    }

    @Override
    public void caseAStringRawExpr(AStringRawExpr node) {
        exprNodeTypes.put(node, BasicType.STRING);
    }

    @Override
    public void caseANameType(ANameType node) {
        final String name = node.getIdenf().getText();
        final Optional<NamedType> namedType = context.resolveType(name);
        if (!namedType.isPresent()) {
            throw new TypeCheckerException(node, "Undeclared type " + name);
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
