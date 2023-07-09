package com.himansh.dataobserver.config;

import com.himansh.dataobserver.constant.ApplicationConstant;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.resps.StreamEntry;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("redisClient")
public class RedisClient {

	public static final Logger log = org.slf4j.LoggerFactory.getLogger(RedisClient.class);

	private JedisPool pool;

	@PostConstruct
	public void initialize() {
		try {
			JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
			jedisPoolConfig.setMaxTotal(ApplicationConstant.REDIS_MAX_TOTAL);
			jedisPoolConfig.setMaxIdle(ApplicationConstant.REDIS_MAX_IDLE);
			jedisPoolConfig.setMinIdle(ApplicationConstant.REDIS_MIN_IDLE);
			URI uri = URI.create(ApplicationConstant.REDIS_SERVER_URL);
			pool = new JedisPool(jedisPoolConfig, uri, ApplicationConstant.REDIS_SERVER_CONNECT_TIMEOUT, ApplicationConstant.REDIS_SERVER_SO_TIMEOUT);
			try (Jedis client = pool.getResource()){
				if (client.isBroken()) throw new JedisConnectionException("Unable to create Jedis pool please check the configurations.");
			}
			log.info("Redis connection established.");
		} catch (JedisConnectionException e) {
			log.error("Error in creating jedis pool", e);
			throw e;
		}
	}

	@PreDestroy
	public void close() {
		if (pool!=null && !pool.isClosed()) {
			pool.close();
		}
	}

	public String get(String key) {
		try (Jedis jedis = pool.getResource()) {
			return jedis.get(key);
		}
	}

	public String set(String key, String value) {
		try (Jedis jedis = pool.getResource()) {
			return jedis.set(key, value);
		}
	}

	public String setValueWithExpiry(String key, String value, int expiry) {
		try (Jedis jedis = pool.getResource()) {
			return jedis.setex(key, expiry, value);
		}
	}

	public void delete(String key) {
		try (Jedis jedis = pool.getResource()) {
			jedis.del(key);
		}
	}

	public void setKeyWithExpiry(String key, int seconds) {
		try (Jedis jedis = pool.getResource()) {
			jedis.expire(key, seconds);
		}
	}

	public void rpush(String listKey, List<String> values) {
		try (Jedis jedis = pool.getResource()) {
			jedis.rpush(listKey, values.toArray(String[]::new));

		}
	}

	public void delMultiple(String[] keys) {
		try (Jedis jedis = pool.getResource()) {
			jedis.del(keys);
		}
	}

	public boolean exits(String key) {
		try (Jedis jedis = pool.getResource()) {
			return jedis.exists(key);
		}
	}

	public Jedis getJedis() {
		return pool.getResource();
	}

	public void closeJedis(Jedis jedis) {
		jedis.close();
	}

	public JedisPool getPool() {
		return pool;
	}

	public List<Map<String, String>> xrange(String key) {
		try (Jedis jedis = pool.getResource()) {
			return jedis.xrange(key, (StreamEntryID) null, (StreamEntryID) null).stream().map(StreamEntry::getFields).collect(Collectors.toList());
		}
	}

	public String xadd(String key, Map<String, String> hash) {
		try (Jedis jedis = pool.getResource()) {
			return jedis.xadd(key, (StreamEntryID) null, hash).toString();
		}
	}

	public long xlen(String key) {
		try (Jedis jedis = pool.getResource()) {
			return jedis.xlen(key);
		}
	}

	public Map<String, String> hgetAll(String key) {
		try (Jedis jedis = pool.getResource()) {
			return jedis.hgetAll(key);
		}
	}

	public long hset(byte[] key, byte[] field, byte[] value) {
		try (Jedis jedis = pool.getResource()) {
			return jedis.hset(key, field, value);
		}
	}

}