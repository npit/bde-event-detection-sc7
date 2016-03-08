/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.clustering.model;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 * @param <ObjTypeFirst>
 * @param <ObjTypeSecond>
 */
public class DPair<ObjTypeFirst, ObjTypeSecond> implements Serializable {

    protected ObjTypeFirst first;
    protected ObjTypeSecond second;

    /**
     * Creates a new instance of Pair, given two objects.
     *
     * @param oFirst The first object.
     * @param oSecond The second object.
     */
    public DPair(ObjTypeFirst oFirst, ObjTypeSecond oSecond) {
        first = oFirst;
        second = oSecond;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.first) + Objects.hashCode(this.second);
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
        final DPair<?, ?> other = (DPair<?, ?>) obj;
        return this.hashCode() == other.hashCode();
    }

    /**
     *
     * @return The first object.
     */
    public ObjTypeFirst getFirst() {
        return first;
    }

    /**
     *
     * @return The second object.
     */
    public ObjTypeSecond getSecond() {
        return second;
    }
}
