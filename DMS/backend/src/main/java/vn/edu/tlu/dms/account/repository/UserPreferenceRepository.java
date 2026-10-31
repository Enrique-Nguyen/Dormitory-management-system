package vn.edu.tlu.dms.account.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import vn.edu.tlu.dms.account.domain.UserPreference;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    /** Khóa dòng sở thích của tài khoản trước khi so version và ghi, tránh hai tab ghi đè nhau. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserPreference p where p.userId = :userId")
    Optional<UserPreference> lockByUserId(Long userId);
}
