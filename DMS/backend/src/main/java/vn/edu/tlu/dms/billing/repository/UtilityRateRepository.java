package vn.edu.tlu.dms.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.billing.domain.UtilityRate;

public interface UtilityRateRepository
        extends JpaRepository<UtilityRate, Long>, JpaSpecificationExecutor<UtilityRate> {}
