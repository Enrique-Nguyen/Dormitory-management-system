package vn.edu.tlu.dms.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.billing.domain.DepositTransaction;

import java.util.List;
import java.util.Optional;

public interface DepositTransactionRepository
        extends JpaRepository<DepositTransaction, Long>,
                JpaSpecificationExecutor<DepositTransaction> {

    List<DepositTransaction> findByContractIdOrderByCreatedAtAscIdAsc(Long contractId);

    Optional<DepositTransaction> findByContractIdAndType(Long contractId, String type);

    Optional<DepositTransaction> findByContractIdAndMaintenanceRequestIdAndType(
            Long contractId, Long maintenanceRequestId, String type);
}
