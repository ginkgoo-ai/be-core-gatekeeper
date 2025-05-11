package com.ginkgooai.core.gatekeeper.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "questionnaire_result", schema = "gatekeeper")
@Getter
@Setter
public class QuestionnaireResult extends BaseAuditableEntity {

	// Assuming
	// BaseAuditableEntity
	// provides createdAt,
	// updatedAt

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(name = "form_definition_id", nullable = false)
	private String formDefinitionId; // Links to the FormDefinition that this response is
										// for

	// @ManyToOne(fetch = FetchType.LAZY)
	// @JoinColumn(name = "form_definition_id", referencedColumnName = "id", insertable =
	// false, updatable = false)
	// private FormDefinition formDefinition; // Optional: if you need to navigate from
	// response to definition easily

	@Column(name = "user_id") // Optional: ID of the user who submitted the form
	private String userId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private JsonNode responseData; // The actual data submitted by the user

	// submittedAt will be covered by BaseAuditableEntity.createdAt if it behaves as a
	// submission timestamp
	// If specific submittedAt is needed and BaseAuditableEntity.createdAt is for record
	// creation only:
	// @Column(nullable = false)
	// private LocalDateTime submittedAt;

	// @PrePersist
	// protected void onCreate() {
	// if (this.submittedAt == null) { // If not using BaseAuditableEntity for this timing
	// this.submittedAt = LocalDateTime.now();
	// }
	// }

}