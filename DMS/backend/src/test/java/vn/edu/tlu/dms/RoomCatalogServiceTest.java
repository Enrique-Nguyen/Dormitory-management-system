package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.edu.tlu.dms.account.service.CatalogService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.Catalog;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceSeverity;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos;
import vn.edu.tlu.dms.room.api.RoomTypeRequest;
import vn.edu.tlu.dms.room.api.SemesterRequest;
import vn.edu.tlu.dms.room.domain.RoomType;
import vn.edu.tlu.dms.room.domain.Semester;
import vn.edu.tlu.dms.room.repository.RoomRepository;
import vn.edu.tlu.dms.room.repository.RoomTypeRepository;
import vn.edu.tlu.dms.room.repository.SemesterRepository;
import vn.edu.tlu.dms.room.service.impl.RoomTypeServiceImpl;
import vn.edu.tlu.dms.room.service.impl.SemesterServiceImpl;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Mockito dùng null tạm cho matcher; ngoại lệ chỉ áp dụng ở các hàm giả lập bên dưới.

class RoomCatalogServiceTest {

    private final RoomTypeRepository roomTypes = mock(RoomTypeRepository.class);
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final SemesterRepository semesters = mock(SemesterRepository.class);
    private final CatalogService catalogs = mock(CatalogService.class);
    private final ScopeGuard guard = mock(ScopeGuard.class);
    private final CurrentUser current = mock(CurrentUser.class);
    private final AuditService audit = mock(AuditService.class);
    private final RoomTypeServiceImpl roomTypeService =
            new RoomTypeServiceImpl(roomTypes, rooms, guard, current, audit, catalogs);
    private final SemesterServiceImpl semesterService =
            new SemesterServiceImpl(semesters, guard, current, audit, catalogs);

    @SuppressWarnings("null")
    @BeforeEach
    void setUp() {
        when(current.get()).thenReturn(new Actor(10L, Role.ADMIN, null, false, false));
        when(roomTypes.saveAndFlush(any(RoomType.class))).thenAnswer(invocation -> {
            RoomType entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(1L);
            }
            return entity;
        });
        when(semesters.saveAndFlush(any(Semester.class))).thenAnswer(invocation -> {
            Semester entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(1L);
            }
            return entity;
        });
        when(catalogs.getById(Catalog.AMENITIES, 1L))
                .thenReturn(Map.of(ApiField.NAME, "Legacy, complete description", ApiField.ACTIVE, false));
        when(catalogs.getById(Catalog.AMENITIES, 2L))
                .thenReturn(Map.of(ApiField.NAME, "Air conditioning", ApiField.ACTIVE, true));
    }

    @Test
    void createValidatesAndDeduplicatesSelectedAmenitiesAndReturnsDisplayNames() {
        var response = roomTypeService.saveOrUpdate(new RoomTypeRequest(
                "Room", (short) 4, 100L, 50L, null, List.of(2L, 1L, 2L), true));

        verify(guard).requirePermission(Permission.ROOM_TYPE_CREATE);
        verify(catalogs).requireActive(Catalog.AMENITIES, 1L);
        verify(catalogs).requireActive(Catalog.AMENITIES, 2L);
        assertThat(response.amenityIds()).containsExactly(1L, 2L);
        assertThat(response.amenities()).isEqualTo("Legacy, complete description, Air conditioning");
    }

    @Test
    void updateRetainsSelectedInactiveAmenityButValidatesNewSelection() {
        RoomType entity = existingRoomType();
        entity.getAmenityIds().add(1L);

        var response = roomTypeService.saveOrUpdate(roomTypeUpdate(List.of(2L, 1L)));

        verify(guard).requirePermission(Permission.ROOM_TYPE_UPDATE);
        verify(catalogs, never()).requireActive(Catalog.AMENITIES, 1L);
        verify(catalogs).requireActive(Catalog.AMENITIES, 2L);
        assertThat(response.amenityIds()).containsExactly(1L, 2L);
        assertThat(response.priceMain()).isEqualTo(100L);
        assertThat(response.bedCount()).isEqualTo((short) 4);
    }

    @Test
    void nullAmenityListKeepsSelectionWhileEmptyListClearsIt() {
        RoomType entity = existingRoomType();
        entity.getAmenityIds().add(1L);

        assertThat(roomTypeService.saveOrUpdate(roomTypeUpdate(null)).amenityIds()).containsExactly(1L);
        var response = roomTypeService.saveOrUpdate(roomTypeUpdate(List.of()));

        assertThat(response.amenityIds()).isEmpty();
        assertThat(response.amenities()).isEmpty();
        assertThat(entity.getAmenityIds()).isEmpty();
        verify(catalogs, never()).requireActive(any(), any());
    }

    @SuppressWarnings("null")
    @Test
    void rejectedNewAmenityDoesNotReplaceExistingLinksOrSave() {
        RoomType entity = existingRoomType();
        entity.getAmenityIds().add(1L);
        doThrow(BusinessException.invalid(ApiField.ID, Catalog.INVALID_VALUE))
                .when(catalogs).requireActive(Catalog.AMENITIES, 3L);

        assertThatThrownBy(() -> roomTypeService.saveOrUpdate(roomTypeUpdate(List.of(2L, 3L))))
                .isInstanceOf(BusinessException.class);

        assertThat(entity.getAmenityIds()).containsExactly(1L);
        verify(roomTypes, never()).saveAndFlush(any());
    }

    @SuppressWarnings("null")
    @Test
    void usedRoomTypeStillCannotChangeBedCount() {
        existingRoomType();
        when(rooms.exists(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<vn.edu.tlu.dms.room.domain.Room>>argThat(specification -> specification != null))).thenReturn(true);

        assertThatThrownBy(() -> roomTypeService.saveOrUpdate(new RoomTypeRequest(
                1L, null, (short) 6, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.RESOURCE_IN_USE);

        verify(roomTypes, never()).saveAndFlush(any());
    }

    @Test
    void semesterCreateAndChangedYearValidateActiveCatalogCode() {
        semesterService.saveOrUpdate(new SemesterRequest("Semester", "2026-2027", SemesterType.HK1,
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 1, 31)));
        verify(catalogs).requireActiveCode(Catalog.ACADEMIC_YEARS, "2026-2027");
        verify(guard).requirePermission(Permission.SEMESTER_CREATE);

        existingSemester();
        var response = semesterService.saveOrUpdate(semesterUpdate("2027-2028"));
        verify(catalogs).requireActiveCode(Catalog.ACADEMIC_YEARS, "2027-2028");
        verify(guard).requirePermission(Permission.SEMESTER_UPDATE);
        assertThat(response.academicYear()).isEqualTo("2027-2028");
    }

    @Test
    void semesterPartialUpdateAndUnchangedInactiveYearDoNotRequireActiveCode() {
        existingSemester();

        assertThat(semesterService.saveOrUpdate(semesterUpdate(null)).academicYear()).isEqualTo("2026-2027");
        assertThat(semesterService.saveOrUpdate(semesterUpdate(" 2026-2027 ")).academicYear())
                .isEqualTo("2026-2027");

        verify(catalogs, never()).requireActiveCode(any(), any());
    }

    @SuppressWarnings("null")
    @Test
    void inactiveChangedAcademicYearIsRejectedBeforeSaving() {
        existingSemester();
        doThrow(BusinessException.invalid(ApiField.CODE, Catalog.INVALID_VALUE))
                .when(catalogs).requireActiveCode(Catalog.ACADEMIC_YEARS, "2027-2028");

        assertThatThrownBy(() -> semesterService.saveOrUpdate(semesterUpdate("2027-2028")))
                .isInstanceOf(BusinessException.class);
        verify(semesters, never()).saveAndFlush(any());
    }

    @Test
    void dtoRejectsNullAndNonpositiveAmenityElementsAndAcceptsDynamicCategoryCode() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (List<Long> ids : List.of(Arrays.asList(1L, null), List.of(0L), List.of(-1L))) {
                assertThat(validator.validate(roomTypeUpdate(ids))).isNotEmpty();
            }
            assertThat(validator.validate(roomTypeUpdate(List.of()))).isEmpty();
            assertThat(validator.validate(new MaintenanceDtos.Save(
                    null, "NETWORK", MaintenanceSeverity.LOW, null, null, null, null))).isEmpty();
            assertThat(validator.validate(new MaintenanceDtos.Save(
                    null, "C".repeat(31), null, null, null, null, null))).isNotEmpty();
        }
    }

    private RoomType existingRoomType() {
        RoomType entity = new RoomType();
        entity.setId(1L);
        entity.setName("Room");
        entity.setBedCount((short) 4);
        entity.setPriceMain(100L);
        entity.setPriceSummer(50L);
        entity.setActive(true);
        when(roomTypes.findById(1L)).thenReturn(Optional.of(entity));
        return entity;
    }

    private Semester existingSemester() {
        Semester entity = new Semester();
        entity.setId(1L);
        entity.setName("Semester");
        entity.setAcademicYear("2026-2027");
        entity.setType(SemesterType.HK1);
        entity.setStartDate(LocalDate.of(2026, 9, 1));
        entity.setEndDate(LocalDate.of(2027, 1, 31));
        when(semesters.findById(1L)).thenReturn(Optional.of(entity));
        return entity;
    }

    private RoomTypeRequest roomTypeUpdate(List<Long> ids) {
        return new RoomTypeRequest(1L, null, null, null, null, null, ids, null);
    }

    private SemesterRequest semesterUpdate(String academicYear) {
        return new SemesterRequest(1L, null, academicYear, null, null, null);
    }
}
