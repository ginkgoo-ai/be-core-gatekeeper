package com.ginkgooai.core.gatekeeper.config.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// Create serializer
		GenericJackson2JsonRedisSerializer serializer = createGenericJackson2JsonRedisSerializer();

		// Set serializers
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(serializer);
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(serializer);

		return template;
	}

	private GenericJackson2JsonRedisSerializer createGenericJackson2JsonRedisSerializer() {
		// Create ObjectMapper with custom configuration
		ObjectMapper objectMapper = JsonMapper.builder()
			.addModule(new JavaTimeModule())
			.activateDefaultTyping(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
					ObjectMapper.DefaultTyping.NON_FINAL)
			.build();

		return new GenericJackson2JsonRedisSerializer(objectMapper);
	}

}
