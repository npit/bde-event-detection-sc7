package gr.demokritos.iit.clustering.parallelngg.traits

import org.apache.spark.SparkContext

/**
 * @author Kontopoulos Ioannis
 */
trait Experiment {

  val sc: SparkContext

  def run(classifier: String)

}
