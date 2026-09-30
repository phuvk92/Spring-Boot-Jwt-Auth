package com.example.svgmanager.entity;

import java.io.Serializable;
import java.util.Objects;

public class SvgFileVehicleNodeId implements Serializable {

    private Long svgFile;
    private Long vehicleNode;

    public SvgFileVehicleNodeId() {
    }

    public SvgFileVehicleNodeId(Long svgFile, Long vehicleNode) {
        this.svgFile = svgFile;
        this.vehicleNode = vehicleNode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SvgFileVehicleNodeId that)) {
            return false;
        }
        return Objects.equals(svgFile, that.svgFile) && Objects.equals(vehicleNode, that.vehicleNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(svgFile, vehicleNode);
    }
}
