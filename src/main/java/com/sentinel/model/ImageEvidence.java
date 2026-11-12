package com.sentinel.model;
import java.nio.file.Path;
public final class ImageEvidence extends Evidence {
    private final String imageFormat;
    private final String resolution;
    public ImageEvidence(String id,String name,Path path,String imageFormat,String resolution) {
        super(id,name,path,"IMAGE"); this.imageFormat=imageFormat; this.resolution=resolution;
    }
    @Override public String getEvidenceDetails() { return "Image format: "+imageFormat+", resolution: "+resolution; }
}
