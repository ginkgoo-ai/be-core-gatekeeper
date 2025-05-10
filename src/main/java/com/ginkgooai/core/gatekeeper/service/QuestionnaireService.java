package com.ginkgooai.core.gatekeeper.service;

import com.ginkgooai.core.gatekeeper.dto.request.QuestionnaireSubmissionRequest;
import com.ginkgooai.core.gatekeeper.domain.QuestionnaireResult;

public interface QuestionnaireService {

	QuestionnaireResult saveResponse(QuestionnaireSubmissionRequest submissionRequest);

}