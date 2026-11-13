package com.example.chargeconnect.discovery.repo;

import com.example.chargeconnect.charger.model.Charger;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscoveryRepository extends JpaRepository<Charger, Long> {

    @Query(value = """
        SELECT
            c.id AS id,
            c.host_id AS hostId,
            u.full_name AS hostName,
            c.title AS title,
            c.description AS description,
            c.location_type AS locationType,
            c.address_line AS addressLine,
            c.city AS city,
            c.state AS state,
            c.postal_code AS postalCode,
            c.country AS country,
            c.latitude AS latitude,
            c.longitude AS longitude,
            c.power_type AS powerType,
            c.max_power_kw AS maxPowerKw,
            c.price_per_kwh AS pricePerKwh,
            c.availability_status AS availabilityStatus,
            ROUND(CAST(ST_Distance(c.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) AS numeric), 1) AS distanceMeters,
            STRING_AGG(DISTINCT cc.connector_type, ',') AS connectorTypesCsv
        FROM chargers c
        JOIN users u ON u.id = c.host_id
        JOIN charger_connectors cc ON cc.charger_id = c.id
        WHERE c.verification_status = 'APPROVED'
          AND c.availability_status = 'AVAILABLE'
          AND ST_DWithin(c.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters)
          AND c.id IN (
              SELECT cc_sub.charger_id
              FROM charger_connectors cc_sub
              WHERE cc_sub.connector_type IN (:connectorTypes)
          )
        GROUP BY c.id, u.full_name
        ORDER BY distanceMeters ASC
    """, nativeQuery = true)
    List<DiscoveredChargerProjection> findNearbyApprovedMatchingConnectors(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("connectorTypes") Collection<String> connectorTypes
    );
}
