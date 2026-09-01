package academic.academic.domain.student.service;

import academic.academic.domain.attendance.repository.AttendanceRepository;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.monthlyexam.repository.MonthlyExamRecordRepository;
import academic.academic.domain.student.dto.NotificationBadgeResponse;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.test.repository.TestRecordRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * 학부모/학생 알림 배지 (FR-08-01, 선택 기능). 외부 발송(문자·알림톡) 연동 없이,
 * since 이후 새로 입력된 항목 건수만 세어 화면 배지 표시에 사용하도록 한다(8장 향후 확장 참고).
 * 클라이언트가 마지막으로 확인한 시각을 since로 넘기며, 서버는 별도의 읽음 상태를 저장하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationBadgeService {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final HomeworkRecordRepository homeworkRecordRepository;
    private final TestRecordRepository testRecordRepository;
    private final MonthlyExamRecordRepository monthlyExamRecordRepository;

    public NotificationBadgeResponse getBadge(Long studentId, String since) {
        if (!studentRepository.existsById(studentId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=" + studentId);
        }
        LocalDateTime sinceDateTime = parseSince(since);

        int attendanceCount = (int) attendanceRepository.countByStudentIdAndCreatedAtAfter(studentId, sinceDateTime);
        int homeworkCount = (int) homeworkRecordRepository.countByStudentIdAndCreatedAtAfter(studentId, sinceDateTime);
        int testCount = (int) testRecordRepository.countByStudentIdAndCreatedAtAfter(studentId, sinceDateTime);
        int monthlyExamCount = (int) monthlyExamRecordRepository.countByStudentIdAndCreatedAtAfter(studentId, sinceDateTime);
        int totalCount = attendanceCount + homeworkCount + testCount + monthlyExamCount;

        return new NotificationBadgeResponse(sinceDateTime, attendanceCount, homeworkCount, testCount,
                monthlyExamCount, totalCount);
    }

    private LocalDateTime parseSince(String since) {
        if (since == null) {
            return LocalDateTime.now().toLocalDate().atStartOfDay();
        }
        try {
            return LocalDateTime.parse(since);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "since 형식이 올바르지 않습니다. (yyyy-MM-ddTHH:mm:ss)");
        }
    }
}
