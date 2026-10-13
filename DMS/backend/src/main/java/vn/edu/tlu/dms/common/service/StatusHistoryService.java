package vn.edu.tlu.dms.common.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.domain.StatusHistory;
import vn.edu.tlu.dms.common.repository.StatusHistoryRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class StatusHistoryService {

    private final StatusHistoryRepository repository;

    public StatusHistoryService(StatusHistoryRepository repository) {
        this.repository = repository;
    }

    public void record(String type, Long id, String from, String to, Long actorId, String reason) {
        StatusHistory row = new StatusHistory();
        row.setEntityType(type);
        row.setEntityId(id);
        row.setFromStatus(from);
        row.setToStatus(to);
        row.setActorId(actorId);
        row.setReason(reason);
        row.setCreatedAt(OffsetDateTime.now());
        repository.save(row);
    }

    public record Entry(
            String fromStatus, String toStatus, Long actorId, String reason, OffsetDateTime at) {}

    @Transactional(readOnly = true)
    public List<Entry> list(String type, Long id) {
        Specification<StatusHistory> spec =
                (r, q, b) ->
                        b.and(b.equal(r.get("entityType"), type), b.equal(r.get("entityId"), id));
        return repository.findAll(spec, Sort.by("createdAt", "id")).stream()
                .map(
                        row ->
                                new Entry(
                                        row.getFromStatus(),
                                        row.getToStatus(),
                                        row.getActorId(),
                                        row.getReason(),
                                        row.getCreatedAt()))
                .toList();
    }
}
