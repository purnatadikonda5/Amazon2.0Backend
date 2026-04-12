package com.purna.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * RedisConfig
 * 
 * WHY USE THIS:
 * While @Cacheable handles automated DB-result caching natively, we also need direct structural 
 * access to the AWS Redis node to manually save TTL (Time-To-Live) Refresh Tokens securely. 
 * StringRedisTemplate is incredibly fast and memory-efficient.
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
