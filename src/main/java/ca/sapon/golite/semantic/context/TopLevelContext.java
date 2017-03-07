package ca.sapon.golite.semantic.context;

/**
 *
 */
public class TopLevelContext extends Context {
    public TopLevelContext() {
        super(UniverseContext.INSTANCE, 1);
    }

    @Override
    public String getSignature() {
        return "TopLevel";
    }
}
