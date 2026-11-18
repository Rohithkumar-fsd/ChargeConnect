package com.example.chargeconnect.charger.repo;

import com.example.chargeconnect.charger.dto.VerificationStatus;
import com.example.chargeconnect.charger.model.Charger;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargerRepository extends JpaRepository<Charger, Long> {

    @EntityGraph(attributePaths = {"connectors", "host", "verifiedBy"})
    List<Charger> findAllByHostIdOrderByCreatedAtDesc(Long hostId);

    @EntityGraph(attributePaths = {"connectors", "host", "verifiedBy"})
    Optional<Charger> findByIdAndHostId(Long id, Long hostId);

    @EntityGraph(attributePaths = {"connectors", "host", "verifiedBy"})
    List<Charger> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"connectors", "host", "verifiedBy"})
    List<Charger> findAllByVerificationStatusOrderByCreatedAtDesc(VerificationStatus verificationStatus);

    @EntityGraph(attributePaths = {"connectors", "host", "verifiedBy"})
    Optional<Charger> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"connectors", "host", "verifiedBy"})
    Optional<Charger> findByQrCode(String qrCode);
}

