package academic.academic.domain.test.service;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.test.dto.TestSessionCreateRequest;
import academic.academic.domain.test.dto.TestSessionResponse;
import academic.academic.domain.test.entity.TestSession;
import academic.academic.domain.test.repository.TestSessionRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestSessionService {

    private final TestSessionRepository testSessionRepository;
    private final SchoolClassRepository schoolClassRepository;

    @Transactional
    public TestSessionResponse create(TestSessionCreateRequest request) {
        SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "반을 찾을 수 없습니다. id=" + request.classId()));

        TestSession session = testSessionRepository.save(TestSession.builder()
                .schoolClass(schoolClass)
                .title(request.title())
                .testDate(request.testDate())
                .build());
        return TestSessionResponse.from(session);
    }

    public List<TestSessionResponse> search(Long classId) {
        schoolClassRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "반을 찾을 수 없습니다. id=" + classId));
        return testSessionRepository.findBySchoolClassIdOrderByTestDateDescIdDesc(classId).stream()
                .map(TestSessionResponse::from)
                .toList();
    }
}
