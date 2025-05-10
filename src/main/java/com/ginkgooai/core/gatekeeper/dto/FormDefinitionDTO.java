package com.ginkgooai.core.gatekeeper.dto;

import com.ginkgooai.core.gatekeeper.domain.FormDefinition;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ginkgooai.core.gatekeeper.enums.FormType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class FormDefinitionDTO {

	private String id;

	private String name;

	private String version;

	private String description;

	private String targetAudience;

	private FormDefinition.FormStatus status;

	private FormType formType;

	private JsonNode initialLogic = JsonNodeFactory.instance.objectNode();

	private JsonNode submissionLogic = JsonNodeFactory.instance.objectNode();

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private List<SectionDefinitionDTO> sections;

	// Custom getter for submissionLogic.targetService
	public String getSubmissionLogicTargetService() {
		if (this.submissionLogic != null && this.submissionLogic.has("targetService")) {
			JsonNode targetServiceNode = this.submissionLogic.get("targetService");
			if (targetServiceNode != null && targetServiceNode.isTextual()) {
				return targetServiceNode.asText();
			}
		}
		return null; // Or empty string, depending on desired default
	}

	// Custom setter for submissionLogic.targetService
	public void setSubmissionLogicTargetService(String targetServiceValue) {
		if (this.submissionLogic == null || !this.submissionLogic.isObject()) {
			// Ensure submissionLogic is an ObjectNode
			this.submissionLogic = JsonNodeFactory.instance.objectNode();
		}
		if (targetServiceValue != null) {
			((ObjectNode) this.submissionLogic).put("targetService", targetServiceValue);
		}
		else {
			// If the value is null, we might want to remove the field or set it to null
			// JsonNode
			((ObjectNode) this.submissionLogic).remove("targetService"); // Option: remove
																			// if null
			// Or: ((ObjectNode) this.submissionLogic).putNull("targetService"); //
			// Option: set as JSON null
		}
	}

	public static FormDefinitionDTO fromEntity(FormDefinition entity) {
		if (entity == null) {
			return null;
		}
		FormDefinitionDTO dto = new FormDefinitionDTO();
		dto.setId(entity.getId());
		dto.setName(entity.getName());
		dto.setVersion(entity.getVersion());
		dto.setDescription(entity.getDescription());
		dto.setTargetAudience(entity.getTargetAudience());
		dto.setStatus(entity.getStatus());
		dto.setFormType(entity.getFormType());

		if (entity.getInitialLogic() != null) {
			dto.setInitialLogic(entity.getInitialLogic());
		}
		if (entity.getSubmissionLogic() != null) {
			dto.setSubmissionLogic(entity.getSubmissionLogic());
		}

		dto.setCreatedAt(entity.getCreatedAt());
		dto.setUpdatedAt(entity.getUpdatedAt());

		if (entity.getSections() != null) {
			dto.setSections(
					entity.getSections().stream().map(SectionDefinitionDTO::fromEntity).collect(Collectors.toList()));
		}
		return dto;
	}

}
