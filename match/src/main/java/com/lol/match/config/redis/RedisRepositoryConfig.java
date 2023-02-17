package com.lol.match.config.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
@EnableRedisRepositories
public class RedisRepositoryConfig {
    
    @Value("${spring.redis.port}")
    private int REDIS_PORT;

    @Value("${spring.redis.host}")
    private String REDIS_HOST;

    // lettuce
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(REDIS_HOST, REDIS_PORT); // Lettuce 사용
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>(); 
        
        redisTemplate.setConnectionFactory(redisConnectionFactory());  
        redisTemplate.setKeySerializer(new StringRedisSerializer());   // Key: String 
        redisTemplate.setValueSerializer(new StringRedisSerializer()); // Value: 직렬화에 사용할 Object 사용하기   

        /*
         * 해쉬 사용시 사용
         */
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

}
