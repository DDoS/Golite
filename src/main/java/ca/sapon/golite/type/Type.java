package ca.sapon.golite.type;

/**
 *
 */
public interface Type {
    boolean assignableTo(Type type);

    String toString();
}
