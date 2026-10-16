package vn.edu.tlu.dms.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.room.domain.Building;

public interface BuildingRepository
        extends JpaRepository<Building, Long>, JpaSpecificationExecutor<Building> {}
