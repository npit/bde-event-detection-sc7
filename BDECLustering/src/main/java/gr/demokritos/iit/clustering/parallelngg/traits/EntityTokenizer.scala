package gr.demokritos.iit.clustering.parallelngg.traits

import org.apache.spark.rdd.RDD

/**
  * @author Kontopoulos Ioannis
  */
trait EntityTokenizer {

  def getTokens(e: Entity): RDD[Atom]

}
