package vn.edu.tlu.dms.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.account.domain.StudentProfile;

public interface StudentProfileRepository
        extends JpaRepository<StudentProfile, Long>, JpaSpecificationExecutor<StudentProfile> {}
