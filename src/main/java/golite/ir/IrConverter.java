package golite.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import golite.analysis.AnalysisAdapter;
import golite.ir.node.Expr;
import golite.ir.node.FunctionDecl;
import golite.ir.node.IntLit;
import golite.ir.node.PrintInt;
import golite.ir.node.PrintString;
import golite.ir.node.Program;
import golite.ir.node.Stmt;
import golite.ir.node.StringLit;
import golite.ir.node.VoidReturn;
import golite.node.AFuncDecl;
import golite.node.AIntDecExpr;
import golite.node.AIntHexExpr;
import golite.node.AIntOctExpr;
import golite.node.APkg;
import golite.node.APrintStmt;
import golite.node.APrintlnStmt;
import golite.node.AProg;
import golite.node.AReturnStmt;
import golite.node.AStringIntrExpr;
import golite.node.AStringRawExpr;
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
        // If the function has no return value and does not have a final return statement, then add one
        if (!symbol.getType().getReturnType().isPresent()
                && (stmts.isEmpty() || !(stmts.get(stmts.size() - 1) instanceof VoidReturn))) {
            stmts.add(new VoidReturn());
        }
        convertedFunctions.put(node, new FunctionDecl(symbol, stmts));
    }

    @Override
    public void caseAPrintStmt(APrintStmt node) {
        final List<Stmt> stmts = convertPrintStmt(node.getExpr());
        convertedStmts.put(node, stmts);
    }

    @Override
    public void caseAPrintlnStmt(APrintlnStmt node) {
        final List<Stmt> stmts = convertPrintStmt(node.getExpr());
        stmts.add(new PrintString(new StringLit("\n")));
        convertedStmts.put(node, stmts);
    }

    private List<Stmt> convertPrintStmt(LinkedList<PExpr> exprs) {
        final List<Stmt> stmts = new ArrayList<>();
        for (int i = 0, exprsSize = exprs.size(); i < exprsSize; i++) {
            final PExpr expr = exprs.get(i);
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
            } else if (type == BasicType.STRING) {
                stmts.add(new PrintString(converted));
            } else {
                throw new IllegalStateException("Unexpected type: " + type);
            }
            // Add spaces between expressions
            if (i < exprsSize - 1) {
                stmts.add(new PrintString(new StringLit(" ")));
            }
        }
        return stmts;
    }

    @Override
    public void caseAReturnStmt(AReturnStmt node) {
        if (node.getExpr() == null) {
            convertedStmts.put(node, Collections.singletonList(new VoidReturn()));
        } else {
            // TODO: return with value
        }
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
    public void defaultCase(Node node) {
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
        // You might be wondering why not use the \\uXXXX notation instead of casting int to chars?
        // Well go ahead and remove that second    ^ backslash. Java is a special boy.
    }
}
