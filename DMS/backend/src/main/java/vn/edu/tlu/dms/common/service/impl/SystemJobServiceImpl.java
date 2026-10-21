package vn.edu.tlu.dms.common.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.SystemJobService;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class SystemJobServiceImpl implements SystemJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemJobServiceImpl.class);
    private final AuditService audit;
    private final TransactionTemplate transaction;

    public SystemJobServiceImpl(AuditService audit, PlatformTransactionManager manager) {
        this.audit = audit;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public RunResult run(
            String name, Supplier<List<Long>> candidates, Function<Long, Change> processOne) {
        var previousContext = SecurityContextHolder.getContext();
        var previousRequestId = MDC.get("requestId");
        int processed = 0;
        int skipped = 0;
        int failed = 0;
        SecurityContextHolder.clearContext();
        try {
            List<Long> ids;
            try {
                ids = candidates.get();
            } catch (RuntimeException error) {
                LOGGER.error(
                        "SYSTEM job {} cannot select candidates ({})",
                        name,
                        error.getClass().getSimpleName());
                return new RunResult(0, 0, 1);
            }
            for (Long id : ids) {
                MDC.put("requestId", UUID.randomUUID().toString());
                try {
                    Boolean changed =
                            transaction.execute(
                                    status -> {
                                        Change change = processOne.apply(id);
                                        if (change == null) {
                                            return false;
                                        }
                                        audit.record(
                                                null,
                                                change.action(),
                                                change.entityType(),
                                                change.entityId(),
                                                change.before(),
                                                change.after(),
                                                change.reason());
                                        return true;
                                    });
                    if (Boolean.TRUE.equals(changed)) {
                        processed++;
                    } else {
                        skipped++;
                    }
                } catch (RuntimeException error) {
                    failed++;
                    // Không đưa dữ liệu/secret trong exception message vào log job.
                    LOGGER.error(
                            "SYSTEM job {} failed for id {} request {} ({})",
                            name,
                            id,
                            MDC.get("requestId"),
                            error.getClass().getSimpleName());
                }
            }
            return new RunResult(processed, skipped, failed);
        } finally {
            SecurityContextHolder.setContext(previousContext);
            if (previousRequestId == null) {
                MDC.remove("requestId");
            } else {
                MDC.put("requestId", previousRequestId);
            }
        }
    }
}
