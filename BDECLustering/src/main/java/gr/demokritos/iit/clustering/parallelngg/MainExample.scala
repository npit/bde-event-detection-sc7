package gr.demokritos.iit.clustering.parallelngg

import gr.demokritos.iit.clustering.parallelngg.graph._
import gr.demokritos.iit.clustering.parallelngg.nlp.{OpenNLPSentenceSplitter, StringEntityTokenizer}
import gr.demokritos.iit.clustering.parallelngg.structs.StringEntity
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}

/**
 * @author George K. <gkiom@iit.demokritos.gr>
 */
object MainExample {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("ParallelNGG")
      .setMaster("local[*]")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryoserializer.buffer", "64mb")
      .registerKryoClasses(Array(classOf[MergeOperator], classOf[IntersectOperator], classOf[InverseIntersectOperator], classOf[DeltaOperator], classOf[GraphSimilarityCalculator], classOf[StringEntityTokenizer], classOf[OpenNLPSentenceSplitter]))
      .set("spark.executor.memory", "2g")
    val sc = new SparkContext(conf)

    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)

    val start = System.currentTimeMillis

    val numPartitions = 4
    val nggc1 = new NGramGraphCreator(sc, numPartitions, 3, 3)
    val nggc2 = new NGramGraphCreator(sc, numPartitions, 3, 3)
    val e1 = new StringEntity
    val e2 = new StringEntity

    if (args.length >= 2) {
      val s1 = args(0)
      val s2 = args(1)
      println(s1 + " : " + s2)
      e1.readFile(sc, s1, numPartitions)

      e2.readFile(sc, s2, numPartitions)

    }
    else {
      Logger.getLogger("org").warn("Using default strings, since not enough arguments were provided.")

      e1.setString(sc, "This is a test string of some length.")
      e2.setString(sc, "This is another, longer, test string of some length. By the way, it also contains a second " +
        "sentence!")
    }


    val ngg1 = nggc1.getGraph(e1)
    val ngg2 = nggc2.getGraph(e2)
    val gsc = new GraphSimilarityCalculator
    val gs = gsc.getSimilarity(ngg1, ngg2)

    println("overall similarity: " + gs.getOverallSimilarity)
    println("size similarity: " + gs.getSimilarityComponents("size"))
    println("value similarity: " + gs.getSimilarityComponents("value"))
    println("containment similarity: " + gs.getSimilarityComponents("containment"))
    println("normalized similarity: " + gs.getSimilarityComponents("normalized"))

    // Self similarity
    val gsSelf = gsc.getSimilarity(ngg1, ngg1)

    println("overall similarity: " + gsSelf.getOverallSimilarity)
    println("size similarity: " + gsSelf.getSimilarityComponents("size"))
    println("value similarity: " + gsSelf.getSimilarityComponents("value"))
    println("containment similarity: " + gsSelf.getSimilarityComponents("containment"))
    println("normalized similarity: " + gsSelf.getSimilarityComponents("normalized"))

    val end = System.currentTimeMillis

    println("\nDuration: " + (end-start).toDouble/1000 + " seconds")
  }
}
