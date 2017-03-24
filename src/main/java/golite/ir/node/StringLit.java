package golite.ir.node;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class StringLit extends Expr {
    private String value;
    private ByteBuffer utf8Data;

    public StringLit(String value) {
        super(BasicType.STRING);
        this.value = value;
        final byte[] data;
        try {
            data = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException exception) {
            throw new RuntimeException(exception);
        }
        utf8Data = ByteBuffer.allocateDirect(data.length).put(data);
        utf8Data.flip();
    }

    public String getValue() {
        return value;
    }

    public ByteBuffer getUtf8Data() {
        return utf8Data;
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
