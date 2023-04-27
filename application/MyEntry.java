package multitile.application;

import java.util.Map;


public class MyEntry<K, V> implements Map.Entry<K, V> {
    private final K key;
    private V value;

    public MyEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        V old = this.value;
        this.value = value;
        return old;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof MyEntry<?, ?>))
            return false;
        @SuppressWarnings("unchecked")
        MyEntry<K,V> pair = (MyEntry<K,V>) other;
        return
            key.equals(pair.getKey()) &&
            value.equals(pair.getValue());
    }

    @Override
    public int hashCode() {
        return key.hashCode() ^ value.hashCode();
    }

}