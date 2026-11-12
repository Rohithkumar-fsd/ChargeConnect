package com.example.chargeconnect.charger;

import com.example.chargeconnect.vehicle.ConnectorType;
import jakarta.persistence.*;

@Entity
@Table(
        name = "charger_connectors",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_charger_connector",
                columnNames = {"charger_id", "connector_type"}
        )
)
public class ChargerConnector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charger_id", nullable = false)
    private Charger charger;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_type", nullable = false, length = 50)
    private ConnectorType connectorType;

    protected ChargerConnector() {}

    public ChargerConnector(Charger charger, ConnectorType connectorType) {
        this.charger = charger;
        this.connectorType = connectorType;
    }

    public Long getId() { return id; }
    public Charger getCharger() { return charger; }
    public ConnectorType getConnectorType() { return connectorType; }
}

