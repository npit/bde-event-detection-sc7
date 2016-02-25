/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.clustering.util;

import java.util.Objects;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;
import scala.Tuple4;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class DocumentPairGenerationFilterFunction implements Function<Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>>, Boolean> {

    // TODO: keep a hash reference in RAM of each pair, and return item if the hash of each other is not equal or the pair does  not exist.
    
    @Override
    public Boolean call(Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> v1) throws Exception {
        new TwoWayPair(String a, String b)
    }
    
    class TwoWayPair<ObjTypeFirst, ObjTypeSecond> extends Pair {

    public TwoWayPair(Object oFirst, Object oSecond) {
        super(oFirst, oSecond);
    }

    @Override
    public final int hashCode() {
        int hash = 3;
        hash = 43 * hash + Objects.hashCode(this.first) + Objects.hashCode(this.second);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TwoWayPair other = (TwoWayPair) obj;
        return this.hashCode() == other.hashCode();
    }
}
