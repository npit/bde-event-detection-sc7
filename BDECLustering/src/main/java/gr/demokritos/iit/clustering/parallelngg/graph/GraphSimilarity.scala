package gr.demokritos.iit.clustering.parallelngg.graph

import gr.demokritos.iit.clustering.parallelngg.traits.Similarity

/**
  * @author Kontopoulos Ioannis
  */
class GraphSimilarity(private val sizeSimilarity: Double, private val valueSimilarity: Double, private val containmentSimilarity: Double) extends Similarity {

  /**
    * Calculates overall similarity
    *
    * @return overall similarity
    */
  override def getOverallSimilarity = sizeSimilarity * valueSimilarity * containmentSimilarity

  /**
    * @return map with similarity components
    */
  override def getSimilarityComponents: Map[String, Double] = {
    val components = Map(
      (Similarities.SIZE_SIMILARITY, sizeSimilarity),
      (Similarities.VALUE_SIMILARITY, valueSimilarity),
      (Similarities.CONTAINMENT_SIMILARITY, containmentSimilarity),
      (Similarities.NORMALIZED_SIMILARITY, valueSimilarity / sizeSimilarity)
    )
    components
  }

}