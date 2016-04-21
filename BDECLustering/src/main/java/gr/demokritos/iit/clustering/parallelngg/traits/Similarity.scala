package gr.demokritos.iit.clustering.parallelngg.traits

/**
  * @author Kontopoulos Ioannis
  */
trait Similarity {

  object Similarities {
    val SIZE_SIMILARITY = "size";
    val CONTAINMENT_SIMILARITY = "containment";
    val VALUE_SIMILARITY = "value";
    val NORMALIZED_SIMILARITY = "normalized";
  }

  //@return overall similarity
  def getOverallSimilarity: Double

  //@return map with similarity components
  def getSimilarityComponents: Map[String, Double]

}