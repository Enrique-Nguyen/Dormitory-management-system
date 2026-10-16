package vn.edu.tlu.dms.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.registration.domain.RegistrationPeriodUnit;
import vn.edu.tlu.dms.registration.domain.RegistrationPeriodUnitId;

public interface RegistrationPeriodUnitRepository
        extends JpaRepository<RegistrationPeriodUnit, RegistrationPeriodUnitId>,
                JpaSpecificationExecutor<RegistrationPeriodUnit> {}
