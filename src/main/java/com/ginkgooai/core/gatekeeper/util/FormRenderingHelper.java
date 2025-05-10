package com.ginkgooai.core.gatekeeper.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.ginkgooai.core.gatekeeper.domain.types.ValidationRuleType; // Import the enum
import com.ginkgooai.core.gatekeeper.dto.ValidationRuleDTO; // Import the DTO
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional; // Import Optional

@Component
@Slf4j
public class FormRenderingHelper {

	private final ObjectMapper objectMapper;

	public FormRenderingHelper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	// --- Existing methods ---
	public List<Map<String, String>> parseOptions(String staticOptionsJson) {
		// ... (implementation as before) ...
		if (staticOptionsJson == null || staticOptionsJson.isBlank()) {
			return Collections.emptyList();
		}
		try {
			TypeReference<List<Map<String, String>>> typeRef = new TypeReference<>() {
			};
			return objectMapper.readValue(staticOptionsJson, typeRef);
		}
		catch (JsonProcessingException e) {
			log.warn("Failed to parse staticOptions JSON: {}", staticOptionsJson, e);
			return Collections.emptyList();
		}
	}

	public Map<String, Object> parseUiProperties(String uiPropertiesJson) {
		// ... This method might still be used elsewhere, or can be deprecated/removed
		// if not.
		// Keep it for now in case other parts of the code rely on parsing the String
		// version.
		if (uiPropertiesJson == null || uiPropertiesJson.isBlank()) {
			return Collections.emptyMap();
		}
		try {
			TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {
			};
			return objectMapper.readValue(uiPropertiesJson, typeRef);
		}
		catch (JsonProcessingException e) {
			log.warn("Failed to parse uiProperties JSON: {}", uiPropertiesJson, e);
			return Collections.emptyMap();
		}
	}

	// --- Updated methods to accept JsonNode ---

	public String getUiPropString(JsonNode props, String key) {
		if (props == null || !props.has(key) || !props.get(key).isTextual()) {
			return null;
		}
		return props.get(key).asText();
	}

	public Integer getUiPropInt(JsonNode props, String key) {
		if (props == null || !props.has(key) || !props.get(key).canConvertToInt()) {
			return null;
		}
		// Use asInt() which handles potential conversion from text node
		return props.get(key).asInt();
	}

	public Boolean getUiPropBool(JsonNode props, String key) {
		if (props == null || !props.has(key)) {
			return null; // Or return default false? Depends on requirement
		}
		// Use asBoolean() which handles true/false literals and potentially textual
		// "true"/"false"
		return props.get(key).asBoolean();
	}

	// --- Existing helper methods for validations ---

	// Find a specific validation rule DTO by type
	private Optional<ValidationRuleDTO> findRuleByType(List<ValidationRuleDTO> rules, ValidationRuleType type) {
		if (rules == null || type == null) {
			return Optional.empty();
		}
		return rules.stream().filter(rule -> type.equals(rule.getType())).findFirst();
	}

	// Check if a rule of a specific type exists
	public boolean hasRule(List<ValidationRuleDTO> rules, ValidationRuleType type) {
		return findRuleByType(rules, type).isPresent();
	}

	// Get the value of a rule as String, if it exists
	public String getRuleValue(List<ValidationRuleDTO> rules, ValidationRuleType type) {
		return findRuleByType(rules, type).map(ValidationRuleDTO::getValue) // Assumes
																			// ValidationRuleDTO
																			// has
																			// getValue()
			.orElse(null);
	}

	// Get the value of a rule as Integer, if it exists and is parseable
	public Integer getRuleValueAsInt(List<ValidationRuleDTO> rules, ValidationRuleType type) {
		String value = getRuleValue(rules, type);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			}
			catch (NumberFormatException e) {
				log.warn("Could not parse validation rule value as Integer for type {}: {}", type, value);
				return null;
			}
		}
		return null;
	}

	// Get min/max values for NUMBER_RANGE (assuming value is like "10-50")
	public Integer getRangeMin(List<ValidationRuleDTO> rules) {
		String range = getRuleValue(rules, ValidationRuleType.NUMBER_RANGE);
		if (range != null && range.contains("-")) {
			try {
				return Integer.parseInt(range.split("-")[0].trim());
			}
			catch (Exception e) {
				/* ignore */ }
		}
		return null;
	}

	public Integer getRangeMax(List<ValidationRuleDTO> rules) {
		String range = getRuleValue(rules, ValidationRuleType.NUMBER_RANGE);
		if (range != null && range.contains("-")) {
			try {
				return Integer.parseInt(range.split("-")[1].trim());
			}
			catch (Exception e) {
				/* ignore */ }
		}
		return null;
	}

}