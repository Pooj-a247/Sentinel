package com.sentinel;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class HashUtilTest {
 @Test void sha256KnownVector() throws Exception {
  Path p=Files.createTempFile("sentinel","txt");
  try { Files.writeString(p,"abc"); assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",HashUtil.sha256(p)); }
  finally { Files.deleteIfExists(p); }
 }
}
