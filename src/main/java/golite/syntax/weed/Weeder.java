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
package golite.syntax.weed;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import golite.analysis.AnalysisAdapter;
import golite.analysis.DepthFirstAdapter;
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
import golite.node.ABreakStmt;
import golite.node.ACallExpr;
import golite.node.AClauseForCondition;
import golite.node.AContinueStmt;
import golite.node.ADeclVarShortStmt;
import golite.node.ADecrStmt;
import golite.node.ADefaultCase;
import golite.node.AEnclosedExpr;
import golite.node.AExprStmt;
import golite.node.AForStmt;
import golite.node.AFuncDecl;
import golite.node.AIdentExpr;
import golite.node.AIfStmt;
import golite.node.AIncrStmt;
import golite.node.AIndexExpr;
import golite.node.AParam;
import golite.node.AProg;
import golite.node.AReturnStmt;
import golite.node.ASelectExpr;
import golite.node.AStructField;
import golite.node.AStructType;
import golite.node.ASwitchStmt;
import golite.node.ATypeDecl;
import golite.node.AVarDecl;
import golite.node.Node;
import golite.node.PExpr;
import golite.node.TIdenf;
import golite.node.Token;

/**
 * Weeds out the usage of certain statements and expressions when it is not possible to do so in the grammar file.
 * <p>For example: left hand side of an assignment, {@code break} and {@code continue}.</p>
 */
public class Weeder extends DepthFirstAdapter {
    private static final String BLANK_IDENTIFIER = "_";
    private final Deque<Scope> scopeStack = new ArrayDeque<>();

    @Override
    public void inAProg(AProg node) {
        scopeStack.push(Scope.TOP);
    }

    @Override
    public void outAProg(AProg node) {
        popScope(Scope.TOP);
    }

    @Override
    public void inAVarDecl(AVarDecl node) {
        if (!areNamesUnique(node.getIdenf().stream())) {
            throw new WeederException(node, "Multiple declared variables have the same name");
        }
    }

    @Override
    public void outAVarDecl(AVarDecl node) {
        if (!node.getExpr().isEmpty() && node.getIdenf().size() != node.getExpr().size()) {
            throw new WeederException(node, "The number of expressions on the right do not match the number of identifiers");
        }
    }

    @Override
    public void inAFuncDecl(AFuncDecl node) {
        if (!areNamesUnique(node.getParam().stream().flatMap(field -> ((AParam) field).getIdenf().stream()))) {
            throw new WeederException(node, "Multiple parameters have the same name");
        }
        scopeStack.push(Scope.FUNC);
    }

    @Override
    public void outAFuncDecl(AFuncDecl node) {
        popScope(Scope.FUNC);
    }

    @Override
    public void inAIfStmt(AIfStmt node) {
        scopeStack.push(Scope.IF);
    }

    @Override
    public void outAIfStmt(AIfStmt node) {
        popScope(Scope.IF);
    }

    @Override
    public void inAForStmt(AForStmt node) {
        scopeStack.push(Scope.FOR);
    }

    @Override
    public void outAForStmt(AForStmt node) {
        popScope(Scope.FOR);
    }

    @Override
    public void inASwitchStmt(ASwitchStmt node) {
        scopeStack.push(Scope.SWITCH);
        boolean alreadyDefault = false;
        for (Node case_ : node.getCase()) {
            if (case_.getClass() == ADefaultCase.class) {
                if (alreadyDefault) {
                    throw new WeederException(case_, "There can only be one default case in a switch statement");
                }
                alreadyDefault = true;
            }
        }
    }

    @Override
    public void outASwitchStmt(ASwitchStmt node) {
        popScope(Scope.SWITCH);
    }

    @Override
    public void outAAssignStmt(AAssignStmt node) {
        node.getLeft().forEach(n -> n.apply(AssignableWeeder.INSTANCE));
        if (node.getLeft().size() != node.getRight().size()) {
            throw new WeederException(node, "The number of expressions on the right do not match the number of identifiers");
        }
    }

    @Override
    public void outAAssignMulStmt(AAssignMulStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignDivStmt(AAssignDivStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignRemStmt(AAssignRemStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignLshiftStmt(AAssignLshiftStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignRshiftStmt(AAssignRshiftStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignBitAndStmt(AAssignBitAndStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignBitAndNotStmt(AAssignBitAndNotStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignAddStmt(AAssignAddStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignSubStmt(AAssignSubStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignBitOrStmt(AAssignBitOrStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignBitXorStmt(AAssignBitXorStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAClauseForCondition(AClauseForCondition node) {
        if (node.getPost() instanceof ADeclVarShortStmt) {
            throw new WeederException(node.getPost(), "Cannot have a short variable declaration as the post statement of a \"for\" clause");
        }
    }

    @Override
    public void outAIncrStmt(AIncrStmt node) {
        node.getExpr().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outADecrStmt(ADecrStmt node) {
        node.getExpr().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outADeclVarShortStmt(ADeclVarShortStmt node) {
        final List<TIdenf> idenfs = new ArrayList<>();
        for (Node left : node.getLeft()) {
            if (!(left instanceof AIdentExpr)) {
                throw new WeederException(left, "The left side of the declaration must contain identifiers");
            }
            idenfs.add(((AIdentExpr) left).getIdenf());
        }
        if (!areNamesUnique(idenfs.stream())) {
            throw new WeederException(node, "Multiple variables on the left have the same name");
        }

        if (node.getLeft().size() != node.getRight().size()) {
            throw new WeederException(node, "The number of expressions on the right do not match the number of identifiers");
        }
    }

    @Override
    public void outAExprStmt(AExprStmt node) {
        PExpr expr = node.getExpr();
        while (expr instanceof AEnclosedExpr) {
            expr = ((AEnclosedExpr) expr).getExpr();
        }
        if (expr.getClass() != ACallExpr.class) {
            throw new WeederException(expr, "Expected a call expression");
        }
    }

    @Override
    public void outABreakStmt(ABreakStmt node) {
        if (!scopeStack.contains(Scope.FOR) && !scopeStack.contains(Scope.SWITCH)) {
            throw new WeederException(node, "The break keyword cannot be used outside a loop or switch case");
        }
    }

    @Override
    public void outAContinueStmt(AContinueStmt node) {
        if (!scopeStack.contains(Scope.FOR)) {
            throw new WeederException(node, "The continue keyword cannot be used outside a loop");
        }
    }

    @Override
    public void outAReturnStmt(AReturnStmt node) {
        if (!scopeStack.contains(Scope.FUNC)) {
            throw new WeederException(node, "The return keyword cannot be used outside a function");
        }
    }

    @Override
    public void outAStructType(AStructType node) {
        if (!areNamesUnique(node.getFields().stream().flatMap(field -> ((AStructField) field).getNames().stream()))) {
            throw new WeederException(node, "Multiple struct fields have the same name");
        }
    }

    @Override
    public void caseTIdenf(TIdenf idenf) {
        if (!idenf.getText().equals(BLANK_IDENTIFIER)) {
            return;
        }
        /*
            The blank identifier can only be used in the following cases:
                As a declaration name
                As a field name in a struct type declaration
                As a parameter name in a function declaration
                As an operand on the left side of an assignment or short variable declaration
        */
        final Node parent = idenf.parent();
        // Valid if it is the identifier of a declaration
        if (parent instanceof AVarDecl && ((AVarDecl) parent).getIdenf().contains(idenf)) {
            return;
        }
        if (parent instanceof ATypeDecl && ((ATypeDecl) parent).getIdenf() == idenf) {
            return;
        }
        if (parent instanceof AFuncDecl && ((AFuncDecl) parent).getIdenf() == idenf) {
            return;
        }
        // Valid if it is the identifier of a parameter
        if (parent instanceof AStructField && ((AStructField) parent).getNames().contains(idenf)) {
            return;
        }
        // Valid if it is the identifier of a struct field
        if (parent instanceof AParam && ((AParam) parent).getIdenf().contains(idenf)) {
            return;
        }
        // Valid if used in an identifier expression on the left side of a regular assignment variable or short declaration
        if (parent instanceof AIdentExpr) {
            // When used as an expression we can ignore the enclosing parenthesis
            PExpr expr = (AIdentExpr) parent;
            while (expr.parent() instanceof AEnclosedExpr) {
                expr = (AEnclosedExpr) expr.parent();
            }
            // The expression must be on the left side of the assignment or variable short declaration
            final Node exprParent = expr.parent();
            if (exprParent instanceof AAssignStmt && ((AAssignStmt) exprParent).getLeft().contains(expr)) {
                return;
            }
            if (exprParent instanceof ADeclVarShortStmt && ((ADeclVarShortStmt) exprParent).getLeft().contains(expr)) {
                return;
            }
        }
        throw new WeederException(idenf, "Invalid usage of the blank identifier");
    }

    private void popScope(Scope out) {
        final Scope scope = scopeStack.pop();
        if (scope != out) {
            throw new IllegalStateException("Expected out of scope " + out + ", but got " + scope);
        }
    }

    private static boolean areNamesUnique(Stream<? extends TIdenf> idenfs) {
        final Set<String> uniques = new HashSet<>();
        final Iterator<String> names = idenfs.map(Token::getText).filter(name -> !name.equals(BLANK_IDENTIFIER)).iterator();
        while (names.hasNext()) {
            if (!uniques.add(names.next())) {
                return false;
            }
        }
        return true;
    }

    private enum Scope {
        TOP, FUNC, IF, FOR, SWITCH
    }

    private static class AssignableWeeder extends AnalysisAdapter {
        private static final AssignableWeeder INSTANCE = new AssignableWeeder();

        @Override
        public void caseAIdentExpr(AIdentExpr node) {
            // Always valid, don't throw
        }

        @Override
        public void caseAEnclosedExpr(AEnclosedExpr node) {
            // We can ignore parenthesis: apply to the expression
            node.getExpr().apply(INSTANCE);
        }

        @Override
        public void caseASelectExpr(ASelectExpr node) {
            // Valid if the value expression is also assignable
            node.getValue().apply(INSTANCE);
        }

        @Override
        public void caseAIndexExpr(AIndexExpr node) {
            // Valid if the value expression is also assignable
            node.getValue().apply(INSTANCE);
        }

        @Override
        public void defaultCase(Node node) {
            throw new WeederException(node, "Not an assignable expression");
        }
    }
}
