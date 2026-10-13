package vn.edu.tlu.dms.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.registration.domain.RegistrationPeriod;

public interface RegistrationPeriodRepository
        extends JpaRepository<RegistrationPeriod, Long>,
                JpaSpecificationExecutor<RegistrationPeriod> {}
