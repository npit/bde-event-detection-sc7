package gr.demokritos.iit.clustering.parallelngg.traits

/**
 * @author Kontopoulos Ioannis
 */
trait Distance {

  /**
   * Calculates distance between two objects
   */
  def getResult(o1: Object, o2: Object): Double

}
