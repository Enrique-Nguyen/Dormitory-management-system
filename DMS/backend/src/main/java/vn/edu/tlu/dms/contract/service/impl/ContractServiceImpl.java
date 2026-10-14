package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.common.support.PageQuery;
import vn.edu.tlu.dms.contract.api.ContractDtos;
import vn.edu.tlu.dms.contract.domain.DormitoryContract;
import vn.edu.tlu.dms.contract.repository.DormitoryContractRepository;
import vn.edu.tlu.dms.contract.service.ContractService;

import java.util.List;
import java.util.Set;

/**
 * Khung truy vấn hợp đồng. Hợp đồng được sinh bởi nghiệp vụ phân giường, không được tạo, sửa trạng
 * thái hoặc xóa bằng CRUD tổng quát vì sẽ làm lệch lịch sử phân phòng và sổ cọc.
 */
@Service
@Transactional(readOnly = true)
public class ContractServiceImpl implements ContractService {

    private final DormitoryContractRepository repository;

    private final CurrentUser currentUser;

    private final ScopeGuard scopeGuard;

    private final StatusHistoryService historyService;

    public ContractServiceImpl(
            DormitoryContractRepository repository,
            CurrentUser currentUser,
            ScopeGuard scopeGuard,
            StatusHistoryService historyService) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.scopeGuard = scopeGuard;
        this.historyService = historyService;
    }

    @Override
    public PageResponse<ContractDtos.Response> searchContracts(
            int page, int size, String status, boolean mine) {
        return searchContracts(page, size, status, mine, "");
    }

    @Override
    public PageResponse<ContractDtos.Response> searchContracts(
            int page, int size, String status, boolean mine, String q) {
        scopeGuard.requireRole(mine ? new String[] {"STUDENT"} : new String[] {"STAFF"});
        var actor = currentUser.get();
        PageQuery.of(page, size, null, Set.of("id"), "id,desc");
        return PageResponse.from(
                repository
                        .searchAccessibleContracts(
                                actor.role(),
                                actor.id(),
                                actor.unitId(),
                                actor.crossUnitAccess(),
                                status,
                                "%" + q.trim() + "%",
                                PageRequest.of(page, size))
                        .map(contract -> toContractResponse(contract, false)));
    }

    @Override
    public ContractDtos.Response getContractById(Long id) {
        scopeGuard.requireRole("STUDENT", "STAFF");
        var actor = currentUser.get();
        DormitoryContract contract =
                repository
                        .findAccessibleContract(
                                id,
                                actor.role(),
                                actor.id(),
                                actor.unitId(),
                                actor.crossUnitAccess())
                        .orElseThrow(BusinessException::notFound);
        return toContractResponse(contract, true);
    }

    @Override
    public ContractDtos.Response getMyCurrentContract() {
        scopeGuard.requireRole("STUDENT");
        return repository
                .findAll(
                        (root, query, builder) ->
                                builder.and(
                                        builder.equal(
                                                root.get("studentId"), currentUser.get().id()),
                                        root.get("status").in("PENDING", "ACTIVE")))
                .stream()
                .findFirst()
                .map(contract -> toContractResponse(contract, true))
                .orElse(null);
    }

    @Override
    public boolean hasLiveContractForApplication(Long applicationId) {
        return repository.exists(
                (root, query, builder) ->
                        builder.and(
                                builder.equal(root.get("applicationId"), applicationId),
                                root.get("status").in("PENDING", "ACTIVE")));
    }

    private ContractDtos.Response toContractResponse(DormitoryContract contract, boolean detail) {
        // Chỉ quảng bá hành động đã triển khai. Kích hoạt/trả phòng cần transaction liên module.
        return new ContractDtos.Response(
                contract.getId(),
                contract.getCode(),
                contract.getStudentId(),
                contract.getApplicationId(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getStatus(),
                contract.getActivationDeadline(),
                contract.getDepositRequired(),
                contract.getActivatedAt(),
                contract.getStatusReason(),
                detail ? List.of() : null,
                detail ? historyService.list("CONTRACT", contract.getId()) : null);
    }
}
