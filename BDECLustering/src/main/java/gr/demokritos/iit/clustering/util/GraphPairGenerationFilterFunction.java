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
package gr.demokritos.iit.clustering.util;

import gr.demokritos.iit.clustering.model.DPair;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.graphx.Edge;
import org.apache.spark.graphx.Graph;
import scala.Tuple2;
import scala.Tuple4;

import java.util.*;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class GraphPairGenerationFilterFunction implements Function<Tuple2<Graph<String, Object>, Graph<String, Object>>, Boolean> {

    // keep a hash reference in RAM of each pair, and return item if the hash of each other is not equal or the pair does not exist
    private final Set<Integer> hash_cache;

    public GraphPairGenerationFilterFunction() {
        this.hash_cache = new HashSet();
    }
    // expects the full combination list of n articles (n*n). Filters the same instances of (i,j) - i.e where i=j and the 
    // around ones i.e. i, j = j, i
    @Override
    public Boolean call(Tuple2<Graph<String, Object>,Graph<String, Object>> arg) throws Exception {

        Edge [] edges1 = (Edge []) arg._1().edges().collect();
        Edge [] edges2 = (Edge []) arg._2().edges().collect();
        Tuple2<Object,String> [] vertices1 = (Tuple2<Object,String>[]) arg._1().vertices().collect();
        Tuple2<Object,String> [] vertices2 = (Tuple2<Object,String>[]) arg._2().vertices().collect();

        List<Edge> edgeL1 = new ArrayList<>(); edgeL1.addAll(Arrays.asList(edges1));
        List<Edge> edgeL2 = new ArrayList<>(); edgeL2.addAll(Arrays.asList(edges2));
        List<Tuple2<Object,String>> vertL1 = new ArrayList<>(); vertL1.addAll(Arrays.asList(vertices1));
        List<Tuple2<Object,String>> vertL2 = new ArrayList<>(); vertL2.addAll(Arrays.asList(vertices2));

        System.out.println(String.format("E/V %d/%d , E/V %d/%d",edgeL1.size(),vertL1.size(),edgeL2.size(),vertL2.size()));
        boolean notIdentical = true;
        if(vertL1.equals(vertL2) && edgeL1.equals(edgeL2)) {
            notIdentical = false; // same graph
            System.out.println("EQUAL");
            return false;
        }
        int hashCode1 = new DPair(vertL1,edgeL1).hashCode();
        int hashCode2 =  new DPair(vertL2,edgeL2).hashCode();
        int totalHash = new DPair(hashCode1,hashCode2).hashCode();
        if(hash_cache.contains(totalHash))
            return false;
        // add the codes
        hash_cache.add(totalHash);

        System.out.println("Obj hashcodes:" + hashCode1 + " " + hashCode2);
        System.out.println("\tInserting. Code: " + totalHash  + " \n.\t\t Cache now: " + hash_cache);


        return   true;

    }
}
