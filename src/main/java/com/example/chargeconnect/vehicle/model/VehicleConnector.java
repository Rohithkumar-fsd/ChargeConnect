package com.example.chargeconnect.vehicle.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "vehicle_connectors",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_vehicle_connector",
                columnNames = {"vehicle_id", "connector_type"}
        )
)
public class VehicleConnector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_type", nullable = false, length = 50)
    private ConnectorType connectorType;

    protected VehicleConnector() {}

    public VehicleConnector(Vehicle vehicle, ConnectorType connectorType) {
        this.vehicle = vehicle;
        this.connectorType = connectorType;
    }

    public Long getId() { return id; }
    public Vehicle getVehicle() { return vehicle; }
    public ConnectorType getConnectorType() { return connectorType; }
}

