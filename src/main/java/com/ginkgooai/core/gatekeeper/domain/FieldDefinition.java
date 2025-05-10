package com.ginkgooai.core.gatekeeper.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.ginkgooai.core.gatekeeper.domain.types.FieldType;
import com.ginkgooai.core.gatekeeper.domain.types.OptionsSourceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "field_definitions", schema = "gatekeeper")
@Getter
@Setter
@ToString(exclude = { "sectionDefinition", "validations" }) // Avoid circular dependency
public class FieldDefinition extends BaseAuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@NotBlank(message = "Field name (key) cannot be blank")
	@Column(nullable = false)
	private String name; // Backend storage key

	@NotBlank(message = "Field label cannot be blank")
	@Column(nullable = false)
	private String label;

	@NotNull(message = "Field type cannot be null")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FieldType fieldType;

	@Column
	private String placeholder;

	@Column
	private String defaultValue;

	@Enumerated(EnumType.STRING)
	private OptionsSourceType optionsSourceType;

	@JdbcTypeCode(SqlTypes.JSON) // Store as JSON
	@Column(columnDefinition = "jsonb")
	private JsonNode staticOptions; // JSON structure: [{value: 'v1', label: 'Option1'},
									// ...]

	@Column
	private String apiEndpoint; // API endpoint for dynamic options

	@JdbcTypeCode(SqlTypes.JSON) // Store as JSON
	@Column(columnDefinition = "jsonb")
	private JsonNode uiProperties; // JSON structure: {maxLength: 50, readOnly: false,
									// ...}

	@Column(length = 1000) // Condition for displaying the field
	private String condition;

	@JdbcTypeCode(SqlTypes.JSON) // Store as JSON array of JsonNode or a single JsonNode
	@Column(columnDefinition = "jsonb")
	private JsonNode dependencies; // JSON structure: ["field1", "field2"]

	@Column(name = "display_order") // "order" is often a reserved keyword
	private Integer order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "section_definition_id", nullable = false)
	private SectionDefinition sectionDefinition;

	@OneToMany(mappedBy = "fieldDefinition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<ValidationRule> validations = new ArrayList<>();

	// Helper methods for managing bidirectional relationship with ValidationRule
	public void addValidationRule(ValidationRule rule) {
		validations.add(rule);
		rule.setFieldDefinition(this);
	}

	public void removeValidationRule(ValidationRule rule) {
		validations.remove(rule);
		rule.setFieldDefinition(null);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		FieldDefinition that = (FieldDefinition) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
