CREATE TABLE room_type_amenity (
    room_type_id BIGINT NOT NULL REFERENCES room_type(id) ON DELETE CASCADE,
    amenity_id BIGINT NOT NULL REFERENCES amenity(id),
    PRIMARY KEY (room_type_id, amenity_id)
);

-- V10 preserved each complete legacy description as one amenity name.
INSERT INTO room_type_amenity (room_type_id, amenity_id)
SELECT rt.id, a.id
FROM room_type rt
JOIN amenity a ON a.name = rt.amenities;

CREATE INDEX ix_room_type_amenity_amenity ON room_type_amenity(amenity_id);

ALTER TABLE room_type DROP COLUMN amenities;
