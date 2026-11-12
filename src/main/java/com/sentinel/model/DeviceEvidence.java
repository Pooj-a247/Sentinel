package com.sentinel.model;
import java.nio.file.Path;
public final class DeviceEvidence extends Evidence {
    private final String deviceType;
    private final String manufacturer;
    public DeviceEvidence(String id,String name,Path path,String deviceType,String manufacturer) {
        super(id,name,path,"DEVICE"); this.deviceType=deviceType; this.manufacturer=manufacturer;
    }
    @Override public String getEvidenceDetails() { return "Device type: "+deviceType+", manufacturer: "+manufacturer; }
}
