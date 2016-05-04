package gr.demokritos.iit.clustering.structs;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public enum SimilarityMode {

    /**
     * map newsum similarity codes to parallelngg codes.
     */
    NVS("normalized"), VS("value"), CS("containment"), SS("size");
    private final String graphSim;

    SimilarityMode(String graphSimilarity) {
        this.graphSim = graphSimilarity;
    }

    public String getGraphSimilarity() {
        return graphSim;
    }
}
