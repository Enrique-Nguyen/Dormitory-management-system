package vn.edu.tlu.dms.contract.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.contract.domain.StorageRecord;

public interface StorageRecordRepository
        extends JpaRepository<StorageRecord, Long>, JpaSpecificationExecutor<StorageRecord> {}
