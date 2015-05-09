package org.croudtrip.utils;

/**
 * A simple pair class.
 * Created by Frederik Simon on 08.05.2015.
 */
public class Pair<K,V> {
    private K key;
    private V value;

    public Pair(K key, V value ) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public void setKey( K key ) {
        this.key = key;
    }

    public void setValue ( V value ) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return key.hashCode() ^ value.hashCode();
    }

    @Override
    public boolean equals( Object o ) {
        if( o == null ) return false;
        if( !(o instanceof Pair)) return false;

        Pair p = (Pair) o;
        return this.key.equals(p.key) && this.value.equals(p.value);
    }
}
