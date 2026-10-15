package vn.edu.tlu.dms.room.service.impl;

import org.springframework.stereotype.Service;

import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Resource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.impl.AbstractCrudServiceImpl;
import vn.edu.tlu.dms.room.api.RoomTypeRequest;
import vn.edu.tlu.dms.room.api.RoomTypeResponse;
import vn.edu.tlu.dms.room.domain.RoomType;
import vn.edu.tlu.dms.room.repository.RoomRepository;
import vn.edu.tlu.dms.room.repository.RoomTypeRepository;
import vn.edu.tlu.dms.room.service.RoomTypeService;

@Service
public class RoomTypeServiceImpl
        extends AbstractCrudServiceImpl<RoomType, RoomTypeRequest, RoomTypeResponse>
        implements RoomTypeService {

    private final ScopeGuard guard;

    private final CurrentUser currentUser;

    private final RoomRepository rooms;

    public RoomTypeServiceImpl(
            RoomTypeRepository repository,
            RoomRepository rooms,
            ScopeGuard guard,
            CurrentUser currentUser,
            AuditService auditService) {
        super(repository, auditService, currentUser);
        this.guard = guard;
        this.currentUser = currentUser;
        this.rooms = rooms;
    }

    @Override
    protected void checkRead() {
        guard.requirePermission(Permission.ROOM_TYPE_READ);
    }

    @Override
    protected void checkWrite() {
        guard.requireRole(Role.STAFF);
    }

    @Override
    protected void checkPermission(String action) {
        guard.requirePermission(Resource.ROOM_TYPE + DmsConstants.PERMISSION_SEPARATOR + action);
    }

    @Override
    protected RoomType newEntity() {
        return new RoomType();
    }

    @Override
    protected void apply(RoomType entity, RoomTypeRequest input, boolean creating) {
        if (creating) {
            requireValue(input.name(), "name");
        }
        if (input.name() != null) {
            entity.setName(trimRequiredText(input.name(), "name"));
        }
        if (creating) {
            requireValue(input.bedCount(), "bedCount");
        }
        if (input.bedCount() != null) {
            if (!creating
                    && !input.bedCount().equals(entity.getBedCount())
                    && rooms.exists((r, q, b) -> b.equal(r.get("roomTypeId"), entity.getId()))) {
                // Phòng đã tạo giữ số giường cũ; đổi sức chứa của loại phòng đang dùng làm lệch.
                throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
            }
            entity.setBedCount(input.bedCount());
        }
        if (creating) {
            requireValue(input.priceMain(), "priceMain");
        }
        if (input.priceMain() != null) {
            entity.setPriceMain(input.priceMain());
        }
        if (creating) {
            requireValue(input.priceSummer(), "priceSummer");
        }
        if (input.priceSummer() != null) {
            entity.setPriceSummer(input.priceSummer());
        }
        if (input.areaM2() != null) {
            entity.setAreaM2(input.areaM2());
        }
        if (input.amenities() != null) {
            entity.setAmenities(input.amenities());
        }
        if (creating) {
            entity.setActive(input.active() == null ? true : input.active());
        }
        if (input.active() != null) {
            entity.setActive(input.active());
        }
    }

    @Override
    protected RoomTypeResponse toResponse(RoomType entity) {
        return new RoomTypeResponse(
                entity.getId(),
                entity.getName(),
                entity.getBedCount(),
                entity.getPriceMain(),
                entity.getPriceSummer(),
                entity.getAreaM2(),
                entity.getAmenities(),
                entity.getActive());
    }

    @Override
    protected Long getEntityId(RoomType entity) {
        return entity.getId();
    }
}
