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
import org.apache.spark.graphx.Graph;
import scala.Tuple2;
import scala.Tuple4;

import java.util.HashSet;
import java.util.Set;

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
    public Boolean call(Tuple2<Graph<String, Object>,Graph<String, Object>> v1) throws Exception {

        return !v1._1().equals(v1._2()) && hash_cache.add(new DPair(v1._1(), v1._2()).hashCode());
    }
}
