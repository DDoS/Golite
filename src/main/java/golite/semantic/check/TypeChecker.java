package golite.semantic.check;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import golite.analysis.AnalysisAdapter;
import golite.node.AAddExpr;
import golite.node.AAppendExpr;
import golite.node.AArrayType;
import golite.node.AAssignAddStmt;
import golite.node.AAssignBitAndNotStmt;
import golite.node.AAssignBitAndStmt;
import golite.node.AAssignBitOrStmt;
import golite.node.AAssignBitXorStmt;
import golite.node.AAssignDivStmt;
import golite.node.AAssignLshiftStmt;
import golite.node.AAssignMulStmt;
import golite.node.AAssignRemStmt;
import golite.node.AAssignRshiftStmt;
import golite.node.AAssignStmt;
import golite.node.AAssignSubStmt;
import golite.node.ABitAndExpr;
import golite.node.ABitAndNotExpr;
import golite.node.ABitNotExpr;
import golite.node.ABitOrExpr;
import golite.node.ABitXorExpr;
import golite.node.ABlockStmt;
import golite.node.ABreakStmt;
import golite.node.ACallExpr;
import golite.node.AClauseForCondition;
import golite.node.AContinueStmt;
import golite.node.ADeclStmt;
import golite.node.ADeclVarShortStmt;
import golite.node.ADecrStmt;
import golite.node.ADefaultCase;
import golite.node.ADivExpr;
import golite.node.AEmptyForCondition;
import golite.node.AEmptyStmt;
import golite.node.AEqExpr;
import golite.node.AExprCase;
import golite.node.AExprForCondition;
import golite.node.AExprStmt;
import golite.node.AFloatExpr;
import golite.node.AForStmt;
import golite.node.AFuncDecl;
import golite.node.AGreatEqExpr;
import golite.node.AGreatExpr;
import golite.node.AIdentExpr;
import golite.node.AIfBlock;
import golite.node.AIfStmt;
import golite.node.AIncrStmt;
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
import golite.node.ASwitchStmt;
import golite.node.ATypeDecl;
import golite.node.AVarDecl;
import golite.node.Node;
import golite.node.PCase;
import golite.node.PExpr;
import golite.node.PForCondition;
import golite.node.PIfBlock;
import golite.node.PParam;
import golite.node.PStmt;
import golite.node.PStructField;
import golite.node.PType;
import golite.node.Start;
import golite.node.TIdenf;
import golite.semantic.LiteralUtil;
import golite.semantic.SemanticData;
import golite.semantic.context.CodeBlockContext;
import golite.semantic.context.CodeBlockContext.Kind;
import golite.semantic.context.Context;
import golite.semantic.context.FunctionContext;
import golite.semantic.context.TopLevelContext;
import golite.semantic.context.UniverseContext;
import golite.semantic.symbol.DeclaredType;
import golite.semantic.symbol.Function;
import golite.semantic.symbol.Symbol;
import golite.semantic.symbol.Variable;
import golite.semantic.type.AliasType;
import golite.semantic.type.ArrayType;
import golite.semantic.type.BasicType;
import golite.semantic.type.FunctionType;
import golite.semantic.type.FunctionType.Parameter;
import golite.semantic.type.IndexableType;
import golite.semantic.type.SliceType;
import golite.semantic.type.StructType;
import golite.semantic.type.StructType.Field;
import golite.semantic.type.Type;
import golite.util.NodePosition;

/**
 * Resolves symbols and type-checks the program.
 */
public class TypeChecker extends AnalysisAdapter {
    private final Map<PExpr, Type> exprNodeTypes = new HashMap<>();
    private final Map<PType, Type> typeNodeTypes = new HashMap<>();
    private final Map<AFuncDecl, Function> funcSymbols = new HashMap<>();
    private final Map<Node, List<Variable<?>>> varSymbols = new HashMap<>();
    private final Map<AIdentExpr, Symbol<?>> identSymbols = new HashMap<>();
    private final Map<Context, Node> contextNodes = new HashMap<>();
    private Context context;
    private int nextContextID = 0;

    public SemanticData getGeneratedData() {
        return new SemanticData(exprNodeTypes, contextNodes, funcSymbols, varSymbols, identSymbols);
    }

    @Override
    public void caseStart(Start node) {
        context = UniverseContext.INSTANCE;
        nextContextID++;
        contextNodes.put(context, node);
        node.getPProg().apply(this);
    }

    @Override
    public void caseAProg(AProg node) {
        context = new TopLevelContext();
        nextContextID++;
        contextNodes.put(context, node);

        node.getDecl().forEach(decl -> decl.apply(this));
        // Check that the main has the proper signature (no parameters or return type)
        boolean mainDefined = false;
        for (Entry<AFuncDecl, Function> entry : funcSymbols.entrySet()) {
            final Function function = entry.getValue();
            if (function.getName().equals("main")) {
                if (function.getType().getReturnType().isPresent()) {
                    throw new TypeCheckerException(entry.getKey(), "The main function should not have a return type");
                }
                if (!function.getType().getParameters().isEmpty()) {
                    throw new TypeCheckerException(entry.getKey(), "The main function shouldn't have any parameters");
                }
                mainDefined = true;
                break;
            }
        }

        if (!mainDefined) {
            throw new TypeCheckerException(node, "The main function must be defined");
        }
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
        final List<Variable<?>> variables = new ArrayList<>();
        if (node.getType() != null) {
            // If we have the type, then declare a variable for each identifier, all with the same type
            node.getType().apply(this);
            final Type type = typeNodeTypes.get(node.getType());
            // Check that the values have the same type as the variable (this is skipped if there are no values)
            for (int i = 0; i < valueTypes.size(); i++) {
                final Type valueType = valueTypes.get(i);
                if (!valueType.equals(type)) {
                    throw new TypeCheckerException(node.getExpr().get(i), "Cannot assign type " + valueType + " to type " + type);
                }
            }
            // Declare the variables
            for (TIdenf idenf : node.getIdenf()) {
                final Variable<?> variable = new Variable<>(position, idenf.getText(), type);
                context.declareSymbol(variable);
                variables.add(variable);
            }
        } else {
            // Otherwise declare the variable for each identifier using the value types
            final List<TIdenf> idenfs = node.getIdenf();
            for (int i = 0; i < idenfs.size(); i++) {
                final Variable<?> variable = new Variable<>(position, idenfs.get(i).getText(), valueTypes.get(i));
                context.declareSymbol(variable);
                variables.add(variable);
            }
        }
        varSymbols.put(node, variables);
    }

    @Override
    public void caseADeclVarShortStmt(ADeclVarShortStmt node) {
        // Check for at least one non-blank undeclared idenf
        boolean undeclaredVar = false;
        for (PExpr left : node.getLeft()) {
            // The weeder pass guarantees that the left contains only identifier expressions
            final String idenf = ((AIdentExpr) left).getIdenf().getText();
            if (!idenf.equals("_") && !context.lookupSymbol(idenf, false).isPresent()) {
                undeclaredVar = true;
                break;
            }
        }
        if (!undeclaredVar) {
            throw new TypeCheckerException(node, "No new variables on the left side");
        }
        // Check that all exprs on RHS are well-typed
        node.getRight().forEach(exp -> exp.apply(this));
        // Check that vars already declared are assigned to expressions of the same type
        final List<Variable<?>> variables = new ArrayList<>();
        for (int i = 0; i < node.getLeft().size(); i++) {
            node.getRight().get(i).apply(this);
            final Type rightType = exprNodeTypes.get(node.getRight().get(i));
            // If blank identifier, skip to the next idenf
            final AIdentExpr leftNode = (AIdentExpr) node.getLeft().get(i);
            final String idenf = leftNode.getIdenf().getText();
            if (idenf.equals("_")) {
                continue;
            }
            // If the var has already been declared, make sure RHS expr has the same type
            final Optional<Symbol<?>> optVar = context.lookupSymbol(idenf, false);
            if (optVar.isPresent() && optVar.get() instanceof Variable<?>) {
                final Symbol<?> var = optVar.get();
                final Type leftType = var.getType();
                if (!leftType.equals(rightType)) {
                    throw new TypeCheckerException(node, "Cannot assign type " + rightType + " to type " + leftType);
                }
                exprNodeTypes.put(leftNode, leftType);
                identSymbols.put(leftNode, var);
                variables.add(null);
            } else {
                // Var hasn't been declared - declare as new var with the same type as RHS expr
                final Variable<?> variable = new Variable<>(new NodePosition(leftNode), idenf, rightType);
                context.declareSymbol(variable);
                variables.add(variable);
            }
        }
        varSymbols.put(node, variables);
    }

    @Override
    public void caseATypeDecl(ATypeDecl node) {
        node.getType().apply(this);
        // The type to declare is an alias to that type
        final AliasType alias = new AliasType(context.getID(), node.getIdenf().getText(), typeNodeTypes.get(node.getType()));
        context.declareSymbol(new DeclaredType<>(new NodePosition(node), node.getIdenf().getText(), alias));
    }

    @Override
    public void caseAFuncDecl(AFuncDecl node) {
        // Create variables for the parameters, which will be declared into the function context
        final List<Variable<?>> params = new ArrayList<>();
        for (PParam pParam : node.getParam()) {
            final NodePosition paramPos = new NodePosition(pParam);
            final AParam param = (AParam) pParam;
            param.getType().apply(this);
            final Type paramType = typeNodeTypes.get(param.getType());
            param.getIdenf().forEach(idenf -> params.add(new Variable<>(paramPos, idenf.getText(), paramType)));
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
        final Function function = new Function(new NodePosition(node), node.getIdenf().getText(), type, params);
        context.declareSymbol(function);
        funcSymbols.put(node, function);
        // Enter the function body
        context = new FunctionContext((TopLevelContext) context, nextContextID, function);
        nextContextID++;
        contextNodes.put(context, node);
        // Declare the parameters as variables
        params.forEach(context::declareSymbol);
        varSymbols.put(node, params);
        // Type check the statements
        node.getStmt().forEach(stmt -> stmt.apply(this));
        // Check that all code paths return if the function returns a value
        if (function.getType().getReturnType().isPresent()) {
            final TerminatingStmtChecker terminatingChecker = new TerminatingStmtChecker();
            node.apply(terminatingChecker);
        }
        // Exit the function body
        context = context.getParent();
    }

    @Override
    public void caseABlockStmt(ABlockStmt node) {
        // Open a context for the block
        context = new CodeBlockContext(context, nextContextID, Kind.BLOCK);
        nextContextID++;
        contextNodes.put(context, node);
        // Evaluate the stmts in the block
        node.getStmt().forEach(stmt -> stmt.apply(this));
        // Close the context
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
        // No need to check if return appears in a function (the weeder already does that)
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        final Function function = context.getEnclosingFunction().get();
        // If the function returns, check that the expression has the same type
        final Optional<Type> optReturnType = function.getType().getReturnType();
        if (!optReturnType.isPresent()) {
            if (node.getExpr() != null) {
                throw new TypeCheckerException(node.getExpr(), "The function does not return");
            }
        } else if (node.getExpr() == null) {
            throw new TypeCheckerException(node, "Expected a return value");
        } else {
            final Type returnType = optReturnType.get();
            node.getExpr().apply(this);
            final Type exprType = exprNodeTypes.get(node.getExpr());
            if (!exprType.equals(returnType)) {
                throw new TypeCheckerException(node.getExpr(), "Cannot return type " + exprType + " instead of " + returnType);
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
                throw new TypeCheckerException(node, "Cannot assign type " + rightType + " to type " + leftType);
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
                throw new TypeCheckerException(arg, "Cannot print type " + argType);
            }
        }
    }

    @Override
    public void caseADeclStmt(ADeclStmt node) {
        node.getDecl().forEach(decl -> decl.apply(this));
    }

    @Override
    public void caseAExprStmt(AExprStmt node) {
        // This is already weeded, but we still have a special case for calls (no return is allowed)
        if (node.getExpr() instanceof ACallExpr) {
            final boolean isCast = typeCheckCall((ACallExpr) node.getExpr(), false);
            if (isCast) {
                throw new TypeCheckerException(node, "Cannot use a cast expression as a statement");
            }
        } else {
            node.getExpr().apply(this);
        }
    }

    @Override
    public void caseAForStmt(AForStmt node) {
        // Open a block to place the condition in a new context
        context = new CodeBlockContext(context, nextContextID, Kind.BLOCK);
        nextContextID++;
        contextNodes.put(context, node);
        // Type check the condition
        final PForCondition condition = node.getForCondition();
        condition.apply(this);
        // Open a new context for the "for" body
        context = new CodeBlockContext(context, nextContextID, Kind.FOR);
        nextContextID++;
        contextNodes.put(context, node);
        // Type check the for statements
        node.getStmt().forEach(stmt -> stmt.apply(this));
        // Close the "for" body
        context = context.getParent();
        // Close the outer block
        context = context.getParent();
    }

    @Override
    public void caseAEmptyForCondition(AEmptyForCondition node) {
    }

    @Override
    public void caseAExprForCondition(AExprForCondition node) {
        final PExpr condition = node.getExpr();
        condition.apply(this);
        final Type conditionType = exprNodeTypes.get(condition).resolve();
        if (!conditionType.equals(BasicType.BOOL)) {
            throw new TypeCheckerException(condition, "Not a bool: " + conditionType);
        }
    }

    @Override
    public void caseAClauseForCondition(AClauseForCondition node) {
        node.getInit().apply(this);
        final PExpr condition = node.getCond();
        if (condition != null) {
            condition.apply(this);
            final Type conditionType = exprNodeTypes.get(condition).resolve();
            if (!conditionType.equals(BasicType.BOOL)) {
                throw new TypeCheckerException(condition, "Not a bool: " + conditionType);
            }
        }
        node.getPost().apply(this);
    }

    @Override
    public void caseAIfStmt(AIfStmt node) {
        // Type-check the if-blocks
        for (PIfBlock pIfBlock : node.getIfBlock()) {
            final AIfBlock ifBlock = (AIfBlock) pIfBlock;
            // Open a block to place the condition in a new context
            context = new CodeBlockContext(context, nextContextID, Kind.BLOCK);
            nextContextID++;
            contextNodes.put(context, ifBlock);
            // Type-check the init statement
            if (ifBlock.getInit() != null) {
                ifBlock.getInit().apply(this);
            }
            // Type-check the condition: it should be a bool
            final PExpr condition = ifBlock.getCond();
            condition.apply(this);
            final Type conditionType = exprNodeTypes.get(condition).resolve();
            if (conditionType != BasicType.BOOL) {
                throw new TypeCheckerException(condition, "Not a bool: " + conditionType);
            }
            // Open a block to place the body in a new context
            context = new CodeBlockContext(context, nextContextID, Kind.IF);
            nextContextID++;
            contextNodes.put(context, ifBlock);
            // Type-check the body
            ifBlock.getBlock().forEach(stmt -> stmt.apply(this));
            // Close the body context
            context = context.getParent();
            // Don't close the condition context yet, since they nest successively
        }
        // Open a block to place the else-block in a new context
        context = new CodeBlockContext(context, nextContextID, Kind.ELSE);
        nextContextID++;
        contextNodes.put(context, node);
        // Type-check the else-block
        node.getElse().forEach(stmt -> stmt.apply(this));
        // Close the else-block context
        context = context.getParent();
        // Now we close all the nested condition contexts
        node.getIfBlock().forEach(block -> context = context.getParent());
    }

    @Override
    public void caseASwitchStmt(ASwitchStmt node) {
        // Open a block to place the switch in a new context
        context = new CodeBlockContext(context, nextContextID, Kind.SWITCH);
        nextContextID++;
        contextNodes.put(context, node);
        // Type-check the init statement
        if (node.getInit() != null) {
            node.getInit().apply(this);
        }
        // Type-check the expression
        final Type switchType;
        if (node.getValue() != null) {
            node.getValue().apply(this);
            switchType = exprNodeTypes.get(node.getValue());
        } else {
            // Empty condition defaults to the bool type
            switchType = BasicType.BOOL;
        }
        // Type-check the case blocks
        for (PCase case_ : node.getCase()) {
            // Open a block to place the case in a new context
            context = new CodeBlockContext(context, nextContextID, Kind.CASE);
            nextContextID++;
            contextNodes.put(context, node);
            // Get the stmts for the case, and do any extra type checking if necessary
            final List<PStmt> stmts;
            if (case_ instanceof AExprCase) {
                final AExprCase exprCase = (AExprCase) case_;
                // Type-check the condition expression for the case
                for (PExpr expr : exprCase.getExpr()) {
                    // Get the condition type
                    expr.apply(this);
                    final Type conditionType = exprNodeTypes.get(expr);
                    // If the type in the case is the same as the one specified in switch condition
                    if (!switchType.equals(conditionType)) {
                        throw new TypeCheckerException(expr, "The condition type " + conditionType
                                + " does not match the switch type " + switchType);
                    }
                }
                stmts = exprCase.getStmt();
            } else if (case_ instanceof ADefaultCase) {
                // For the default case, we don't have any extra type-checking to do
                stmts = ((ADefaultCase) case_).getStmt();
            } else {
                throw new IllegalStateException("Unknown kind of case: " + case_.getClass());
            }
            // Type check the stmts of the case
            stmts.forEach(stmt -> stmt.apply(this));
            // Close the case context
            context = context.getParent();
        }
        // Close the switch context
        context = context.getParent();
    }

    @Override
    public void caseAIncrStmt(AIncrStmt node) {
        // Get the expression type
        final PExpr expr = node.getExpr();
        expr.apply(this);
        final Type type = exprNodeTypes.get(expr);
        // The type must resolve to numeric
        if (!type.resolve().isNumeric()) {
            throw new TypeCheckerException(expr, "Cannot use the operator " + BinaryOperator.ADD + " on the type " + type);
        }
    }

    @Override
    public void caseADecrStmt(ADecrStmt node) {
        // Get the expression type
        final PExpr expr = node.getExpr();
        expr.apply(this);
        final Type type = exprNodeTypes.get(expr);
        // The type must resolve to numeric
        if (!type.resolve().isNumeric()) {
            throw new TypeCheckerException(expr, "Cannot use the operator " + BinaryOperator.SUB + " on the type " + type);
        }
    }

    @Override
    public void caseAAssignMulStmt(AAssignMulStmt node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.MUL);
    }

    @Override
    public void caseAAssignDivStmt(AAssignDivStmt node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.DIV);
    }

    @Override
    public void caseAAssignRemStmt(AAssignRemStmt node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.REM);
    }

    @Override
    public void caseAAssignLshiftStmt(AAssignLshiftStmt node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.LSHIFT);
    }

    @Override
    public void caseAAssignRshiftStmt(AAssignRshiftStmt node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.RSHIFT);
    }

    @Override
    public void caseAAssignBitAndStmt(AAssignBitAndStmt node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.BIT_AND);
    }

    @Override
    public void caseAAssignBitAndNotStmt(AAssignBitAndNotStmt node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.BIT_AND_NOT);
    }

    @Override
    public void caseAAssignAddStmt(AAssignAddStmt node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.ADD);
    }

    @Override
    public void caseAAssignSubStmt(AAssignSubStmt node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.SUB);
    }

    @Override
    public void caseAAssignBitOrStmt(AAssignBitOrStmt node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.BIT_OR);
    }

    @Override
    public void caseAAssignBitXorStmt(AAssignBitXorStmt node) {
        typeCheckBinary(node, node.getLeft(), node.getRight(), BinaryOperator.BIT_XOR);
    }

    @Override
    public void caseAIdentExpr(AIdentExpr node) {
        // Resolve the symbol for the name
        final String name = node.getIdenf().getText();
        final Optional<Symbol<?>> optSymbol = context.lookupSymbol(name);
        if (!optSymbol.isPresent()) {
            throw new TypeCheckerException(node, "Undeclared symbol: " + name);
        }
        final Symbol<?> symbol = optSymbol.get();
        // If the symbol is a variable or function, add the type
        if (symbol instanceof Variable<?> || symbol instanceof Function) {
            exprNodeTypes.put(node, symbol.getType());
            identSymbols.put(node, symbol);
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
            throw new TypeCheckerException(node.getIndex(), "Not an int: " + valueType);
        }
        // The type is that of the component
        exprNodeTypes.put(node, ((IndexableType) valueType).getComponent());
    }

    @Override
    public void caseACallExpr(ACallExpr node) {
        typeCheckCall(node, true);
    }

    // Returns true if it is a cast instead of call
    private boolean typeCheckCall(ACallExpr node, boolean mustReturn) {
        // The call might be a cast. In this case the identifier will be a single symbol pointing to a type
        final PExpr value = node.getValue();
        if (value instanceof AIdentExpr) {
            final AIdentExpr identExpr = (AIdentExpr) value;
            final Optional<Symbol<?>> symbol = context.lookupSymbol((identExpr).getIdenf().getText());
            if (symbol.isPresent() && symbol.get() instanceof DeclaredType) {
                // It's a cast
                identSymbols.put(identExpr, symbol.get());
                typeCheckCast(node, symbol.get().getType());
                return true;
            }
        }
        // It's a call
        value.apply(this);
        final Type valueType = exprNodeTypes.get(value);
        // The value should be a function type
        if (!(valueType instanceof FunctionType)) {
            throw new TypeCheckerException(value, "Not a function type: " + valueType);
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
                    + " and in arguments for the call: " + parameters.size() + " != " + argTypes.size());
        }
        for (int i = 0; i < parameters.size(); i++) {
            final Type paramType = parameters.get(i).getType();
            if (!paramType.equals(argTypes.get(i))) {
                throw new TypeCheckerException(node.getArgs().get(i), "Cannot assign type " + argTypes.get(i) + " to type " + paramType);
            }
        }
        // The return type (if any) is the type of the expression
        if (functionType.getReturnType().isPresent()) {
            exprNodeTypes.put(node, functionType.getReturnType().get());
        }
        return false;
    }

    private void typeCheckCast(ACallExpr node, Type castType) {
        // Check that the resolved type can be used for a cast
        final Type resolvedCast = castType.resolve();
        if (!(resolvedCast instanceof BasicType)) {
            throw new TypeCheckerException(node.getValue(), "Cannot cast to non-basic type " + castType);
        }
        final BasicType basicCastType = (BasicType) resolvedCast;
        if (!basicCastType.canCastTo()) {
            throw new TypeCheckerException(node.getValue(), "Cannot cast to type " + castType);
        }
        // Get the argument types
        node.getArgs().forEach(arg -> arg.apply(this));
        final List<Type> argTypes = node.getArgs().stream().map(exprNodeTypes::get).collect(Collectors.toList());
        // There should only be one argument
        if (argTypes.size() != 1) {
            throw new TypeCheckerException(node, "Expected only one argument for a cast");
        }
        // Check that we can cast the argument to the cast type
        if (!basicCastType.canCastFrom(argTypes.get(0).resolve())) {
            throw new TypeCheckerException(node, "Cannot cast from " + argTypes.get(0) + " to type " + castType);
        }
        // The type is that of the cast, without the resolution
        exprNodeTypes.put(node, castType);
    }

    @Override
    public void caseAAppendExpr(AAppendExpr node) {
        // Check that the left argument is a slice type, after resolution
        node.getLeft().apply(this);
        final Type leftType = exprNodeTypes.get(node.getLeft());
        final Type resolvedLeftType = leftType.resolve();
        if (!(resolvedLeftType instanceof SliceType)) {
            throw new TypeCheckerException(node.getLeft(), "Not a slice type: " + leftType);
        }
        final SliceType sliceType = (SliceType) resolvedLeftType;
        // Check that the right argument has the same type as the slice component
        node.getRight().apply(this);
        final Type rightType = exprNodeTypes.get(node.getRight());
        if (!rightType.equals(sliceType.getComponent())) {
            throw new TypeCheckerException(node.getRight(), "Cannot append type " + rightType + " to type " + leftType);
        }
        // The type is the unresolved left type
        exprNodeTypes.put(node, leftType);
    }

    @Override
    public void caseALogicNotExpr(ALogicNotExpr node) {
        // Check that the inner type resolves to a bool
        final PExpr inner = node.getInner();
        inner.apply(this);
        final Type innerType = exprNodeTypes.get(inner);
        if (innerType.resolve() != BasicType.BOOL) {
            throw new TypeCheckerException(inner, "Not a bool: " + innerType);
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
        if (!innerType.resolve().isNumeric()) {
            throw new TypeCheckerException(inner, "Not a numeric type: " + innerType);
        }
        // The type is the same as the inner
        exprNodeTypes.put(node, innerType);
    }

    @Override
    public void caseABitNotExpr(ABitNotExpr node) {
        // Check that the inner type resolves to an integer
        final PExpr inner = node.getInner();
        inner.apply(this);
        final Type innerType = exprNodeTypes.get(inner);
        if (!innerType.resolve().isInteger()) {
            throw new TypeCheckerException(inner, "Not an integer type: " + innerType);
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

    private void typeCheckBinary(Node node, PExpr left, PExpr right, BinaryOperator operator) {
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
        if (node instanceof PExpr) {
            exprNodeTypes.put((PExpr) node, resultType);
        }
    }

    @Override
    public void caseANameType(ANameType node) {
        // Resolve the symbol for the name
        final String name = node.getIdenf().getText();
        final Optional<Symbol<?>> optSymbol = context.lookupSymbol(name);
        if (!optSymbol.isPresent()) {
            throw new TypeCheckerException(node.getIdenf(), "Undeclared symbol: " + name);
        }
        // Check that the symbol is a type
        final Symbol<?> symbol = optSymbol.get();
        if (!(symbol instanceof DeclaredType)) {
            throw new TypeCheckerException(node.getIdenf(), "Not a type: " + symbol);
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
        // The literal should be valid integer thanks to the grammar, but could be too big
        final int length;
        try {
            length = LiteralUtil.parseInt(node.getExpr());
        } catch (NumberFormatException exception) {
            throw new TypeCheckerException(node.getExpr(), "Signed integer overflow");
        }
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

    private enum BinaryOperator {
        MUL, DIV, REM, LSHIFT, RSHIFT, BIT_AND, BIT_AND_NOT, ADD, SUB, BIT_OR,
        BIT_XOR, EQ, NEQ, LESS, LESS_EQ, GREAT, GREAT_EQ, LOGIC_AND, LOGIC_OR
    }
}
