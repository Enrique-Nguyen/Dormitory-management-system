package vn.edu.tlu.dms.room.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import vn.edu.tlu.dms.common.constants.DmsConstants.Facility;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "room_type")
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "bed_count", nullable = false)
    private Short bedCount;

    @Column(name = "price_main", nullable = false)
    private Long priceMain;

    @Column(name = "price_summer", nullable = false)
    private Long priceSummer;

    @Column(name = "area_m2", precision = 5, scale = 1)
    private BigDecimal areaM2;

    @ElementCollection
    @CollectionTable(name = "room_type_amenity", joinColumns = @JoinColumn(name = "room_type_id"))
    @Column(name = "amenity_id", nullable = false)
    private Set<Long> amenityIds = new LinkedHashSet<>();

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "code", length = 30)
    private String code;

    @Column(name = "private_bathroom", nullable = false)
    private Boolean privateBathroom = Boolean.FALSE;

    @Column(name = "has_balcony", nullable = false)
    private Boolean hasBalcony = Boolean.FALSE;

    @Column(name = "bed_style", length = 10)
    private String bedStyle = Facility.BED_STYLE_BUNK;

    @Column(name = "has_kitchen", nullable = false)
    private Boolean hasKitchen = Boolean.FALSE;

    @Column(name = "description", length = 1000)
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public Short getBedCount() {
        return bedCount;
    }

    public void setBedCount(Short value) {
        this.bedCount = value;
    }

    public Long getPriceMain() {
        return priceMain;
    }

    public void setPriceMain(Long value) {
        this.priceMain = value;
    }

    public Long getPriceSummer() {
        return priceSummer;
    }

    public void setPriceSummer(Long value) {
        this.priceSummer = value;
    }

    public BigDecimal getAreaM2() {
        return areaM2;
    }

    public void setAreaM2(BigDecimal value) {
        this.areaM2 = value;
    }

    public Set<Long> getAmenityIds() {
        return amenityIds;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean value) {
        this.active = value;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String value) {
        this.code = value;
    }

    public Boolean getPrivateBathroom() {
        return privateBathroom;
    }

    public void setPrivateBathroom(Boolean value) {
        this.privateBathroom = value;
    }

    public Boolean getHasBalcony() {
        return hasBalcony;
    }

    public void setHasBalcony(Boolean value) {
        this.hasBalcony = value;
    }

    public String getBedStyle() {
        return bedStyle;
    }

    public void setBedStyle(String value) {
        this.bedStyle = value;
    }

    public Boolean getHasKitchen() {
        return hasKitchen;
    }

    public void setHasKitchen(Boolean value) {
        this.hasKitchen = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }
}
