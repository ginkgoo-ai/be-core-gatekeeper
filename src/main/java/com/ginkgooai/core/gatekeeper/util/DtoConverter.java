package com.ginkgooai.core.gatekeeper.util;

import com.ginkgooai.core.gatekeeper.domain.FieldDefinition;
import com.ginkgooai.core.gatekeeper.domain.ValidationRule;
import com.ginkgooai.core.gatekeeper.dto.FieldDefinitionDTO;
import com.ginkgooai.core.gatekeeper.dto.ValidationRuleDTO;

/**
 * 用于在域模型和DTO之间进行转换的工具类
 */
public class DtoConverter {

	/**
	 * 将 FieldDefinitionDTO 转换为 FieldDefinition 域模型
	 * @param dto DTO对象
	 * @return 域模型对象
	 */
	public static FieldDefinition toEntity(FieldDefinitionDTO dto) {
		if (dto == null) {
			return null;
		}

		FieldDefinition entity = new FieldDefinition();
		entity.setId(dto.getId());
		entity.setName(dto.getName());
		entity.setLabel(dto.getLabel());
		entity.setFieldType(dto.getFieldType());
		entity.setPlaceholder(dto.getPlaceholder());
		entity.setDefaultValue(dto.getDefaultValue());
		entity.setOptionsSourceType(dto.getOptionsSourceType());
		entity.setStaticOptions(dto.getStaticOptions());
		entity.setApiEndpoint(dto.getApiEndpoint());
		entity.setUiProperties(dto.getUiProperties());
		entity.setCondition(dto.getCondition());
		entity.setDependencies(dto.getDependencies());
		entity.setOrder(dto.getOrder());

		// 处理验证规则
		if (dto.getValidations() != null) {
			dto.getValidations().stream().map(DtoConverter::toEntity).forEach(entity::addValidationRule);
		}

		return entity;
	}

	/**
	 * 将 ValidationRuleDTO 转换为 ValidationRule 域模型
	 * @param dto 验证规则DTO
	 * @return 验证规则域模型
	 */
	public static ValidationRule toEntity(ValidationRuleDTO dto) {
		if (dto == null) {
			return null;
		}

		ValidationRule entity = new ValidationRule();
		entity.setId(dto.getId());
		entity.setType(dto.getType());
		entity.setValue(dto.getValue());
		entity.setErrorMessage(dto.getErrorMessage());
		entity.setCustomFunction(dto.getCustomFunction());

		return entity;
	}

}
