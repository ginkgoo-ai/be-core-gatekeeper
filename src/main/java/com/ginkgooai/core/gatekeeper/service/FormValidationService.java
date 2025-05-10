package com.ginkgooai.core.gatekeeper.service;

import com.ginkgooai.core.gatekeeper.dto.FieldDefinitionDTO;
import com.ginkgooai.core.gatekeeper.dto.FormDefinitionDTO;

import java.util.Map;

/**
 * Service interface for form data validation
 */
public interface FormValidationService {

	/**
	 * Validate form data against a form definition
	 * @param formDefinition The form definition to validate against
	 * @param formData The form data to validate
	 * @return A map of field names to error messages, empty if validation passes
	 */
	Map<String, String> validateFormData(FormDefinitionDTO formDefinition, Map<String, Object> formData);

	/**
	 * Validate a single field value against its field definition
	 * @param field The field definition to validate against
	 * @param fieldValue The field value to validate
	 * @return An error message if validation fails, null if validation passes
	 */
	String validateField(FieldDefinitionDTO field, Object fieldValue);

	/**
	 * Execute custom validation rules defined in JavaScript
	 * @param scriptContent The JavaScript validation script
	 * @param formData The complete form data
	 * @param fieldName The name of the field being validated
	 * @param fieldValue The value of the field being validated
	 * @return An error message if validation fails, null if validation passes
	 */
	String executeCustomValidation(String scriptContent, Map<String, Object> formData, String fieldName,
			Object fieldValue);

}
