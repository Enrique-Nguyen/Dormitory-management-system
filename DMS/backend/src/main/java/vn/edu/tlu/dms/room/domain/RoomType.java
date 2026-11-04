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
}
