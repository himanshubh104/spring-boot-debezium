package com.himansh.dataobserver.config;

import com.himansh.dataobserver.constant.ApplicationConstant;
import io.debezium.config.Configuration;
import io.debezium.document.DocumentReader;
import io.debezium.document.DocumentWriter;
import io.debezium.relational.history.*;
import io.debezium.util.DelayStrategy;
import io.debezium.util.Loggings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class RedisSchemaHistory extends AbstractSchemaHistory {

    private final DocumentWriter writer = DocumentWriter.defaultWriter();
    private final DocumentReader reader = DocumentReader.defaultReader();
    private RedisClient client;
    private String schemaKey;
    private Long minRetryDelay;
    private Long maxRetryDelay;

    void connect() {
        try {
            client = AppContext.getBean(RedisClient.class);
            Environment env = AppContext.getBean(Environment.class);
            schemaKey = env.getProperty("schema.history.key");
            minRetryDelay = ApplicationConstant.MIN_RETRY_DELAY;
            maxRetryDelay = ApplicationConstant.MAX_RETRY_DELAY;
            if (schemaKey == null) {
                logger.error("Redis Schema history key is null");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        log.info("Schema key: {}", schemaKey);
    }

    @Override
    public void configure(Configuration config, HistoryRecordComparator comparator, SchemaHistoryListener listener, boolean useCatalogBeforeSchema) {
        super.configure(config, comparator, listener, useCatalogBeforeSchema);
    }

    @Override
    public synchronized void start() {
        super.start();
        log.info("Starting RedisSchemaHistory");
        this.connect();
    }

    /**
     * Save history records to Redis keys
     */
    @Override
    protected void storeRecord(HistoryRecord record) throws SchemaHistoryException {
        if (record == null) {
            return;
        }
        String line;
        try {
            line = writer.write(record.document());
        }
        catch (IOException e) {
            Loggings.logErrorAndTraceRecord(log, record, "Failed to convert record to string", e);
            throw new SchemaHistoryException("Unable to write database schema history record");
        }

        DelayStrategy delayStrategy = DelayStrategy.exponential(Duration.ofMillis(minRetryDelay), Duration.ofMillis(maxRetryDelay));
        boolean isCompleted = false;
        // loop and retry until successful
        while (!isCompleted) {
            try {
                // write the entry to Redis
                client.xadd(schemaKey, Collections.singletonMap("schema", line));
                log.trace("Record written to database schema history in Redis: " + line);
                isCompleted = true;
            } catch (JedisConnectionException e) {
                log.error("Unable to connect to jedis client, will retry");
            } catch (Exception e) {
                log.error("Writing to database schema history stream failed", e);
                isCompleted = true;
            }
            // Failed to execute the transaction, retry...
            delayStrategy.sleepWhen(!isCompleted);
        }
    }

    /**
     * Load history records from Redis keys
     */
    @Override
    protected synchronized void recoverRecords(Consumer<HistoryRecord> records) {
        List<Map<String, String>> entries = new ArrayList<>();
        DelayStrategy delayStrategy = DelayStrategy.exponential(Duration.ofMillis(minRetryDelay), Duration.ofMillis(maxRetryDelay));
        boolean isCompleted = false;
        // loop and retry until successful
        while (!isCompleted) {
            try {
                // read the entries from Redis
                entries = client.xrange(schemaKey);
                isCompleted = true;
            } catch (JedisConnectionException e) {
                log.error("Unable to connect to jedis client, will retry");
            } catch (Exception e) {
                log.error("Reading from database schema history stream failed with exception", e);
                isCompleted = true;
            }
            // Failed to execute the transaction, retry...
            delayStrategy.sleepWhen(!isCompleted);
        }

        log.info("Reading schema records....");
        for (Map<String, String> item : entries) {
            try {
                records.accept(new HistoryRecord(reader.read(item.get("schema"))));
            }
            catch (IOException e) {
                log.error("Failed to convert record to string: {}", item, e);
                return;
            }
        }

    }

    @Override
    public boolean storageExists() {
        return true;
    }

    @Override
    public boolean exists() {
        // check if the stream is not empty
        return client != null && client.xlen(schemaKey) > 0;
    }
}
