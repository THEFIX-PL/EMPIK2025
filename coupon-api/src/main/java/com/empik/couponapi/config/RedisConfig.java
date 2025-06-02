package com.empik.couponapi.config;


import com.empik.shared.event.CouponCreateResponseEvent;
import com.empik.shared.event.CouponUseResponseEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, CouponCreateResponseEvent> createRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, CouponCreateResponseEvent> template = new RedisTemplate<>();
        configureTemplate(template, connectionFactory);
        return template;
    }

    @Bean
    public RedisTemplate<String, CouponUseResponseEvent> useRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, CouponUseResponseEvent> template = new RedisTemplate<>();
        configureTemplate(template, connectionFactory);
        return template;
    }

    private void configureTemplate(RedisTemplate<String, ?> template, RedisConnectionFactory connectionFactory) {
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
    }
}