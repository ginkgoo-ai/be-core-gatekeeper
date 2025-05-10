package com.ginkgooai.core.gatekeeper.repository;

import com.ginkgooai.core.gatekeeper.domain.QuestionnaireResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionnaireResponseRepository extends JpaRepository<QuestionnaireResult, String> {

	// Example custom query methods you might need:
	List<QuestionnaireResult> findByFormDefinitionId(String formDefinitionId);

	List<QuestionnaireResult> findByUserId(String userId);

	List<QuestionnaireResult> findByFormDefinitionIdAndUserId(String formDefinitionId, String userId);

}