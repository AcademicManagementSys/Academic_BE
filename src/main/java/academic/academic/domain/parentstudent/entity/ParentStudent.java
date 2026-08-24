package academic.academic.domain.parentstudent.entity;

import academic.academic.domain.student.entity.Student;
import academic.academic.domain.user.entity.User;
import jakarta.persistence.Entity;
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

    private ParentStudent(User parentUser, Student student) {
        this.parentUser = parentUser;
        this.student = student;
    }

    public static ParentStudent of(User parentUser, Student student) {
        return new ParentStudent(parentUser, student);
    }
}
