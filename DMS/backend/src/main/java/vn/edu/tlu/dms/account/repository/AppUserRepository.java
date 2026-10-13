package vn.edu.tlu.dms.account.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import vn.edu.tlu.dms.account.domain.AppUser;

import java.util.Optional;

public interface AppUserRepository
        extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.username = :username")
    Optional<AppUser> lockByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.id = :id")
    Optional<AppUser> lockById(Long id);
}
