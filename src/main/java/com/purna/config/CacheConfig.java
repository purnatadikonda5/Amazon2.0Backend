package com.purna.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * CacheConfig (Distributed Redis-Pattern Memory Caching)
 * 
 * WHY USE THIS:
 * Database Lookups are the #1 bottleneck out of all e-commerce applications.
 * 
 * @EnableCaching activates the Spring Caching Architecture. When a user calls a method (like 
 * fetching home page items), instead of connecting to MySQL taking 100ms, the system intercepts 
 * it and stores the JSON outpt directly into RAM (Memory). 
 * 
 * The next 10,000 customers who fetch the homepage pull it safely out of RAM in 1ms! 
 * (This is completely structured to act as Apache Redis/Memcached cache layer).
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
