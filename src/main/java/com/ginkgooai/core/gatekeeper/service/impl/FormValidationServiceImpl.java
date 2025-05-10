package com.ginkgooai.core.gatekeeper.service.impl;

import com.ginkgooai.core.gatekeeper.domain.FieldDefinition;
import com.ginkgooai.core.gatekeeper.domain.SectionDefinition;
import com.ginkgooai.core.gatekeeper.domain.ValidationRule;
import com.ginkgooai.core.gatekeeper.domain.types.FieldType;
import com.ginkgooai.core.gatekeeper.domain.types.ValidationRuleType;
import com.ginkgooai.core.gatekeeper.dto.FieldDefinitionDTO;
import com.ginkgooai.core.gatekeeper.dto.FormDefinitionDTO;
import com.ginkgooai.core.gatekeeper.dto.SectionDefinitionDTO;
import com.ginkgooai.core.gatekeeper.dto.ValidationRuleDTO;
import com.ginkgooai.core.gatekeeper.service.FormValidationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Implementation of FormValidationService
 */
@Service
@Slf4j
public class FormValidationServiceImpl implements FormValidationService {

	private final ScriptEngineManager scriptEngineManager;

	public FormValidationServiceImpl() {
		this.scriptEngineManager = new ScriptEngineManager();
	}

	@Override
	public Map<String, String> validateFormData(FormDefinitionDTO formDefinition, Map<String, Object> formData) {
		Map<String, String> validationErrors = new HashMap<>();

		// Validate all fields in all sections
		for (SectionDefinitionDTO section : formDefinition.getSections()) {
			// Check if section should be displayed based on its condition
			boolean sectionVisible = evaluateSectionVisibility(section, formData);
			if (!sectionVisible) {
				continue; // Skip validation for hidden sections
			}

			for (FieldDefinitionDTO field : section.getFields()) {
				// Check if field should be displayed based on its condition
				boolean fieldVisible = evaluateFieldVisibility(field, formData);
				if (!fieldVisible) {
					continue; // Skip validation for hidden fields
				}

				Object fieldValue = formData.get(field.getName());
				String errorMessage = validateField(field, fieldValue);

				if (errorMessage != null) {
					validationErrors.put(field.getName(), errorMessage);
				}
			}
		}

		// Execute form-level custom validations if defined
		// ...

		return validationErrors;
	}

	@Override
	public String validateField(FieldDefinitionDTO field, Object fieldValue) {
		// Check required fields
		if (hasValidationRule(field, ValidationRuleType.REQUIRED)) {
			if (fieldValue == null || (fieldValue instanceof String && ((String) fieldValue).isEmpty())
					|| (fieldValue instanceof Object[] && ((Object[]) fieldValue).length == 0)
					|| (fieldValue instanceof Map && ((Map<?, ?>) fieldValue).isEmpty())
					|| (fieldValue instanceof Iterable && !((Iterable<?>) fieldValue).iterator().hasNext())) {

				return getValidationErrorMessage(field, ValidationRuleType.REQUIRED, "This field is required");
			}
		}

		// If field is not required and value is null/empty, skip other validations
		if (fieldValue == null || (fieldValue instanceof String && ((String) fieldValue).isEmpty())) {
			return null;
		}

		// Type-specific validations
		if (fieldValue instanceof String) {
			String stringValue = (String) fieldValue;

			// Validate string length
			if (hasValidationRule(field, ValidationRuleType.MIN_LENGTH)) {
				int minLength = getValidationRuleValueAsInt(field, ValidationRuleType.MIN_LENGTH);
				if (stringValue.length() < minLength) {
					return getValidationErrorMessage(field, ValidationRuleType.MIN_LENGTH,
							"Minimum length is " + minLength + " characters");
				}
			}

			if (hasValidationRule(field, ValidationRuleType.MAX_LENGTH)) {
				int maxLength = getValidationRuleValueAsInt(field, ValidationRuleType.MAX_LENGTH);
				if (stringValue.length() > maxLength) {
					return getValidationErrorMessage(field, ValidationRuleType.MAX_LENGTH,
							"Maximum length is " + maxLength + " characters");
				}
			}

			// Validate against regex pattern
			if (hasValidationRule(field, ValidationRuleType.REGEX)) {
				String pattern = getValidationRuleValue(field, ValidationRuleType.REGEX);
				if (pattern != null && !pattern.isEmpty()) {
					try {
						if (!Pattern.matches(pattern, stringValue)) {
							return getValidationErrorMessage(field, ValidationRuleType.REGEX,
									"Value does not match the required pattern");
						}
					}
					catch (PatternSyntaxException e) {
						log.error("Invalid regex pattern in validation rule: {}", pattern, e);
						return "Internal validation error: Invalid regex pattern";
					}
				}
			}

			// Validate email format
			if (field.getFieldType() == FieldType.EMAIL || hasValidationRule(field, ValidationRuleType.EMAIL_FORMAT)) {
				String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
				if (!Pattern.matches(emailRegex, stringValue)) {
					return getValidationErrorMessage(field, ValidationRuleType.EMAIL_FORMAT, "Invalid email format");
				}
			}
		}
		else if (fieldValue instanceof Number) {
			Number numValue = (Number) fieldValue;
			double doubleValue = numValue.doubleValue();

			// Validate numeric range
			if (hasValidationRule(field, ValidationRuleType.MIN_VALUE)) {
				double minValue = getValidationRuleValueAsDouble(field, ValidationRuleType.MIN_VALUE);
				if (doubleValue < minValue) {
					return getValidationErrorMessage(field, ValidationRuleType.MIN_VALUE,
							"Value must be at least " + minValue);
				}
			}

			if (hasValidationRule(field, ValidationRuleType.MAX_VALUE)) {
				double maxValue = getValidationRuleValueAsDouble(field, ValidationRuleType.MAX_VALUE);
				if (doubleValue > maxValue) {
					return getValidationErrorMessage(field, ValidationRuleType.MAX_VALUE,
							"Value must be at most " + maxValue);
				}
			}
		}

		// Execute custom validation functions if defined
		List<ValidationRuleDTO> validations = field.getValidations();
		if (validations != null) {
			for (ValidationRuleDTO rule : validations) {
				if (rule.getType() == ValidationRuleType.CUSTOM_FUNCTION && rule.getCustomFunction() != null) {
					Map<String, Object> dummyFormData = new HashMap<>(); // Would need
																			// full form
																			// data in
																			// practice
					String customError = executeCustomValidation(rule.getCustomFunction(), dummyFormData,
							field.getName(), fieldValue);
					if (customError != null) {
						return customError;
					}
				}
			}
		}

		return null; // No validation errors
	}

	@Override
	public String executeCustomValidation(String scriptContent, Map<String, Object> formData, String fieldName,
			Object fieldValue) {
		if (scriptContent == null || scriptContent.isEmpty()) {
			return null;
		}

		try {
			ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");
			if (engine == null) {
				log.error("JavaScript engine not available");
				return null;
			}

			// Create bindings for the script
			SimpleBindings bindings = new SimpleBindings();
			bindings.put("value", fieldValue);
			bindings.put("fieldName", fieldName);
			bindings.put("formData", formData);

			// Execute the validation script, which should return an error message or null
			Object result = engine.eval(scriptContent, bindings);

			if (result == null) {
				return null; // Validation passed
			}
			else {
				return result.toString(); // Return error message
			}

		}
		catch (ScriptException e) {
			log.error("Error executing custom validation script: {}", e.getMessage(), e);
			return "Internal validation error: " + e.getMessage();
		}
	}

	// Helper methods

	private boolean evaluateSectionVisibility(SectionDefinitionDTO section, Map<String, Object> formData) {
		// If no condition is defined, section is always visible
		if (section.getCondition() == null || section.getCondition().isEmpty()) {
			return true;
		}

		// TODO: Implement condition evaluation logic
		// This would typically involve parsing and evaluating the condition expression
		// against the form data

		// For now, return true by default (section is visible)
		return true;
	}

	private boolean evaluateFieldVisibility(FieldDefinitionDTO field, Map<String, Object> formData) {
		// If no condition is defined, field is always visible
		if (field.getCondition() == null || field.getCondition().isEmpty()) {
			return true;
		}

		// TODO: Implement condition evaluation logic for fields

		// For now, return true by default (field is visible)
		return true;
	}

	private boolean hasValidationRule(FieldDefinitionDTO field, ValidationRuleType ruleType) {
		List<ValidationRuleDTO> validations = field.getValidations();
		if (validations == null) {
			return false;
		}
		return validations.stream().anyMatch(rule -> rule.getType() == ruleType);
	}

	private ValidationRuleDTO getValidationRule(FieldDefinitionDTO field, ValidationRuleType ruleType) {
		List<ValidationRuleDTO> validations = field.getValidations();
		if (validations == null) {
			return null;
		}
		return validations.stream().filter(rule -> rule.getType() == ruleType).findFirst().orElse(null);
	}

	private String getValidationRuleValue(FieldDefinitionDTO field, ValidationRuleType ruleType) {
		ValidationRuleDTO rule = getValidationRule(field, ruleType);
		return rule != null ? rule.getValue() : null;
	}

	private int getValidationRuleValueAsInt(FieldDefinitionDTO field, ValidationRuleType ruleType) {
		String value = getValidationRuleValue(field, ruleType);
		if (value == null || value.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
			log.warn("Invalid numeric value in validation rule: {}", value);
			return 0;
		}
	}

	private double getValidationRuleValueAsDouble(FieldDefinitionDTO field, ValidationRuleType ruleType) {
		String value = getValidationRuleValue(field, ruleType);
		if (value == null || value.isEmpty()) {
			return 0.0;
		}
		try {
			return Double.parseDouble(value);
		}
		catch (NumberFormatException e) {
			log.warn("Invalid numeric value in validation rule: {}", value);
			return 0.0;
		}
	}

	private String getValidationErrorMessage(FieldDefinitionDTO field, ValidationRuleType ruleType,
			String defaultMessage) {
		ValidationRuleDTO rule = getValidationRule(field, ruleType);
		if (rule != null && rule.getErrorMessage() != null && !rule.getErrorMessage().isEmpty()) {
			return rule.getErrorMessage();
		}
		return defaultMessage;
	}

}
