package gr.demokritos.iit.location.util;

/**
 * Created by nik on 6/8/17.
 */
public class Pair<T1,T2> {
    T1 first;
    T2 second;

    public T1 first() {
        return first;
    }

    public T2 second() {
        return second;
    }

    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

}
