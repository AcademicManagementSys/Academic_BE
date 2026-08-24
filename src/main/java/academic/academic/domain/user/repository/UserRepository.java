package academic.academic.domain.user.repository;

import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    Optional<User> findByLoginId(String loginId);

    List<User> findByRole(Role role);
}
