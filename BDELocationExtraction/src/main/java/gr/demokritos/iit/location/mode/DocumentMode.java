package gr.demokritos.iit.location.mode;

/**
 * Choose on which collection we should work upon.
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public enum DocumentMode {

    TWEETS("tweets"), ARTICLES("articles"), BOTH("both"), TEXT("text");

    private final String mode;

    private DocumentMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }
}
