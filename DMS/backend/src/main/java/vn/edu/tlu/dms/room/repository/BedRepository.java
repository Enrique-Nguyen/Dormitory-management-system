package vn.edu.tlu.dms.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.room.domain.Bed;

public interface BedRepository extends JpaRepository<Bed, Long>, JpaSpecificationExecutor<Bed> {}
