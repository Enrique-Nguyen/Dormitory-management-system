package vn.edu.tlu.dms.contract.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.contract.domain.MoveOutRecord;

public interface MoveOutRecordRepository
        extends JpaRepository<MoveOutRecord, Long>, JpaSpecificationExecutor<MoveOutRecord> {}
