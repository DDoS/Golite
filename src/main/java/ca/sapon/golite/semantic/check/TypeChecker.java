package ca.sapon.golite.semantic.check;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ca.sapon.golite.semantic.context.CodeBlockContext;
import ca.sapon.golite.semantic.context.CodeBlockContext.Kind;
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
import golite.node.AAddExpr;
import golite.node.AAppendExpr;
import golite.node.AArrayType;
import golite.node.AAssignStmt;
import golite.node.ABitAndExpr;
import golite.node.ABitAndNotExpr;
import golite.node.ABitNotExpr;
import golite.node.ABitOrExpr;
import golite.node.ABitXorExpr;
import golite.node.ABreakStmt;
import golite.node.ACallExpr;
import golite.node.AClauseForCondition;
import golite.node.AContinueStmt;
import golite.node.ADeclVarShortStmt;
import golite.node.ADivExpr;
import golite.node.AEmptyForCondition;
import golite.node.AEmptyStmt;
import golite.node.AEqExpr;
import golite.node.AExprForCondition;
import golite.node.AFloatExpr;
import golite.node.AForStmt;
import golite.node.AFuncDecl;
import golite.node.AGreatEqExpr;
import golite.node.AGreatExpr;
import golite.node.AIdentExpr;
import golite.node.AIfBlock;
import golite.node.AIfStmt;
import golite.node.AIndexExpr;
import golite.node.AIntDecExpr;
import golite.node.AIntHexExpr;
import golite.node.AIntOctExpr;
import golite.node.ALessEqExpr;
import golite.node.ALessExpr;
import golite.node.ALogicAndExpr;
import golite.node.ALogicNotExpr;
import golite.node.ALogicOrExpr;
import golite.node.ALshiftExpr;
import golite.node.AMulExpr;
import golite.node.ANameType;
import golite.node.ANegateExpr;
import golite.node.ANeqExpr;
import golite.node.AParam;
import golite.node.APrintStmt;
import golite.node.APrintlnStmt;
import golite.node.AProg;
import golite.node.AReaffirmExpr;
import golite.node.ARemExpr;
import golite.node.AReturnStmt;
import golite.node.ARshiftExpr;
import golite.node.ARuneExpr;
import golite.node.ASelectExpr;
import golite.node.ASliceType;
import golite.node.AStringIntrExpr;
import golite.node.AStringRawExpr;
import golite.node.AStructField;
import golite.node.AStructType;
import golite.node.ASubExpr;
import golite.node.ATypeDecl;
import golite.node.AVarDecl;
import golite.node.Node;
import golite.node.PExpr;
import golite.node.PForCondition;
import golite.node.PIfBlock;
import golite.node.PParam;
import golite.node.PStmt;
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
    private int nextContextID = 2;

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

        System.out.println(exprNodeTypes);
        System.out.println();
        System.out.println(typeNodeTypes);
        System.out.println();
        System.out.println(context);
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
    public void caseADeclVarShortStmt(ADeclVarShortStmt node) {
        //Check for at least one non-blank undeclared idenf
        boolean undeclaredVar = false;
        for (PExpr left : node.getLeft()) {
            if (left instanceof AIdentExpr) {
                String idenf = ((AIdentExpr) left).getIdenf().getText();
                if (!idenf.equals("_") && !context.lookupSymbol(idenf).isPresent()) {
                    undeclaredVar = true;
                }
            }
        }
        if (!undeclaredVar) {
            throw new TypeCheckerException(node, "No new variables on LHS of :=");
        }
        //Check that all exprs on RHS are well-typed
        node.getRight().forEach(exp -> exp.apply(this));
        
        //Check that vars already declared are assigned to expressions of the same type
        final NodePosition position = new NodePosition(node);
        for (int i = 0; i < node.getLeft().size(); i++) {

            String idenf = ((AIdentExpr) node.getLeft().get(i)).getIdenf().getText();
            node.getRight().get(i).apply(this);
            Type rType = exprNodeTypes.get(node.getRight().get(i));
            //If blank identifier, skip to the next idenf
            if (!idenf.equals("_")) {
                Optional<Symbol> optVar = context.lookupSymbol(idenf);
                Variable var; Type lType;
                //If the var has already been declared, make sure RHS expr has the same type
                if (optVar.isPresent() && optVar.get() instanceof Variable) {
                    var = (Variable) optVar.get();
                    lType = var.getType();
                    if (!lType.equals(rType)) {
                        throw new TypeCheckerException(node, "Cannot use " + rType + " as " + lType + " in assignment");
                    } 
                } else {
                    //Var hasn't been declared - declare as new var with the same type as RHS expr
                    context.declareSymbol(new Variable(position, idenf, rType, false));   
                }
            }
        }
    }

    @Override
    public void caseATypeDecl(ATypeDecl node) {
        node.getType().apply(this);
        // The type to declare is an alias to that type
        final AliasType alias = new AliasType(context.getID(), node.getIdenf().getText(), typeNodeTypes.get(node.getType()));
        context.declareSymbol(new DeclaredType(new NodePosition(node), node.getIdenf().getText(), alias));
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
        context = new FunctionContext((TopLevelContext) context, nextContextID, function);
        nextContextID++;
        nodeContexts.put(node, context);
        // Declare the parameters as variables
        params.forEach(context::declareSymbol);
        // Type check the statements
        node.getStmt().forEach(stmt -> stmt.apply(this));
        // TODO: check that the function returns on each path
        // Exit the function body
        context = context.getParent();
    }

    @Override
    public void caseAEmptyStmt(AEmptyStmt node) {
    }

    @Override
    public void caseABreakStmt(ABreakStmt node) {
    }

    @Override
    public void caseAContinueStmt(AContinueStmt node) {
    }

    @Override
    public void caseAReturnStmt(AReturnStmt node) {
        final Function func = ((FunctionContext) context).getFunction();
        // If the function returns, check that the expression has the same type
        final Optional<Type> optReturnType = ((FunctionType) func.getType()).getReturnType();
        if (!optReturnType.isPresent()) {
            if (node.getExpr() != null) {
                throw new TypeCheckerException(node, "This function should not return anything");
            }
        } else if (node.getExpr() == null) {
            throw new TypeCheckerException(node, "A return expression is required");
        } else {
            final Type returnType = optReturnType.get();
            node.getExpr().apply(this);
            final Type exprType = exprNodeTypes.get(node.getExpr());
            if (!exprType.equals(returnType)) {
                throw new TypeCheckerException(node, "This function should return " + returnType + " instead of " + exprType);
            }
        }
    }

    @Override
    public void caseAAssignStmt(AAssignStmt node) {
        // Check that the left side has valid expressions (ignore blank identifiers)
        for (PExpr left : node.getLeft()) {
            if (left instanceof AIdentExpr && ((AIdentExpr) left).getIdenf().getText().equals("_")) {
                continue;
            }
            left.apply(this);
        }
        // Check tht the right side has valid expressions too
        node.getRight().forEach(rExpr -> rExpr.apply(this));
        // Compare elements at the same position in both sides
        for (int i = 0; i < node.getLeft().size(); i++) {
            final PExpr left = node.getLeft().get(i);
            // Blank identifiers can always be assigned to
            if (left instanceof AIdentExpr && ((AIdentExpr) left).getIdenf().getText().equals("_")) {
                continue;
            }
            final Type leftType = exprNodeTypes.get(left);
            final Type rightType = exprNodeTypes.get(node.getRight().get(i));
            if (!leftType.equals(rightType)) {
                throw new TypeCheckerException(node, "Cannot use type " + rightType + " as type " + leftType + " in assignment");
            }
        }
    }

    @Override
    public void caseAPrintStmt(APrintStmt node) {
        typeCheckPrintArgs(node.getExpr());
    }

    @Override
    public void caseAPrintlnStmt(APrintlnStmt node) {
        typeCheckPrintArgs(node.getExpr());
    }

    private void typeCheckPrintArgs(List<PExpr> args) {
        for (PExpr arg : args) {
            arg.apply(this);
            // Checks if all expressions resolve to a base type
            final Type argType = exprNodeTypes.get(arg).resolve();
            if (!(argType instanceof BasicType)) {
                throw new TypeCheckerException(arg, "Not a printable type " + argType);
            }
        }
    }

    @Override
    public void caseAForStmt(AForStmt node) {
        // Open a block to place the condition in a new context
        context = new CodeBlockContext(context, nextContextID, Kind.BLOCK);
        nextContextID++;
        // Type check the condition
        final PForCondition condition = node.getForCondition();
        condition.apply(this);
        // Open a new context for the "for" body
        context = new CodeBlockContext(context, nextContextID, Kind.FOR);
        nextContextID++;
        nodeContexts.put(node, context);
        // Type check the for statements
        node.getStmt().forEach(stmt -> stmt.apply(this));
        // If the condition is a clause, then we need to type check the post with the rest of the body
        if (condition instanceof AClauseForCondition) {
            ((AClauseForCondition) condition).getPost().apply(this);
        }
        // Close the "for" body
        context = context.getParent();
        // Close the outer block
        context = context.getParent();
    }
    
    @Override
    public void caseAIfBlock(AIfBlock node) {

        if (node.getInit() != null) {
            node.getInit().apply(this);
        }
        if (node.getCond() != null) {
            node.getCond().apply(this);
        }
        
        if (node.getBlock()!= null) {
            // Open a block to place the clause in a new context
            context = new CodeBlockContext(context, nextContextID, Kind.IF);
            nextContextID++;

            for (PStmt stmt : node.getBlock()) {
                stmt.apply(this);
            }
            // Close the context
            context = context.getParent();
        }
    }
    
    @Override
    public void caseAIfStmt(AIfStmt node) {

        // Open a block to place the clause in a new context
        context = new CodeBlockContext(context, nextContextID, Kind.BLOCK);
        nextContextID++;
        
        final LinkedList<PIfBlock> ifBlocks = node.getIfBlock();
        if (ifBlocks != null) {
            for (int i = 0, ifBlocksSize = ifBlocks.size(); i < ifBlocksSize; i++) {
                ifBlocks.get(i).apply(this);          
            }
        }
        final LinkedList<PStmt> ifElseBlocks = node.getElse();        
        if (ifElseBlocks != null) {
            // Open a block to place the clause in a new context
            context = new CodeBlockContext(context, nextContextID, Kind.BLOCK);
            nextContextID++;
            
            for (int i = 0, ifElseBlocksSize = ifElseBlocks.size(); i < ifElseBlocksSize; i++) {
                ifElseBlocks.get(i).apply(this);         
            }
            // Close the context
            context = context.getParent();
        }
        // Close the context
        context = context.getParent(); 
    }
    

    @Override
    public void caseAEmptyForCondition(AEmptyForCondition node) {
    }

    @Override
    public void caseAExprForCondition(AExprForCondition node) {
        node.getExpr().apply(this);
        final Type conditionType = exprNodeTypes.get(node.getExpr()).resolve();
        if (!conditionType.equals(BasicType.BOOL)) {
            throw new TypeCheckerException(node, "Non-bool cannot be used as 'for' condition");
        }
    }

    @Override
    public void caseAClauseForCondition(AClauseForCondition node) {
        node.getInit().apply(this);
        if (node.getCond() != null) {
            node.getCond().apply(this);
            final Type conditionType = exprNodeTypes.get(node.getCond()).resolve();
            if (!conditionType.equals(BasicType.BOOL)) {
                throw new TypeCheckerException(node, "Non-bool cannot be used as 'for' condition");
            }
        }
        // The post condition is checked in the scope of the body, not of the condition!
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
        throw new TypeCheckerException(node, "Cannot use symbol \"" + symbol + "\" as an expression");
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
        typeCheckCall(node, true);
    }

    private void typeCheckCall(ACallExpr node, boolean mustReturn) {
        // The call might be a cast. In this case the identifier will be a single symbol pointing to a type
        final PExpr value = node.getValue();
        if (value instanceof AIdentExpr) {
            final Optional<Symbol> symbol = context.lookupSymbol(((AIdentExpr) value).getIdenf().getText());
            if (symbol.isPresent() && symbol.get() instanceof DeclaredType) {
                // It's a cast
                typeCheckCast(node, symbol.get().getType());
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
        // Enforce the presence of a return type if needed
        if (mustReturn && !functionType.getReturnType().isPresent()) {
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
        // The return type (if any) is the type of the expression
        if (functionType.getReturnType().isPresent()) {
            exprNodeTypes.put(node, functionType.getReturnType().get());
        }
    }

    private void typeCheckCast(ACallExpr node, Type castType) {
        // Check that the resolved type can be used for a cast
        final Type resolvedCast = castType.resolve();
        if (!(resolvedCast instanceof BasicType)) {
            throw new TypeCheckerException(node.getValue(), "Cannot cast to non-basic type " + resolvedCast);
        }
        final BasicType basicCastType = (BasicType) resolvedCast;
        if (!basicCastType.canCastTo()) {
            throw new TypeCheckerException(node.getValue(), "Cannot cast to type " + basicCastType);
        }
        // Get the argument types
        node.getArgs().forEach(arg -> arg.apply(this));
        final List<Type> argTypes = node.getArgs().stream().map(exprNodeTypes::get).collect(Collectors.toList());
        // There should only be one argument
        if (argTypes.size() != 1) {
            throw new TypeCheckerException(node, "Expected only one argument for a cast");
        }
        // Check that we can cast the argument to the cast type
        if (!basicCastType.canCastFrom(argTypes.get(0))) {
            throw new TypeCheckerException(node, "Cannot cast from " + argTypes.get(0) + " to " + castType);
        }
        // The type is that of the cast, without the resolution
        exprNodeTypes.put(node, castType);
    }

    @Override
    public void caseAAppendExpr(AAppendExpr node) {
        // Check that the left argument is a slice type, after resolution
        node.getLeft().apply(this);
        final Type leftType = exprNodeTypes.get(node.getLeft());
        final Type innerLeftType = leftType.resolve();
        if (!(innerLeftType instanceof SliceType)) {
            throw new TypeCheckerException(node.getLeft(), "Not a slice type: " + innerLeftType);
        }
        final SliceType sliceType = (SliceType) innerLeftType;
        // Check that the right argument has the same type as the slice component
        node.getRight().apply(this);
        final Type rightType = exprNodeTypes.get(node.getRight());
        if (!rightType.equals(sliceType.getComponent())) {
            throw new TypeCheckerException(node.getRight(), "Cannot append " + rightType + " to " + innerLeftType);
        }
        // The type is the unresolved left type
        exprNodeTypes.put(node, leftType);
    }

    @Override
    public void caseALogicNotExpr(ALogicNotExpr node) {
        // Check that the inner type resolves to a bool
        node.getInner().apply(this);
        final Type innerType = exprNodeTypes.get(node.getInner());
        if (innerType.resolve() != BasicType.BOOL) {
            throw new TypeCheckerException(node.getInner(), "Not a bool");
        }
        // The type is the same as the inner
        exprNodeTypes.put(node, innerType);
    }

    @Override
    public void caseAReaffirmExpr(AReaffirmExpr node) {
        typeCheckSign(node, node.getInner());
    }

    @Override
    public void caseANegateExpr(ANegateExpr node) {
        typeCheckSign(node, node.getInner());
    }

    private void typeCheckSign(PExpr node, PExpr inner) {
        // Check that the inner type resolves to a numeric
        inner.apply(this);
        final Type innerType = exprNodeTypes.get(inner);
        final Type resolvedType = innerType.resolve();
        if (!resolvedType.isNumeric()) {
            throw new TypeCheckerException(inner, "Not a numeric type");
        }
        // The type is the same as the inner
        exprNodeTypes.put(node, innerType);
    }

    @Override
    public void caseABitNotExpr(ABitNotExpr node) {
        // Check that the inner type resolves to an integer
        node.getInner().apply(this);
        final Type innerType = exprNodeTypes.get(node.getInner());
        final Type resolvedType = innerType.resolve();
        if (!resolvedType.isInteger()) {
            throw new TypeCheckerException(node.getInner(), "Not an integer type");
        }
        // The type is the same as the inner
        exprNodeTypes.put(node, innerType);
    }

    @Override
    public void caseAMulExpr(AMulExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.MUL);
    }

    @Override
    public void caseADivExpr(ADivExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.DIV);
    }

    @Override
    public void caseARemExpr(ARemExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.REM);
    }

    @Override
    public void caseALshiftExpr(ALshiftExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.LSHIFT);
    }

    @Override
    public void caseARshiftExpr(ARshiftExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.RSHIFT);
    }

    @Override
    public void caseABitAndExpr(ABitAndExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.BIT_AND);
    }

    @Override
    public void caseABitAndNotExpr(ABitAndNotExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.BIT_AND_NOT);
    }

    @Override
    public void caseAAddExpr(AAddExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.ADD);
    }

    @Override
    public void caseASubExpr(ASubExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.SUB);
    }

    @Override
    public void caseABitOrExpr(ABitOrExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.BIT_OR);
    }

    @Override
    public void caseABitXorExpr(ABitXorExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.BIT_XOR);
    }

    @Override
    public void caseAEqExpr(AEqExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.EQ);
    }

    @Override
    public void caseANeqExpr(ANeqExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.NEQ);
    }

    @Override
    public void caseALessExpr(ALessExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.LESS);
    }

    @Override
    public void caseALessEqExpr(ALessEqExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.LESS_EQ);
    }

    @Override
    public void caseAGreatExpr(AGreatExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.GREAT);
    }

    @Override
    public void caseAGreatEqExpr(AGreatEqExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.GREAT_EQ);
    }

    @Override
    public void caseALogicAndExpr(ALogicAndExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.LOGIC_AND);
    }

    @Override
    public void caseALogicOrExpr(ALogicOrExpr node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.LOGIC_OR);
    }

    private void typeCheckBinary(PExpr node, PExpr left, PExpr right, BinaryOperator operator) {
        // Check that the left and right are well typed
        left.apply(this);
        final Type leftType = exprNodeTypes.get(left);
        right.apply(this);
        final Type rightType = exprNodeTypes.get(right);
        // Check if they are the same type
        if (!leftType.equals(rightType)) {
            throw new TypeCheckerException(node, "Cannot operate on differing types: " + leftType + " != " + rightType);
        }
        // Check that the type resolves to something valid for the operation (types are equal, so we only need to check one)
        final Type leftResolved = leftType.resolve();
        final boolean valid;
        final Type resultType;
        switch (operator) {
            case MUL:
            case DIV:
            case SUB:
                valid = leftResolved.isNumeric();
                resultType = leftType;
                break;
            case ADD:
                valid = leftResolved.isNumeric() || leftResolved == BasicType.STRING;
                resultType = leftType;
                break;
            case REM:
            case LSHIFT:
            case RSHIFT:
            case BIT_AND:
            case BIT_AND_NOT:
            case BIT_OR:
            case BIT_XOR:
                valid = leftResolved.isInteger();
                resultType = leftType;
                break;
            case EQ:
            case NEQ:
                valid = leftResolved.isComparable();
                resultType = BasicType.BOOL;
                break;
            case LESS:
            case LESS_EQ:
            case GREAT:
            case GREAT_EQ:
                valid = leftResolved.isOrdered();
                resultType = BasicType.BOOL;
                break;
            case LOGIC_AND:
            case LOGIC_OR:
                valid = leftResolved == BasicType.BOOL;
                resultType = BasicType.BOOL;
                break;
            default:
                throw new IllegalStateException("Missing switch case for operator: " + operator);
        }
        if (!valid) {
            throw new TypeCheckerException(node, "Cannot use the operator " + operator + " on the type " + leftType);
        }
        exprNodeTypes.put(node, resultType);
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
        // The type is that of the found type symbol
        typeNodeTypes.put(node, symbol.getType());
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

    private enum BinaryOperator {
        MUL, DIV, REM, LSHIFT, RSHIFT, BIT_AND, BIT_AND_NOT, ADD, SUB, BIT_OR,
        BIT_XOR, EQ, NEQ, LESS, LESS_EQ, GREAT, GREAT_EQ, LOGIC_AND, LOGIC_OR
    }
}
