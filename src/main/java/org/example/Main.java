package org.example;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Main {

    private static Logger log = LoggerFactory.getLogger(Main.class);
    private static final String CACHE_KEY = "KEY";

    public static void main(String[] args) {
        Cache<String, Integer> cache = Caffeine.newBuilder()
                .maximumSize(10000)
                .refreshAfterWrite(Duration.ofSeconds(10))
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build(new CustomCacheLoader());

        Thread cachePooler = new Thread(() -> {
            while (true) {
                log.info("Getting value for key: {} -> {}", CACHE_KEY, cache.get(CACHE_KEY, getFirstCacheValue()));
                waitSeconds(1);
            }
        });

        cachePooler.start();
    }

    private static void waitSeconds(long time) {
        try {
            Thread.sleep(time * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Function<String, @PolyNull Integer> getFirstCacheValue() {
        return key -> {
            log.info("There is no value in the cache for this key, returning the first value: 1000");
            return 1000;
        };
    }

    private static class CustomCacheLoader implements CacheLoader<String, Integer> {

        @Override
        public CompletableFuture<? extends Integer> asyncReload(String key, Integer oldValue, Executor executor) throws Exception {
            return CompletableFuture.supplyAsync(() -> {
                log.info("Computing the next value for key {}, which is: {}", key, oldValue + 1);
                waitSeconds(5); // simulating a call to an external service
                return oldValue + 1;
            });
        }

        @Override
        public @Nullable Integer load(String key) throws Exception {
            log.info("Loading value for key: {}", key);
            return 100;
        }
    }
}