package com.ginkgooai.core.gatekeeper.dto.request;

import com.ginkgooai.core.gatekeeper.domain.types.ValidationRuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateValidationRuleRequest {

	private String id; // 现有ValidationRule的ID

	@NotNull(message = "Validation rule type cannot be null")
	private ValidationRuleType type;

	private String value; // Optional based on type

	@NotBlank(message = "Error message cannot be blank")
	private String errorMessage;

	private String customFunction;

}