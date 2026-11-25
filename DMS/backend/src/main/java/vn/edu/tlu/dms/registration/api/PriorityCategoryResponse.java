package vn.edu.tlu.dms.registration.api;

/** Diện ưu tiên; trọng số ẩn với sinh viên và KTV. Mô tả và giấy tờ minh chứng hiển thị khi tự khai. */
public record PriorityCategoryResponse(
        Long id,
        String code,
        String name,
        Integer weight,
        Boolean active,
        String description,
        String evidenceRequired,
        Boolean evidenceMandatory,
        String legalBasis,
        Short sortOrder,
        String autoRule,
        String note) {}
