package vn.edu.tlu.dms.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.registration.domain.ApplicationPriority;
import vn.edu.tlu.dms.registration.domain.ApplicationPriorityId;

public interface ApplicationPriorityRepository
        extends JpaRepository<ApplicationPriority, ApplicationPriorityId>,
                JpaSpecificationExecutor<ApplicationPriority> {}
