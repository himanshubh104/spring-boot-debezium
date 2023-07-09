package com.himansh.dataobserver.constant;

import com.himansh.dataobserver.config.AppContext;
import org.springframework.core.env.Environment;

import java.util.concurrent.atomic.AtomicBoolean;

public class ApplicationConstant {
    private static final Environment env = AppContext.getBean(Environment.class);

    /*-- Debezium Properties --*/
    public static final String DEBEZIUM_CONNECTOR_CLASS = "io.debezium.connector.mysql.MySqlConnector";
    public static final String DEBEZIUM_OFFSET_STORAGE = "com.himansh.dataobserver.config.RedisOffsetStore";
    public static final String DEBEZIUM_DB_HISTORY = "com.himansh.dataobserver.config.RedisSchemaHistory";
    public static final String CONNECTOR_NAME = "debezium_mysql_connector";

    /*-- Redis Properties --*/
    public static final String REDIS_SERVER_URL = env.getProperty("redis.server.url", "http://127.0.0.1:6379");
    public static final Integer REDIS_MAX_TOTAL = Integer.valueOf(env.getProperty("redis.max.total", "100"));
    public static final Integer REDIS_MAX_WAIT = Integer.valueOf(env.getProperty("redis.max.wait", "20"));
    public static final Integer REDIS_MAX_IDLE = Integer.valueOf(env.getProperty("redis.max.idle", "20"));
    public static final Integer REDIS_MIN_IDLE = Integer.valueOf(env.getProperty("redis.min.idle", "4"));
    public static final Integer REDIS_SERVER_SO_TIMEOUT = Integer.valueOf(env.getProperty("redis.server.so.timeout", "20"));
    public static final Integer REDIS_SERVER_CONNECT_TIMEOUT = Integer.valueOf(env.getProperty("redis.connect.acquire.timeout", "20"));
    public static final Long MIN_RETRY_DELAY = Long.valueOf(env.getProperty("redis.retry.min.duration.millis", "2000"));
    public static final Long MAX_RETRY_DELAY = Long.valueOf(env.getProperty("redis.retry.max.duration.millis", "5000"));

    /*-- Application Properties --*/
    public static volatile AtomicBoolean allowCommit = new AtomicBoolean(true);
}
