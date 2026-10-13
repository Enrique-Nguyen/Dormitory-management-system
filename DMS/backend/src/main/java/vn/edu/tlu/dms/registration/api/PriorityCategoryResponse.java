package vn.edu.tlu.dms.registration.api;

public record PriorityCategoryResponse(
        Long id, String code, String name, Integer weight, Boolean active) {}
