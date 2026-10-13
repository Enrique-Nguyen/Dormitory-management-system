package vn.edu.tlu.dms.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.registration.domain.RoomApplication;

public interface RoomApplicationRepository
        extends JpaRepository<RoomApplication, Long>, JpaSpecificationExecutor<RoomApplication> {}
