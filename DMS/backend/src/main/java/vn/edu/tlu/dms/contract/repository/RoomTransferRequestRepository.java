package vn.edu.tlu.dms.contract.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.contract.domain.RoomTransferRequest;

public interface RoomTransferRequestRepository
        extends JpaRepository<RoomTransferRequest, Long>,
                JpaSpecificationExecutor<RoomTransferRequest> {}
