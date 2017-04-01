package golite.ir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import golite.analysis.AnalysisAdapter;
import golite.ir.node.Append;
import golite.ir.node.Assignment;
import golite.ir.node.BinArFloat64;
import golite.ir.node.BinArInt;
import golite.ir.node.BoolLit;
import golite.ir.node.Call;
import golite.ir.node.Cast;
import golite.ir.node.CmpBool;
import golite.ir.node.CmpFloat64;
import golite.ir.node.CmpInt;
import golite.ir.node.CmpInt.Op;
import golite.ir.node.CmpString;
import golite.ir.node.ConcatString;
import golite.ir.node.Expr;
import golite.ir.node.Float64Lit;
import golite.ir.node.FunctionDecl;
import golite.ir.node.Identifier;
import golite.ir.node.Indexing;
import golite.ir.node.IntLit;
import golite.ir.node.Jump;
import golite.ir.node.JumpCond;
import golite.ir.node.Label;
import golite.ir.node.LogicAnd;
import golite.ir.node.LogicNot;
import golite.ir.node.LogicOr;
import golite.ir.node.MemsetZero;
import golite.ir.node.PrintBool;
import golite.ir.node.PrintFloat64;
import golite.ir.node.PrintInt;
import golite.ir.node.PrintRune;
import golite.ir.node.PrintString;
import golite.ir.node.Program;
import golite.ir.node.Select;
import golite.ir.node.Stmt;
import golite.ir.node.StringLit;
import golite.ir.node.UnaArFloat64;
import golite.ir.node.UnaArInt;
import golite.ir.node.ValueReturn;
import golite.ir.node.VariableDecl;
import golite.ir.node.VoidReturn;
import golite.node.AAddExpr;
import golite.node.AAppendExpr;
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
import golite.node.AContinueStmt;
import golite.node.ADeclStmt;
import golite.node.ADeclVarShortStmt;
import golite.node.ADecrStmt;
import golite.node.ADivExpr;
import golite.node.AEmptyStmt;
import golite.node.AEqExpr;
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
import golite.node.ANegateExpr;
import golite.node.ANeqExpr;
import golite.node.APkg;
import golite.node.APrintStmt;
import golite.node.APrintlnStmt;
import golite.node.AProg;
import golite.node.AReaffirmExpr;
import golite.node.ARemExpr;
import golite.node.AReturnStmt;
import golite.node.ARshiftExpr;
import golite.node.ARuneExpr;
import golite.node.ASelectExpr;
import golite.node.AStringIntrExpr;
import golite.node.AStringRawExpr;
import golite.node.ASubExpr;
import golite.node.ASwitchStmt;
import golite.node.ATypeDecl;
import golite.node.AVarDecl;
import golite.node.Node;
import golite.node.PDecl;
import golite.node.PExpr;
import golite.node.PIfBlock;
import golite.node.PStmt;
import golite.node.Start;
import golite.node.TIdenf;
import golite.semantic.LiteralUtil;
import golite.semantic.SemanticData;
import golite.semantic.context.UniverseContext;
import golite.semantic.symbol.DeclaredType;
import golite.semantic.symbol.Function;
import golite.semantic.symbol.Symbol;
import golite.semantic.symbol.Variable;
import golite.semantic.type.ArrayType;
import golite.semantic.type.BasicType;
import golite.semantic.type.FunctionType;
import golite.semantic.type.IndexableType;
import golite.semantic.type.SliceType;
import golite.semantic.type.StructType;
import golite.semantic.type.StructType.Field;
import golite.semantic.type.Type;

/**
 *
 */
public class IrConverter extends AnalysisAdapter {
    private final SemanticData semantics;
    private Program convertedProgram;
    private final Map<AFuncDecl, Optional<FunctionDecl>> convertedFunctions = new HashMap<>();
    private final List<Stmt> functionStmts = new ArrayList<>();
    private final Map<PExpr, Expr<?>> convertedExprs = new HashMap<>();
    private final Map<Variable<?>, String> uniqueVarNames = new HashMap<>();
    private final Map<Label, String> uniqueLabelNames = new HashMap<>();

    public IrConverter(SemanticData semantics) {
        this.semantics = semantics;
    }

    public Program getProgram() {
        if (convertedProgram == null) {
            throw new IllegalStateException("The converter hasn't been applied yet");
        }
        return convertedProgram;
    }

    @Override
    public void caseStart(Start node) {
        node.getPProg().apply(this);
    }

    @Override
    public void caseAProg(AProg node) {
        // First we handle the global variables
        node.getDecl().stream().filter(decl -> decl instanceof AVarDecl).forEach(decl -> decl.apply(this));
        final List<VariableDecl<?>> globals = new ArrayList<>();
        // Variable declarations will create additional statements, which are put in a function called before the main
        final List<Stmt> staticInitialization = new ArrayList<>();
        for (Stmt stmt : functionStmts) {
            if (stmt instanceof VariableDecl<?>) {
                globals.add((VariableDecl<?>) stmt);
            } else {
                staticInitialization.add(stmt);
            }
        }
        staticInitialization.add(new VoidReturn());
        // Then we do the function declarations
        node.getDecl().stream().filter(decl -> decl instanceof AFuncDecl).forEach(decl -> decl.apply(this));
        final List<FunctionDecl> functions = new ArrayList<>();
        for (PDecl decl : node.getDecl()) {
            if (decl instanceof AFuncDecl) {
                convertedFunctions.get(decl).ifPresent(functions::add);
            }
        }
        // Add the static initialization function for the global variables
        final FunctionType staticInitType = new FunctionType(Collections.emptyList(), null);
        final Function staticInit = new Function(0, 0, 0, 0, "staticInit", staticInitType,
                Collections.emptyList());
        functions.add(new FunctionDecl(staticInit, Collections.emptyList(), staticInitialization, true));
        // Create the program
        final String packageName = ((APkg) node.getPkg()).getIdenf().getText();
        convertedProgram = new Program(packageName, globals, functions);
    }

    @Override
    public void caseAFuncDecl(AFuncDecl node) {
        // Ignore blank functions
        if (node.getIdenf().getText().equals("_")) {
            convertedFunctions.put(node, Optional.empty());
            return;
        }
        final Function symbol = semantics.getFunctionSymbol(node).get().dealias();
        // Find unique names for the parameters
        final List<String> uniqueParamNames = symbol.getParameters().stream()
                .map(this::findUniqueName).collect(Collectors.toList());
        // Type check the statements
        functionStmts.clear();
        uniqueLabelNames.clear();
        node.getStmt().forEach(stmt -> stmt.apply(this));
        // If the function has no return value and does not have a final return statement, then add one
        if (!symbol.getType().getReturnType().isPresent()
                && (functionStmts.isEmpty() || !(functionStmts.get(functionStmts.size() - 1) instanceof VoidReturn))) {
            functionStmts.add(new VoidReturn());
        }
        // Create the function and apply the flow sanitizer to help with codegen later on
        final FunctionDecl functionDecl = new FunctionDecl(symbol, uniqueParamNames, new ArrayList<>(functionStmts));
        IrFlowSanitizer.sanitize(functionDecl);
        convertedFunctions.put(node, Optional.of(functionDecl));
    }

    @Override
    public void caseAVarDecl(AVarDecl node) {
        // Two statements per variable declaration: declaration and initialization
        final List<Variable<?>> variables = semantics.getVariableSymbols(node).get();
        final List<TIdenf> idenfs = node.getIdenf();
        final List<PExpr> exprs = node.getExpr();
        for (int i = 0; i < idenfs.size(); i++) {
            final String variableName = idenfs.get(i).getText();
            // Ignore blank variables
            if (variableName.equals("_")) {
                continue;
            }
            // Find the symbol for the variable that was declared
            final Variable<?> variable = variables.stream()
                    .filter(var -> var.getName().equals(variableName))
                    .findFirst().get();
            // The declare it
            convertVariableDecl(functionStmts, variable, exprs.isEmpty() ? null : exprs.get(i));
        }
    }

    @Override
    public void caseADeclVarShortStmt(ADeclVarShortStmt node) {
        final List<Variable<?>> variables = semantics.getVariableSymbols(node).get();
        // First we handle the new variables, and create temporary variables for the assignments
        final List<PExpr> lefts = node.getLeft();
        final List<PExpr> rights = node.getRight();
        final List<Variable<?>> tmpVars = new ArrayList<>();
        for (int i = 0; i < lefts.size(); i++) {
            final Variable<?> variable = variables.get(i);
            if (variable == null) {
                // Just an assignment, so handle it the same way
                final PExpr right = rights.get(i);
                right.apply(this);
                createTmpAssignVar(functionStmts, tmpVars, convertedExprs.get(right));
            } else {
                // New variable declaration
                convertVariableDecl(functionStmts, variable, rights.get(i));
            }
        }
        // Then we assign the intermediate variables to the right expressions
        for (int i = 0, j = 0; i < lefts.size(); i++) {
            if (variables.get(i) != null) {
                // Not an assignment: skip it
                continue;
            }
            final PExpr left = lefts.get(i);
            left.apply(this);
            functionStmts.add(createAssignFromVar(convertedExprs.get(left), tmpVars.get(j++)));
        }
    }

    private <T extends Type> void convertVariableDecl(List<Stmt> stmts, Variable<T> variable, PExpr initializer) {
        // First dealias the variable
        variable = variable.dealias();
        // Find unique names for variables to prevent conflicts from removing scopes
        final String uniqueName = findUniqueName(variable);
        stmts.add(new VariableDecl<>(variable, uniqueName));
        // Then initialize the variables with assignments
        final Identifier<T> variableExpr = new Identifier<>(variable, uniqueName);
        final Stmt initialAssign;
        if (initializer == null) {
            // No explicit initializer, use a default
            initialAssign = defaultInitializer(variableExpr, variable.getType());
        } else {
            // Otherwise assign to the given value
            initializer.apply(this);
            initialAssign = new Assignment(variableExpr, convertedExprs.get(initializer));
        }
        stmts.add(initialAssign);
    }

    @Override
    public void caseATypeDecl(ATypeDecl node) {
        // Type declarations don't matter after type-checking
    }

    @Override
    public void caseAEmptyStmt(AEmptyStmt node) {
        // Nothing to do here
    }

    @Override
    public void caseAAssignStmt(AAssignStmt node) {
        final List<PExpr> lefts = node.getLeft();
        lefts.forEach(left -> left.apply(this));
        final List<PExpr> rights = node.getRight();
        rights.forEach(right -> right.apply(this));
        // For single assignments, we can just assign the right to the left immediately
        if (lefts.size() == 1) {
            final Expr<?> left = convertedExprs.get(lefts.get(0));
            final Expr<?> right = convertedExprs.get(rights.get(0));
            functionStmts.add(new Assignment(left, right));
            return;
        }
        // For multiple assignments, we first need to assign each right to a new intermediate variable
        final List<Variable<?>> tmpVars = new ArrayList<>();
        for (int i = 0; i < lefts.size(); i++) {
            createTmpAssignVar(functionStmts, tmpVars, convertedExprs.get(rights.get(i)));
        }
        // Then we assign the intermediate variables to the right expressions
        for (int i = 0; i < lefts.size(); i++) {
            functionStmts.add(createAssignFromVar(convertedExprs.get(lefts.get(i)), tmpVars.get(i)));
        }
    }

    private <T extends Type> void createTmpAssignVar(List<Stmt> stmts, List<Variable<?>> tmpVars, Expr<T> right) {
        final Variable<T> leftVar = newVariable(right.getType(), "assignTmp");
        tmpVars.add(leftVar);
        stmts.add(new VariableDecl<>(leftVar));
        final Identifier<T> left = new Identifier<>(leftVar);
        stmts.add(new Assignment(left, right));
    }

    private <T extends Type> Assignment createAssignFromVar(Expr<?> left, Variable<T> variable) {
        final Identifier<T> right = new Identifier<>(variable);
        return new Assignment(left, right);
    }

    @Override
    public void caseAPrintStmt(APrintStmt node) {
        convertPrintStmt(node.getExpr());
    }

    @Override
    public void caseAPrintlnStmt(APrintlnStmt node) {
        convertPrintStmt(node.getExpr());
        functionStmts.add(new PrintString(new StringLit(System.lineSeparator())));
    }

    private void convertPrintStmt(LinkedList<PExpr> exprs) {
        for (PExpr expr : exprs) {
            expr.apply(this);
            @SuppressWarnings("unchecked")
            final Expr<BasicType> converted = (Expr<BasicType>) convertedExprs.get(expr);
            final Type type = semantics.getExprType(expr).get().deepResolve();
            final Stmt printStmt;
            if (type == BasicType.BOOL) {
                printStmt = new PrintBool(converted);
            } else if (type == BasicType.INT) {
                printStmt = new PrintInt(converted);
            } else if (type == BasicType.RUNE) {
                printStmt = new PrintRune(converted);
            } else if (type == BasicType.FLOAT64) {
                printStmt = new PrintFloat64(converted);
            } else if (type == BasicType.STRING) {
                printStmt = new PrintString(converted);
            } else {
                throw new IllegalStateException("Unexpected type: " + type);
            }
            functionStmts.add(printStmt);
        }
    }

    @Override
    public void caseAReturnStmt(AReturnStmt node) {
        final Stmt return_;
        final PExpr value = node.getExpr();
        if (value == null) {
            return_ = new VoidReturn();
        } else {
            value.apply(this);
            return_ = new ValueReturn(convertedExprs.get(value));
        }
        functionStmts.add(return_);
    }

    @Override
    public void caseABreakStmt(ABreakStmt node) {
        // This should work by storing the end label of a "for" or "switch" statement in a stack
        // The statement then gets converted to an unconditional jump to the label on the top of the stack (peek)
        // This will be the end of the inner most "for" or "switch" statement
        // For a stack collection use Deque (ArrayDeque)
    }

    @Override
    public void caseAContinueStmt(AContinueStmt node) {
        // Same idea as break, but with a different stack that stores the beginning label of "for" loops
    }

    @Override
    public void caseABlockStmt(ABlockStmt node) {
        for (PStmt stmt : node.getStmt()) {
            stmt.apply(this);
        }
    }

    @Override
    public void caseAIfStmt(AIfStmt node) {
        final Label endLabel = newLabel("endIf");
        // Create one label per if-block
        final List<Label> ifLabels = new ArrayList<>();
        for (PIfBlock pIfBlock : node.getIfBlock()) {
            final AIfBlock ifBlock = (AIfBlock) pIfBlock;
            final Label ifLabel = newLabel("ifCase");
            ifLabels.add(ifLabel);
            // Convert the init, and create a jump to the label using the condition
            if (ifBlock.getInit() != null) {
                ifBlock.getInit().apply(this);
            }
            ifBlock.getCond().apply(this);
            @SuppressWarnings("unchecked")
            final Expr<BasicType> cond = (Expr<BasicType>) convertedExprs.get(ifBlock.getCond());
            functionStmts.add(new JumpCond(ifLabel, cond));
        }
        // Do the same for the else-block, but use an unconditional jump
        final Label elseLabel = newLabel("elseCase");
        functionStmts.add(new Jump(elseLabel));
        // Convert the body of each if-block, adding its label before, and a jump to the end after
        final List<PIfBlock> ifBlock = node.getIfBlock();
        for (int i = 0; i < ifBlock.size(); i++) {
            functionStmts.add(ifLabels.get(i));
            ((AIfBlock) ifBlock.get(i)).getBlock().forEach(stmt -> stmt.apply(this));
            functionStmts.add(new Jump(endLabel));
        }
        // Do the same for the else-block
        functionStmts.add(elseLabel);
        node.getElse().forEach(stmt -> stmt.apply(this));
        functionStmts.add(new Jump(endLabel));
        // End the if-stmt with the end label
        functionStmts.add(endLabel);
    }

    @Override
    public void caseASwitchStmt(ASwitchStmt node) {
        // A switch in the form
        /*
            switch init; expr {
                case a1, a2, a3, ...:
                    stmtsA
                case b1, b2, ...:
                    stmtsB
                default:
                    stmtsC
            }
        */
        // Is the same as
        /*
            init
            var e = expr
            if e == a1 || e == a2 || e == a3 || ... {
                stmtsA
            } else if e == b1 || e == b2 || ... {
                stmtsB
            } else {
                stmtsC
            }
        */
        // Note the new variable to prevent the expression from being re-evaluated in each if-condition
        // If there's no default case, then it's the same as no "else" block
        // So just convert this the like the "if" stmt
        // There's one difference: once the end label is created, it needs to be pushed in the stack described in the break stmt
        //     It must also be popped of once conversion is done
    }

    @Override
    public void caseAForStmt(AForStmt node) {
        // Start by converting the init statement (if it's a clause condition)
        // Then place a start label in the stmts, and allocate an end label (to be placed later)
        // Next we create a jump to the end label if the condition isn't true
        //     Convert the condition (if any), or use an unconditional jump for an empty condition
        //     You can negate an expression with LogicalNot
        // Push the labels in the stacks described in break and continue stmts
        // Then we convert the body of the for loop
        // If it's a clause condition then convert the post stmt here, at the end of the body
        // Finally add the end label to the stmts
        // Pop the labels from the stacks mentioned earlier
    }

    @Override
    public void caseADeclStmt(ADeclStmt node) {
        for (PDecl decl : node.getDecl()) {
            decl.apply(this);
        }
    }

    @Override
    public void caseAExprStmt(AExprStmt node) {
        node.getExpr().apply(this);
        // The IR nodes for expressions that are statements all implement the Stmt interface
        functionStmts.add((Stmt) convertedExprs.get(node.getExpr()));
    }

    @Override
    public void caseAIncrStmt(AIncrStmt node) {
        // Convert expr++ as expr = expr + IntLit(1)
    }

    @Override
    public void caseADecrStmt(ADecrStmt node) {
        // Convert expr-- as expr = expr - IntLit(1)
    }

    @Override
    public void caseAAssignMulStmt(AAssignMulStmt node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        final Expr<BasicType> mul = convertMul(left, right);
        functionStmts.add(new Assignment(left, mul));
    }

    @Override
    public void caseAAssignDivStmt(AAssignDivStmt node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        final Expr<BasicType> div = convertDiv(left, right);
        functionStmts.add(new Assignment(left, div));
    }

    @Override
    public void caseAAssignRemStmt(AAssignRemStmt node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        final Expr<BasicType> rem = convertRem(left, right);
        functionStmts.add(new Assignment(left, rem));
    }

    @Override
    public void caseAAssignLshiftStmt(AAssignLshiftStmt node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        final Expr<BasicType> lShift = convertLshift(left, right);
        functionStmts.add(new Assignment(left, lShift));
    }

    @Override
    public void caseAAssignRshiftStmt(AAssignRshiftStmt node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        final Expr<BasicType> rShift = convertRshift(left, right);
        functionStmts.add(new Assignment(left, rShift));
    }

    @Override
    public void caseAAssignBitAndStmt(AAssignBitAndStmt node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        final Expr<BasicType> bitAnd = convertBitAnd(left, right);
        functionStmts.add(new Assignment(left, bitAnd));
    }

    @Override
    public void caseAAssignBitAndNotStmt(AAssignBitAndNotStmt node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        final Expr<BasicType> bitAndNot = convertBitAndNot(left, right);
        functionStmts.add(new Assignment(left, bitAndNot));
    }

    @Override
    public void caseAAssignAddStmt(AAssignAddStmt node) { 
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        final Expr<BasicType> add = convertAdd(left, right);
        functionStmts.add(new Assignment(left, add));
    }

    @Override
    public void caseAAssignSubStmt(AAssignSubStmt node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        final Expr<BasicType> sub = convertSub(left, right);
        functionStmts.add(new Assignment(left, sub));
    }

    @Override
    public void caseAAssignBitOrStmt(AAssignBitOrStmt node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        final Expr<BasicType> bitOr = convertBitOr(left, right);
        functionStmts.add(new Assignment(left, bitOr));
    }

    @Override
    public void caseAAssignBitXorStmt(AAssignBitXorStmt node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        final Expr<BasicType> bitXor = convertBitXor(left, right);
        functionStmts.add(new Assignment(left, bitXor));
    }

    @Override
    public void caseAIntDecExpr(AIntDecExpr node) {
        convertedExprs.put(node, new IntLit(LiteralUtil.parseInt(node)));
    }

    @Override
    public void caseAIntOctExpr(AIntOctExpr node) {
        convertedExprs.put(node, new IntLit(LiteralUtil.parseInt(node)));
    }

    @Override
    public void caseAIntHexExpr(AIntHexExpr node) {
        convertedExprs.put(node, new IntLit(LiteralUtil.parseInt(node)));
    }

    @Override
    public void caseARuneExpr(ARuneExpr node) {
        final String text = node.getRuneLit().getText();
        char c = text.charAt(1);
        if (c == '\\') {
            c = decodeCharEscape(text.charAt(2), false);
        }
        convertedExprs.put(node, new IntLit(c));
    }

    @Override
    public void caseAFloatExpr(AFloatExpr node) {
        convertedExprs.put(node, new Float64Lit(Double.parseDouble(node.getFloatLit().getText())));
    }

    @Override
    public void caseAStringIntrExpr(AStringIntrExpr node) {
        final String text = node.getInterpretedStringLit().getText();
        // Remove surrounding quotes
        final String stringData = text.substring(1, text.length() - 1);
        convertedExprs.put(node, new StringLit(decodeStringContent(stringData)));
    }

    @Override
    public void caseAStringRawExpr(AStringRawExpr node) {
        final String text = node.getRawStringLit().getText();
        // Remove surrounding quotes
        final String stringData = text.substring(1, text.length() - 1);
        // Must remove \r characters according to the spec
        convertedExprs.put(node, new StringLit(stringData.replace("\r", "")));
    }

    @Override
    public void caseAIdentExpr(AIdentExpr node) {
        final Symbol<?> symbol = semantics.getIdentifierSymbol(node).get();
        if (!(symbol instanceof Variable)) {
            throw new IllegalStateException("Non-variable identifiers should have been handled earlier");
        }
        final Variable<?> variable = ((Variable<?>) symbol).dealias();
        final Expr<?> expr;
        // Special case for the pre-declared booleans identifiers: convert them to bool literals
        if (variable.equals(UniverseContext.TRUE_VARIABLE)) {
            expr = new BoolLit(true);
        } else if (variable.equals(UniverseContext.FALSE_VARIABLE)) {
            expr = new BoolLit(false);
        } else {
            expr = new Identifier<>(variable, uniqueVarNames.get(variable));
        }
        convertedExprs.put(node, expr);
    }

    @Override
    public void caseASelectExpr(ASelectExpr node) {
        final PExpr value = node.getValue();
        value.apply(this);
        @SuppressWarnings("unchecked")
        final Expr<StructType> convertedValue = (Expr<StructType>) convertedExprs.get(value);
        convertedExprs.put(node, new Select(convertedValue, node.getIdenf().getText()));
    }

    @Override
    public void caseAIndexExpr(AIndexExpr node) {
        node.getValue().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<IndexableType> value = (Expr<IndexableType>) convertedExprs.get(node.getValue());
        node.getIndex().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> index = (Expr<BasicType>) convertedExprs.get(node.getIndex());
        convertedExprs.put(node, new Indexing<>(value, index));
    }

    @Override
    public void caseACallExpr(ACallExpr node) {
        // Convert the arguments
        final List<PExpr> args = node.getArgs();
        args.forEach(arg -> arg.apply(this));
        // The called values will always be an identifier
        final AIdentExpr callIdent = (AIdentExpr) node.getValue();
        // Get the symbol being called (function or type)
        final Symbol<?> symbol = semantics.getIdentifierSymbol(callIdent).get();
        if (symbol instanceof DeclaredType) {
            // Cast
            if (args.size() != 1) {
                throw new IllegalStateException("Expected only one argument in the cast");
            }
            @SuppressWarnings("unchecked")
            final Expr<BasicType> convertedArg = (Expr<BasicType>) convertedExprs.get(args.get(0));
            final BasicType castType = (BasicType) symbol.getType().deepResolve();
            convertedExprs.put(node, new Cast(castType, convertedArg));
            return;
        }
        if (symbol instanceof Function) {
            // Call
            final List<Expr<?>> convertedArgs = args.stream().map(convertedExprs::get).collect(Collectors.toList());
            convertedExprs.put(node, new Call(((Function) symbol).dealias(), convertedArgs));
            return;
        }
        throw new IllegalStateException("Unexpected call to symbol type: " + symbol.getClass());
    }

    @Override
    public void caseAAppendExpr(AAppendExpr node) {
        node.getLeft().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<SliceType> left = (Expr<SliceType>) convertedExprs.get(node.getLeft());
        node.getRight().apply(this);
        final Expr<?> right = convertedExprs.get(node.getRight());
        convertedExprs.put(node, new Append(left, right));
    }

    @Override
    public void caseALogicNotExpr(ALogicNotExpr node) {
        node.getInner().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> inner = (Expr<BasicType>) convertedExprs.get(node.getInner());
        convertedExprs.put(node, new LogicNot(inner));
    }

    @Override
    public void caseAReaffirmExpr(AReaffirmExpr node) {
        node.getInner().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> inner = (Expr<BasicType>) convertedExprs.get(node.getInner());
        if (inner.getType() == BasicType.FLOAT64) {
            convertedExprs.put(node, new UnaArFloat64(inner, UnaArFloat64.Op.NOP));
        } else if (inner.getType() == BasicType.INT) {
            convertedExprs.put(node, new UnaArInt(inner, UnaArInt.Op.NOP));
        } else {
            throw new IllegalStateException("Expected integer or float64-typed expressions");
        }
    }

    @Override
    public void caseANegateExpr(ANegateExpr node) {
        node.getInner().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> inner = (Expr<BasicType>) convertedExprs.get(node.getInner());
        if (inner.getType() == BasicType.FLOAT64) {
            convertedExprs.put(node, new UnaArFloat64(inner, UnaArFloat64.Op.NEG));
        } else if (inner.getType() == BasicType.INT) {
            convertedExprs.put(node, new UnaArInt(inner, UnaArInt.Op.NEG));
        } else {
            throw new IllegalStateException("Expected integer or float64-typed expressions");
        }
    }

    @Override
    public void caseABitNotExpr(ABitNotExpr node) {
        node.getInner().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> inner = (Expr<BasicType>) convertedExprs.get(node.getInner());
        if (inner.getType() == BasicType.INT) {
            convertedExprs.put(node, new UnaArInt(inner, UnaArInt.Op.BIT_NEG));
        } else {
            throw new IllegalStateException("Expected integer or float64-typed expressions");
        }
    }

    @Override
    public void caseAMulExpr(AMulExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        convertedExprs.put(node, convertMul(left, right));
    }
    
    
    private Expr<BasicType> convertMul(Expr<BasicType> left, Expr<BasicType> right) {        
        if (left.getType() == BasicType.INT) {
        	return new BinArInt(left, right, BinArInt.Op.MUL);
        } else if (left.getType() == BasicType.FLOAT64) {
        	return new BinArFloat64(left, right, BinArFloat64.Op.MUL);
        } else {
            throw new IllegalStateException("Expected integer or float64-typed expressions");
        }
    }

    @Override
    public void caseADivExpr(ADivExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        convertedExprs.put(node, convertDiv(left, right));
    }
        
    private Expr<BasicType> convertDiv(Expr<BasicType> left, Expr<BasicType> right) {
        if (left.getType() == BasicType.FLOAT64) {
        	return new BinArFloat64(left, right, BinArFloat64.Op.DIV);
        } else if (left.getType() == BasicType.INT) {
        	return new BinArInt(left, right, BinArInt.Op.DIV);
        } else {
            throw new IllegalStateException("Expected integer or float64-typed expressions");
        }
    }

    @Override
    public void caseARemExpr(ARemExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        convertedExprs.put(node, convertRem(left, right));
    }

    private Expr<BasicType> convertRem(Expr<BasicType> left, Expr<BasicType> right) {
        if (left.getType() == BasicType.INT) {
            return new BinArInt(left, right, BinArInt.Op.REM);
        } else {
            throw new IllegalStateException("Expected integer-typed expressions");
        }
    }

    @Override
    public void caseALshiftExpr(ALshiftExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        convertedExprs.put(node, convertLshift(left, right));
    }
        
    private Expr<BasicType> convertLshift(Expr<BasicType> left, Expr<BasicType> right) {
        if (left.getType() == BasicType.INT) {
        	return new BinArInt(left, right, BinArInt.Op.LSHIFT);
        } else {
            throw new IllegalStateException("Expected integer-typed expressions");
        }
    }

    @Override
    public void caseARshiftExpr(ARshiftExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        convertedExprs.put(node, convertRshift(left, right));
    }
        
    private Expr<BasicType> convertRshift(Expr<BasicType> left, Expr<BasicType> right) {
        if (left.getType() == BasicType.INT) {
        	return new BinArInt(left, right, BinArInt.Op.RSHIFT);
        } else {
            throw new IllegalStateException("Expected integer-typed expressions");
        }
    }

    @Override
    public void caseABitAndExpr(ABitAndExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        convertedExprs.put(node, convertBitAnd(left, right));
    }
    

    private Expr<BasicType> convertBitAnd(Expr<BasicType> left, Expr<BasicType> right) {    
        if (left.getType() == BasicType.INT) {
            return new BinArInt(left, right, BinArInt.Op.BIT_AND);
        } else {
            throw new IllegalStateException("Expected integer-typed expressions");
        }
    }

    @Override
    public void caseABitAndNotExpr(ABitAndNotExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        convertedExprs.put(node, convertBitAndNot(left, right));
        
    }
    
    private Expr<BasicType> convertBitAndNot(Expr<BasicType> left, Expr<BasicType> right) {  
        if (left.getType() == BasicType.INT) {
            return new BinArInt(left, right, BinArInt.Op.BIT_AND_NOT);
        } else {
            throw new IllegalStateException("Expected integer-typed expressions");
        }
    }

    @Override
    public void caseAAddExpr(AAddExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        convertedExprs.put(node, convertAdd(left, right));
    }
    

    private Expr<BasicType> convertAdd (Expr<BasicType> left, Expr<BasicType> right) {   
        if (left.getType() == BasicType.STRING) {
        	return new ConcatString(left, right);
           
        }
        if (left.getType() == BasicType.FLOAT64) {
        	return new BinArFloat64(left, right, BinArFloat64.Op.ADD);
        } else if (left.getType() == BasicType.INT) {
        	return new BinArInt(left, right, BinArInt.Op.ADD);
        } else {
            throw new IllegalStateException("Expected integer, float64, or string-typed expressions");
        }
    }

    @Override
    public void caseASubExpr(ASubExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        convertedExprs.put(node, convertSub(left, right));
    }
        
    private Expr<BasicType> convertSub(Expr<BasicType> left, Expr<BasicType> right) {
        if (left.getType() == BasicType.FLOAT64) {
        	return new BinArFloat64(left, right, BinArFloat64.Op.SUB);
        } else if (left.getType() == BasicType.INT) {
        	return new BinArInt(left, right, BinArInt.Op.SUB);
        } else {
            throw new IllegalStateException("Expected integer or float64-typed expressions");
        }
    }

    @Override
    public void caseABitOrExpr(ABitOrExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        convertedExprs.put(node, convertBitOr(left, right));
    }
     
    
    private Expr<BasicType> convertBitOr(Expr<BasicType> left, Expr<BasicType> right) {
        if (left.getType() == BasicType.INT) {
            return new BinArInt(left, right, BinArInt.Op.BIT_OR);
        } else {
            throw new IllegalStateException("Expected integer-typed expressions");
        }
    }

    @Override
    public void caseABitXorExpr(ABitXorExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        convertedExprs.put(node, convertBitXor(left, right));
    }
     
    
    private Expr<BasicType> convertBitXor(Expr<BasicType> left, Expr<BasicType> right) {
        if (left.getType() == BasicType.INT) {
        	return new BinArInt(left, right, BinArInt.Op.BIT_XOR);
        } else {
            throw new IllegalStateException("Expected integer-typed expressions");
        }
    }

    @Override
    public void caseAEqExpr(AEqExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        final Expr<?> left = convertedExprs.get(node.getLeft());
        final Expr<?> right = convertedExprs.get(node.getRight());
        final Expr<BasicType> equal = convertEqual(false, left, right);
        convertedExprs.put(node, equal);
    }

    @Override
    public void caseANeqExpr(ANeqExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        final Expr<?> left = convertedExprs.get(node.getLeft());
        final Expr<?> right = convertedExprs.get(node.getRight());
        final Expr<BasicType> equal = convertEqual(true, left, right);
        convertedExprs.put(node, equal);
    }

    private Expr<BasicType> convertEqual(boolean not, Expr<?> left, Expr<?> right) {
        final Type leftType = left.getType();
        if (leftType == BasicType.BOOL) {
            @SuppressWarnings("unchecked")
            final CmpBool equal = new CmpBool((Expr<BasicType>) left, (Expr<BasicType>) right,
                    not ? CmpBool.Op.NEQ : CmpBool.Op.EQ);
            return equal;
        }
        if (leftType.isInteger()) {
            @SuppressWarnings("unchecked")
            final CmpInt equal = new CmpInt((Expr<BasicType>) left, (Expr<BasicType>) right,
                    not ? CmpInt.Op.NEQ : CmpInt.Op.EQ);
            return equal;
        }
        if (leftType == BasicType.FLOAT64) {
            @SuppressWarnings("unchecked")
            final CmpFloat64 equal = new CmpFloat64((Expr<BasicType>) left, (Expr<BasicType>) right,
                    not ? CmpFloat64.Op.NEQ : CmpFloat64.Op.EQ);
            return equal;
        }
        if (leftType == BasicType.STRING) {
            @SuppressWarnings("unchecked")
            final CmpString equal = new CmpString((Expr<BasicType>) left, (Expr<BasicType>) right,
                    not ? CmpString.Op.NEQ : CmpString.Op.EQ);
            return equal;
        }
        if (leftType instanceof ArrayType) {
            // Arrays need to be compared element per element (memcmp does not work with structs, bools, floats or padding data)
            final ArrayType arrayType = (ArrayType) leftType;
            // arrLeft := left expr
            final Variable<ArrayType> arrLeftVar = newVariable(arrayType, "arrLeft");
            final VariableDecl<ArrayType> arrLeft = new VariableDecl<>(arrLeftVar);
            final Assignment arrLeftInit = new Assignment(new Identifier<>(arrLeftVar), left);
            // arrRight := right expr
            final Variable<ArrayType> arrRightVar = newVariable(arrayType, "arrRight");
            final VariableDecl<ArrayType> arrRight = new VariableDecl<>(arrRightVar);
            final Assignment arrRightInit = new Assignment(new Identifier<>(arrRightVar), right);
            // arrEq := true
            final Variable<BasicType> equalVar = newVariable(BasicType.BOOL, "arrEq");
            final VariableDecl<BasicType> equal = new VariableDecl<>(equalVar);
            final Assignment equalInit = new Assignment(new Identifier<>(equalVar), new BoolLit(true));
            // arrIdx := 0
            final Variable<BasicType> indexVar = newVariable(BasicType.INT, "arrIdx");
            final VariableDecl<BasicType> index = new VariableDecl<>(indexVar);
            final Assignment indexInit = new Assignment(new Identifier<>(indexVar), new IntLit(0));
            // Labels at the start and end of the loop body
            final Label startLabel = newLabel("startLoop");
            final Label endLabel = newLabel("endLoop");
            // Jump to end label if arrIdx >= arrayLength || !arrEq
            final CmpInt cmpIndex = new CmpInt(new Identifier<>(indexVar), new IntLit(arrayType.getLength()), Op.GREAT_EQ);
            final LogicOr loopCondition = new LogicOr(cmpIndex, new LogicNot(new Identifier<>(equalVar)));
            final JumpCond loopEntry = new JumpCond(endLabel, loopCondition);
            // Add all the statements so far to the list
            functionStmts.addAll(Arrays.asList(
                    arrLeft, arrLeftInit,
                    arrRight, arrRightInit,
                    equal, equalInit,
                    index, indexInit,
                    startLabel,
                    loopEntry
            ));
            // arrEq = equals(arr1[arrIdx], arr2[arrIdx])
            @SuppressWarnings("unchecked")
            final Indexing<?> leftComp = new Indexing<>(new Identifier<>(arrLeftVar), new Identifier<>(indexVar));
            @SuppressWarnings("unchecked")
            final Indexing<?> rightComp = new Indexing<>(new Identifier<>(arrRightVar), new Identifier<>(indexVar));
            final Expr<BasicType> compEqual = convertEqual(false, leftComp, rightComp);
            final Assignment updateEqual = new Assignment(new Identifier<>(equalVar), compEqual);
            // arrIdx++
            final BinArInt add1Idx = new BinArInt(new Identifier<>(indexVar), new IntLit(1), BinArInt.Op.ADD);
            final Assignment incrIdx = new Assignment(new Identifier<>(indexVar), add1Idx);
            // Jump to start of loop
            final Jump loopBack = new Jump(startLabel);
            // Add all the statements so far to the list
            functionStmts.addAll(Arrays.asList(
                    updateEqual,
                    incrIdx,
                    loopBack,
                    endLabel
            ));
            // Negate the result if needed
            final Expr<BasicType> result;
            if (not) {
                result = new LogicNot(new Identifier<>(equalVar));
            } else {
                result = new Identifier<>(equalVar);
            }
            return result;
        }
        if (leftType instanceof StructType) {
            // Same idea with structs, except that we compare fields (but not _ fields)
            final StructType structType = (StructType) leftType;
            // structLeft := left expr
            final Variable<StructType> structLeftVar = newVariable(structType, "structLeft");
            final VariableDecl<StructType> structLeft = new VariableDecl<>(structLeftVar);
            final Assignment structLeftInit = new Assignment(new Identifier<>(structLeftVar), left);
            // structRight := right expr
            final Variable<StructType> structRightVar = newVariable(structType, "structRight");
            final VariableDecl<StructType> structRight = new VariableDecl<>(structRightVar);
            final Assignment structRightInit = new Assignment(new Identifier<>(structRightVar), right);
            // structEq := true
            final Variable<BasicType> equalVar = newVariable(BasicType.BOOL, "structEq");
            final VariableDecl<BasicType> equal = new VariableDecl<>(equalVar);
            final Assignment equalInit = new Assignment(new Identifier<>(equalVar), new BoolLit(true));
            // Add all the statements so far to the list
            functionStmts.addAll(Arrays.asList(
                    structLeft, structLeftInit,
                    structRight, structRightInit,
                    equal, equalInit
            ));
            // Labels to structEq = false and to the end
            final Label notEqualLabel = newLabel("structNeq");
            final Label endLabel = newLabel("endStructEq");
            // Compare each field
            for (Field field : structType.getFields()) {
                final String fieldName = field.getName();
                if (fieldName.equals("_")) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                final Select leftField = new Select(new Identifier<>(structLeftVar), fieldName);
                @SuppressWarnings("unchecked")
                final Select rightField = new Select(new Identifier<>(structRightVar), fieldName);
                final Expr<BasicType> compEqual = new LogicNot(convertEqual(false, leftField, rightField));
                // Jump to not equal if !equals(struct1.field, struct2.field)
                final JumpCond notEqualJump = new JumpCond(notEqualLabel, compEqual);
                // Add the statement to the list
                functionStmts.add(notEqualJump);
            }
            // Jump to end
            final Jump equalJump = new Jump(endLabel);
            // structEq := false
            final Assignment updateNotEqual = new Assignment(new Identifier<>(equalVar), new BoolLit(false));
            // Add all the statements so far to the list
            functionStmts.addAll(Arrays.asList(
                    equalJump,
                    notEqualLabel,
                    updateNotEqual,
                    endLabel
            ));
            // Negate the result if needed
            final Expr<BasicType> result;
            if (not) {
                result = new LogicNot(new Identifier<>(equalVar));
            } else {
                result = new Identifier<>(equalVar);
            }
            return result;
        }
        throw new UnsupportedOperationException(leftType.toString());
    }

    @Override
    public void caseALessExpr(ALessExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        if (left.getType() == BasicType.STRING) {
            convertedExprs.put(node, new CmpString(left, right, CmpString.Op.LESS));
            return;
        }
        if (left.getType() == BasicType.FLOAT64) {
            convertedExprs.put(node, new CmpFloat64(left, right, CmpFloat64.Op.LESS));
        } else if (left.getType() == BasicType.INT) {
            convertedExprs.put(node, new CmpInt(left, right, CmpInt.Op.LESS));
        } else {
            throw new IllegalStateException("Expected integer, string or float64-typed expressions");
        }
    }

    @Override
    public void caseALessEqExpr(ALessEqExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        if (left.getType() == BasicType.STRING) {
            convertedExprs.put(node, new CmpString(left, right, CmpString.Op.LESS_EQ));
            return;
        }
        if (left.getType() == BasicType.FLOAT64) {
            convertedExprs.put(node, new CmpFloat64(left, right, CmpFloat64.Op.LESS_EQ));
        } else if (left.getType() == BasicType.INT) {
            convertedExprs.put(node, new CmpInt(left, right, CmpInt.Op.LESS_EQ));
        } else {
            throw new IllegalStateException("Expected integer, string or float64-typed expressions");
        }
    }

    @Override
    public void caseAGreatExpr(AGreatExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        if (left.getType() == BasicType.STRING) {
            convertedExprs.put(node, new CmpString(left, right, CmpString.Op.GREAT));
            return;
        }
        if (left.getType() == BasicType.FLOAT64) {
            convertedExprs.put(node, new CmpFloat64(left, right, CmpFloat64.Op.GREAT));
        } else if (left.getType() == BasicType.INT) {
            convertedExprs.put(node, new CmpInt(left, right, CmpInt.Op.GREAT));
        } else {
            throw new IllegalStateException("Expected integer, string or float64-typed expressions");
        }
    }

    @Override
    public void caseAGreatEqExpr(AGreatEqExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        if (left.getType() == BasicType.STRING) {
            convertedExprs.put(node, new CmpString(left, right, CmpString.Op.GREAT_EQ));
            return;
        }
        if (left.getType() == BasicType.FLOAT64) {
            convertedExprs.put(node, new CmpFloat64(left, right, CmpFloat64.Op.GREAT_EQ));
        } else if (left.getType() == BasicType.INT) {
            convertedExprs.put(node, new CmpInt(left, right, CmpInt.Op.GREAT_EQ));
        } else {
            throw new IllegalStateException("Expected integer, string or float64-typed expressions");
        }
    }

    @Override
    public void caseALogicAndExpr(ALogicAndExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        final Expr<BasicType> and = new LogicAnd(left, right);
        convertedExprs.put(node, and);
    }

    @Override
    public void caseALogicOrExpr(ALogicOrExpr node) {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> left = (Expr<BasicType>) convertedExprs.get(node.getLeft());
        @SuppressWarnings("unchecked")
        final Expr<BasicType> right = (Expr<BasicType>) convertedExprs.get(node.getRight());
        final Expr<BasicType> and = new LogicOr(left, right);
        convertedExprs.put(node, and);
    }

    @Override
    public void defaultCase(Node node) {
    }

    private String findUniqueName(Variable<?> variable) {
        if (uniqueVarNames.containsKey(variable)) {
            throw new IllegalStateException("This method should not have been called twice for the same variable");
        }
        String uniqueName = findUniqueName(variable.getName(), uniqueVarNames.values());
        uniqueVarNames.put(variable, uniqueName);
        return uniqueName;
    }

    private String findUniqueName(String name, Collection<String> names) {
        String uniqueName = name;
        int id = 0;
        boolean isUnique;
        do {
            isUnique = true;
            for (String existingName : names) {
                if (uniqueName.equals(existingName)) {
                    isUnique = false;
                    uniqueName = name + id;
                    id++;
                }
            }
        } while (!isUnique);
        return uniqueName;
    }

    private <T extends Type> Variable<T> newVariable(T type, String name) {
        final String uniqueName = findUniqueName(name, uniqueVarNames.values());
        final Variable<T> variable = new Variable<>(0, 0, 0, 0, uniqueName, type);
        uniqueVarNames.put(variable, uniqueName);
        return variable;
    }

    private Label newLabel(String name) {
        final String uniqueName = findUniqueName(name, uniqueLabelNames.values());
        final Label label = new Label(uniqueName);
        uniqueLabelNames.put(label, uniqueName);
        return label;
    }

    private static <T extends Type> Stmt defaultInitializer(Identifier<T> variableExpr, T type) {
        final Type resolved = type.deepResolve();
        if (resolved.isInteger()) {
            return new Assignment(variableExpr, new IntLit(0));
        }
        if (resolved == BasicType.FLOAT64) {
            return new Assignment(variableExpr, new Float64Lit(0));
        }
        if (resolved == BasicType.BOOL) {
            return new Assignment(variableExpr, new BoolLit(false));
        }
        if (resolved == BasicType.STRING || resolved instanceof IndexableType || resolved instanceof StructType) {
            return new MemsetZero(variableExpr);
        }
        throw new UnsupportedOperationException("Unsupported type: " + resolved);
    }

    private static String decodeStringContent(String data) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < data.length(); ) {
            char c = data.charAt(i);
            i += 1;
            if (c == '\\') {
                c = data.charAt(i);
                i += 1;
                c = decodeCharEscape(c, true);
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private static char decodeCharEscape(char c, boolean inString) {
        if (inString && c == '"') {
            return '"';
        } else if (!inString && c == '\'') {
            return '\'';
        }
        final Character decode = CHAR_TO_ESCAPE.get(c);
        if (decode == null) {
            throw new IllegalStateException("Not an escape character " + Character.getName(c));
        }
        return decode;
    }

    private static final Map<Character, Character> CHAR_TO_ESCAPE;

    static {
        CHAR_TO_ESCAPE = new HashMap<>();
        CHAR_TO_ESCAPE.put('\\', (char) 0x5c);
        CHAR_TO_ESCAPE.put('a', (char) 0x7);
        CHAR_TO_ESCAPE.put('b', '\b');
        CHAR_TO_ESCAPE.put('f', '\f');
        CHAR_TO_ESCAPE.put('n', '\n');
        CHAR_TO_ESCAPE.put('r', '\r');
        CHAR_TO_ESCAPE.put('t', '\t');
        CHAR_TO_ESCAPE.put('v', (char) 0xb);
        // You might be wondering why not use the \\uXXXX notation instead of casting int to char?
        // Well go ahead and remove that second    ^ backslash. Java is a special boy.
    }
}
