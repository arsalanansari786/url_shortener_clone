package org.arsalanansari.usp.Configuration;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RedisConfiguration {


    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory(){
        try {
            RedisStandaloneConfiguration redisStandaloneConfiguration=new RedisStandaloneConfiguration("127.0.0.1", 6379);
           LettuceClientConfiguration clientConfig =LettucePoolingClientConfiguration.builder()
                                                    .commandTimeout(Duration.ofSeconds(2))
                                                    .shutdownTimeout(Duration.ZERO)
                                                    .build();

            return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
           } catch (Exception e) {
            log.error("Failed to create Redis connection factory", e);
        }
        return null;
    }

    public RedisTemplate<String,Object>redisTemplate(LettuceConnectionFactory lettuceConnectionFactory){
        try {
            if(lettuceConnectionFactory==null){
                log.warn("Redis connection is not established, skipping RedisTemplate setup");
                return null;
            }
            RedisTemplate<String ,Object>redisTemplate=new RedisTemplate<>();
            redisTemplate.setConnectionFactory(lettuceConnectionFactory);
            redisTemplate.setKeySerializer(new StringRedisSerializer());
            redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            redisTemplate.afterPropertiesSet();
            return redisTemplate;
        } catch (Exception e) {
            log.error("Failed to create RedisTemplate", e);
        }
        return null;
    }
}
