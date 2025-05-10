package com.ginkgooai.core.gatekeeper.domain.types;

public enum ValidationRuleType {

	REQUIRED, MIN_LENGTH, MAX_LENGTH, REGEX, EMAIL_FORMAT, NUMBER_RANGE, CUSTOM_FUNCTION,
	// Add more specific types as needed
	MIN_VALUE, // For numbers
	MAX_VALUE, // For numbers
	URL_FORMAT, FILE_TYPE, // For file uploads (e.g., "image/jpeg, application/pdf")
	MAX_FILE_SIZE // For file uploads (e.g., "2MB")

}
