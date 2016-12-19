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

        import java.util.ArrayList;
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
    private  final Set<Integer>  hash_cache;

    public DocumentPairGenerationFilterFunction() {
        this.hash_cache = new HashSet();
    }
    // expects the full combination list of n articles (n*n). Filters the same instances of (i,j) - i.e where i=j and the
    // around ones i.e. i, j = j, i
    @Override
    public Boolean call(Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> arg) throws Exception {
//        String msg1 = ("cache b4: " + hash_cache);
//        boolean value = !arg._1._1().equals(arg._2._1()) && hash_cache.add(new DPair(arg._1._1(), arg._2._1()).hashCode());
//        String msg2  = ("\ndocs " + arg._1._1() + "  |||  " + arg._2._1() + " : " + value);
//        String msg3 = ("cache after: " + hash_cache);
//        ArrayList<String> l = new ArrayList<>();
//        System.out.println("\n" + msg1 + "\n" + msg2 + "\n" + msg3);
        int hash1 = (arg._1()._1() + arg._1()._2()).hashCode();
        int hash2 = (arg._2()._1() + arg._2()._2()).hashCode();

        return hash1 > hash2;
    }
}