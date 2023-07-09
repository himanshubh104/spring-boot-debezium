package com.himansh.dataobserver.config;

import com.himansh.dataobserver.constant.ApplicationConstant;
import io.debezium.util.DelayStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;
import org.springframework.core.env.Environment;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RedisOffsetStore extends MemoryOffsetBackingStore {
    private RedisClient redisClient;
    private String redisOffsetStoreKey;
    private Long minRetryDelay;
    private Long maxRetryDelay;

    void connect() {
        try {
            this.redisClient = AppContext.getBean(RedisClient.class);
            Environment env = AppContext.getBean(Environment.class);
            this.redisOffsetStoreKey = env.getProperty("offset.store.key");
            minRetryDelay = ApplicationConstant.MIN_RETRY_DELAY;
            maxRetryDelay = ApplicationConstant.MAX_RETRY_DELAY;
            log.info("Schema key: {}", redisOffsetStoreKey);
            if (redisOffsetStoreKey == null) {
                log.error("Redis Schema history key is null");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public synchronized void start() {
        super.start();
        log.info("Starting RedisOffsetBackingStore");
        this.connect();
        this.load();
    }

    /**
     * Load offsets from Redis keys
     */
    private void load() {
        DelayStrategy delayStrategy = DelayStrategy.exponential(Duration.ofMillis(minRetryDelay), Duration.ofMillis(maxRetryDelay));
        boolean isCompleted = false;
        // fetch the value from Redis
        try {
            Map<String, String> offsets = redisClient.hgetAll(redisOffsetStoreKey);
            this.data = new HashMap<>();
            for (Map.Entry<String, String> mapEntry : offsets.entrySet()) {
                ByteBuffer key = (mapEntry.getKey() != null) ? ByteBuffer.wrap(mapEntry.getKey().getBytes()) : null;
                ByteBuffer value = (mapEntry.getValue() != null) ? ByteBuffer.wrap(mapEntry.getValue().getBytes()) : null;
                data.put(key, value);
            }
            isCompleted = true;
        } catch (JedisConnectionException e) {
            log.error("Unable to connect to jedis client, will retry");
        } catch (Exception e) {
            log.info("Unable to save offset to redis", e);
            isCompleted = true;
        }
        // Failed to execute the transaction, retry...
        delayStrategy.sleepWhen(!isCompleted);
    }

    /**
     * Save offsets to Redis keys
     */
    @Override
    protected void save() {
        DelayStrategy delayStrategy = DelayStrategy.exponential(Duration.ofMillis(minRetryDelay), Duration.ofMillis(maxRetryDelay));
        boolean isCompleted = false;
        // loop and retry until successful
        while (!isCompleted) {
            try {
                if (ApplicationConstant.allowCommit.get()) {
                    for (Map.Entry<ByteBuffer, ByteBuffer> mapEntry : data.entrySet()) {
                        byte[] key = (mapEntry.getKey() != null) ? mapEntry.getKey().array() : null;
                        byte[] value = (mapEntry.getValue() != null) ? mapEntry.getValue().array() : null;
                        // set the value in Redis
                        long status = redisClient.hset(redisOffsetStoreKey.getBytes(), key, value);
                        log.info("Offset Stored status: {}", status);
                    }
                }
                isCompleted = true;
            } catch (JedisConnectionException e) {
                log.error("Unable to connect to jedis client, will retry");
            } catch (Exception e) {
                log.info("Unable to save offset to redis", e);
                isCompleted = true;
            }
            // Failed to execute the transaction, retry...
            delayStrategy.sleepWhen(!isCompleted);
        }
    }

}
