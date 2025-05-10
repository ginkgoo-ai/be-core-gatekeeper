package com.ginkgooai.core.gatekeeper.domain;

import com.ginkgooai.core.gatekeeper.domain.types.ValidationRuleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Entity
@Table(name = "validation_rules", schema = "gatekeeper")
@Getter
@Setter
@ToString(exclude = "fieldDefinition") // Avoid circular dependency in toString
public class ValidationRule extends BaseAuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@NotNull(message = "Validation rule type cannot be null")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ValidationRuleType type;

	@Column // Value can be optional depending on the rule type (e.g., REQUIRED doesn't
			// need a
			// value)
	private String value;

	@NotBlank(message = "Error message cannot be blank")
	@Column(nullable = false, length = 500)
	private String errorMessage;

	@Column
	private String customFunction; // Identifier or script for custom validation

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "field_definition_id", nullable = false)
	private FieldDefinition fieldDefinition;

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ValidationRule that = (ValidationRule) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
