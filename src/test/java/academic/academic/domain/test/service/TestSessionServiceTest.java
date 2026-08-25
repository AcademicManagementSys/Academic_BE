package academic.academic.domain.test.service;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.test.dto.TestSessionCreateRequest;
import academic.academic.domain.test.dto.TestSessionResponse;
import academic.academic.domain.test.entity.TestSession;
import academic.academic.domain.test.repository.TestSessionRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TestSessionServiceTest {

    @Mock
    private TestSessionRepository testSessionRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;

    private TestSessionService testSessionService;

    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        testSessionService = new TestSessionService(testSessionRepository, schoolClassRepository);

        schoolClass = SchoolClass.builder().name("중2 심화반").build();
        ReflectionTestUtils.setField(schoolClass, "id", 3L);
    }

    @Nested
    class Create {

        @Test
        void 테스트_회차를_생성한다() {
            TestSessionCreateRequest request = new TestSessionCreateRequest(3L, "8월 3주차 테스트", LocalDate.of(2026, 8, 19));
            given(schoolClassRepository.findById(3L)).willReturn(Optional.of(schoolClass));
            given(testSessionRepository.save(any(TestSession.class))).willAnswer(invocation -> invocation.getArgument(0));

            TestSessionResponse response = testSessionService.create(request);

            assertThat(response.classId()).isEqualTo(3L);
            assertThat(response.title()).isEqualTo("8월 3주차 테스트");
            assertThat(response.testDate()).isEqualTo(LocalDate.of(2026, 8, 19));
        }

        @Test
        void 반이_존재하지_않으면_NOT_FOUND_예외() {
            TestSessionCreateRequest request = new TestSessionCreateRequest(999L, "8월 3주차 테스트", LocalDate.of(2026, 8, 19));
            given(schoolClassRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> testSessionService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class Search {

        @Test
        void 반의_테스트_회차_목록을_조회한다() {
            TestSession session = TestSession.builder().schoolClass(schoolClass).title("8월 3주차 테스트")
                    .testDate(LocalDate.of(2026, 8, 19)).build();
            ReflectionTestUtils.setField(session, "id", 901L);

            given(schoolClassRepository.findById(3L)).willReturn(Optional.of(schoolClass));
            given(testSessionRepository.findBySchoolClassIdOrderByTestDateDescIdDesc(3L)).willReturn(List.of(session));

            List<TestSessionResponse> result = testSessionService.search(3L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("8월 3주차 테스트");
        }

        @Test
        void 반이_존재하지_않으면_NOT_FOUND_예외() {
            given(schoolClassRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> testSessionService.search(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }
}
