/*
 * This file is part of GoLite, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2017 Aleksi Sapon, Rohit Verma, Ayesha Krishnamurthy <https://github.com/DDoS/Golite>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package golite.ir;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
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
import golite.node.AClauseForCondition;
import golite.node.AContinueStmt;
import golite.node.ADeclStmt;
import golite.node.ADeclVarShortStmt;
import golite.node.ADecrStmt;
import golite.node.ADefaultCase;
import golite.node.ADivExpr;
import golite.node.AEmptyStmt;
import golite.node.AEnclosedExpr;
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
import golite.node.PCase;
import golite.node.PDecl;
import golite.node.PExpr;
import golite.node.PForCondition;
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
    private final Deque<Label> loopContinueLabels = new ArrayDeque<>();
    private final Deque<Label> flowBreakLabels = new ArrayDeque<>();

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
        // Add an empty main if none was declared
        final FunctionType noParamNoReturnFunc = new FunctionType(Collections.emptyList(), null);
        if (functions.stream().noneMatch(FunctionDecl::isMain)) {
            final Function main = new Function(0, 0, 0, 0, "main", noParamNoReturnFunc,
                    Collections.emptyList());
            functions.add(new FunctionDecl(main, Collections.emptyList(), Collections.singletonList(new VoidReturn()), false));
        }
        // Add the static initialization function for the global variables
        final Function staticInit = new Function(0, 0, 0, 0, "staticInit", noParamNoReturnFunc,
                Collections.emptyList());
        final FunctionDecl staticInitFunction = new FunctionDecl(staticInit, Collections.emptyList(), staticInitialization, true);
        IrFlowSanitizer.sanitize(staticInitFunction);
        functions.add(staticInitFunction);
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
            // For blank variables, we create a temporary one if it has an initializer, so it gets evaluated
            if (variableName.equals("_")) {
                if (!exprs.isEmpty()) {
                    final PExpr right = exprs.get(i);
                    right.apply(this);
                    createTmpAssignVar(functionStmts, convertedExprs.get(right));
                }
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
        int variableIndex = 0;
        for (PExpr leftExpr : lefts) {
            final AIdentExpr left = (AIdentExpr) leftExpr;
            // We handle blank variables like assignments, so the the right expression is evaluated
            final boolean isBlank = left.getIdenf().getText().equals("_");
            final Variable<?> variable = isBlank ? null : variables.get(variableIndex);
            if (variable == null) {
                // Just an assignment, so handle it the same way
                final PExpr right = rights.get(variableIndex);
                right.apply(this);
                tmpVars.add(createTmpAssignVar(functionStmts, convertedExprs.get(right)));
            } else {
                // New variable declaration
                convertVariableDecl(functionStmts, variable, rights.get(variableIndex));
            }
            // Blanks don't have variables, so don't increment the index
            if (!isBlank) {
                variableIndex++;
            }
        }
        // Then we assign the intermediate variables to the right expressions
        variableIndex = 0;
        int tempVariableIndex = 0;
        for (PExpr leftExpr : lefts) {
            final AIdentExpr left = (AIdentExpr) leftExpr;
            // Ignore blank identifiers, since they don't have any variable
            if (left.getIdenf().getText().equals("_")) {
                continue;
            }
            // We only need to assign the temporary variable to the right expression
            if (variables.get(variableIndex++) == null) {
                left.apply(this);
                functionStmts.add(createAssignFromVar(convertedExprs.get(left), tmpVars.get(tempVariableIndex++)));
            }
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
            tmpVars.add(createTmpAssignVar(functionStmts, convertedExprs.get(rights.get(i))));
        }
        // Then we assign the intermediate variables to the right expressions
        for (int i = 0; i < lefts.size(); i++) {
            functionStmts.add(createAssignFromVar(convertedExprs.get(lefts.get(i)), tmpVars.get(i)));
        }
    }

    private <T extends Type> Variable<T> createTmpAssignVar(List<Stmt> stmts, Expr<T> right) {
        final Variable<T> leftVar = newVariable(right.getType(), "assignTmp");
        stmts.add(new VariableDecl<>(leftVar));
        final Identifier<T> left = new Identifier<>(leftVar);
        stmts.add(new Assignment(left, right));
        return leftVar;
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
            } else if (type.isInteger()) {
                printStmt = new PrintInt(converted);
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
        functionStmts.add(new Jump(flowBreakLabels.peek()));
    }

    @Override
    public void caseAContinueStmt(AContinueStmt node) {
        functionStmts.add(new Jump(loopContinueLabels.peek()));
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
        // Convert the init statement, which is the first one
        if (node.getInit() != null) {
            node.getInit().apply(this);
        }
        // Convert the condition, or use "true" if missing
        final Expr<BasicType> condition;
        if (node.getValue() != null) {
            node.getValue().apply(this);
            @SuppressWarnings("unchecked")
            final Expr<BasicType> basicExpr = (Expr<BasicType>) convertedExprs.get(node.getValue());
            condition = basicExpr;
        } else {
            condition = new BoolLit(true);
        }
        // Assign the condition to a temporary variable to prevent re-evaluation on each case
        final Variable<BasicType> switchVar = newVariable(condition.getType(), "switchVar");
        final VariableDecl<BasicType> switchValue = new VariableDecl<>(switchVar);
        final Assignment switchValueInit = new Assignment(new Identifier<>(switchVar), condition);
        functionStmts.add(switchValue);
        functionStmts.add(switchValueInit);
        // For each (non-default) case, create conditional jumps for each equality check
        final List<Label> caseLabels = new ArrayList<>();
        for (PCase pCase : node.getCase()) {
            if (pCase instanceof ADefaultCase) {
                continue;
            }
            final AExprCase case_ = (AExprCase) pCase;
            // Add one label per case
            final Label caseLabel = newLabel("ifCase");
            caseLabels.add(caseLabel);
            for (PExpr expr : case_.getExpr()) {
                // Add a jump to the same label for each expr in a case
                expr.apply(this);
                @SuppressWarnings("unchecked")
                final Expr<BasicType> caseValue = (Expr<BasicType>) convertedExprs.get(expr);
                final Expr<BasicType> equal = convertEqual(false, new Identifier<>(switchVar), caseValue);
                functionStmts.add(new JumpCond(caseLabel, equal));
            }
        }
        // Use an unconditional jump for the default case
        final Label defaultLabel = newLabel("elseCase");
        functionStmts.add(new Jump(defaultLabel));
        // Create the end label, and add it to then end labels for using in conversion of break statements
        final Label endLabel = newLabel("endIf");
        flowBreakLabels.push(endLabel);
        // Convert the body of each case
        boolean implicitDefault = true;
        int labelIndex = 0;
        for (PCase pCase : node.getCase()) {
            // Start by placing the label
            final List<PStmt> stmts;
            if (pCase instanceof AExprCase) {
                functionStmts.add(caseLabels.get(labelIndex++));
                stmts = ((AExprCase) pCase).getStmt();
            } else {
                implicitDefault = false;
                functionStmts.add(defaultLabel);
                stmts = ((ADefaultCase) pCase).getStmt();
            }
            // Then convert the body of the case
            stmts.forEach(stmt -> stmt.apply(this));
            // Finally end with the end label
            functionStmts.add(new Jump(endLabel));
        }
        // If the default case is implicit (missing), then make it explicit and empty (makes codegen easier)
        if (implicitDefault) {
            functionStmts.add(defaultLabel);
            functionStmts.add(new Jump(endLabel));
        }
        // The last statement is the end label
        functionStmts.add(endLabel);
        // Don't forget to pop the end label from the stack
        flowBreakLabels.pop();
    }

    @Override
    public void caseAForStmt(AForStmt node) {
        final PForCondition pCondition = node.getForCondition();
        // First, generate the init statement in a clause for-loop
        if (pCondition instanceof AClauseForCondition) {
            final AClauseForCondition condition = (AClauseForCondition) pCondition;
            if (condition.getInit() != null) {
                condition.getInit().apply(this);
            }
        }
        // Then add the label that begins the loop
        final Label startLabel = newLabel("startFor");
        functionStmts.add(startLabel);
        // Now create the end label, and a jump to it, if the condition is false (or never jump if we don't have one)
        final Label endLabel = newLabel("endFor");
        if (pCondition instanceof AClauseForCondition) {
            final AClauseForCondition condition = (AClauseForCondition) pCondition;
            if (condition.getCond() != null) {
                condition.getCond().apply(this);
                @SuppressWarnings("unchecked")
                final Expr<BasicType> conditionValue = (Expr<BasicType>) convertedExprs.get(condition.getCond());
                functionStmts.add(new JumpCond(endLabel, new LogicNot(conditionValue)));
            }
        } else if (pCondition instanceof AExprForCondition) {
            final AExprForCondition condition = (AExprForCondition) pCondition;
            condition.getExpr().apply(this);
            @SuppressWarnings("unchecked")
            final Expr<BasicType> conditionValue = (Expr<BasicType>) convertedExprs.get(condition.getExpr());
            functionStmts.add(new JumpCond(endLabel, new LogicNot(conditionValue)));
        }
        // Now add a label that the continue statements can jump to
        final Label continueLabel = newLabel("continueFor");
        loopContinueLabels.push(continueLabel);
        // Break statements will jump to the end label
        flowBreakLabels.push(endLabel);
        // Convert the body of the for-loop
        node.getStmt().forEach(stmt -> stmt.apply(this));
        // Remove the labels from the stacks now that the body is complete
        loopContinueLabels.pop();
        flowBreakLabels.pop();
        // The continue label is at the end of the loop, but before the post statement
        functionStmts.add(continueLabel);
        // Now we can convert the post statement, if any
        if (pCondition instanceof AClauseForCondition) {
            final AClauseForCondition condition = (AClauseForCondition) pCondition;
            if (condition.getPost() != null) {
                condition.getPost().apply(this);
            }
        }
        // Finally we add a jump back to the start
        functionStmts.add(new Jump(startLabel));
        // The last statement is the end label
        functionStmts.add(endLabel);
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
        node.getExpr().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> expr = (Expr<BasicType>) convertedExprs.get(node.getExpr());
        if (expr.getType().isInteger()) {
            functionStmts.add(new Assignment(expr, new BinArInt(expr, new IntLit(1), BinArInt.Op.ADD)));
        } else if (expr.getType() == BasicType.FLOAT64) {
            functionStmts.add(new Assignment(expr, new BinArFloat64(expr, new Float64Lit(1), BinArFloat64.Op.ADD)));
        } else {
            throw new IllegalStateException("Expected integer or float64-typed expressions");
        }
    }

    @Override
    public void caseADecrStmt(ADecrStmt node) {
        node.getExpr().apply(this);
        @SuppressWarnings("unchecked")
        final Expr<BasicType> expr = (Expr<BasicType>) convertedExprs.get(node.getExpr());
        if (expr.getType().isInteger()) {
            functionStmts.add(new Assignment(expr, new BinArInt(expr, new IntLit(1), BinArInt.Op.SUB)));
        } else if (expr.getType() == BasicType.FLOAT64) {
            functionStmts.add(new Assignment(expr, new BinArFloat64(expr, new Float64Lit(1), BinArFloat64.Op.SUB)));
        } else {
            throw new IllegalStateException("Expected integer or float64-typed expressions");
        }
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
    public void caseAEnclosedExpr(AEnclosedExpr node) {
        throw new IllegalStateException("Enclosed expressions should have been removed earlier");
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
        } else if (inner.getType().isInteger()) {
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
        } else if (inner.getType().isInteger()) {
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
        if (inner.getType().isInteger()) {
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
        if (left.getType().isInteger()) {
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
        } else if (left.getType().isInteger()) {
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
        if (left.getType().isInteger()) {
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
        if (left.getType().isInteger()) {
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
        if (left.getType().isInteger()) {
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
        if (left.getType().isInteger()) {
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
        if (left.getType().isInteger()) {
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

    private Expr<BasicType> convertAdd(Expr<BasicType> left, Expr<BasicType> right) {
        if (left.getType() == BasicType.STRING) {
            return new ConcatString(left, right);
        }
        if (left.getType() == BasicType.FLOAT64) {
            return new BinArFloat64(left, right, BinArFloat64.Op.ADD);
        } else if (left.getType().isInteger()) {
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
        } else if (left.getType().isInteger()) {
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
        if (left.getType().isInteger()) {
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
        if (left.getType().isInteger()) {
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
        } else if (left.getType().isInteger()) {
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
        } else if (left.getType().isInteger()) {
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
        } else if (left.getType().isInteger()) {
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
        } else if (left.getType().isInteger()) {
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
