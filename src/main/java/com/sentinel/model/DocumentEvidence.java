package com.sentinel.model;
import java.nio.file.Path;
public final class DocumentEvidence extends Evidence {
    private final String documentFormat;
    private final int pageCount;
    public DocumentEvidence(String id,String name,Path path,String documentFormat,int pageCount) {
        super(id,name,path,"DOCUMENT"); this.documentFormat=documentFormat; this.pageCount=pageCount;
    }
    @Override public String getEvidenceDetails() { return "Document format: "+documentFormat+", pages: "+pageCount; }
}
