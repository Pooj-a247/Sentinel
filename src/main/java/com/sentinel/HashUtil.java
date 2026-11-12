package com.sentinel;
import java.io.*;import java.nio.file.*;import java.security.*;import java.util.HexFormat;
public final class HashUtil {
 private HashUtil(){}
 public static String sha256(Path p)throws IOException{
  try{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=Files.newInputStream(p)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)d.update(b,0,n);}return HexFormat.of().formatHex(d.digest());}
  catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 public static String sha256(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
