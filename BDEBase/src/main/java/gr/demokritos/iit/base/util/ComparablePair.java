package gr.demokritos.iit.base.util;

public class ComparablePair<T1,T2 extends Comparable> implements Comparable<ComparablePair>{
    T1 data;

    public T1 getData() {
        return data;
    }

    public ComparablePair(T1 data, T2 comparableData) {
        this.data = data;
        this.comparableData = comparableData;
    }

    public T2 getComparableData() {
        return comparableData;
    }

    T2 comparableData;
    @Override
    public int compareTo(ComparablePair o) {
        ComparablePair<T1,T2> other = (ComparablePair<T1,T2>) o;
        return this.getComparableData().compareTo(other.comparableData);
    }
}
