package com.sentinel.model;
import java.nio.file.Path;
public final class VideoEvidence extends Evidence {
    private final String videoFormat;
    private final double durationSeconds;
    public VideoEvidence(String id,String name,Path path,String videoFormat,double durationSeconds) {
        super(id,name,path,"VIDEO"); this.videoFormat=videoFormat; this.durationSeconds=durationSeconds;
    }
    @Override public String getEvidenceDetails() { return "Video format: "+videoFormat+", duration: "+durationSeconds+" seconds"; }
}
