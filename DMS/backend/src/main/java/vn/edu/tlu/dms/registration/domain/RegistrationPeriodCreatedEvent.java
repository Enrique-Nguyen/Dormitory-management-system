package vn.edu.tlu.dms.registration.domain;

import java.util.List;

/** Sự kiện chỉ chứa ID/giá trị; notification xử lý sau commit, không gọi ngược repository. */
public record RegistrationPeriodCreatedEvent(
        Long periodId, String name, String kind, List<Long> unitIds, Long actorId) {}
