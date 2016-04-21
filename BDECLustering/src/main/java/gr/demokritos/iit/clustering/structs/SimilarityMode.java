package gr.demokritos.iit.clustering.structs;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public enum SimilarityMode {

    /**
     * map newsum similarity codes to parallelngg codes.
     */
    NVS("normalized"), VS("value"), CS("containment"), SS("size");
    private final String graphsimilarity;

    SimilarityMode(String graphsimilarity) {
        this.graphsimilarity = graphsimilarity;
    }

    public String getGraphsimilarity() {
        return graphsimilarity;
    }
}
