package golite.ir.node;

import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class StringLit extends Expr {
    private String value;

    public StringLit(String value) {
        super(BasicType.STRING);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitStringLit(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        final StringBuilder escaped = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (Character.isISOControl(c) || Character.isWhitespace(c)) {
                escaped.append("\\").append(Integer.toHexString(c));
            } else if (c == '\"') {
                escaped.append("\\\"");
            } else {
                escaped.append(c);
            }
        }
        printer.print("\"").print(escaped.toString()).print("\"");
    }
}
