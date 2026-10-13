package vn.edu.tlu.dms.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.registration.domain.PriorityCategory;

public interface PriorityCategoryRepository
        extends JpaRepository<PriorityCategory, Long>, JpaSpecificationExecutor<PriorityCategory> {}
