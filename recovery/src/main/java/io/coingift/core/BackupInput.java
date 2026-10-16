package io.coingift.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.concurrent.*;

/** CLI input has both an actual byte limit and a deadline, including opening the file. */
final class BackupInput {
    private BackupInput() {}

    static String read(Path path) throws IOException {
        ExecutorService reader=Executors.newSingleThreadExecutor(task->{
            Thread thread=new Thread(task,"bittap-backup-reader");
            // A stuck filesystem operation must not keep this command-line process alive.
            thread.setDaemon(true);return thread;
        });
        Future<String> result=reader.submit(()->readRegularFile(path));
        try { return result.get(10,TimeUnit.SECONDS); }
        catch(TimeoutException ex) { throw new IOException("Backup read timed out",ex); }
        catch(InterruptedException ex) { Thread.currentThread().interrupt();throw new IOException("Backup read cancelled",ex); }
        catch(ExecutionException ex) { throw new IOException("Cannot read backup",ex.getCause()); }
        finally { result.cancel(true);reader.shutdownNow(); }
    }

    private static String readRegularFile(Path path) throws IOException {
        if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Backup must be a regular file, not a device, pipe or symbolic link");
        // Do not trust a prior Files.size(): the file can grow between check and read.
        try(InputStream input=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS)) {
            return new String(BackupFiles.readBounded(input),StandardCharsets.UTF_8).trim();
        }
    }
}
