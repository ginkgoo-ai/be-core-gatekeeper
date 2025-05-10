package com.ginkgooai.core.gatekeeper.dto;

import com.ginkgooai.core.gatekeeper.domain.ValidationRule;
import com.ginkgooai.core.gatekeeper.domain.types.ValidationRuleType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ValidationRuleDTO {

	private String id;

	private ValidationRuleType type;

	private String value;

	private String errorMessage;

	private String customFunction;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	public static ValidationRuleDTO fromEntity(ValidationRule entity) {
		if (entity == null) {
			return null;
		}
		ValidationRuleDTO dto = new ValidationRuleDTO();
		dto.setId(entity.getId());
		dto.setType(entity.getType());
		dto.setValue(entity.getValue());
		dto.setErrorMessage(entity.getErrorMessage());
		dto.setCustomFunction(entity.getCustomFunction());
		dto.setCreatedAt(entity.getCreatedAt());
		dto.setUpdatedAt(entity.getUpdatedAt());
		return dto;
	}

}
