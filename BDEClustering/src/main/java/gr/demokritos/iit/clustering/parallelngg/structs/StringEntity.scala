package gr.demokritos.iit.clustering.parallelngg.structs

import java.util.Arrays.ArrayList

import gr.demokritos.iit.clustering.parallelngg.traits.Entity
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

/**
 * @author Kontopoulos Ioannis
 */
class StringEntity extends Entity {

  //single string of the entity
  private var singleString = ""
  //RDD containing the lines of a text file
  var dataStringRDD: RDD[String] = null


  /**
   * Returns the actual data of the entity
    *
    * @return dataString
   */
  override def getPayload: String = {
    this.singleString
  }

  /**
    * Sets the value of dataString, creating a corresponding RDD to support parallelization.
    *
    * @param value of dataString
    */
  def setString(sc: SparkContext, value: String) = {
    this.singleString = value
    this.dataStringRDD = sc.makeRDD(List[String] { value }); // TODO: Check if splitting string makes sense
  }

  /**
    * Sets the value of dataString
    * TODO: If only setString is called, then getGraph results in NullPointer exception.
    *
    * @param value of dataString
    */
  def setString(value: String) = {
    this.singleString = value
  }

  /**
    * Reads dataString from a file, distributed version
 *
    * @param sc SparkContext
    * @param file file to read
    * @param numPartitions number of partitions
    * @return lines of file
    */
  def readFile(sc: SparkContext, file: String, numPartitions: Int) = {
    dataStringRDD = sc.textFile(file, numPartitions)
  }
}
