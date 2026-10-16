package io.coingift.core;

import org.junit.Test;
import org.junit.Assume;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.Assert.*;

public class BackupInputTest {
    @Test public void readsNormalFileAndTrimsLineEnding() throws Exception {
        Path file=Files.createTempFile("bittap-public-input-",".txt");
        try { Files.writeString(file,"CG3:public-fixture-only\n");assertEquals("CG3:public-fixture-only",BackupInput.read(file)); }
        finally { Files.deleteIfExists(file); }
    }
    @Test public void refusesOversizedActualContent() throws Exception {
        Path file=Files.createTempFile("bittap-public-large-",".txt");
        try { Files.write(file,new byte[1024*1024]);assertThrows(IOException.class,()->BackupInput.read(file)); }
        finally { Files.deleteIfExists(file); }
    }
    @Test public void refusesDirectory() throws Exception {
        Path dir=Files.createTempDirectory("bittap-public-dir-");
        try { assertThrows(IOException.class,()->BackupInput.read(dir)); }
        finally { Files.deleteIfExists(dir); }
    }
    @Test public void refusesMissingFile() {
        assertThrows(IOException.class,()->BackupInput.read(Path.of("bittap-nonexistent-test-input")));
    }
    @Test public void refusesDeviceWithoutReadingIt() {
        Path device=Path.of("/dev/zero");Assume.assumeTrue(Files.exists(device));
        assertThrows(IOException.class,()->BackupInput.read(device));
    }
    @Test public void refusesSymlink() throws Exception {
        Path dir=Files.createTempDirectory("bittap-public-link-");Path file=dir.resolve("fixture"),link=dir.resolve("link");
        try {
            Files.writeString(file,"public-fixture-only");
            try { Files.createSymbolicLink(link,file); }
            catch(UnsupportedOperationException|java.nio.file.FileSystemException ex) { Assume.assumeNoException(ex); }
            assertThrows(IOException.class,()->BackupInput.read(link));
        } finally { Files.deleteIfExists(link);Files.deleteIfExists(file);Files.deleteIfExists(dir); }
    }
}
