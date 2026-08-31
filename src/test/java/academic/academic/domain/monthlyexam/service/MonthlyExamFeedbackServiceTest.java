package academic.academic.domain.monthlyexam.service;

import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordDetailResponse;
import academic.academic.domain.monthlyexam.dto.ScoreFeedbackResponse;
import academic.academic.domain.monthlyexam.dto.ScoreFeedbackUpsertRequest;
import academic.academic.domain.monthlyexam.dto.TypeFeedbackCreateRequest;
import academic.academic.domain.monthlyexam.dto.TypeFeedbackResponse;
import academic.academic.domain.monthlyexam.dto.TypeFeedbackUpdateRequest;
import academic.academic.domain.monthlyexam.entity.FeedbackStatus;
import academic.academic.domain.monthlyexam.entity.MonthlyExam;
import academic.academic.domain.monthlyexam.entity.MonthlyExamRecord;
import academic.academic.domain.monthlyexam.entity.MonthlyExamScoreFeedback;
import academic.academic.domain.monthlyexam.entity.MonthlyExamTypeFeedback;
import academic.academic.domain.monthlyexam.entity.TypeCategory;
import academic.academic.domain.monthlyexam.repository.MonthlyExamRecordRepository;
import academic.academic.domain.monthlyexam.repository.MonthlyExamScoreFeedbackRepository;
import academic.academic.domain.monthlyexam.repository.MonthlyExamTypeFeedbackRepository;
import academic.academic.domain.monthlyexam.repository.TypeCategoryRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MonthlyExamFeedbackServiceTest {

    @Mock
    private MonthlyExamRecordRepository monthlyExamRecordRepository;
    @Mock
    private TypeCategoryRepository typeCategoryRepository;
    @Mock
    private MonthlyExamTypeFeedbackRepository typeFeedbackRepository;
    @Mock
    private MonthlyExamScoreFeedbackRepository scoreFeedbackRepository;

    private MonthlyExamFeedbackService monthlyExamFeedbackService;

    private MonthlyExamRecord record;
    private TypeCategory typeCategory;

    @BeforeEach
    void setUp() {
        monthlyExamFeedbackService = new MonthlyExamFeedbackService(
                monthlyExamRecordRepository, typeCategoryRepository, typeFeedbackRepository, scoreFeedbackRepository);

        MonthlyExam exam = MonthlyExam.builder().examName("8월 학평").examMonth("2026-08").build();
        ReflectionTestUtils.setField(exam, "id", 12L);
        Student student = Student.builder().name("김민준").build();
        ReflectionTestUtils.setField(student, "id", 101L);
        record = MonthlyExamRecord.builder().monthlyExam(exam).student(student).rawScore(82).build();
        ReflectionTestUtils.setField(record, "id", 5000L);

        typeCategory = TypeCategory.builder().name("어휘").build();
        ReflectionTestUtils.setField(typeCategory, "id", 1L);
    }

    @Nested
    class GetDetail {

        @Test
        void 성적_피드백_전체를_조회한다() {
            MonthlyExamTypeFeedback typeFeedback = MonthlyExamTypeFeedback.builder()
                    .monthlyExamRecord(record).typeCategory(typeCategory)
                    .status(FeedbackStatus.STRENGTH).feedbackText("우수합니다.").build();
            MonthlyExamScoreFeedback scoreFeedback = MonthlyExamScoreFeedback.builder()
                    .monthlyExamRecord(record).scoreBand("80점대").feedbackText("탄탄합니다.").build();

            given(monthlyExamRecordRepository.findById(5000L)).willReturn(Optional.of(record));
            given(typeFeedbackRepository.findByMonthlyExamRecordId(5000L)).willReturn(List.of(typeFeedback));
            given(scoreFeedbackRepository.findByMonthlyExamRecordId(5000L)).willReturn(Optional.of(scoreFeedback));

            MonthlyExamRecordDetailResponse response = monthlyExamFeedbackService.getDetail(5000L);

            assertThat(response.record().rawScore()).isEqualTo(82);
            assertThat(response.typeFeedbacks()).hasSize(1);
            assertThat(response.scoreFeedback().scoreBand()).isEqualTo("80점대");
        }

        @Test
        void 점수대별_피드백이_없으면_null을_반환한다() {
            given(monthlyExamRecordRepository.findById(5000L)).willReturn(Optional.of(record));
            given(typeFeedbackRepository.findByMonthlyExamRecordId(5000L)).willReturn(List.of());
            given(scoreFeedbackRepository.findByMonthlyExamRecordId(5000L)).willReturn(Optional.empty());

            MonthlyExamRecordDetailResponse response = monthlyExamFeedbackService.getDetail(5000L);

            assertThat(response.scoreFeedback()).isNull();
        }

        @Test
        void 성적이_없으면_NOT_FOUND_예외() {
            given(monthlyExamRecordRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> monthlyExamFeedbackService.getDetail(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class AddTypeFeedback {

        @Test
        void 유형별_피드백을_추가한다() {
            TypeFeedbackCreateRequest request = new TypeFeedbackCreateRequest(1L, FeedbackStatus.STRENGTH, "우수합니다.");
            given(monthlyExamRecordRepository.findById(5000L)).willReturn(Optional.of(record));
            given(typeCategoryRepository.findById(1L)).willReturn(Optional.of(typeCategory));
            given(typeFeedbackRepository.save(any(MonthlyExamTypeFeedback.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            TypeFeedbackResponse response = monthlyExamFeedbackService.addTypeFeedback(5000L, request);

            assertThat(response.typeCategory()).isEqualTo("어휘");
            assertThat(response.status()).isEqualTo(FeedbackStatus.STRENGTH);
        }

        @Test
        void 유형_카테고리가_없으면_NOT_FOUND_예외() {
            TypeFeedbackCreateRequest request = new TypeFeedbackCreateRequest(999L, FeedbackStatus.STRENGTH, null);
            given(monthlyExamRecordRepository.findById(5000L)).willReturn(Optional.of(record));
            given(typeCategoryRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> monthlyExamFeedbackService.addTypeFeedback(5000L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class UpdateAndDeleteTypeFeedback {

        @Test
        void 유형별_피드백을_수정한다() {
            MonthlyExamTypeFeedback feedback = MonthlyExamTypeFeedback.builder()
                    .monthlyExamRecord(record).typeCategory(typeCategory)
                    .status(FeedbackStatus.NEEDS_WORK).feedbackText("보완 필요").build();
            ReflectionTestUtils.setField(feedback, "id", 7000L);
            given(typeFeedbackRepository.findById(7000L)).willReturn(Optional.of(feedback));

            TypeFeedbackResponse response = monthlyExamFeedbackService
                    .updateTypeFeedback(7000L, new TypeFeedbackUpdateRequest(FeedbackStatus.STRENGTH, "개선됨"));

            assertThat(response.status()).isEqualTo(FeedbackStatus.STRENGTH);
            assertThat(response.feedbackText()).isEqualTo("개선됨");
        }

        @Test
        void 유형별_피드백을_삭제한다() {
            MonthlyExamTypeFeedback feedback = MonthlyExamTypeFeedback.builder()
                    .monthlyExamRecord(record).typeCategory(typeCategory)
                    .status(FeedbackStatus.STRENGTH).build();
            ReflectionTestUtils.setField(feedback, "id", 7000L);
            given(typeFeedbackRepository.findById(7000L)).willReturn(Optional.of(feedback));

            monthlyExamFeedbackService.deleteTypeFeedback(7000L);

            verify(typeFeedbackRepository).delete(feedback);
        }

        @Test
        void 피드백이_없으면_NOT_FOUND_예외() {
            given(typeFeedbackRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> monthlyExamFeedbackService.deleteTypeFeedback(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class UpsertScoreFeedback {

        @Test
        void 점수대별_피드백이_없으면_새로_생성한다() {
            ScoreFeedbackUpsertRequest request = new ScoreFeedbackUpsertRequest("80점대", "탄탄합니다.");
            given(monthlyExamRecordRepository.findById(5000L)).willReturn(Optional.of(record));
            given(scoreFeedbackRepository.findByMonthlyExamRecordId(5000L)).willReturn(Optional.empty());
            given(scoreFeedbackRepository.save(any(MonthlyExamScoreFeedback.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            ScoreFeedbackResponse response = monthlyExamFeedbackService.upsertScoreFeedback(5000L, request);

            assertThat(response.scoreBand()).isEqualTo("80점대");
        }

        @Test
        void 이미_있으면_갱신하고_새로_생성하지_않는다() {
            MonthlyExamScoreFeedback existing = MonthlyExamScoreFeedback.builder()
                    .monthlyExamRecord(record).scoreBand("70점대").feedbackText("이전").build();
            ScoreFeedbackUpsertRequest request = new ScoreFeedbackUpsertRequest("80점대", "탄탄합니다.");

            given(monthlyExamRecordRepository.findById(5000L)).willReturn(Optional.of(record));
            given(scoreFeedbackRepository.findByMonthlyExamRecordId(5000L)).willReturn(Optional.of(existing));

            ScoreFeedbackResponse response = monthlyExamFeedbackService.upsertScoreFeedback(5000L, request);

            assertThat(response.scoreBand()).isEqualTo("80점대");
            verify(scoreFeedbackRepository, never()).save(any(MonthlyExamScoreFeedback.class));
        }
    }
}
