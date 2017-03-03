package ca.sapon.golite.semantic.symbol;

/**
 *
 */
public abstract class Symbol {
    private final String name;

    protected Symbol(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
