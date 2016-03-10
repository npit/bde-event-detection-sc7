package gr.demokritos.iit.clustering.parallelngg.traits

/**
  * @author Kontopoulos Ioannis
  */
trait Clustering {

  def getClusters(path: String): Array[(Int, String)]

}
