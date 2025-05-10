package com.ginkgooai.core.gatekeeper.repository;

import com.ginkgooai.core.gatekeeper.domain.SectionDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionDefinitionRepository
		extends JpaRepository<SectionDefinition, String>, JpaSpecificationExecutor<SectionDefinition> {

	// Find all sections for a given form definition, ordered by 'order'
	List<SectionDefinition> findByFormDefinitionIdOrderByOrderAsc(String formDefinitionId);

}
