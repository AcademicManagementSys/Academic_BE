package academic.academic.domain.monthlyexam.dto;

import academic.academic.domain.monthlyexam.entity.MonthlyExam;
import academic.academic.domain.monthlyexam.entity.MonthlyExamRecord;
import academic.academic.domain.student.entity.Student;

public record MonthlyExamRecordResponse(
        Long id,
        Long monthlyExamId,
        String examName,
        String examMonth,
        Long studentId,
        String studentName,
        Integer rawScore,
        Integer stdScore,
        Integer percentile,
        String grade
) {
    public static MonthlyExamRecordResponse from(MonthlyExamRecord record) {
        MonthlyExam exam = record.getMonthlyExam();
        return new MonthlyExamRecordResponse(
                record.getId(),
                exam.getId(),
                exam.getExamName(),
                exam.getExamMonth(),
                record.getStudent().getId(),
                record.getStudent().getName(),
                record.getRawScore(),
                record.getStdScore(),
                record.getPercentile(),
                record.getGrade()
        );
    }

    public static MonthlyExamRecordResponse unrecorded(MonthlyExam exam, Student student) {
        return new MonthlyExamRecordResponse(
                null,
                exam.getId(),
                exam.getExamName(),
                exam.getExamMonth(),
                student.getId(),
                student.getName(),
                null,
                null,
                null,
                null
        );
    }
}
