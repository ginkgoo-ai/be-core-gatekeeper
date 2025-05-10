package com.ginkgooai.core.gatekeeper.dto.request;

import com.ginkgooai.core.gatekeeper.domain.FormDefinition; // Keep for FormStatus enum if needed,
															// or define status in request
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.fasterxml.jackson.databind.JsonNode;
import com.ginkgooai.core.gatekeeper.enums.FormType;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateFormDefinitionRequest {

	@NotBlank(message = "Form name cannot be blank")
	@Size(max = 255, message = "Form name cannot exceed 255 characters")
	private String name;

	@Size(max = 255, message = "Version cannot exceed 255 characters")
	private String version; // Optional, can be auto-generated or defaulted

	@Size(max = 1000, message = "Description cannot exceed 1000 characters")
	private String description;

	private String targetAudience;

	// Status is usually not set directly via create DTO; defaults to DRAFT in
	// entity.
	// If explicit status setting is needed: private FormDefinition.FormStatus
	// status;

	@NotNull(message = "Form type must be specified")
	private FormType formType;

	private JsonNode initialLogic; // JSON object

	private JsonNode submissionLogic; // JSON object

	@Valid // Enable validation for nested objects
	private List<CreateSectionDefinitionRequest> sections = new ArrayList<>();

}
