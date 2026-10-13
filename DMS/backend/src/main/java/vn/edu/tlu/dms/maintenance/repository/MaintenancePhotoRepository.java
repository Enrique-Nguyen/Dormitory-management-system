package vn.edu.tlu.dms.maintenance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.maintenance.domain.MaintenancePhoto;

public interface MaintenancePhotoRepository
        extends JpaRepository<MaintenancePhoto, Long>, JpaSpecificationExecutor<MaintenancePhoto> {}
