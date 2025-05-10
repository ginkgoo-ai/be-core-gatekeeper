package com.ginkgooai.core.gatekeeper.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.ginkgooai.core.gatekeeper.domain.FormDefinition;
import com.ginkgooai.core.gatekeeper.dto.request.UpdateSectionDefinitionRequest;
import com.ginkgooai.core.gatekeeper.enums.FormType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateFormDefinitionRequest {

	// Name might be updatable or not, depending on requirements.
	// For now, let's assume it's not directly updatable via this DTO to prevent
	// accidental changes
	// to the unique identifier.
	// @Size(max = 255, message = "Form name cannot exceed 255 characters")
	// private String name;

	@Size(max = 1000, message = "Description cannot exceed 1000 characters")
	private String description;

	@NotNull(message = "Status cannot be null")
	private FormDefinition.FormStatus status;

	@NotNull(message = "Form type must be specified")
	private FormType formType;

	private String targetAudience;

	private JsonNode initialLogic; // JSON object

	private JsonNode submissionLogic; // JSON object

	// sections支持完整的表单结构编辑
	@Valid
	private List<UpdateSectionDefinitionRequest> sections = new ArrayList<>();

}
