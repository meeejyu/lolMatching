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

import com.lol.match.domain.dto.UserMatchDto;

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

    // @Bean
    // public RedisTemplate<String, ?> redisTemplate(RedisConnectionFactory connectionFactory) {
    //     // redisTemplate를 받아와서 set, get, delete를 사용
    //     RedisTemplate<String, ?> redisTemplate = new RedisTemplate<>();
    //      /**
    //      * setKeySerializer, setValueSerializer 설정
    //      * redis-cli을 통해 직접 데이터를 조회 시 알아볼 수 없는 형태로 출력되는 것을 방지
    //      * StringRedisSerializer string으로 인코딩해준다
    //      */
    //     redisTemplate.setKeySerializer(new StringRedisSerializer());
    //     redisTemplate.setValueSerializer(new StringRedisSerializer());
    //     redisTemplate.setConnectionFactory(connectionFactory);
    
    //     /*
    //      * 해쉬 사용시 사용
    //      */
    //     redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    //     redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        
    //     return redisTemplate;
    // }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>(); 
        
        redisTemplate.setConnectionFactory(redisConnectionFactory());  
        redisTemplate.setKeySerializer(new StringRedisSerializer());   // Key: String 
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(String.class)); // Value: 직렬화에 사용할 Object 사용하기   

        /*
         * 해쉬 사용시 사용
         */
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        // redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(String.class)); // Value: 직렬화에 사용할 Object 사용하기   

        return redisTemplate;
    }

}
