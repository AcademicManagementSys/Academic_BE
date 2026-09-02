package academic.academic.domain.monthlyexam.service;

import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordDetailResponse;
import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordResponse;
import academic.academic.domain.monthlyexam.dto.ScoreFeedbackResponse;
import academic.academic.domain.monthlyexam.dto.ScoreFeedbackUpsertRequest;
import academic.academic.domain.monthlyexam.dto.TypeFeedbackCreateRequest;
import academic.academic.domain.monthlyexam.dto.TypeFeedbackResponse;
import academic.academic.domain.monthlyexam.dto.TypeFeedbackUpdateRequest;
import academic.academic.domain.monthlyexam.entity.MonthlyExamRecord;
import academic.academic.domain.monthlyexam.entity.MonthlyExamScoreFeedback;
import academic.academic.domain.monthlyexam.entity.MonthlyExamTypeFeedback;
import academic.academic.domain.monthlyexam.entity.TypeCategory;
import academic.academic.domain.monthlyexam.repository.MonthlyExamRecordRepository;
import academic.academic.domain.monthlyexam.repository.MonthlyExamScoreFeedbackRepository;
import academic.academic.domain.monthlyexam.repository.MonthlyExamTypeFeedbackRepository;
import academic.academic.domain.monthlyexam.repository.TypeCategoryRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyExamFeedbackService {

    private final MonthlyExamRecordRepository monthlyExamRecordRepository;
    private final TypeCategoryRepository typeCategoryRepository;
    private final MonthlyExamTypeFeedbackRepository typeFeedbackRepository;
    private final MonthlyExamScoreFeedbackRepository scoreFeedbackRepository;

    public MonthlyExamRecordDetailResponse getDetail(Long recordId) {
        MonthlyExamRecord record = getRecord(recordId);
        List<TypeFeedbackResponse> typeFeedbacks = typeFeedbackRepository.findByMonthlyExamRecordId(recordId).stream()
                .map(TypeFeedbackResponse::from)
                .toList();
        ScoreFeedbackResponse scoreFeedback = scoreFeedbackRepository.findByMonthlyExamRecordId(recordId)
                .map(ScoreFeedbackResponse::from)
                .orElse(null);
        return new MonthlyExamRecordDetailResponse(MonthlyExamRecordResponse.from(record), typeFeedbacks, scoreFeedback);
    }

    @Transactional
    public TypeFeedbackResponse addTypeFeedback(Long recordId, TypeFeedbackCreateRequest request) {
        MonthlyExamRecord record = getRecord(recordId);
        TypeCategory typeCategory = typeCategoryRepository.findById(request.typeCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "유형 카테고리를 찾을 수 없습니다. id=" + request.typeCategoryId()));

        MonthlyExamTypeFeedback feedback = typeFeedbackRepository.save(MonthlyExamTypeFeedback.builder()
                .monthlyExamRecord(record)
                .typeCategory(typeCategory)
                .status(request.status())
                .feedbackText(request.feedbackText())
                .build());
        return TypeFeedbackResponse.from(feedback);
    }

    @Transactional
    public TypeFeedbackResponse updateTypeFeedback(Long feedbackId, TypeFeedbackUpdateRequest request) {
        MonthlyExamTypeFeedback feedback = getTypeFeedback(feedbackId);
        feedback.update(request.status(), request.feedbackText());
        return TypeFeedbackResponse.from(feedback);
    }

    @Transactional
    public void deleteTypeFeedback(Long feedbackId) {
        MonthlyExamTypeFeedback feedback = getTypeFeedback(feedbackId);
        typeFeedbackRepository.delete(feedback);
    }

    @Transactional
    public ScoreFeedbackResponse upsertScoreFeedback(Long recordId, ScoreFeedbackUpsertRequest request) {
        MonthlyExamRecord record = getRecord(recordId);

        MonthlyExamScoreFeedback feedback = scoreFeedbackRepository.findByMonthlyExamRecordId(recordId).orElse(null);
        if (feedback == null) {
            feedback = scoreFeedbackRepository.save(MonthlyExamScoreFeedback.builder()
                    .monthlyExamRecord(record)
                    .scoreBand(request.scoreBand())
                    .feedbackText(request.feedbackText())
                    .build());
        } else {
            feedback.update(request.scoreBand(), request.feedbackText());
        }
        return ScoreFeedbackResponse.from(feedback);
    }

    private MonthlyExamRecord getRecord(Long id) {
        return monthlyExamRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "월말모의고사 성적을 찾을 수 없습니다. id=" + id));
    }

    private MonthlyExamTypeFeedback getTypeFeedback(Long id) {
        return typeFeedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "유형별 피드백을 찾을 수 없습니다. id=" + id));
    }

    /** 소유권 체크용 — 성적 id로 학생을 조회한다. */
    public Long getStudentIdForRecord(Long recordId) {
        return getRecord(recordId).getStudent().getId();
    }

    /** 소유권 체크용 — 유형별 피드백 id로 학생을 조회한다. */
    public Long getStudentIdForTypeFeedback(Long feedbackId) {
        return getTypeFeedback(feedbackId).getMonthlyExamRecord().getStudent().getId();
    }
}
