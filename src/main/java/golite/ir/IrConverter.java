package golite.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import golite.analysis.AnalysisAdapter;
import golite.node.AFuncDecl;
import golite.node.AIntDecExpr;
import golite.node.AIntHexExpr;
import golite.node.AIntOctExpr;
import golite.node.APkg;
import golite.node.APrintStmt;
import golite.node.APrintlnStmt;
import golite.node.AProg;
import golite.node.Node;
import golite.node.PExpr;
import golite.node.PStmt;
import golite.node.Start;
import golite.semantic.LiteralUtil;
import golite.semantic.SemanticData;
import golite.semantic.symbol.Function;
import golite.semantic.type.BasicType;
import golite.semantic.type.Type;

/**
 *
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class IrConverter extends AnalysisAdapter {
    private final SemanticData semantics;
    private Program convertedProgram;
    private final Map<AFuncDecl, FunctionDecl> convertedFunctions = new HashMap<>();
    private final Map<PStmt, List<Stmt>> convertedStmts = new HashMap<>();
    private final Map<PExpr, Expr> convertedExprs = new HashMap<>();

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
        node.getDecl().forEach(decl -> decl.apply(this));
        final List<FunctionDecl> functions = new ArrayList<>();
        node.getDecl().stream()
                .filter(decl -> decl instanceof AFuncDecl)
                .forEach(decl -> functions.add(convertedFunctions.get((AFuncDecl) decl)));
        convertedProgram = new Program(((APkg) node.getPkg()).getIdenf().getText(), functions);
    }

    @Override
    public void caseAFuncDecl(AFuncDecl node) {
        final Function symbol = semantics.getFunctionSymbol(node).get();
        node.getStmt().forEach(stmt -> stmt.apply(this));
        final List<Stmt> stmts = new ArrayList<>();
        node.getStmt().forEach(stmt -> {
            // TODO: remove this check when all are implemented
            if (convertedStmts.containsKey(stmt)) {
                stmts.addAll(convertedStmts.get(stmt));
            }
        });
        convertedFunctions.put(node, new FunctionDecl(symbol, stmts));
    }

    @Override
    public void caseAPrintStmt(APrintStmt node) {
        final List<Stmt> stmts = new ArrayList<>();
        for (PExpr expr : node.getExpr()) {
            expr.apply(this);
            final Expr converted = convertedExprs.get(expr);
            // TODO: remove this check when all are implemented
            if (converted == null) {
                continue;
            }
            final Type type = semantics.getExprType(expr).get().resolve();
            // TODO: other basic types
            if (type == BasicType.INT) {
                stmts.add(new PrintInt(converted));
            } else {
                throw new IllegalStateException("Unexpected type: " + type);
            }
        }
        convertedStmts.put(node, stmts);
    }

    @Override
    public void caseAPrintlnStmt(APrintlnStmt node) {
        super.caseAPrintlnStmt(node);
    }

    @Override
    public void caseAIntDecExpr(AIntDecExpr node) {
        // TODO: handle integer overflow
        convertedExprs.put(node, new IntLit(LiteralUtil.parseInt(node)));
    }

    @Override
    public void caseAIntOctExpr(AIntOctExpr node) {
        // TODO: handle integer overflow
        convertedExprs.put(node, new IntLit(LiteralUtil.parseInt(node)));
    }

    @Override
    public void caseAIntHexExpr(AIntHexExpr node) {
        // TODO: handle integer overflow
        convertedExprs.put(node, new IntLit(LiteralUtil.parseInt(node)));
    }

    @Override
    public void defaultCase(Node node) {
    }
}
