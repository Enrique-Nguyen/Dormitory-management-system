package vn.edu.tlu.dms.billing.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.api.UtilityRateDtos;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.SqlPage;

import java.time.LocalDate;

/** CRUD giá tương lai chưa dùng; giá đã có hiệu lực phải thêm phiên bản mới để giữ lịch sử. */
@Service
@Transactional
public class UtilityRateCrudService {

    private final JdbcTemplate jdbc;

    private final SqlPage queries;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final AuditService audit;

    public UtilityRateCrudService(
            JdbcTemplate jdbc,
            SqlPage queries,
            CurrentUser current,
            ScopeGuard guard,
            AuditService audit) {
        this.jdbc = jdbc;
        this.queries = queries;
        this.current = current;
        this.guard = guard;
        this.audit = audit;
    }

    private Object requireUnusedRate(Long id) {
        guard.requireRole("STAFF");
        var row = queries.one("SELECT * FROM utility_rate WHERE id=? FOR UPDATE", id);
        if (!current.get().id().equals(row.get("createdBy"))) {
            throw BusinessException.notFound();
        }
        if (!((LocalDate) row.get("effectiveFrom")).isAfter(LocalDate.now())) {
            throw BusinessException.conflict("RATE_ALREADY_EFFECTIVE");
        }
        return row;
    }

    public Object updateRate(Long id, UtilityRateDtos.Create input) {
        var before = requireUnusedRate(id);
        if (!input.effectiveFrom().isAfter(LocalDate.now())) {
            throw BusinessException.conflict("RATE_ALREADY_EFFECTIVE");
        }
        jdbc.update(
                "UPDATE utility_rate SET meter_type=?,unit_price=?,effective_from=? WHERE id=?",
                input.meterType(),
                input.unitPrice(),
                input.effectiveFrom(),
                id);
        audit.record(current.get().id(), "RATE_UPDATE", "UTILITY_RATE", id, before, input, null);
        return queries.one(
                "SELECT id,meter_type,unit_price,effective_from FROM utility_rate WHERE id=?", id);
    }

    public void deleteRate(Long id) {
        var before = requireUnusedRate(id);
        jdbc.update("DELETE FROM utility_rate WHERE id=?", id);
        audit.record(current.get().id(), "RATE_DELETE", "UTILITY_RATE", id, before, null, null);
    }
}
