package com.ginkgooai.core.gatekeeper.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.ginkgooai.core.gatekeeper.enums.FormType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "form_definitions", schema = "gatekeeper")
@Getter
@Setter
@ToString(exclude = "sections") // Avoid circular dependency
public class FormDefinition extends BaseAuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@NotBlank(message = "Form name cannot be blank")
	@Column(nullable = false, unique = true)
	private String name;

	@Column(nullable = false, length = 50) // Example length, adjust as needed
	private String version = "1.0.0"; // Default version

	@Column(length = 1000)
	private String description;

	@Column
	private String targetAudience;

	@NotNull(message = "Status cannot be null")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FormStatus status = FormStatus.DRAFT;

	@NotNull(message = "Form type cannot be null")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FormType formType = FormType.GENERIC_FORM;

	@JdbcTypeCode(SqlTypes.JSON) // Store as JSON
	@Column(columnDefinition = "jsonb")
	private JsonNode initialLogic; // JSON object representing logic

	@JdbcTypeCode(SqlTypes.JSON) // Store as JSON
	@Column(columnDefinition = "jsonb")
	private JsonNode submissionLogic; // JSON object representing logic

	@OneToMany(mappedBy = "formDefinition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("order ASC") // Keep sections ordered
	private List<SectionDefinition> sections = new ArrayList<>();

	// Helper methods for managing bidirectional relationship with SectionDefinition
	public void addSection(SectionDefinition section) {
		sections.add(section);
		section.setFormDefinition(this);
	}

	public void removeSection(SectionDefinition section) {
		sections.remove(section);
		section.setFormDefinition(null);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		FormDefinition that = (FormDefinition) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(version, that.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, version);
	}

	// Enum for Status
	public enum FormStatus {

		DRAFT, PUBLISHED, ARCHIVED

	}

}
