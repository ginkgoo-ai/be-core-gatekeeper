package com.ginkgooai.core.gatekeeper.repository;

import com.ginkgooai.core.gatekeeper.domain.FormDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormDefinitionRepository
		extends JpaRepository<FormDefinition, String>, JpaSpecificationExecutor<FormDefinition> {

	// Find by unique name
	Optional<FormDefinition> findByName(String name);

	// Find all forms by status
	List<FormDefinition> findByStatus(FormDefinition.FormStatus status);

	// Add custom queries if needed later

}
