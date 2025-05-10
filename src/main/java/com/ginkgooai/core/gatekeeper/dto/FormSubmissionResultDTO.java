package com.ginkgooai.core.gatekeeper.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Form submission result data transfer object Used to return the result of form
 * submission processing
 */
@Data
@NoArgsConstructor
public class FormSubmissionResultDTO {

	/**
	 * Whether the submission was successful
	 */
	private boolean success;

	/**
	 * Result message
	 */
	private String message;

	/**
	 * Response timestamp
	 */
	private Date timestamp;

	/**
	 * Form ID
	 */
	private String formId;

	/**
	 * Submission tracking ID
	 */
	private String submissionId;

	/**
	 * Submission result data
	 */
	private Object data;

	/**
	 * Redirect URL, if redirection is needed after submission
	 */
	private String redirectUrl;

	/**
	 * Additional information
	 */
	private Map<String, Object> additionalInfo = new HashMap<>();

}