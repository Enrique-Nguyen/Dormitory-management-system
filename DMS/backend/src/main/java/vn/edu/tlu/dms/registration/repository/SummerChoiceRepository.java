package vn.edu.tlu.dms.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.registration.domain.SummerChoice;

public interface SummerChoiceRepository
        extends JpaRepository<SummerChoice, Long>, JpaSpecificationExecutor<SummerChoice> {}
