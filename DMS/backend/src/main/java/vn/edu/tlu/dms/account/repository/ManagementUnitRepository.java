package vn.edu.tlu.dms.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.account.domain.ManagementUnit;

public interface ManagementUnitRepository
        extends JpaRepository<ManagementUnit, Long>, JpaSpecificationExecutor<ManagementUnit> {}
