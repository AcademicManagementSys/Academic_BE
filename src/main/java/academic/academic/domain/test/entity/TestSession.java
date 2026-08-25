package academic.academic.domain.test.entity;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 테스트 회차 (FR-04-01). 반 단위로 생성되며, 회차 내에서 4개 영역(TestSubject)이 선택적으로 입력된다.
 */
@Entity
@Table(name = "test_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "test_date", nullable = false)
    private LocalDate testDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public TestSession(SchoolClass schoolClass, String title, LocalDate testDate) {
        this.schoolClass = schoolClass;
        this.title = title;
        this.testDate = testDate;
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
}
