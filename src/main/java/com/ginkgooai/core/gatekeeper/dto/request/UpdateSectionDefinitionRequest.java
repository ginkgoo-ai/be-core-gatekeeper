package com.ginkgooai.core.gatekeeper.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateSectionDefinitionRequest {

	private String id; // 现有Section的ID

	@NotBlank(message = "Section title cannot be blank")
	private String title;

	private Integer order;

	private String condition;

	@Valid // Enable validation for nested objects
	private List<UpdateFieldDefinitionRequest> fields = new ArrayList<>();

}