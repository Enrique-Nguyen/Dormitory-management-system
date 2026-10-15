package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
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
    public PageResponse<ContractDtos.Response> searchByPage(SearchRequest input, boolean mine) {
        scopeGuard.requirePermission(Permission.CONTRACT_READ);
        scopeGuard.requireRole(mine ? new String[] {Role.STUDENT} : new String[] {Role.STAFF});
        var actor = currentUser.get();
        var pageable =
                PageQuery.of(
                        input.page(),
                        input.size(),
                        input.sort(),
                        Set.of("id", "code", "status", "startDate", "endDate", "createdAt"),
                        "createdAt,desc");
        // Native SQL dùng tên cột được ánh xạ từ whitelist, tuyệt đối không nhận tên SQL từ client.
        var orders =
                pageable.getSort().stream()
                        .map(
                                order ->
                                        new Sort.Order(
                                                order.getDirection(),
                                                switch (order.getProperty()) {
                                                    case "startDate" -> "start_date";
                                                    case "endDate" -> "end_date";
                                                    case "createdAt" -> "created_at";
                                                    default -> order.getProperty();
                                                }))
                        .toList();
        List<String> statuses = input.statuses();
        return PageResponse.from(
                repository
                        .searchAccessibleContracts(
                                actor.role(),
                                actor.id(),
                                actor.unitId(),
                                actor.crossUnitAccess(),
                                mine,
                                statuses == null || statuses.isEmpty(),
                                statuses == null || statuses.isEmpty() ? List.of("") : statuses,
                                "%" + input.q().trim() + "%",
                                PageRequest.of(input.page(), input.size(), Sort.by(orders)))
                        .map(contract -> toContractResponse(contract, false)));
    }

    @Override
    public ContractDtos.Response getContractById(Long id) {
        scopeGuard.requirePermission(Permission.CONTRACT_READ);
        scopeGuard.requireRole(Role.STUDENT, Role.STAFF);
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
        scopeGuard.requirePermission(Permission.CONTRACT_READ);
        scopeGuard.requireRole(Role.STUDENT);
        return repository
                .findAll(
                        (root, query, builder) ->
                                builder.and(
                                        builder.equal(
                                                root.get("studentId"), currentUser.get().id()),
                                        root.get("status")
                                                .in(ContractStatus.PENDING, ContractStatus.ACTIVE)))
                .stream()
                .findFirst()
                .map(contract -> toContractResponse(contract, true))
                .orElse(null);
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
                detail ? historyService.list(EntityType.CONTRACT, contract.getId()) : null);
    }
}
