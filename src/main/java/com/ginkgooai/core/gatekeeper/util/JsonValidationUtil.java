package com.ginkgooai.core.gatekeeper.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * JSON字段验证工具类
 */
public class JsonValidationUtil {

	private static final Logger logger = LoggerFactory.getLogger(JsonValidationUtil.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 验证字符串是否为有效的JSON格式，并返回JsonNode对象
	 * @param jsonValue 要验证的JSON字符串
	 * @param fieldName 字段名称（用于记录日志）
	 * @return 解析后的JsonNode对象，如果输入为空则返回null
	 * @throws IllegalArgumentException 如果JSON格式无效
	 */
	public static JsonNode validateJson(String jsonValue, String fieldName) {
		if (!StringUtils.hasText(jsonValue)) {
			return null;
		}

		try {
			// 尝试解析JSON
			return objectMapper.readTree(jsonValue);
		}
		catch (JsonProcessingException e) {
			logger.warn("Invalid JSON format for {}: {}", fieldName, e.getMessage());
			throw new IllegalArgumentException(fieldName + " must be valid JSON: " + e.getMessage());
		}
	}

}
