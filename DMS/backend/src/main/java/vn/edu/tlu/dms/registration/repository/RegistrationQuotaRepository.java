package vn.edu.tlu.dms.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.registration.domain.RegistrationQuota;
import vn.edu.tlu.dms.registration.domain.RegistrationQuotaId;

public interface RegistrationQuotaRepository
        extends JpaRepository<RegistrationQuota, RegistrationQuotaId>,
                JpaSpecificationExecutor<RegistrationQuota> {}
