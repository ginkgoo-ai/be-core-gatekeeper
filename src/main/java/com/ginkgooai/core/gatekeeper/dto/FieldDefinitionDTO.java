package com.ginkgooai.core.gatekeeper.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.ginkgooai.core.gatekeeper.domain.FieldDefinition;
import com.ginkgooai.core.gatekeeper.domain.types.FieldType;
import com.ginkgooai.core.gatekeeper.domain.types.OptionsSourceType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class FieldDefinitionDTO {

	private String id;

	private String name;

	private String label;

	private FieldType fieldType;

	private String placeholder;

	private String defaultValue;

	private OptionsSourceType optionsSourceType;

	private JsonNode staticOptions; // JSON structure

	private String apiEndpoint;

	private JsonNode uiProperties; // JSON structure

	private String condition;

	private JsonNode dependencies; // JSON structure

	private Integer order;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private List<ValidationRuleDTO> validations;

	public static FieldDefinitionDTO fromEntity(FieldDefinition entity) {
		if (entity == null) {
			return null;
		}
		FieldDefinitionDTO dto = new FieldDefinitionDTO();
		dto.setId(entity.getId());
		dto.setName(entity.getName());
		dto.setLabel(entity.getLabel());
		dto.setFieldType(entity.getFieldType());
		dto.setPlaceholder(entity.getPlaceholder());
		dto.setDefaultValue(entity.getDefaultValue());
		dto.setOptionsSourceType(entity.getOptionsSourceType());
		dto.setStaticOptions(entity.getStaticOptions());
		dto.setApiEndpoint(entity.getApiEndpoint());
		dto.setUiProperties(entity.getUiProperties());
		dto.setCondition(entity.getCondition());
		dto.setDependencies(entity.getDependencies());
		dto.setOrder(entity.getOrder());
		dto.setCreatedAt(entity.getCreatedAt());
		dto.setUpdatedAt(entity.getUpdatedAt());
		if (entity.getValidations() != null) {
			dto.setValidations(
					entity.getValidations().stream().map(ValidationRuleDTO::fromEntity).collect(Collectors.toList()));
		}
		return dto;
	}

}
