package me.soels.thesis.analysis;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service class to extract a .zip file to a temporary directory.
 * <p>
 * Derived from <a href="https://www.baeldung.com/java-compress-and-uncompress">a Baeldung guide</a>.
 */
public class ZipExtractor {
    private ZipExtractor() {
        // Utility class, do not instantiate
        // TODO: This is actually a service. Once we have an injection mechanism (e.g. Spring), inject this service instead.
    }

    public static Path extractZip(Path pathToZip) {
        try (var zipStream = new ZipInputStream(Files.newInputStream(pathToZip))) {
            var tempDirectory = Files.createTempDirectory(pathToZip.getFileName().toString());
            convertZipStreamToFiles(tempDirectory, zipStream);
            return tempDirectory;
        } catch (IOException e) {
            throw new IllegalStateException("Could not extract .zip file " + pathToZip, e);
        }
    }

    private static void convertZipStreamToFiles(Path tempDirectory, ZipInputStream zipStream) throws IOException {
        var zipEntry = zipStream.getNextEntry();
        while (zipEntry != null) {
            var newFile = newFile(tempDirectory.toFile(), zipEntry);

            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                try (var outputStream = new FileOutputStream(newFile)) {
                    IOUtils.copy(zipStream, outputStream);
                }
            }
            zipEntry = zipStream.getNextEntry();
        }
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
