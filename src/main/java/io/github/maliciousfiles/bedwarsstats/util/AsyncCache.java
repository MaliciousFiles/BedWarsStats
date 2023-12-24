package io.github.maliciousfiles.bedwarsstats.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AsyncCache<K, V> {
    private final Map<K, Long> expirations = new HashMap<>();
    private final Map<K, V> inner = new HashMap<>();

    private final List<K> refreshing = new ArrayList<>();

    private final Function<K, Map.Entry<V, Long>> supplier;

    public AsyncCache(Function<K, Map.Entry<V, Long>> supplier) {
        super();

        this.supplier = supplier;
    }

    public V get(Object key) {
        if (System.currentTimeMillis() >= expirations.getOrDefault(key, 0L)) {
            inner.remove(key);

            if (!refreshing.contains(key)) {
                refreshing.add((K) key);
                new Thread(() -> {
                    Map.Entry<V, Long> entry = supplier.apply((K) key);
                    inner.put((K) key, entry.getKey());
                    expirations.put((K) key, System.currentTimeMillis()+entry.getValue());
                    refreshing.remove(key);
                }).start();
            }

            return null;
        }

        return inner.get(key);
    }
}
