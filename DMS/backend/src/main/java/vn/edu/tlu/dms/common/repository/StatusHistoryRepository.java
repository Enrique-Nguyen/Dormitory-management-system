package vn.edu.tlu.dms.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.common.domain.StatusHistory;

public interface StatusHistoryRepository
        extends JpaRepository<StatusHistory, Long>, JpaSpecificationExecutor<StatusHistory> {}
