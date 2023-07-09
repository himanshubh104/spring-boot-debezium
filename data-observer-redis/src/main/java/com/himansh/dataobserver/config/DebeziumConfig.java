package com.himansh.dataobserver.config;

import com.himansh.dataobserver.constant.ApplicationConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
public class DebeziumConfig {
    @Bean
    public io.debezium.config.Configuration connector(Environment env) {
        log.info("Initializing Debezium Configs...");
        return io.debezium.config.Configuration.create()
                .with("name", ApplicationConstant.CONNECTOR_NAME)
                .with("connector.class", ApplicationConstant.DEBEZIUM_CONNECTOR_CLASS)
                // debezium-redis-offset-configs
                .with("offset.storage", ApplicationConstant.DEBEZIUM_OFFSET_STORAGE)
                .with("offset.flush.interval.ms", env.getProperty("offset.flush.interval.ms"))
                // debezium-database-configs
                .with("database.server.id", env.getProperty("datasource.server-id"))
                .with("database.server.name", "my-db-server")
                .with("database.hostname", env.getProperty("datasource.host"))
                .with("database.port", env.getProperty("datasource.port"))
                .with("database.user", env.getProperty("datasource.username"))
                .with("database.password", env.getProperty("datasource.password"))
                .with("include.schema.changes", env.getProperty("include.schema.changes"))
                .with("database.include.list", env.getProperty("datasource.database"))
                .with("table.include.list", env.getProperty("datasource.tables.list"))
                // debezium-database-schema-related-configs
                .with("topic.prefix", "db_observer")
                .with("schema.history.internal", ApplicationConstant.DEBEZIUM_DB_HISTORY)
                .build();
    }
}
