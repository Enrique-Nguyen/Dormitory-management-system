package vn.edu.tlu.dms.room.service.impl;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.edu.tlu.dms.account.service.CatalogService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.Catalog;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Resource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.dto.SearchRequest;
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

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomTypeServiceImpl
        extends AbstractCrudServiceImpl<RoomType, RoomTypeRequest, RoomTypeResponse>
        implements RoomTypeService {

    private final ScopeGuard guard;

    private final RoomRepository rooms;

    private final CatalogService catalogs;

    public RoomTypeServiceImpl(
            RoomTypeRepository repository,
            RoomRepository rooms,
            ScopeGuard guard,
            CurrentUser currentUser,
            AuditService auditService,
            CatalogService catalogs) {
        super(repository, auditService, currentUser);
        this.guard = guard;
        this.rooms = rooms;
        this.catalogs = catalogs;
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
    protected Long getInputId(RoomTypeRequest input) {
        return input.id();
    }

    @Override
    protected Set<String> searchableFields() {
        return Set.of("name", "code");
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of(
                "id",
                "code",
                "name",
                "bedCount",
                "priceMain",
                "priceSummer",
                "areaM2",
                "active");
    }

    @Override
    protected Specification<RoomType> searchScope(SearchRequest search) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (search.name() != null && !search.name().isBlank()) {
                predicate = builder.and(predicate, builder.greaterThan(builder.locate(
                        builder.lower(root.get("name")), search.name().trim().toLowerCase(Locale.ROOT)), 0));
            }
            if (search.active() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("active"), search.active()));
            }
            if (search.code() != null && !search.code().isBlank()) {
                predicate = builder.and(predicate, builder.greaterThan(builder.locate(
                        builder.lower(root.get("code")), search.code().trim().toLowerCase(Locale.ROOT)), 0));
            }
            if (search.bedCount() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("bedCount"), search.bedCount()));
            }
            if (search.priceMain() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("priceMain"), search.priceMain()));
            }
            if (search.priceSummer() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("priceSummer"), search.priceSummer()));
            }
            return predicate;
        };
    }

    @Override
    protected RoomType newEntity() {
        return new RoomType();
    }

    @Override
    protected void apply(RoomType entity, RoomTypeRequest input, boolean creating) {
        if (creating) {
            requireValue(input.code(), "code");
            requireValue(input.name(), "name");
        }
        if (input.code() != null && !input.code().equals(entity.getCode())) {
            // Mã là khóa tham chiếu trong Excel; không đổi khi đã có phòng dùng loại này.
            if (!creating && countRooms(entity.getId()) > 0) {
                throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
            }
            entity.setCode(trimRequiredText(input.code(), "code"));
        }
        entity.setPrivateBathroom(mergeValue(input.privateBathroom(), entity.getPrivateBathroom()));
        entity.setHasBalcony(mergeValue(input.hasBalcony(), entity.getHasBalcony()));
        entity.setBedStyle(mergeValue(input.bedStyle(), entity.getBedStyle()));
        entity.setHasKitchen(mergeValue(input.hasKitchen(), entity.getHasKitchen()));
        entity.setDescription(mergeText(input.description(), entity.getDescription()));
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
        if (input.amenityIds() != null) {
            Set<Long> selected = new LinkedHashSet<>(input.amenityIds());
            // Giữ tiện nghi cũ đã ngừng hoạt động; chỉ kiểm tra các lựa chọn mới.
            for (Long amenityId : selected) {
                if (amenityId == null || amenityId <= 0) {
                    throw BusinessException.invalid(Catalog.AMENITIES, Catalog.INVALID_VALUE);
                }
                if (creating || !entity.getAmenityIds().contains(amenityId)) {
                    catalogs.requireActive(Catalog.AMENITIES, amenityId);
                }
            }
            entity.getAmenityIds().clear();
            entity.getAmenityIds().addAll(selected);
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
        var amenityIds = entity.getAmenityIds().stream().sorted().toList();
        String amenities = amenityIds.stream()
                .map(id -> (String) catalogs.getById(Catalog.AMENITIES, id).get(ApiField.NAME))
                .collect(Collectors.joining(", "));
        return new RoomTypeResponse(
                entity.getId(),
                entity.getName(),
                entity.getBedCount(),
                entity.getPriceMain(),
                entity.getPriceSummer(),
                entity.getAreaM2(),
                amenityIds,
                amenities,
                entity.getActive(),
                entity.getCode(),
                entity.getPrivateBathroom(),
                entity.getHasBalcony(),
                entity.getBedStyle(),
                entity.getHasKitchen(),
                entity.getDescription(),
                entity.getId() == null ? 0 : countRooms(entity.getId()));
    }

    /** Số phòng đang dùng loại phòng; dùng cho hiển thị và chặn đổi mã/số giường. */
    private long countRooms(Long roomTypeId) {
        return rooms.count((r, q, b) -> b.equal(r.get("roomTypeId"), roomTypeId));
    }

    @Override
    protected Long getEntityId(RoomType entity) {
        return entity.getId();
    }
}
