package academic.academic.domain.monthlyexam.service;

import academic.academic.domain.monthlyexam.dto.MonthlyExamCreateRequest;
import academic.academic.domain.monthlyexam.dto.MonthlyExamResponse;
import academic.academic.domain.monthlyexam.entity.MonthlyExam;
import academic.academic.domain.monthlyexam.repository.MonthlyExamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MonthlyExamServiceTest {

    @Mock
    private MonthlyExamRepository monthlyExamRepository;

    private MonthlyExamService monthlyExamService;

    @BeforeEach
    void setUp() {
        monthlyExamService = new MonthlyExamService(monthlyExamRepository);
    }

    @Nested
    class Create {

        @Test
        void 월말모의고사_회차를_생성한다() {
            MonthlyExamCreateRequest request = new MonthlyExamCreateRequest("8월 학평", "2026-08");
            given(monthlyExamRepository.save(any(MonthlyExam.class))).willAnswer(invocation -> invocation.getArgument(0));

            MonthlyExamResponse response = monthlyExamService.create(request);

            assertThat(response.examName()).isEqualTo("8월 학평");
            assertThat(response.examMonth()).isEqualTo("2026-08");
        }
    }

    @Nested
    class ListExams {

        @Test
        void 회차_목록을_시행연월_내림차순으로_조회한다() {
            MonthlyExam exam = MonthlyExam.builder().examName("8월 학평").examMonth("2026-08").build();
            ReflectionTestUtils.setField(exam, "id", 12L);
            given(monthlyExamRepository.findAllByOrderByExamMonthDescIdDesc()).willReturn(List.of(exam));

            List<MonthlyExamResponse> result = monthlyExamService.list();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).examName()).isEqualTo("8월 학평");
        }
    }
}
