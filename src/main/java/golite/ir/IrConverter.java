package golite.ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import golite.analysis.AnalysisAdapter;
import golite.ir.node.Assignment;
import golite.ir.node.BoolLit;
import golite.ir.node.Call;
import golite.ir.node.Cast;
import golite.ir.node.Expr;
import golite.ir.node.Float64Lit;
import golite.ir.node.FunctionDecl;
import golite.ir.node.Identifier;
import golite.ir.node.IntLit;
import golite.ir.node.MemsetZero;
import golite.ir.node.PrintBool;
import golite.ir.node.PrintFloat64;
import golite.ir.node.PrintInt;
import golite.ir.node.PrintRune;
import golite.ir.node.PrintString;
import golite.ir.node.Program;
import golite.ir.node.Stmt;
import golite.ir.node.StringLit;
import golite.ir.node.ValueReturn;
import golite.ir.node.VariableDecl;
import golite.ir.node.VoidReturn;
import golite.node.ABlockStmt;
import golite.node.ACallExpr;
import golite.node.ADeclStmt;
import golite.node.AEmptyStmt;
import golite.node.AExprStmt;
import golite.node.AFloatExpr;
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
import golite.node.ARuneExpr;
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
import golite.semantic.context.UniverseContext;
import golite.semantic.symbol.DeclaredType;
import golite.semantic.symbol.Function;
import golite.semantic.symbol.Symbol;
import golite.semantic.symbol.Variable;
import golite.semantic.type.BasicType;
import golite.semantic.type.FunctionType;
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
    private final Map<AFuncDecl, Optional<FunctionDecl>> convertedFunctions = new HashMap<>();
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
        final List<VariableDecl> globals = new ArrayList<>();
        final List<Stmt> staticInitialization = new ArrayList<>();
        for (PDecl decl : node.getDecl()) {
            if (decl instanceof AFuncDecl) {
                convertedFunctions.get(decl).ifPresent(functions::add);
            } else if (decl instanceof AVarDecl) {
                // Variable declarations will create additional statements,
                // Which are put in a function called before the main
                for (Stmt stmt : convertedStmts.get(decl)) {
                    if (stmt instanceof VariableDecl) {
                        globals.add((VariableDecl) stmt);
                    } else {
                        staticInitialization.add(stmt);
                    }
                }
            }
        }
        //staticInitialization.clear();
        staticInitialization.add(new VoidReturn());
        // Add the static initialization function for the global variables
        final FunctionType staticInitType = new FunctionType(Collections.emptyList(), null);
        final String staticInitName = findUniqueName("staticInit",
                functions.stream().map(function -> function.getFunction().getName()).collect(Collectors.toSet()));
        final Function staticInit = new Function(0, 0, 0, 0, staticInitName, staticInitType,
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
        final List<Stmt> stmts = new ArrayList<>();
        node.getStmt().forEach(stmt -> {
            stmt.apply(this);
            if (convertedStmts.containsKey(stmt)) {
                // TODO: remove this check when done
                stmts.addAll(convertedStmts.get(stmt));
            }
        });
        // If the function has no return value and does not have a final return statement, then add one
        if (!symbol.getType().getReturnType().isPresent()
                && (stmts.isEmpty() || !(stmts.get(stmts.size() - 1) instanceof VoidReturn))) {
            stmts.add(new VoidReturn());
        }
        convertedFunctions.put(node, Optional.of(new FunctionDecl(symbol, uniqueParamNames, stmts)));
    }

    @Override
    public void caseAVarDecl(AVarDecl node) {
        final List<Stmt> stmts = new ArrayList<>();
        // Two statements per variable declaration: declaration and initialization
        final Set<Variable> variables = semantics.getVariableSymbols(node).get();
        final List<TIdenf> idenfs = node.getIdenf();
        for (int i = 0; i < idenfs.size(); i++) {
            final String variableName = idenfs.get(i).getText();
            // Ignore blank variables
            if (variableName.equals("_")) {
                continue;
            }
            // Find the symbol for the variable that was declared
            final Variable variable = variables.stream()
                    .filter(var -> var.getName().equals(variableName))
                    .findFirst().get().dealias();
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
    public void caseAEmptyStmt(AEmptyStmt node) {
        convertedStmts.put(node, Collections.emptyList());
    }

    @Override
    public void caseAExprStmt(AExprStmt node) {
        node.getExpr().apply(this);
        // The IR for expressions that are statements implement the Stmt interface
        final Stmt stmt = (Stmt) convertedExprs.get(node.getExpr());
        convertedStmts.put(node, Collections.singletonList(stmt));
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
        for (PExpr expr : exprs) {
            expr.apply(this);
            final Expr converted = convertedExprs.get(expr);
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
            stmts.add(printStmt);
        }
        return stmts;
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
        convertedStmts.put(node, Collections.singletonList(return_));
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
        final Symbol symbol = semantics.getIdentifierSymbol(node).get();
        if (!(symbol instanceof Variable)) {
            throw new IllegalStateException("Non-variable identifiers should have been handled earlier");
        }
        final Variable variable = ((Variable) symbol).dealias();
        final Expr expr;
        // Special case for the pre-declared booleans identifiers: convert them to bool literals
        if (variable.equals(UniverseContext.TRUE_VARIABLE)) {
            expr = new BoolLit(true);
        } else if (variable.equals(UniverseContext.FALSE_VARIABLE)) {
            expr = new BoolLit(false);
        } else {
            expr = new Identifier(variable, uniqueVarNames.get(variable));
        }
        convertedExprs.put(node, expr);
    }

    @Override
    public void caseACallExpr(ACallExpr node) {
        // Convert the arguments
        final List<PExpr> args = node.getArgs();
        args.forEach(arg -> arg.apply(this));
        // The called values will always be an identifier
        final AIdentExpr callIdent = (AIdentExpr) node.getValue();
        // Get the symbol being called (function or type)
        final Symbol symbol = semantics.getIdentifierSymbol(callIdent).get();
        if (symbol instanceof DeclaredType) {
            // Cast
            if (args.size() != 1) {
                throw new IllegalStateException("Expected only one argument in the cast");
            }
            final Expr convertedArg = convertedExprs.get(args.get(0));
            final BasicType castType = (BasicType) symbol.getType().deepResolve();
            convertedExprs.put(node, new Cast(castType, convertedArg));
            return;
        }
        if (symbol instanceof Function) {
            // Call
            final List<Expr> convertedArgs = args.stream().map(convertedExprs::get).collect(Collectors.toList());
            convertedExprs.put(node, new Call(((Function) symbol).dealias(), convertedArgs));
            return;
        }
        throw new IllegalStateException("Unexpected call to symbol type: " + symbol.getClass());
    }

    @Override
    public void defaultCase(Node node) {
    }

    private String findUniqueName(Variable variable) {
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

    private static Stmt defaultInitializer(Identifier variableExpr, Type type) {
        type = type.deepResolve();
        if (type.isInteger()) {
            return new Assignment(variableExpr, new IntLit(0));
        }
        if (type == BasicType.FLOAT64) {
            return new Assignment(variableExpr, new Float64Lit(0));
        }
        if (type == BasicType.BOOL) {
            return new Assignment(variableExpr, new BoolLit(false));
        }
        if (type == BasicType.STRING || type instanceof IndexableType || type instanceof StructType) {
            return new MemsetZero(variableExpr);
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
