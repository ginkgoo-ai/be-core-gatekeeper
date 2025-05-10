package com.ginkgooai.core.gatekeeper.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.ginkgooai.core.gatekeeper.domain.types.FieldType;
import com.ginkgooai.core.gatekeeper.domain.types.OptionsSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateFieldDefinitionRequest {

	private String id; // 现有Field的ID

	@NotBlank(message = "Field name (key) cannot be blank")
	private String name;

	@NotBlank(message = "Field label cannot be blank")
	private String label;

	@NotNull(message = "Field type cannot be null")
	private FieldType fieldType;

	private String placeholder;

	private String defaultValue;

	private OptionsSourceType optionsSourceType;

	private JsonNode staticOptions; // Expect JSON structure

	private String apiEndpoint;

	private JsonNode uiProperties; // Expect JSON structure

	private String condition;

	private JsonNode dependencies; // Expect JSON structure

	private Integer order;

	@Valid // Enable validation for nested objects
	private List<UpdateValidationRuleRequest> validations = new ArrayList<>();

}