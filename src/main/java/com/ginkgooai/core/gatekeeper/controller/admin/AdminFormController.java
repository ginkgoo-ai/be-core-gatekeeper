package com.ginkgooai.core.gatekeeper.controller.admin;

import com.ginkgooai.core.gatekeeper.domain.FormDefinition;
import com.ginkgooai.core.gatekeeper.dto.FormDefinitionDTO;
import com.ginkgooai.core.gatekeeper.dto.UpdateFormDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.request.CreateFormDefinitionRequest;
import com.ginkgooai.core.gatekeeper.exception.ResourceNotFoundException;
import com.ginkgooai.core.gatekeeper.service.FormDefinitionService;
import com.ginkgooai.core.gatekeeper.service.FormImageProcessingService;
import com.ginkgooai.core.gatekeeper.util.FormRenderingHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/admin/forms")
@RequiredArgsConstructor
@Tag(name = "Form Definition Management", description = "APIs for managing form definitions")
@Slf4j
public class AdminFormController {

	private final FormDefinitionService formDefinitionService;

	private final FormRenderingHelper formRenderingHelper;

	private final FormImageProcessingService formImageProcessingService;

	@PostMapping
	@Operation(summary = "Create a new form definition via form data",
			responses = {
					@ApiResponse(responseCode = "201", description = "Form definition created successfully",
							content = @Content(schema = @Schema(implementation = FormDefinitionDTO.class))),
					@ApiResponse(responseCode = "400", description = "Invalid input data or duplicate name") })
	public ResponseEntity<?> createFormDefinition(@Valid @RequestBody CreateFormDefinitionRequest request) {
		try {
			FormDefinitionDTO createdDto = formDefinitionService.createFormDefinition(request);
			URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/gatekeeper/v1/forms/{id}")
				.buildAndExpand(createdDto.getId())
				.toUri();
			return ResponseEntity.created(location).body(createdDto);
		}
		catch (DataIntegrityViolationException e) {
			log.warn("Failed to create form definition due to data integrity violation (e.g., duplicate name '{}'): {}",
					request.getName(), e.getMessage());
			return ResponseEntity.status(HttpStatus.CONFLICT)
				.body("Form definition creation failed: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("Unexpected error during form definition creation for name '{}'", request.getName(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
		}
	}

	@PostMapping("/json")
	@Operation(summary = "Import and create a new form definition from JSON",
			description = "Accepts a JSON body matching the CreateFormDefinitionRequest structure.",
			responses = {
					@ApiResponse(responseCode = "201", description = "Form definition imported successfully",
							content = @Content(schema = @Schema(implementation = FormDefinitionDTO.class))),
					@ApiResponse(responseCode = "400", description = "Invalid JSON data or validation errors"),
					@ApiResponse(responseCode = "409", description = "Duplicate form name detected") })
	public ResponseEntity<?> importFormDefinition(@Valid @RequestBody CreateFormDefinitionRequest request) {
		try {
			log.info("Attempting to import form definition with name: {}", request.getName());
			FormDefinitionDTO createdDto = formDefinitionService.createFormDefinition(request);
			URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/gatekeeper/v1/forms/{id}")
				.buildAndExpand(createdDto.getId())
				.toUri();
			log.info("Successfully imported form definition with ID: {}", createdDto.getId());
			return ResponseEntity.created(location).body(createdDto);
		}
		catch (DataIntegrityViolationException e) {
			log.warn("Failed to import form definition due to data integrity violation (e.g., duplicate name '{}'): {}",
					request.getName(), e.getMessage());
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Form definition import failed: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("Unexpected error during form definition import for name '{}'", request.getName(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("An unexpected error occurred during import.");
		}
	}

	@PostMapping(value = "/file", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<?> handleImageImport(@RequestParam("imageFile") MultipartFile imageFile) {
		if (imageFile.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Image file is empty"));
		}

		try {
			log.info("Processing image import: {}", imageFile.getOriginalFilename());
			String generatedJson = formImageProcessingService.processImageToFormJson(imageFile);
			log.info("Generated JSON from image: {}", generatedJson);
			return ResponseEntity.ok(generatedJson);
		}
		catch (IOException e) {
			log.error("IO Error processing image: {}", imageFile.getOriginalFilename(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "IO error processing image: " + e.getMessage()));
		}
		catch (Exception e) {
			log.error("Error processing image with AI: {}", imageFile.getOriginalFilename(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "Failed to process image with AI: " + e.getMessage()));
		}
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a form definition by ID",
			parameters = @Parameter(name = "id", description = "ID of the form definition to retrieve",
					required = true),
			responses = {
					@ApiResponse(responseCode = "200", description = "Form definition found",
							content = @Content(schema = @Schema(implementation = FormDefinitionDTO.class))),
					@ApiResponse(responseCode = "404", description = "Form definition not found") })
	public ResponseEntity<FormDefinitionDTO> getFormDefinitionById(@PathVariable String id) {
		return formDefinitionService.findFormDefinitionById(id)
			.map(ResponseEntity::ok)
			.orElseThrow(() -> new ResourceNotFoundException("FormDefinition", "id", id));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an existing form definition (top-level fields only for now)",
			parameters = @Parameter(name = "id", description = "ID of the form definition to update", required = true),
			responses = {
					@ApiResponse(responseCode = "200", description = "Form definition updated successfully",
							content = @Content(schema = @Schema(implementation = FormDefinitionDTO.class))),
					@ApiResponse(responseCode = "400", description = "Invalid input data"),
					@ApiResponse(responseCode = "404", description = "Form definition not found") })
	public ResponseEntity<FormDefinitionDTO> updateFormDefinition(@PathVariable String id,
			@Valid @RequestBody UpdateFormDefinitionRequest request) {
		FormDefinitionDTO updatedDto = formDefinitionService.updateFormDefinition(id, request);
		return ResponseEntity.ok(updatedDto);
	}

	@GetMapping
	@Operation(summary = "List form definitions",
			description = "Retrieves a paginated list of form definitions, optionally filtered by name or status.",
			parameters = {
					@Parameter(name = "name", description = "Filter by form name (case-insensitive, partial match)",
							required = false),
					@Parameter(name = "status", description = "Filter by form status (DRAFT, PUBLISHED, ARCHIVED)",
							required = false),
					@Parameter(name = "pageable",
							description = "Pagination and sorting parameters (e.g., page=0&size=10&sort=name,asc)",
							hidden = true) },
			responses = { @ApiResponse(responseCode = "200", description = "List of form definitions retrieved") })
	public ResponseEntity<Page<FormDefinitionDTO>> listFormDefinitions(@RequestParam(required = false) String name,
			@RequestParam(required = false) FormDefinition.FormStatus status,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		Page<FormDefinitionDTO> page = formDefinitionService.findFormDefinitions(pageable, name, status);
		return ResponseEntity.ok(page);
	}

}
