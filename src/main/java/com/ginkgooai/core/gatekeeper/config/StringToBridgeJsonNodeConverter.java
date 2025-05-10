package com.ginkgooai.core.gatekeeper.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class StringToBridgeJsonNodeConverter implements Converter<String, JsonNode> {

	private final ObjectMapper objectMapper;

	public StringToBridgeJsonNodeConverter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	@Nullable
	public JsonNode convert(String source) {
		if (!StringUtils.hasText(source)) {
			// 对于空字符串或仅包含空白的字符串，返回null
			// 或者你可以选择返回一个空的JsonNode: objectMapper.createObjectNode() 或
			// objectMapper.nullNode()
			return null;
		}
		try {
			return objectMapper.readTree(source);
		}
		catch (IOException e) {
			// 可以选择记录日志或抛出自定义异常
			// 这里简单地返回null，或者你可以根据需求抛出IllegalArgumentException
			// throw new IllegalArgumentException(\"Failed to convert String to JsonNode:
			// \"
			// + source, e);
			return null;
		}
	}

}