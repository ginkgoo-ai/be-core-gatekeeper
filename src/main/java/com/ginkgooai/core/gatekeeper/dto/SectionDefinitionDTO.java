package com.ginkgooai.core.gatekeeper.dto;

import com.ginkgooai.core.gatekeeper.domain.SectionDefinition;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class SectionDefinitionDTO {

	private String id;

	private String title;

	private Integer order;

	private String condition;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private List<FieldDefinitionDTO> fields;

	public static SectionDefinitionDTO fromEntity(SectionDefinition entity) {
		if (entity == null) {
			return null;
		}
		SectionDefinitionDTO dto = new SectionDefinitionDTO();
		dto.setId(entity.getId());
		dto.setTitle(entity.getTitle());
		dto.setOrder(entity.getOrder());
		dto.setCondition(entity.getCondition());
		dto.setCreatedAt(entity.getCreatedAt());
		dto.setUpdatedAt(entity.getUpdatedAt());
		if (entity.getFields() != null) {
			dto.setFields(entity.getFields().stream().map(FieldDefinitionDTO::fromEntity).collect(Collectors.toList()));
		}
		return dto;
	}

}
