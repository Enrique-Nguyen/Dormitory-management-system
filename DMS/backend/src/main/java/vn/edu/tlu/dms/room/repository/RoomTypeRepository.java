package vn.edu.tlu.dms.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.room.domain.RoomType;

public interface RoomTypeRepository
        extends JpaRepository<RoomType, Long>, JpaSpecificationExecutor<RoomType> {}
