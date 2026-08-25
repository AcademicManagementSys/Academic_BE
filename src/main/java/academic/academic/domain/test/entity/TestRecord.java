package academic.academic.domain.test.entity;

import academic.academic.domain.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 영역별(단어/독해/문법/구문독해) 테스트 결과 (FR-04-03).
 */
@Entity
@Table(name = "test_records", uniqueConstraints = @UniqueConstraint(
        name = "uk_test_record_session_student_subject", columnNames = {"test_session_id", "student_id", "subject"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_session_id", nullable = false)
    private TestSession testSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestSubject subject;

    @Column(name = "is_taken", nullable = false)
    private boolean taken;

    private Integer score;

    @Column(name = "max_score")
    private Integer maxScore;

    @Column(length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public TestRecord(TestSession testSession, Student student, TestSubject subject, boolean taken,
                       Integer score, Integer maxScore, String comment) {
        this.testSession = testSession;
        this.student = student;
        this.subject = subject;
        this.taken = taken;
        this.score = score;
        this.maxScore = maxScore;
        this.comment = comment;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(boolean taken, Integer score, Integer maxScore, String comment) {
        this.taken = taken;
        this.score = score;
        this.maxScore = maxScore;
        this.comment = comment;
    }
}
