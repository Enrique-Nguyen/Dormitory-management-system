package vn.edu.tlu.dms.room.api;

import java.time.LocalDate;

public record SemesterResponse(
        Long id,
        String name,
        String academicYear,
        String type,
        LocalDate startDate,
        LocalDate endDate) {}
