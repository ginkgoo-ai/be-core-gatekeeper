package com.ginkgooai.core.gatekeeper.domain.types;

public enum FieldType {

	TEXT, NUMBER, EMAIL, PASSWORD, DATE, SELECT, MULTI_SELECT, RADIO, CHECKBOX, FILE_UPLOAD, RICH_TEXT_EDITOR,
	ADDRESS_PICKER,
	// Add more types as needed
	TEXTAREA, // Often used for longer text inputs
	BOOLEAN // For simple true/false, often rendered as a checkbox or toggle

}
