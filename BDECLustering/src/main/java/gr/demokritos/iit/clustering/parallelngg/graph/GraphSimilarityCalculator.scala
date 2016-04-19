package gr.demokritos.iit.clustering.parallelngg.graph

import gr.demokritos.iit.clustering.parallelngg.traits.{Similarity, SimilarityCalculator}
import org.apache.spark.graphx.{Edge, Graph}

/**
 * @author Kontopoulos Ioannis
 * @author George Giannakopoulos
 */
class GraphSimilarityCalculator extends SimilarityCalculator with Serializable {

  /**
   * Gets the similarity between two graphs
    *
    * @param g1 graph1
   * @param g2 graph2
   * @return Similarity
   */
  override def getSimilarity(g1: Graph[String, Double], g2: Graph[String, Double]): Similarity = {
    //number of edges of graph1
    val g1EdgeCount = g1.edges.distinct.count
    //number of edges of graph2
    val g2EdgeCount = g2.edges.distinct.count
    //calculate size similarity
    val sSimil = Math.min(g1EdgeCount, g2EdgeCount).toDouble/Math.max(g1EdgeCount, g2EdgeCount)
    // TODO: We should hash same vertex strings to same vertex Id
    //pair edges so the common edges are the ones with same vertices pair
    def edgeToPair (e: Edge[Double]) = ((e.srcId, e.dstId), e.attr)
    val pairs1 = g1.edges.map(edgeToPair)
    val pairs2 = g2.edges.map(edgeToPair)
    val commonEdges = pairs1.join(pairs2)
    commonEdges.cache
    //d holds the max number of edges over the two graphs
    val d = Math.max(pairs1.count(), pairs2.count())
    //c holds the number of common edges over the two graphs
    val c = commonEdges.count()
    var minEdgeWeight = 1.0
    var maxEdgeWeight = 1.0
    //if there are common edges
    // DONE: Fix. Works over ALL edges. Not individually.
    var vSimil = 0.0

    if (c != 0) {
      //edge ratio of the common edges
      vSimil = commonEdges.map(e => Math.min(e._2._1, e._2._2) / Math.min(e._2._1, e._2._2)).sum / d
    }
    commonEdges.unpersist()
    //for each common edge add (minimum edge weight/maximum edge weight)/maximum graph size to a sum

    //for each common edge add 1/min to a sum
    val cSimil = (1.toDouble/Math.min(g1EdgeCount, g2EdgeCount))*c
    val gs = new GraphSimilarity(sSimil, vSimil, cSimil)
    gs
  }

}