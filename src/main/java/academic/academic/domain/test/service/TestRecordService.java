package academic.academic.domain.test.service;

import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.test.dto.TestRecordBulkRequest;
import academic.academic.domain.test.dto.TestRecordItem;
import academic.academic.domain.test.dto.TestRecordResponse;
import academic.academic.domain.test.entity.TestRecord;
import academic.academic.domain.test.entity.TestSession;
import academic.academic.domain.test.entity.TestSubject;
import academic.academic.domain.test.repository.TestRecordRepository;
import academic.academic.domain.test.repository.TestSessionRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestRecordService {

    private final TestSessionRepository testSessionRepository;
    private final TestRecordRepository testRecordRepository;
    private final StudentRepository studentRepository;

    public List<TestRecordResponse> getSessionRecords(Long testSessionId) {
        TestSession session = getSession(testSessionId);
        List<Student> students = studentRepository.findBySchoolClassId(session.getSchoolClass().getId());

        Map<Long, List<TestRecord>> byStudentId = testRecordRepository.findByTestSessionId(testSessionId).stream()
                .collect(Collectors.groupingBy(r -> r.getStudent().getId()));

        List<TestRecordResponse> responses = new ArrayList<>();
        for (Student student : students) {
            List<TestRecord> studentRecords = byStudentId.getOrDefault(student.getId(), List.of());
            for (TestSubject subject : TestSubject.values()) {
                TestRecord record = studentRecords.stream()
                        .filter(r -> r.getSubject() == subject)
                        .findFirst()
                        .orElse(null);
                responses.add(record != null
                        ? TestRecordResponse.from(record)
                        : TestRecordResponse.unchecked(session, student, subject));
            }
        }
        return responses;
    }

    @Transactional
    public List<TestRecordResponse> saveBulk(TestRecordBulkRequest request) {
        TestSession session = getSession(request.testSessionId());

        List<TestRecordResponse> responses = new ArrayList<>();
        for (TestRecordItem item : request.records()) {
            Student student = studentRepository.findById(item.studentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=" + item.studentId()));
            if (student.getSchoolClass() == null || !student.getSchoolClass().getId().equals(session.getSchoolClass().getId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "해당 반 소속 학생이 아닙니다. studentId=" + item.studentId());
            }

            TestRecord record = testRecordRepository
                    .findByTestSessionIdAndStudentIdAndSubject(session.getId(), student.getId(), item.subject())
                    .orElse(null);
            if (record == null) {
                record = testRecordRepository.save(TestRecord.builder()
                        .testSession(session)
                        .student(student)
                        .subject(item.subject())
                        .taken(item.isTaken())
                        .score(item.score())
                        .maxScore(item.maxScore())
                        .comment(item.comment())
                        .build());
            } else {
                record.update(item.isTaken(), item.score(), item.maxScore(), item.comment());
            }
            responses.add(TestRecordResponse.from(record));
        }
        return responses;
    }

    public List<TestRecordResponse> getStudentTests(Long studentId, int limit) {
        if (!studentRepository.existsById(studentId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=" + studentId);
        }

        List<TestSession> sessions = testSessionRepository.findRecentSessionsByStudentId(studentId, PageRequest.of(0, limit));
        if (sessions.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> sessionOrder = new HashMap<>();
        for (int i = 0; i < sessions.size(); i++) {
            sessionOrder.put(sessions.get(i).getId(), i);
        }
        List<Long> sessionIds = sessions.stream().map(TestSession::getId).toList();

        return testRecordRepository.findByStudentIdAndTestSessionIdIn(studentId, sessionIds).stream()
                .sorted(Comparator.<TestRecord>comparingInt(r -> sessionOrder.get(r.getTestSession().getId()))
                        .thenComparing(r -> r.getSubject().ordinal()))
                .map(TestRecordResponse::from)
                .toList();
    }

    private TestSession getSession(Long id) {
        return testSessionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "테스트 회차를 찾을 수 없습니다. id=" + id));
    }
}
