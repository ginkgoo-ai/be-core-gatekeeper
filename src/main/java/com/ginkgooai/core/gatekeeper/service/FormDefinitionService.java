package com.ginkgooai.core.gatekeeper.service;

import com.ginkgooai.core.gatekeeper.domain.FormDefinition;
import com.ginkgooai.core.gatekeeper.dto.FormDefinitionDTO;
import com.ginkgooai.core.gatekeeper.dto.request.CreateFormDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.UpdateFormDefinitionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface FormDefinitionService {

	/**
	 * Creates a new form definition with its sections, fields, and validation rules.
	 * @param request DTO containing data for the new form definition and its nested
	 * structure.
	 * @return DTO of the created form definition.
	 */
	FormDefinitionDTO createFormDefinition(CreateFormDefinitionRequest request);

	/**
	 * Finds a form definition by its ID.
	 * @param id The ID of the form definition.
	 * @return An Optional containing the DTO if found, otherwise empty.
	 */
	Optional<FormDefinitionDTO> findFormDefinitionById(String id);

	/**
	 * Updates an existing form definition.
	 * @param id The ID of the form definition to update.
	 * @param request DTO containing the updated data.
	 * @return DTO of the updated form definition.
	 * @throws RuntimeException if the form definition with the given ID is not found.
	 */
	FormDefinitionDTO updateFormDefinition(String id, UpdateFormDefinitionRequest request);

	/**
	 * Retrieves a paginated list of form definitions, optionally filtered.
	 * @param pageable Pagination information.
	 * @param name Optional filter by name (partial match).
	 * @param status Optional filter by status.
	 * @return A Page of FormDefinitionDTOs.
	 */
	Page<FormDefinitionDTO> findFormDefinitions(Pageable pageable, String name, FormDefinition.FormStatus status);

	/**
	 * Finds all published form definitions.
	 * @return A list of published form definitions.
	 */
	List<FormDefinitionDTO> findAllPublishedForms();

	/**
	 * Deletes a form definition by its ID.
	 * @param id The ID of the form definition to delete.
	 * @throws ResourceNotFoundException if the form definition is not found.
	 */
	void deleteFormDefinition(String id);

	/**
	 * Updates the status of an existing form definition.
	 * @param formId The ID of the form definition to update.
	 * @param newStatus The new status for the form definition.
	 * @return DTO of the updated form definition.
	 * @throws ResourceNotFoundException if the form definition with the given ID is not
	 * found.
	 */
	FormDefinitionDTO updateFormStatus(String formId, FormDefinition.FormStatus newStatus);

}
