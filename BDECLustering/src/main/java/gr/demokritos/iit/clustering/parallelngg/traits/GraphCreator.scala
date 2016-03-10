package gr.demokritos.iit.clustering.parallelngg.traits

import org.apache.spark.SparkContext
import org.apache.spark.graphx.Graph

/**
 * @author Kontopoulos Ioannis
 */
trait GraphCreator {

  val sc: SparkContext

  def getGraph(e: Entity): Graph[String, Double]

}
