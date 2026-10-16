package vn.edu.tlu.dms.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.room.domain.Semester;

public interface SemesterRepository
        extends JpaRepository<Semester, Long>, JpaSpecificationExecutor<Semester> {}
