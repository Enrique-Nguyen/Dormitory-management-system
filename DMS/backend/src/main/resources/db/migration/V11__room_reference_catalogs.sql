CREATE TABLE building_floor (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES building(id),
    floor SMALLINT NOT NULL CHECK (floor >= 0),
    name VARCHAR(100) NOT NULL CHECK (btrim(name) <> ''),
    active BOOLEAN NOT NULL DEFAULT true,
    UNIQUE (building_id, floor)
);

INSERT INTO building_floor(building_id, floor, name)
SELECT building_id, floor, 'Tầng ' || floor FROM (
    SELECT building_id, floor FROM room
    UNION SELECT building_id, floor FROM maintenance_request WHERE building_id IS NOT NULL AND floor IS NOT NULL
    UNION SELECT building_id, floor FROM notification WHERE building_id IS NOT NULL AND floor IS NOT NULL
) existing;

ALTER TABLE room ADD CONSTRAINT fk_room_building_floor
    FOREIGN KEY (building_id, floor) REFERENCES building_floor(building_id, floor);
ALTER TABLE maintenance_request ADD CONSTRAINT fk_maintenance_building_floor
    FOREIGN KEY (building_id, floor) REFERENCES building_floor(building_id, floor);
ALTER TABLE notification ADD CONSTRAINT fk_notification_building_floor
    FOREIGN KEY (building_id, floor) REFERENCES building_floor(building_id, floor);
