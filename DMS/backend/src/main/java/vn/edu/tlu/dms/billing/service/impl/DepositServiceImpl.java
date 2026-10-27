package vn.edu.tlu.dms.billing.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.api.DepositDtos;
import vn.edu.tlu.dms.billing.domain.DepositTransaction;
import vn.edu.tlu.dms.billing.repository.DepositTransactionRepository;
import vn.edu.tlu.dms.billing.service.DepositService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.DepositType;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.SqlPage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Cài đặt dịch vụ sổ cọc và các giao dịch tiền cọc theo quy tắc nghiệp vụ BR-10. Khóa hợp đồng khi
 * thay đổi tiền cọc; số dư cọc tính động từ sổ cái qua view v_deposit_balance.
 */
@Service
public class DepositServiceImpl implements DepositService {

    private static final String BALANCE_SELECT =
            """
            SELECT c.id AS id, c.id AS contract_id, c.student_id, c.status, c.deposit_required, ap.preferred_unit_id,
                   COALESCE(v.balance, (SELECT COALESCE(SUM(t.amount),0)
                       FROM deposit_transaction t WHERE t.contract_id=c.id)) AS balance,
                   COALESCE(v.topup_needed,0) AS topup_needed, v.oldest_unpaid_due,
                   COALESCE(v.topup_overdue,false) AS topup_overdue,
                   COALESCE(v.deposit_debt,0) AS deposit_debt,
                   c.code AS contract_code, u.full_name AS student_name, sp.student_code
            FROM dormitory_contract c
            JOIN room_application ap ON ap.id=c.application_id
            JOIN app_user u ON u.id=c.student_id
            LEFT JOIN student_profile sp ON sp.user_id=c.student_id
            LEFT JOIN v_deposit_balance v ON v.contract_id=c.id
            """;

    private static final Set<String> BALANCE_SORT_COLUMNS =
            Set.of(
                    "contractId",
                    "contractCode",
                    "studentName",
                    "studentCode",
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

    /** Thu đúng mức cọc của hợp đồng PENDING hoặc ACTIVE sau khi khóa và kiểm tra phạm vi. */
    @Override
    @Transactional
    public DepositDtos.Transaction collectInitial(Long contractId, Long amount, String note) {
        scopeGuard.requirePermission(Permission.DEPOSIT_CREATE);
        scopeGuard.requireRole(Role.STAFF, Role.ADMIN);

        if (amount == null || amount <= 0) {
            throw BusinessException.invalid(
                    ApiField.AMOUNT, ValidationMessage.DEPOSIT_AMOUNT_POSITIVE);
        }

        lockContract(contractId);
        checkContractWriteScope(contractId);

        var contract = getBalance(contractId);
        if (!Set.of(ContractStatus.PENDING, ContractStatus.ACTIVE).contains(contract.status())
                || !amount.equals(contract.depositRequired())) {
            throw BusinessException.conflict(ErrorCode.DEPOSIT_INVALID);
        }

        // Kiểm tra không thu cọc trùng lặp trên cùng hợp đồng
        if (transactionRepository
                .findByContractIdAndType(contractId, DepositType.DEPOSIT)
                .isPresent()) {
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

        tx = transactionRepository.saveAndFlush(tx);

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

    /** Ghi khoản trừ cọc gắn ticket, giữ nguyên lỗi ràng buộc để ánh xạ chính xác. */
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
            throw BusinessException.invalid(
                    ApiField.AMOUNT, ValidationMessage.DEDUCTION_AMOUNT_POSITIVE);
        }
        if (dueDate == null) {
            throw BusinessException.invalid(
                    ApiField.DUE_DATE, ValidationMessage.TOPUP_DUE_DATE_REQUIRED);
        }
        if (maintenanceRequestId == null) {
            throw BusinessException.invalid(
                    ApiField.MAINTENANCE_REQUEST_ID,
                    ValidationMessage.MAINTENANCE_REQUEST_REQUIRED);
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

        tx = transactionRepository.saveAndFlush(tx);

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

    /** Ghi nhận nộp bù cọc cho hợp đồng đang ACTIVE trong đơn vị được ghi. */
    @Override
    @Transactional
    public DepositDtos.Transaction topup(Long contractId, Long amount, String note) {
        scopeGuard.requirePermission(Permission.DEPOSIT_CREATE);
        scopeGuard.requireRole(Role.STAFF, Role.ADMIN);

        if (amount == null || amount <= 0) {
            throw BusinessException.invalid(
                    ApiField.AMOUNT, ValidationMessage.TOPUP_AMOUNT_POSITIVE);
        }

        lockContract(contractId);
        checkContractWriteScope(contractId);

        var balance = getBalance(contractId);
        if (!ContractStatus.ACTIVE.equals(balance.status())) {
            throw BusinessException.conflict(ErrorCode.CONTRACT_NOT_ACTIVE);
        }
        long topupNeeded = balance.topupNeeded() == null ? 0L : balance.topupNeeded();
        if (topupNeeded <= 0) {
            throw BusinessException.conflict(ErrorCode.NO_TOPUP_REQUIRED);
        }
        if (amount > topupNeeded) {
            throw BusinessException.conflict(ErrorCode.TOPUP_EXCEEDS_REQUIRED);
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

    /** Hoàn cọc cho hợp đồng đã kết thúc, kiểm tra số dư và không hoàn hai lần. */
    @Override
    @Transactional
    public DepositDtos.Transaction refund(Long contractId, Long amount, String note) {
        scopeGuard.requirePermission(Permission.DEPOSIT_CREATE);
        scopeGuard.requireRole(Role.STAFF, Role.ADMIN);

        if (amount == null || amount <= 0) {
            throw BusinessException.invalid(
                    ApiField.AMOUNT, ValidationMessage.REFUND_AMOUNT_POSITIVE);
        }

        lockContract(contractId);
        checkContractWriteScope(contractId);

        if (transactionRepository
                .findByContractIdAndType(contractId, DepositType.REFUND)
                .isPresent()) {
            throw BusinessException.conflict(ErrorCode.DUPLICATE_REFUND);
        }

        var balance = getBalance(contractId);
        if (!Set.of(ContractStatus.EXPIRED, ContractStatus.TERMINATED).contains(balance.status())) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
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

        tx = transactionRepository.saveAndFlush(tx);

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

    /** Đọc số dư và hành động được phép của hợp đồng, bao gồm cả PENDING. */
    @Override
    @Transactional(readOnly = true)
    public DepositDtos.Balance getBalance(Long contractId) {
        scopeGuard.requirePermission(Permission.DEPOSIT_READ);
        scopeGuard.requireRole(Role.STAFF, Role.STUDENT);

        checkContractReadScope(contractId);

        return jdbc.query(BALANCE_SELECT + " WHERE c.id=?", this::mapBalance, contractId).stream()
                .findFirst()
                .orElseThrow(BusinessException::notFound);
    }

    /**
     * Đọc sổ cọc qua cùng luồng kiểm tra quyền/phạm vi của getBalance và gói gọn vào record của tầng
     * Service, bao gồm khoản cần bù và trạng thái bù quá hạn từ v_deposit_balance.
     */
    @Override
    @Transactional(readOnly = true)
    public BalanceView getBalanceView(Long contractId) {
        DepositDtos.Balance balance = getBalance(contractId);
        return new BalanceView(
                balance.contractId(),
                balance.balance(),
                balance.depositRequired(),
                balance.status(),
                balance.topupNeeded(),
                Boolean.TRUE.equals(balance.topupOverdue()));
    }

    /** Đọc lịch sử sổ cọc theo thời gian sau khi kiểm tra hợp đồng và phạm vi. */
    @Override
    @Transactional(readOnly = true)
    public List<DepositDtos.Transaction> listTransactions(Long contractId) {
        scopeGuard.requirePermission(Permission.DEPOSIT_READ);
        scopeGuard.requireRole(Role.STAFF, Role.STUDENT);

        checkContractReadScope(contractId);

        return transactionRepository.findByContractIdOrderByCreatedAtAscIdAsc(contractId).stream()
                .map(this::toTransactionDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositEntry> listEntries(Long contractId) {
        scopeGuard.requirePermission(Permission.DEPOSIT_READ);
        scopeGuard.requireRole(Role.STAFF, Role.STUDENT);
        checkContractReadScope(contractId);
        return transactionRepository.findByContractIdOrderByCreatedAtAscIdAsc(contractId).stream()
                .map(
                        transaction ->
                                new DepositEntry(
                                        transaction.getId(),
                                        transaction.getContractId(),
                                        transaction.getType(),
                                        transaction.getAmount(),
                                        transaction.getMaintenanceRequestId(),
                                        transaction.getDueDate(),
                                        transaction.getConfirmedBy(),
                                        transaction.getCreatedAt(),
                                        transaction.getNote()))
                .toList();
    }

    /** Tìm sổ cọc theo mã hợp đồng hoặc tên sinh viên, giữ đúng phạm vi trước phân trang và đếm. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepositDtos.Balance> searchBalancesByPage(
            SearchRequest input, boolean mine) {
        scopeGuard.requirePermission(Permission.DEPOSIT_READ);
        scopeGuard.requireRole(Role.STAFF, Role.STUDENT);

        var args = new ArrayList<Object>();
        String filter = contractScope(args, mine);
        args.add(LikePattern.contains(input.q()));

        String sql =
                BALANCE_SELECT
                        + " WHERE "
                        + filter
                        + " AND (u.full_name ILIKE ? OR c.code ILIKE ?)";
        args.add(LikePattern.contains(input.q()));
        var rawPage = sqlPage.search(sql, args, input, BALANCE_SORT_COLUMNS, "contractId,desc");
        List<DepositDtos.Balance> items =
                rawPage.content().stream()
                        .map(
                                row ->
                                        new DepositDtos.Balance(
                                                ((Number) row.get("contractId")).longValue(),
                                                ((Number) row.get("studentId")).longValue(),
                                                (String) row.get("status"),
                                                ((Number) row.get("depositRequired")).longValue(),
                                                ((Number) row.get("balance")).longValue(),
                                                ((Number) row.get("topupNeeded")).longValue(),
                                                row.get("oldestUnpaidDue") == null
                                                        ? null
                                                        : (LocalDate) row.get("oldestUnpaidDue"),
                                                (Boolean) row.get("topupOverdue"),
                                                ((Number) row.get("depositDebt")).longValue(),
                                                (String) row.get("contractCode"),
                                                (String) row.get("studentName"),
                                                (String) row.get("studentCode"),
                                                allowedDepositActions(
                                                        ((Number) row.get("contractId"))
                                                                .longValue(),
                                                        (String) row.get("status"),
                                                        ((Number) row.get("balance")).longValue(),
                                                        ((Number) row.get("topupNeeded"))
                                                                .longValue(),
                                                        ((Number) row.get("preferredUnitId"))
                                                                .longValue())))
                        .toList();

        return new PageResponse<>(
                items,
                rawPage.page(),
                rawPage.size(),
                rawPage.totalElements(),
                rawPage.totalPages());
    }

    private DepositDtos.Balance mapBalance(ResultSet rs, int rowNum) throws SQLException {
        return new DepositDtos.Balance(
                rs.getLong("contract_id"),
                rs.getLong("student_id"),
                rs.getString("status"),
                rs.getLong("deposit_required"),
                rs.getLong("balance"),
                rs.getLong("topup_needed"),
                rs.getObject("oldest_unpaid_due", LocalDate.class),
                rs.getBoolean("topup_overdue"),
                rs.getLong("deposit_debt"),
                rs.getString("contract_code"),
                rs.getString("student_name"),
                rs.getString("student_code"),
                allowedDepositActions(
                        rs.getLong("contract_id"),
                        rs.getString("status"),
                        rs.getLong("balance"),
                        rs.getLong("topup_needed"),
                        rs.getLong("preferred_unit_id")));
    }

    /** Hành động giao diện dùng cùng trạng thái, quyền động và phạm vi ghi với API. */
    private List<String> allowedDepositActions(
            Long contractId, String status, long balance, long topupNeeded, Long unitId) {
        var actor = currentUser.get();
        if ((!actor.isAdmin() && (!actor.is(Role.STAFF) || !Objects.equals(actor.unitId(), unitId)))
                || !scopeGuard.hasPermission(Permission.DEPOSIT_CREATE)) {
            return List.of();
        }
        if (ContractStatus.ACTIVE.equals(status) && topupNeeded > 0) {
            return List.of(BusinessAction.DEPOSIT_TOPUP);
        }
        if (Set.of(ContractStatus.EXPIRED, ContractStatus.TERMINATED).contains(status)
                && balance > 0
                && transactionRepository
                        .findByContractIdAndType(contractId, DepositType.REFUND)
                        .isEmpty()) {
            return List.of(BusinessAction.DEPOSIT_REFUND);
        }
        return List.of();
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
        checkContractScope(contractId, false);
    }

    private void checkContractWriteScope(Long contractId) {
        checkContractScope(contractId, true);
    }

    /** Tồn tại và phạm vi được kiểm tra ngay cả khi đọc tổng hợp hoặc đọc lịch sử rỗng. */
    private void checkContractScope(Long contractId, boolean writing) {
        var rows =
                jdbc.queryForList(
                        """
                        SELECT c.student_id, ap.preferred_unit_id FROM dormitory_contract c
                        JOIN room_application ap ON ap.id=c.application_id WHERE c.id=?
                        """,
                        contractId);
        if (rows.isEmpty()) {
            throw BusinessException.notFound();
        }
        var actor = currentUser.get();
        var row = rows.get(0);
        if (actor.isAdmin()) {
            return;
        }
        if (actor.is(Role.STUDENT)) {
            if (writing || !actor.id().equals(((Number) row.get("student_id")).longValue())) {
                throw BusinessException.notFound();
            }
        } else if ((writing || !actor.canReadAllUnits())
                && !Objects.equals(
                        actor.unitId(), ((Number) row.get("preferred_unit_id")).longValue())) {
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
