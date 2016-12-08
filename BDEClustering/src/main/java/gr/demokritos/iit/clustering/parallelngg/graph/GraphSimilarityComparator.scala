package gr.demokritos.iit.clustering.parallelngg.graph;
import gr.demokritos.iit.clustering.parallelngg.traits.SimilarityComparator;
import gr.demokritos.iit.clustering.parallelngg.traits.Similarity;


/**
  * @author Kontopoulos Ioannis
  */
class GraphSimilarityComparator extends SimilarityComparator with Serializable {

  /**
    * Gets the similarity between two graphs
    * @param g1 first graph
    * @param g2 second graph
    * @return similarity of graphs
    */
  override def getSimilarity(g1: NGramGraph, g2: NGramGraph): Similarity = {
    //number of edges of graph1
    val g1EdgeCount = g1.numEdges
    //number of edges of graph2
    val g2EdgeCount = g2.numEdges
    //calculate size similarity
    val sSimil = Math.min(g1EdgeCount, g2EdgeCount).toDouble/Math.max(g1EdgeCount, g2EdgeCount)
    // get the common edges hash
    val commonEdgesHash = g1.edges.keySet.intersect(g2.edges.keySet)
    // get the edge weights of both graphs for the common edges
    val commonEdges = commonEdgesHash.map(e => (e,(g1.edges(e),g2.edges(e))))
    // number of common edges
    val commonEdgesCount = commonEdges.size
    var vSimil = 0.0
    //if there are common edges
    if (commonEdgesCount != 0) {
      vSimil = commonEdges.toVector.map(e => Math.min(e._2._1, e._2._2)/Math.max(e._2._1, e._2._2)).sum/Math.max(g1EdgeCount,g2EdgeCount)
    }
    //for each common edge add 1/min to a sum
    val cSimil = (1.toDouble/Math.min(g1EdgeCount, g2EdgeCount))*commonEdgesCount
    val gs = new GraphSimilarity(sSimil, vSimil, cSimil)
    gs
  }

}