package com.ginkgooai.core.gatekeeper.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "section_definitions", schema = "gatekeeper")
@Getter
@Setter
@ToString(exclude = { "formDefinition", "fields" }) // Avoid circular dependency
public class SectionDefinition extends BaseAuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@NotBlank(message = "Section title cannot be blank")
	@Column(nullable = false)
	private String title;

	@Column(name = "display_order") // "order" is often a reserved keyword
	private Integer order;

	@Column(length = 1000) // Condition for displaying the section
	private String condition;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "form_definition_id", nullable = false)
	private FormDefinition formDefinition;

	@OneToMany(mappedBy = "sectionDefinition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("order ASC") // Keep fields ordered
	private List<FieldDefinition> fields = new ArrayList<>();

	// Helper methods for managing bidirectional relationship with FieldDefinition
	public void addField(FieldDefinition field) {
		fields.add(field);
		field.setSectionDefinition(this);
	}

	public void removeField(FieldDefinition field) {
		fields.remove(field);
		field.setSectionDefinition(null);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SectionDefinition that = (SectionDefinition) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
