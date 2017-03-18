package golite.semantic.context;

/**
 * The context of all top level declarations
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
