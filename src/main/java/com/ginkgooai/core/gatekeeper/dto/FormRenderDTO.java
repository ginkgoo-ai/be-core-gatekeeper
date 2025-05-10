package com.ginkgooai.core.gatekeeper.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Form rendering data transfer object, specialized for frontend rendering Extends
 * FormDefinitionDTO with additional rendering-specific information
 */
@Data
@NoArgsConstructor
public class FormRenderDTO extends FormDefinitionDTO {

	/**
	 * Form rendering configuration
	 */
	private Map<String, Object> renderConfig = new HashMap<>();

	/**
	 * Prefill data
	 */
	private Map<String, Object> prefillData = new HashMap<>();

	/**
	 * User context information
	 */
	private Map<String, Object> userContext = new HashMap<>();

	/**
	 * Client-side rendering instructions
	 */
	private Map<String, Object> clientInstructions = new HashMap<>();

	/**
	 * Create a rendering version from a FormDefinitionDTO
	 */
	public FormRenderDTO(FormDefinitionDTO formDefinition) {
		// Copy all base properties
		this.setId(formDefinition.getId());
		this.setName(formDefinition.getName());
		this.setVersion(formDefinition.getVersion());
		this.setDescription(formDefinition.getDescription());
		this.setTargetAudience(formDefinition.getTargetAudience());
		this.setStatus(formDefinition.getStatus());
		this.setCreatedAt(formDefinition.getCreatedAt());
		this.setUpdatedAt(formDefinition.getUpdatedAt());
		this.setSections(formDefinition.getSections());
		this.setInitialLogic(formDefinition.getInitialLogic());
		this.setSubmissionLogic(formDefinition.getSubmissionLogic());

		// Default rendering configuration
		this.renderConfig.put("validationMode", "onBlur");
		this.renderConfig.put("showProgressBar", true);
		this.renderConfig.put("allowSaveAndContinue", false);
	}

}
