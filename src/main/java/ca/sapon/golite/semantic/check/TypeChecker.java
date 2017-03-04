package ca.sapon.golite.semantic.check;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ca.sapon.golite.semantic.context.Context;
import ca.sapon.golite.semantic.context.FunctionContext;
import ca.sapon.golite.semantic.context.TopLevelContext;
import ca.sapon.golite.semantic.context.UniverseContext;
import ca.sapon.golite.semantic.symbol.DeclaredType;
import ca.sapon.golite.semantic.symbol.Function;
import ca.sapon.golite.semantic.symbol.Symbol;
import ca.sapon.golite.semantic.symbol.Variable;
import ca.sapon.golite.semantic.type.AliasType;
import ca.sapon.golite.semantic.type.ArrayType;
import ca.sapon.golite.semantic.type.BasicType;
import ca.sapon.golite.semantic.type.FunctionType;
import ca.sapon.golite.semantic.type.FunctionType.Parameter;
import ca.sapon.golite.semantic.type.IndexableType;
import ca.sapon.golite.semantic.type.SliceType;
import ca.sapon.golite.semantic.type.StructType;
import ca.sapon.golite.semantic.type.StructType.Field;
import ca.sapon.golite.semantic.type.Type;
import ca.sapon.golite.util.NodePosition;
import golite.analysis.AnalysisAdapter;
import golite.node.AAppendExpr;
import golite.node.AArrayType;
import golite.node.ACallExpr;
import golite.node.AFloatExpr;
import golite.node.AFuncDecl;
import golite.node.AIdentExpr;
import golite.node.AIndexExpr;
import golite.node.AIntDecExpr;
import golite.node.AIntHexExpr;
import golite.node.AIntOctExpr;
import golite.node.ANameType;
import golite.node.AParam;
import golite.node.AProg;
import golite.node.ARuneExpr;
import golite.node.ASelectExpr;
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
        context = UniverseContext.INSTANCE;
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
                    throw new TypeCheckerException(node.getExpr().get(i), "Cannot assign type " + valueType + " to " + type);
                }
            }
            // Declare the variables
            node.getIdenf().stream()
                    .map(idenf -> new Variable(position, idenf.getText(), type, false))
                    .forEach(context::declareSymbol);
        } else {
            // Otherwise declare the variable for each identifier using the value types
            final List<TIdenf> idenfs = node.getIdenf();
            for (int i = 0; i < idenfs.size(); i++) {
                context.declareSymbol(new Variable(position, idenfs.get(i).getText(), valueTypes.get(i), false));
            }
        }
    }

    @Override
    public void caseATypeDecl(ATypeDecl node) {
        node.getType().apply(this);
        context.declareSymbol(new DeclaredType(new NodePosition(node), node.getIdenf().getText(), typeNodeTypes.get(node.getType())));
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
        // Create the function type
        final List<Parameter> parameterTypes = params.stream()
                .map(param -> new Parameter(param.getName(), param.getType()))
                .collect(Collectors.toList());
        final FunctionType type = new FunctionType(parameterTypes, returnType);
        // Now declare the function
        final Function function = new Function(new NodePosition(node), node.getIdenf().getText(), type);
        context.declareSymbol(function);
        // Enter the function body
        context = new FunctionContext((TopLevelContext) context, function);
        nodeContexts.put(node, context);
        // Declare the parameters as variables
        params.forEach(context::declareSymbol);
        // TODO: type check the statements
        // TODO: check that the function returns on each path
        // Exit the function body
        context = context.getParent();
    }

    @Override
    public void caseAIdentExpr(AIdentExpr node) {
        // Resolve the symbol for the name
        final String name = node.getIdenf().getText();
        final Optional<Symbol> optSymbol = context.lookupSymbol(name);
        if (!optSymbol.isPresent()) {
            throw new TypeCheckerException(node, "Undeclared symbol " + name);
        }
        final Symbol symbol = optSymbol.get();
        // If the symbol is a variable or function, add the type
        if (symbol instanceof Variable || symbol instanceof Function) {
            exprNodeTypes.put(node, symbol.getType());
            return;
        }
        // Otherwise the symbol can't be used an expression
        throw new TypeCheckerException(node, "Cannot use symbol " + symbol + " as an expression");
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
    public void caseASelectExpr(ASelectExpr node) {
        // Check that the value is a struct type
        node.getValue().apply(this);
        final Type valueType = exprNodeTypes.get(node.getValue()).resolve();
        if (!(valueType instanceof StructType)) {
            throw new TypeCheckerException(node.getValue(), "Not a struct type: " + valueType);
        }
        // Check that the struct has a field with the selected name
        final String fieldName = node.getIdenf().getText();
        final Optional<Field> optField = ((StructType) valueType).getField(fieldName);
        if (!optField.isPresent()) {
            throw new TypeCheckerException(node.getIdenf(), "No field named " + fieldName + " in type " + valueType);
        }
        // The type is that of the field
        exprNodeTypes.put(node, optField.get().getType());
    }

    @Override
    public void caseAIndexExpr(AIndexExpr node) {
        // Check that the value is a slice or array type (indexable type)
        node.getValue().apply(this);
        final Type valueType = exprNodeTypes.get(node.getValue()).resolve();
        if (!(valueType instanceof IndexableType)) {
            throw new TypeCheckerException(node.getValue(), "Not a slice or array type: " + valueType);
        }
        // Check the the index expression has type int
        node.getIndex().apply(this);
        final Type indexType = exprNodeTypes.get(node.getIndex()).resolve();
        if (indexType != BasicType.INT) {
            throw new TypeCheckerException(node.getIndex(), "Not an int " + valueType);
        }
        // The type is that of the component
        exprNodeTypes.put(node, ((IndexableType) valueType).getComponent());
    }

    @Override
    public void caseACallExpr(ACallExpr node) {
        // The call might be a cast. In this case the identifier will be a single symbol pointing to a type
        final PExpr value = node.getValue();
        if (value instanceof AIdentExpr) {
            final Optional<Symbol> symbol = context.lookupSymbol(((AIdentExpr) value).getIdenf().getText());
            if (symbol.isPresent() && symbol.get() instanceof DeclaredType) {
                // It's a cast
                // TODO: casts
                return;
            }
        }
        // It's a call
        value.apply(this);
        final Type valueType = exprNodeTypes.get(value);
        // The value should be a function type
        if (!(valueType instanceof FunctionType)) {
            throw new TypeCheckerException(value, "Not a function type " + valueType);
        }
        final FunctionType functionType = (FunctionType) valueType;
        // The function should have a return type
        if (!functionType.getReturnType().isPresent()) {
            throw new TypeCheckerException(value, "The function type " + functionType + " does not return");
        }
        // Get the argument types
        node.getArgs().forEach(arg -> arg.apply(this));
        final List<Type> argTypes = node.getArgs().stream().map(exprNodeTypes::get).collect(Collectors.toList());
        // The argument and parameter types should be the same
        final List<Parameter> parameters = functionType.getParameters();
        if (parameters.size() != argTypes.size()) {
            throw new TypeCheckerException(node, "Mistmatch in the number of parameters in " + functionType
                    + " and arguments for the call: " + parameters.size() + " != " + argTypes.size());
        }
        for (int i = 0; i < parameters.size(); i++) {
            final Type paramType = parameters.get(i).getType();
            if (!paramType.equals(argTypes.get(i))) {
                throw new TypeCheckerException(node.getArgs().get(i), "Cannot assign type " + argTypes.get(i) + " to " + paramType);
            }
        }
        // The return type if the type of the expression
        exprNodeTypes.put(node, functionType.getReturnType().get());
    }

    @Override
    public void caseAAppendExpr(AAppendExpr node) {
        super.caseAAppendExpr(node);
    }

    @Override
    public void caseANameType(ANameType node) {
        // Resolve the symbol for the name
        final String name = node.getIdenf().getText();
        final Optional<Symbol> optSymbol = context.lookupSymbol(name);
        if (!optSymbol.isPresent()) {
            throw new TypeCheckerException(node.getIdenf(), "Undeclared symbol " + name);
        }
        // Check that the symbol is a type
        final Symbol symbol = optSymbol.get();
        if (!(symbol instanceof DeclaredType)) {
            throw new TypeCheckerException(node.getIdenf(), "Not a type " + symbol);
        }
        // The type is an alias to that type
        typeNodeTypes.put(node, new AliasType(name, symbol.getType()));
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
