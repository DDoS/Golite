package golite.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import golite.analysis.AnalysisAdapter;
import golite.ir.node.Assignment;
import golite.ir.node.BoolLit;
import golite.ir.node.Expr;
import golite.ir.node.FloatLit;
import golite.ir.node.FunctionDecl;
import golite.ir.node.Identifier;
import golite.ir.node.IntLit;
import golite.ir.node.PrintBool;
import golite.ir.node.PrintInt;
import golite.ir.node.PrintString;
import golite.ir.node.Program;
import golite.ir.node.Stmt;
import golite.ir.node.StringLit;
import golite.ir.node.VariableDecl;
import golite.ir.node.VoidReturn;
import golite.node.ABlockStmt;
import golite.node.ADeclStmt;
import golite.node.AFuncDecl;
import golite.node.AIdentExpr;
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
import golite.node.ATypeDecl;
import golite.node.AVarDecl;
import golite.node.Node;
import golite.node.PDecl;
import golite.node.PExpr;
import golite.node.PStmt;
import golite.node.Start;
import golite.node.TIdenf;
import golite.semantic.LiteralUtil;
import golite.semantic.SemanticData;
import golite.semantic.symbol.Function;
import golite.semantic.symbol.Symbol;
import golite.semantic.symbol.Variable;
import golite.semantic.type.BasicType;
import golite.semantic.type.IndexableType;
import golite.semantic.type.StructType;
import golite.semantic.type.Type;

/**
 *
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class IrConverter extends AnalysisAdapter {
    private final SemanticData semantics;
    private Program convertedProgram;
    private final Map<AFuncDecl, FunctionDecl> convertedFunctions = new HashMap<>();
    private final Map<Node, List<Stmt>> convertedStmts = new HashMap<>();
    private final Map<PExpr, Expr> convertedExprs = new HashMap<>();
    private final Map<Variable, String> uniqueVarNames = new HashMap<>();

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
        final List<Stmt> stmts = new ArrayList<>();
        node.getStmt().forEach(stmt -> {
            stmt.apply(this);
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
    public void caseAVarDecl(AVarDecl node) {
        final List<Stmt> stmts = new ArrayList<>();
        // Two statements per variable declaration: declaration and initialization
        final Set<Variable> variables = semantics.getVariableSymbols(node).get();
        final List<TIdenf> idenfs = node.getIdenf();
        for (int i = 0; i < idenfs.size(); i++) {
            final String variableName = idenfs.get(i).getText();
            final Variable variable = variables.stream()
                    .filter(var -> var.getName().equals(variableName))
                    .findFirst().get();
            // Find unique names for variables to prevent conflicts from removing scopes
            final String uniqueName = findUniqueName(variable);
            stmts.add(new VariableDecl(variable, uniqueName));
            // Then initialize the variables with assignments
            final Identifier variableExpr = new Identifier(variable, uniqueName);
            final Stmt initializer;
            if (node.getExpr().isEmpty()) {
                // No explicit initializer, use a default
                initializer = defaultInitializer(variableExpr, variable.getType());
            } else {
                final PExpr expr = node.getExpr().get(i);
                expr.apply(this);
                convertedExprs.get(expr);
                initializer = new Assignment(variableExpr, convertedExprs.get(expr));
            }
            stmts.add(initializer);
        }
        convertedStmts.put(node, stmts);
    }

    @Override
    public void caseATypeDecl(ATypeDecl node) {
        // Type declarations don't matter after type-checking
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
            if (type == BasicType.BOOL) {
                stmts.add(new PrintBool(converted));
            } else if (type == BasicType.INT) {
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
    public void caseABlockStmt(ABlockStmt node) {
        final List<Stmt> stmts = new ArrayList<>();
        for (PStmt stmt : node.getStmt()) {
            stmt.apply(this);
            stmts.addAll(convertedStmts.get(stmt));
        }
        convertedStmts.put(node, stmts);
    }

    @Override
    public void caseADeclStmt(ADeclStmt node) {
        final List<Stmt> stmts = new ArrayList<>();
        for (PDecl decl : node.getDecl()) {
            decl.apply(this);
            stmts.addAll(convertedStmts.get(decl));
        }
        convertedStmts.put(node, stmts);
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
        final Symbol symbol = semantics.getIdentifierSymbol(node).get();
        convertedExprs.put(node, new Identifier(symbol));
    }

    @Override
    public void defaultCase(Node node) {
    }

    private String findUniqueName(Variable variable) {
        if (uniqueVarNames.containsKey(variable)) {
            throw new IllegalStateException("This method should not have been called twice for the same variable");
        }
        final String name = variable.getName();
        String uniqueName = name;
        int id = 0;
        boolean isUnique;
        do {
            isUnique = true;
            for (String existingName : uniqueVarNames.values()) {
                if (uniqueName.equals(existingName)) {
                    isUnique = false;
                    uniqueName = name + id;
                    id++;
                }
            }
        } while (!isUnique);
        uniqueVarNames.put(variable, uniqueName);
        return uniqueName;
    }

    private static Stmt defaultInitializer(Identifier variableExpr, Type type) {
        type = type.resolve();
        if (type.isInteger()) {
            return new Assignment(variableExpr, new IntLit(0));
        }
        if (type == BasicType.FLOAT64) {
            return new Assignment(variableExpr, new FloatLit(0));
        }
        if (type == BasicType.BOOL) {
            return new Assignment(variableExpr, new BoolLit(false));
        }
        if (type == BasicType.STRING) {
            return new Assignment(variableExpr, new StringLit(""));
        }
        if (type instanceof IndexableType || type instanceof StructType) {
            // TODO: memset 0 stmt
        }
        throw new UnsupportedOperationException("Unsupported type: " + type);
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
