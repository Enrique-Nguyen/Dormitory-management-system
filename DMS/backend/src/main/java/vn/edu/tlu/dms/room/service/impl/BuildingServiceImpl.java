package vn.edu.tlu.dms.room.service.impl;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Facility;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Resource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.impl.AbstractCrudServiceImpl;
import vn.edu.tlu.dms.room.api.BuildingRequest;
import vn.edu.tlu.dms.room.api.BuildingResponse;
import vn.edu.tlu.dms.room.domain.Building;
import vn.edu.tlu.dms.room.repository.BuildingRepository;
import vn.edu.tlu.dms.room.service.BuildingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class BuildingServiceImpl
        extends AbstractCrudServiceImpl<Building, BuildingRequest, BuildingResponse>
        implements BuildingService {

    private final ScopeGuard guard;

    private final CurrentUser currentUser;

    private final AccountReferenceService references;

    private final JdbcTemplate jdbc;

    public BuildingServiceImpl(
            BuildingRepository repository,
            ScopeGuard guard,
            CurrentUser currentUser,
            AuditService auditService,
            AccountReferenceService references,
            JdbcTemplate jdbc) {
        super(repository, auditService, currentUser);
        this.guard = guard;
        this.currentUser = currentUser;
        this.references = references;
        this.jdbc = jdbc;
    }

    @Override
    protected void checkRead() {
        guard.requirePermission(Permission.BUILDING_READ);
    }

    @Override
    protected void checkWrite() {
        guard.requireRole(Role.ADMIN);
    }

    @Override
    protected void checkPermission(String action) {
        guard.requirePermission(Resource.BUILDING + DmsConstants.PERMISSION_SEPARATOR + action);
    }

    @Override
    protected Long getInputId(BuildingRequest input) {
        return input.id();
    }

    @Override
    protected Set<String> searchableFields() {
        return Set.of("code", "name");
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of(
                "id", "managementUnitId", "code", "name", "depositAmount", "floorCount",
                "genderPolicy");
    }

    @Override
    protected Specification<Building> searchScope(SearchRequest search) {
        Actor actor = currentUser.get();
        return (root, query, builder) -> {
            var filters = new ArrayList<Predicate>();
            // Cùng giới hạn như getAll: lọc trước cả truy vấn dữ liệu và truy vấn đếm.
            if (actor.is(Role.STAFF) && !actor.canReadAllUnits() || actor.is(Role.TECHNICIAN)) {
                filters.add(builder.equal(root.get("managementUnitId"), actor.unitId()));
            }
            if (search.managementUnitId() != null) {
                filters.add(builder.equal(root.get("managementUnitId"), search.managementUnitId()));
            }
            if (search.code() != null && !search.code().isBlank()) {
                filters.add(builder.greaterThan(builder.locate(builder.lower(root.get("code")),
                        search.code().trim().toLowerCase(Locale.ROOT)), 0));
            }
            if (search.name() != null && !search.name().isBlank()) {
                filters.add(builder.greaterThan(builder.locate(builder.lower(root.get("name")),
                        search.name().trim().toLowerCase(Locale.ROOT)), 0));
            }
            if (search.gender() != null && !search.gender().isBlank()) {
                filters.add(builder.equal(root.get("genderPolicy"), search.gender().trim()));
            }
            if (search.depositAmount() != null) {
                filters.add(builder.equal(root.get("depositAmount"), search.depositAmount()));
            }
            return builder.and(filters.toArray(Predicate[]::new));
        };
    }

    @Override
    protected Building newEntity() {
        return new Building();
    }

    @Override
    protected void apply(Building entity, BuildingRequest input, boolean creating) {
        if (creating) {
            requireValue(input.managementUnitId(), "managementUnitId");
            requireValue(input.code(), "code");
            requireValue(input.name(), "name");
            requireValue(input.depositAmount(), "depositAmount");
            requireValue(input.floorCount(), "floorCount");
            requireValue(input.genderPolicy(), "genderPolicy");
        }
        boolean unitChanged =
                input.managementUnitId() != null
                        && !input.managementUnitId().equals(entity.getManagementUnitId());
        if (unitChanged) {
            // Đổi đơn vị khi tòa còn người ở sẽ làm lệch phạm vi Staff của hợp đồng đang chạy.
            if (!creating && countOpenAssignments(entity.getId()) > 0) {
                throw BusinessException.conflict(ErrorCode.ROOM_OCCUPIED);
            }
            entity.setManagementUnitId(input.managementUnitId());
        }
        if (input.code() != null) {
            entity.setCode(trimRequiredText(input.code(), "code"));
        }
        if (input.name() != null) {
            entity.setName(trimRequiredText(input.name(), "name"));
        }
        entity.setDepositAmount(mergeValue(input.depositAmount(), entity.getDepositAmount()));
        entity.setHasGroundFloor(mergeValue(input.hasGroundFloor(), entity.getHasGroundFloor()));
        if (input.floorCount() != null) {
            if (!creating && input.floorCount() < highestUsedFloor(entity.getId())) {
                throw BusinessException.conflict(ErrorCode.FLOOR_IN_USE);
            }
            entity.setFloorCount(input.floorCount());
        }
        if (input.genderPolicy() != null) {
            if (!creating
                    && !Facility.GENDER_MIXED.equals(input.genderPolicy())
                    && hasRoomOfOtherGender(entity.getId(), input.genderPolicy())) {
                throw BusinessException.conflict(ErrorCode.GENDER_MISMATCH);
            }
            entity.setGenderPolicy(input.genderPolicy());
        }
        entity.setAddress(mergeText(input.address(), entity.getAddress()));
        entity.setCommissionedYear(mergeValue(input.commissionedYear(), entity.getCommissionedYear()));
        entity.setFloorAreaM2(mergeValue(input.floorAreaM2(), entity.getFloorAreaM2()));
        entity.setHotline(mergeText(input.hotline(), entity.getHotline()));
        entity.setOpenTime(mergeValue(input.openTime(), entity.getOpenTime()));
        entity.setCloseTime(mergeValue(input.closeTime(), entity.getCloseTime()));
        entity.setNote(mergeText(input.note(), entity.getNote()));
        references.requireUnit(entity.getManagementUnitId());
        if (input.managerUserId() != null) {
            references.requireStaffOfUnit(input.managerUserId(), entity.getManagementUnitId());
            entity.setManagerUserId(input.managerUserId());
        } else if (unitChanged && entity.getManagerUserId() != null) {
            // Quản lý tòa cũ không còn hợp lệ khi tòa chuyển sang đơn vị khác.
            references.requireStaffOfUnit(entity.getManagerUserId(), entity.getManagementUnitId());
        }
    }

    /** Tầng cao nhất đang có phòng hoặc khai báo tầng; số tầng của tòa không được nhỏ hơn. */
    private int highestUsedFloor(Long buildingId) {
        Integer floor =
                jdbc.queryForObject(
                        "SELECT GREATEST(COALESCE((SELECT MAX(floor) FROM room WHERE building_id=?),0),"
                                + " COALESCE((SELECT MAX(floor) FROM building_floor"
                                + " WHERE building_id=?),0))",
                        Integer.class,
                        buildingId,
                        buildingId);
        return floor == null ? 0 : floor;
    }

    private boolean hasRoomOfOtherGender(Long buildingId, String gender) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM room WHERE building_id=? AND gender<>?)",
                        Boolean.class,
                        buildingId,
                        gender));
    }

    private long countOpenAssignments(Long buildingId) {
        Long count =
                jdbc.queryForObject(
                        "SELECT COALESCE(SUM(held_beds + occupied_beds),0) FROM v_room_occupancy"
                                + " WHERE building_id=?",
                        Long.class,
                        buildingId);
        return count == null ? 0 : count;
    }

    @Override
    public List<BuildingResponse> getAll() {
        checkRead();
        Actor actor = currentUser.get();
        if (actor.is(Role.STAFF) && !actor.canReadAllUnits() || actor.is(Role.TECHNICIAN)) {
            return ((BuildingRepository) repository)
                            .findAll(
                                    (r, q, b) -> b.equal(r.get("managementUnitId"), actor.unitId()))
                            .stream()
                            .map(this::toResponse)
                            .toList();
        }
        return super.getAll();
    }

    @Override
    public BuildingResponse getById(Long id) {
        checkRead();
        var entity = findEntityById(id);
        Actor actor = currentUser.get();
        if (actor.is(Role.STAFF) || actor.is(Role.TECHNICIAN)) {
            guard.requireReadableUnit(entity.getManagementUnitId());
        }
        return toResponse(entity);
    }

    @Override
    protected BuildingResponse toResponse(Building entity) {
        var counts =
                jdbc.queryForMap(
                        "SELECT COUNT(*) AS rooms, COALESCE(SUM(active_beds),0) AS capacity,"
                                + " COALESCE(SUM(occupied_beds),0) AS occupied"
                                + " FROM v_room_occupancy WHERE building_id=?",
                        entity.getId());
        String unitName =
                references.unitNames(List.of(entity.getManagementUnitId()))
                        .get(entity.getManagementUnitId());
        String managerName = null;
        if (entity.getManagerUserId() != null) {
            var manager =
                    references.users(List.of(entity.getManagerUserId()))
                            .get(entity.getManagerUserId());
            managerName = manager == null ? null : manager.fullName();
        }
        return new BuildingResponse(
                entity.getId(),
                entity.getManagementUnitId(),
                unitName,
                entity.getCode(),
                entity.getName(),
                entity.getDepositAmount(),
                entity.getFloorCount(),
                entity.getHasGroundFloor(),
                entity.getGenderPolicy(),
                entity.getAddress(),
                entity.getCommissionedYear(),
                entity.getFloorAreaM2(),
                entity.getManagerUserId(),
                managerName,
                entity.getHotline(),
                entity.getOpenTime(),
                entity.getCloseTime(),
                entity.getNote(),
                ((Number) counts.get("rooms")).longValue(),
                ((Number) counts.get("capacity")).longValue(),
                ((Number) counts.get("occupied")).longValue());
    }

    @Override
    protected Long getEntityId(Building entity) {
        return entity.getId();
    }
}
