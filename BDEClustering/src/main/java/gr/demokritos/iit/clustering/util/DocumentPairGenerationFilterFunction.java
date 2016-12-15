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
import java.util.HashSet;
import java.util.Set;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;
import scala.Tuple4;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class DocumentPairGenerationFilterFunction implements
        Function<Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>>, Boolean> {

    // keep a hash reference in RAM of each pair, and return item if the hash of each other is not equal or the pair does not exist
    private static final Set<Integer> hash_cache = new HashSet<>();
    private synchronized boolean addToCache(int code)
    {
        return hash_cache.add(code);
    }
    public DocumentPairGenerationFilterFunction() {
    }
    // expects the full combination list of n articles (n*n). Filters the same instances of (i,j) - i.e where i=j and the 
    // around ones i.e. i, j = j, i
    @Override
    public Boolean call(Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> v1) throws Exception {
        boolean value = !v1._1._1().equals(v1._2._1()) && addToCache(new DPair(v1._1._1(), v1._2._1()).hashCode());
        //System.out.println("doc pair value : " + value);
        return value;
    }
}
