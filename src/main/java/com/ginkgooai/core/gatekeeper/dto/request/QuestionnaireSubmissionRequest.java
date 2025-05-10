package com.ginkgooai.core.gatekeeper.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class QuestionnaireSubmissionRequest {

	@NotBlank(message = "Form Definition ID cannot be blank")
	private String formDefinitionId;

	@NotNull(message = "Response data cannot be null")
	private Map<String, Object> responseData;

	private String userId; // Optional: ID of the user submitting the form

}