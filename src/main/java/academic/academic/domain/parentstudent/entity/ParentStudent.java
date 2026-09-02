package academic.academic.domain.parentstudent.entity;

import academic.academic.domain.student.entity.Student;
import academic.academic.domain.user.entity.User;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 학부모-학생(자녀) 연결 (FR-01-04). relationType으로 부/모/기타를 구분한다(v1.1 신규).
 */
@Entity
@Table(name = "parent_students", uniqueConstraints = @UniqueConstraint(
        name = "uk_parent_student", columnNames = {"parent_user_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParentStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_user_id", nullable = false)
    private User parentUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 20)
    private RelationType relationType;

    private ParentStudent(User parentUser, Student student, RelationType relationType) {
        this.parentUser = parentUser;
        this.student = student;
        this.relationType = relationType;
    }

    public static ParentStudent of(User parentUser, Student student, RelationType relationType) {
        return new ParentStudent(parentUser, student, relationType);
    }

    public void changeRelationType(RelationType relationType) {
        this.relationType = relationType;
    }
}
