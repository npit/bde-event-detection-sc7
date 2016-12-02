package gr.demokritos.iit.clustering.parallelngg.traits
import gr.demokritos.iit.clustering.parallelngg.graph.NGramGraph;
/**
  * @author Kontopoulos Ioannis
  */
trait SimilarityComparator {

  //@return similarity between two small graphs
  def getSimilarity(g1: NGramGraph, g2: NGramGraph): Similarity

}