package org.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.example.serializer.JodaDateTimeJsonDeserializer;
import org.example.serializer.JodaDateTimeJsonSerializer;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @date 2021/5/15-17:14
 */
@Component
public class RedisConfig {
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        //首先解决key的序列化方式
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);

        //解决value的序列化方式
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);

        ObjectMapper objectMapper=new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(DateTime.class,new JodaDateTimeJsonSerializer());
        simpleModule.addDeserializer(DateTime.class,new JodaDateTimeJsonDeserializer());
        //在redis中添加类的信息
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

        objectMapper.registerModule(simpleModule);

        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);

        return redisTemplate;
    }
}
