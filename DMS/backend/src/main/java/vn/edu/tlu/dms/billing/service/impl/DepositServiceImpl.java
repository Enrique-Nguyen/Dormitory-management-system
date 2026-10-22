package vn.edu.tlu.dms.billing.service.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.api.DepositDtos;
import vn.edu.tlu.dms.billing.domain.DepositTransaction;
import vn.edu.tlu.dms.billing.repository.DepositTransactionRepository;
import vn.edu.tlu.dms.billing.service.DepositService;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.DepositType;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.SqlPage;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cài đặt dịch vụ sổ cọc và các giao dịch tiền cọc theo quy tắc nghiệp vụ BR-10.
 * Khóa hợp đồng khi thay đổi tiền cọc; số dư cọc tính động từ sổ cái qua view v_deposit_balance.
 */
@Service
public class DepositServiceImpl implements DepositService {

    private static final Set<String> BALANCE_SORT_COLUMNS =
            Set.of(
                    "contractId",
                    "studentId",
                    "status",
                    "depositRequired",
                    "balance",
                    "topupNeeded",
                    "oldestUnpaidDue",
                    "topupOverdue",
                    "depositDebt");

    private final DepositTransactionRepository transactionRepository;
    private final JdbcTemplate jdbc;
    private final SqlPage sqlPage;
    private final ScopeGuard scopeGuard;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public DepositServiceImpl(
            DepositTransactionRepository transactionRepository,
            JdbcTemplate jdbc,
            SqlPage sqlPage,
            ScopeGuard scopeGuard,
            CurrentUser currentUser,
            AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.jdbc = jdbc;
        this.sqlPage = sqlPage;
        this.scopeGuard = scopeGuard;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public DepositDtos.Transaction collectInitial(Long contractId, Long amount, String note) {
        scopeGuard.requirePermission(Permission.DEPOSIT_CREATE);
        scopeGuard.requireRole(Role.STAFF, Role.ADMIN);

        if (amount == null || amount <= 0) {
            throw BusinessException.invalid("amount", "Số tiền cọc phải lớn hơn 0");
        }

        lockContract(contractId);
        checkContractWriteScope(contractId);

        // Kiểm tra không thu cọc trùng lặp trên cùng hợp đồng
        if (transactionRepository.findByContractIdAndType(contractId, DepositType.DEPOSIT).isPresent()) {
            throw BusinessException.conflict(ErrorCode.DUPLICATE_DEPOSIT);
        }

        Long actorId = currentUser.get().id();
        OffsetDateTime now = OffsetDateTime.now();

        var tx = new DepositTransaction();
        tx.setContractId(contractId);
        tx.setType(DepositType.DEPOSIT);
        tx.setAmount(amount);
        tx.setConfirmedBy(actorId);
        tx.setCreatedAt(now);
        tx.setNote(note == null ? null : note.trim());

        try {
            tx = transactionRepository.saveAndFlush(tx);
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.conflict(ErrorCode.DUPLICATE_DEPOSIT);
        }

        auditService.record(
                actorId,
                AuditAction.DEPOSIT_COLLECT,
                EntityType.DEPOSIT_TRANSACTION,
                tx.getId(),
                null,
                Map.of("contractId", contractId, "type", DepositType.DEPOSIT, "amount", amount),
                note);

        return toTransactionDto(tx);
    }

    @Override
    @Transactional
    public DepositDtos.Transaction deduct(
            Long contractId,
            Long maintenanceRequestId,
            Long amount,
            LocalDate dueDate,
            String note) {
        scopeGuard.requirePermission(Permission.DEPOSIT_CREATE);
        scopeGuard.requireRole(Role.STAFF, Role.ADMIN);

        if (amount == null || amount <= 0) {
            throw BusinessException.invalid("amount", "Số tiền trừ cọc phải lớn hơn 0");
        }
        if (dueDate == null) {
            throw BusinessException.invalid("dueDate", "Hạn nộp bù cọc không được để trống");
        }
        if (maintenanceRequestId == null) {
            throw BusinessException.invalid("maintenanceRequestId", "Mã ticket sửa chữa không được để trống");
        }

        lockContract(contractId);
        checkContractWriteScope(contractId);

        // Một ticket chỉ trừ cọc một lần cho mỗi hợp đồng
        if (transactionRepository
                .findByContractIdAndMaintenanceRequestIdAndType(
                        contractId, maintenanceRequestId, DepositType.DEDUCTION)
                .isPresent()) {
            throw BusinessException.conflict(ErrorCode.DUPLICATE_DEDUCTION);
        }

        Long actorId = currentUser.get().id();
        OffsetDateTime now = OffsetDateTime.now();

        var tx = new DepositTransaction();
        tx.setContractId(contractId);
        tx.setType(DepositType.DEDUCTION);
        tx.setAmount(-amount); // Số tiền âm theo ràng buộc CHECK CSDL
        tx.setMaintenanceRequestId(maintenanceRequestId);
        tx.setDueDate(dueDate);
        tx.setConfirmedBy(actorId);
        tx.setCreatedAt(now);
        tx.setNote(note == null ? null : note.trim());

        try {
            tx = transactionRepository.saveAndFlush(tx);
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.conflict(ErrorCode.DUPLICATE_DEDUCTION);
        }

        auditService.record(
                actorId,
                AuditAction.DEPOSIT_DEDUCT,
                EntityType.DEPOSIT_TRANSACTION,
                tx.getId(),
                null,
                Map.of(
                        "contractId", contractId,
                        "maintenanceRequestId", maintenanceRequestId,
                        "type", DepositType.DEDUCTION,
                        "amount", -amount),
                note);

        return toTransactionDto(tx);
    }

    @Override
    @Transactional
    public DepositDtos.Transaction topup(Long contractId, Long amount, String note) {
        scopeGuard.requirePermission(Permission.DEPOSIT_CREATE);
        scopeGuard.requireRole(Role.STAFF, Role.ADMIN);

        if (amount == null || amount <= 0) {
            throw BusinessException.invalid("amount", "Số tiền bù cọc phải lớn hơn 0");
        }

        lockContract(contractId);
        checkContractWriteScope(contractId);

        var balance = getBalance(contractId);
        if (!"ACTIVE".equals(balance.status())) {
            throw BusinessException.conflict(ErrorCode.CONTRACT_NOT_ACTIVE);
        }

        Long actorId = currentUser.get().id();
        OffsetDateTime now = OffsetDateTime.now();

        var tx = new DepositTransaction();
        tx.setContractId(contractId);
        tx.setType(DepositType.TOPUP);
        tx.setAmount(amount); // Số tiền dương
        tx.setConfirmedBy(actorId);
        tx.setCreatedAt(now);
        tx.setNote(note == null ? null : note.trim());

        tx = transactionRepository.saveAndFlush(tx);

        auditService.record(
                actorId,
                AuditAction.DEPOSIT_TOPUP,
                EntityType.DEPOSIT_TRANSACTION,
                tx.getId(),
                null,
                Map.of("contractId", contractId, "type", DepositType.TOPUP, "amount", amount),
                note);

        return toTransactionDto(tx);
    }

    @Override
    @Transactional
    public DepositDtos.Transaction refund(Long contractId, Long amount, String note) {
        scopeGuard.requirePermission(Permission.DEPOSIT_CREATE);
        scopeGuard.requireRole(Role.STAFF, Role.ADMIN);

        if (amount == null || amount <= 0) {
            throw BusinessException.invalid("amount", "Số tiền hoàn cọc phải lớn hơn 0");
        }

        lockContract(contractId);
        checkContractWriteScope(contractId);

        if (transactionRepository.findByContractIdAndType(contractId, DepositType.REFUND).isPresent()) {
            throw BusinessException.conflict(ErrorCode.DUPLICATE_REFUND);
        }

        var balance = getBalance(contractId);
        if (amount > balance.balance()) {
            throw BusinessException.conflict(ErrorCode.REFUND_EXCEEDS_BALANCE);
        }

        Long actorId = currentUser.get().id();
        OffsetDateTime now = OffsetDateTime.now();

        var tx = new DepositTransaction();
        tx.setContractId(contractId);
        tx.setType(DepositType.REFUND);
        tx.setAmount(-amount); // Số tiền âm theo ràng buộc CHECK CSDL
        tx.setConfirmedBy(actorId);
        tx.setCreatedAt(now);
        tx.setNote(note == null ? null : note.trim());

        try {
            tx = transactionRepository.saveAndFlush(tx);
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.conflict(ErrorCode.DUPLICATE_REFUND);
        }

        auditService.record(
                actorId,
                AuditAction.DEPOSIT_REFUND,
                EntityType.DEPOSIT_TRANSACTION,
                tx.getId(),
                null,
                Map.of("contractId", contractId, "type", DepositType.REFUND, "amount", -amount),
                note);

        return toTransactionDto(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public DepositDtos.Balance getBalance(Long contractId) {
        scopeGuard.requirePermission(Permission.DEPOSIT_READ);
        scopeGuard.requireRole(Role.STAFF, Role.STUDENT);

        checkContractReadScope(contractId);

        String sql =
                """
                SELECT contract_id, student_id, status, deposit_required, balance,
                       topup_needed, oldest_unpaid_due, topup_overdue, deposit_debt
                FROM v_deposit_balance
                WHERE contract_id = ?
                """;

        List<DepositDtos.Balance> results =
                jdbc.query(
                        sql,
                        (rs, rowNum) ->
                                new DepositDtos.Balance(
                                        rs.getLong("contract_id"),
                                        rs.getLong("student_id"),
                                        rs.getString("status"),
                                        rs.getLong("deposit_required"),
                                        rs.getLong("balance"),
                                        rs.getLong("topup_needed"),
                                        rs.getDate("oldest_unpaid_due") == null
                                                ? null
                                                : rs.getDate("oldest_unpaid_due").toLocalDate(),
                                        rs.getBoolean("topup_overdue"),
                                        rs.getLong("deposit_debt")),
                        contractId);

        if (results.isEmpty()) {
            // Trường hợp hợp đồng PENDING chưa phát sinh giao dịch, đọc trực tiếp từ dormitory_contract
            return jdbc.query(
                            """
                            SELECT c.id AS contract_id, c.student_id, c.status, c.deposit_required
                            FROM dormitory_contract c
                            WHERE c.id = ?
                            """,
                            (rs, rowNum) ->
                                    new DepositDtos.Balance(
                                            rs.getLong("contract_id"),
                                            rs.getLong("student_id"),
                                            rs.getString("status"),
                                            rs.getLong("deposit_required"),
                                            0L,
                                            0L,
                                            null,
                                            false,
                                            0L),
                            contractId)
                    .stream()
                    .findFirst()
                    .orElseThrow(BusinessException::notFound);
        }

        return results.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositDtos.Transaction> listTransactions(Long contractId) {
        scopeGuard.requirePermission(Permission.DEPOSIT_READ);
        scopeGuard.requireRole(Role.STAFF, Role.STUDENT);

        checkContractReadScope(contractId);

        return transactionRepository
                .findByContractIdOrderByCreatedAtAscIdAsc(contractId)
                .stream()
                .map(this::toTransactionDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepositDtos.Balance> searchBalancesByPage(SearchRequest input, boolean mine) {
        scopeGuard.requirePermission(Permission.DEPOSIT_READ);
        scopeGuard.requireRole(Role.STAFF, Role.STUDENT);

        var args = new ArrayList<Object>();
        String filter = contractScope(args, mine);
        args.add(LikePattern.contains(input.q()));

        String sql =
                """
                SELECT v.contract_id, v.student_id, v.status, v.deposit_required,
                       v.balance, v.topup_needed, v.oldest_unpaid_due, v.topup_overdue, v.deposit_debt
                FROM v_deposit_balance v
                JOIN dormitory_contract c ON c.id = v.contract_id
                JOIN room_application ap ON ap.id = c.application_id
                JOIN app_user u ON u.id = c.student_id
                WHERE %s AND (u.full_name ILIKE ? OR c.code ILIKE ?)
                """.formatted(filter);
        args.add(LikePattern.contains(input.q()));

        var rawPage = sqlPage.search(sql, args, input, BALANCE_SORT_COLUMNS, "contractId,desc");

        List<DepositDtos.Balance> items =
                rawPage.content().stream()
                        .map(
                                row ->
                                        new DepositDtos.Balance(
                                                ((Number) row.get("contract_id")).longValue(),
                                                ((Number) row.get("student_id")).longValue(),
                                                (String) row.get("status"),
                                                ((Number) row.get("deposit_required")).longValue(),
                                                ((Number) row.get("balance")).longValue(),
                                                ((Number) row.get("topup_needed")).longValue(),
                                                row.get("oldest_unpaid_due") == null
                                                        ? null
                                                        : ((java.sql.Date) row.get("oldest_unpaid_due"))
                                                                .toLocalDate(),
                                                (Boolean) row.get("topup_overdue"),
                                                ((Number) row.get("deposit_debt")).longValue()))
                        .toList();

        return new PageResponse<>(
                items,
                rawPage.page(),
                rawPage.size(),
                rawPage.totalElements(),
                rawPage.totalPages());
    }

    private void lockContract(Long contractId) {
        List<Long> ids =
                jdbc.query(
                        "SELECT id FROM dormitory_contract WHERE id = ? FOR UPDATE",
                        (rs, rowNum) -> rs.getLong(1),
                        contractId);
        if (ids.isEmpty()) {
            throw BusinessException.notFound();
        }
    }

    private void checkContractReadScope(Long contractId) {
        var actor = currentUser.get();
        if (actor.isAdmin()) {
            return;
        }
        if (actor.is(Role.STUDENT)) {
            Boolean match =
                    jdbc.queryForObject(
                            "SELECT EXISTS(SELECT 1 FROM dormitory_contract WHERE id = ? AND student_id = ?)",
                            Boolean.class,
                            contractId,
                            actor.id());
            if (!Boolean.TRUE.equals(match)) {
                throw BusinessException.notFound();
            }
            return;
        }
        if (actor.canReadAllUnits()) {
            return;
        }
        Boolean inUnit =
                jdbc.queryForObject(
                        """
                        SELECT EXISTS(
                            SELECT 1 FROM dormitory_contract c
                            JOIN room_application ap ON ap.id = c.application_id
                            WHERE c.id = ? AND ap.preferred_unit_id = ?
                        )
                        """,
                        Boolean.class,
                        contractId,
                        actor.unitId());
        if (!Boolean.TRUE.equals(inUnit)) {
            throw BusinessException.notFound();
        }
    }

    private void checkContractWriteScope(Long contractId) {
        var actor = currentUser.get();
        if (actor.isAdmin()) {
            return;
        }
        Boolean inUnit =
                jdbc.queryForObject(
                        """
                        SELECT EXISTS(
                            SELECT 1 FROM dormitory_contract c
                            JOIN room_application ap ON ap.id = c.application_id
                            WHERE c.id = ? AND ap.preferred_unit_id = ?
                        )
                        """,
                        Boolean.class,
                        contractId,
                        actor.unitId());
        if (!Boolean.TRUE.equals(inUnit)) {
            throw BusinessException.notFound();
        }
    }

    private String contractScope(List<Object> args, boolean mine) {
        var actor = currentUser.get();
        if (mine || actor.is(Role.STUDENT)) {
            args.add(actor.id());
            return "c.student_id = ?";
        }
        if (actor.canReadAllUnits()) {
            return "TRUE";
        }
        args.add(actor.unitId());
        return "ap.preferred_unit_id = ?";
    }

    private DepositDtos.Transaction toTransactionDto(DepositTransaction tx) {
        return new DepositDtos.Transaction(
                tx.getId(),
                tx.getContractId(),
                tx.getType(),
                tx.getAmount(),
                tx.getMaintenanceRequestId(),
                tx.getDueDate(),
                tx.getConfirmedBy(),
                tx.getCreatedAt(),
                tx.getNote());
    }
}
