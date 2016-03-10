/* Copyright 2016 NCSR Demokritos
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package gr.demokritos.iit.clustering.exec;

import gr.demokritos.iit.clustering.config.BDESpark;
import gr.demokritos.iit.clustering.config.BDESparkConf;
import gr.demokritos.iit.clustering.config.ISparkConf;
import gr.demokritos.iit.clustering.parallelngg.graph.GraphSimilarityCalculator;
import gr.demokritos.iit.clustering.parallelngg.graph.NGramGraphCreator;
import gr.demokritos.iit.clustering.parallelngg.structs.StringEntity;
import gr.demokritos.iit.clustering.parallelngg.traits.Similarity;
import org.apache.spark.SparkContext;
import org.apache.spark.graphx.Graph;
import org.apache.spark.rdd.RDD;

/**
 * test scala integration
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class TestParallelNGG {

    public static void main(String[] args) {
        //        ISparkConf conf = new BDESparkConf("spark.properties");
        ISparkConf conf = new BDESparkConf();
        BDESpark bdes = new BDESpark(conf);

        BDEEventDetection bdedet = new BDEEventDetection(bdes);

        SparkContext sc = bdedet.getContext();

        long start = System.currentTimeMillis();

        StringEntity en1 = new StringEntity();
        en1.readFile(sc, "/home/gkioumis/Programming/scala/ParallelNGGFork/res/test1", 3);

        StringEntity en2 = new StringEntity();
        en2.readFile(sc, "/home/gkioumis/Programming/scala/ParallelNGGFork/res/test2", 3);

        NGramGraphCreator ngc1 = new NGramGraphCreator(sc, 4, 3, 3);
        Graph<String, Object> ngg1 = ngc1.getGraph(en1);
        NGramGraphCreator ngc2 = new NGramGraphCreator(sc, 4, 3, 3);
        Graph<String, Object> ngg2 = ngc2.getGraph(en2);


        GraphSimilarityCalculator gsc = new GraphSimilarityCalculator();

        Similarity gs = gsc.getSimilarity(ngg1, ngg2);


        System.out.println("overall similarity: " + gs.getOverallSimilarity());
        System.out.println("size similarity: " + gs.getSimilarityComponents().get("size"));
        System.out.println("value similarity: " + gs.getSimilarityComponents().get("value"));
        System.out.println("containment similarity: " + gs.getSimilarityComponents().get("containment"));
        System.out.println("normalized similarity: " + gs.getSimilarityComponents().get("normalized"));

        long end = System.currentTimeMillis();

        System.out.println("\nDuration: " + (double) (end-start)/1000 + " seconds");
    }
}
