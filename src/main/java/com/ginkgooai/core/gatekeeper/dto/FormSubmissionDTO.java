package com.ginkgooai.core.gatekeeper.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Form submission data transfer object Used to transfer user-submitted form data to
 * backend processing services
 */
@Data
@NoArgsConstructor
public class FormSubmissionDTO {

	/**
	 * Form ID
	 */
	private String formId;

	/**
	 * Form name
	 */
	private String formName;

	/**
	 * Submitting user ID
	 */
	private String userId;

	/**
	 * Submission timestamp
	 */
	private Date submittedAt;

	/**
	 * Form data in key-value format
	 */
	private Map<String, Object> data = new HashMap<>();

	/**
	 * Tracking information, can be used to track the form submission process
	 */
	private Map<String, Object> tracking = new HashMap<>();

	/**
	 * Metadata, can be used to store submission context information
	 */
	private Map<String, Object> metadata = new HashMap<>();

}