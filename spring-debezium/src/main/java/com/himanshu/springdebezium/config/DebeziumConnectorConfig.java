package com.himanshu.springdebezium.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class DebeziumConnectorConfig {
    @Value("${offset.storage.file}")
    private String offsetStorageFile;
    @Value("${database.history.file}")
    private String dbSchemaHistoryFile;

    // Since debezium 2.1 file based db history will not work. Use debezium-> 1.9.3.FINAL to use file based store
/*    @Bean
    public io.debezium.config.Configuration fileBasedConnector() throws IOException {
        return io.debezium.config.Configuration.create()
                .with("name", "db-mysql-connector-1")
                .with("connector.class", "io.debezium.connector.mysql.MySqlConnector")
                .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename", offsetStorageFile)
                .with("offset.flush.interval.ms", "50000")
                .with("database.hostname", "localhost")
                .with("database.port", "3306")
                .with("database.user", "admin")
                .with("database.password", "admin")
                .with("database.include.list", "practice_db")
                .with("include.schema.changes", "false")
                .with("database.server.id", "1")
                .with("database.server.name", "mysql_localhost_connect")
                .with("database.history", "io.debezium.relational.history.FileDatabaseHistory")
                .with("database.history.file.filename", dbSchemaHistoryFile)
                .build();
    }*/

    @Bean
    public io.debezium.config.Configuration redisBasedConnector() throws IOException {
        // full configs- https://debezium.io/documentation/reference/stable/operations/debezium-server.html
        return io.debezium.config.Configuration.create()
                .with("name", "db-mysql-connector-2")
                .with("connector.class", "io.debezium.connector.mysql.MySqlConnector")
                .with("offset.storage", "io.debezium.storage.redis.offset.RedisOffsetBackingStore")
                .with("offset.storage.redis.address", "127.0.0.1:6379")
                .with("offset.storage.redis.key", "some_random_offset_key")
                .with("offset.flush.interval.ms", "50000")
                .with("topic.prefix", "debezium_db")
                .with("database.hostname", "localhost")
                .with("database.port", "3306")
                .with("database.user", "admin")
                .with("database.password", "admin")
                .with("database.include.list", "practice_db")
                .with("include.schema.changes", "false")
                .with("database.server.id", "2")
                .with("database.server.name", "mysql_localhost_connect")
                .with("schema.history.internal", "io.debezium.storage.redis.history.RedisSchemaHistory")
                .with("schema.history.internal.redis.address", "127.0.0.1:6379")
                .with("schema.history.internal.redis.key", "some_random_history_key")
                .build();
    }
}
