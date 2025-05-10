package com.ginkgooai.core.gatekeeper.repository;

import com.ginkgooai.core.gatekeeper.domain.ValidationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ValidationRuleRepository
		extends JpaRepository<ValidationRule, String>, JpaSpecificationExecutor<ValidationRule> {

	// Find all validation rules for a given field definition
	List<ValidationRule> findByFieldDefinitionId(String fieldDefinitionId);

}
