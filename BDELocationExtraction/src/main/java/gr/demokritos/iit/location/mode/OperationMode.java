package gr.demokritos.iit.location.mode;

/**
 * Choose on which collection we should work upon.
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public enum OperationMode {

    TWEETS("tweets"), ARTICLES("articles"), BOTH("both");

    private final String mode;

    private OperationMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }
}
