package me.soels.tocairn.util;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

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
@Service
public class ZipExtractor {
    public Path extractZip(Path pathToZip) {
        try (var zipStream = new ZipInputStream(Files.newInputStream(pathToZip))) {
            var tempDirectory = Files.createTempDirectory(pathToZip.getFileName().toString());
            convertZipStreamToFiles(tempDirectory, zipStream);
            return tempDirectory;
        } catch (IOException e) {
            throw new IllegalStateException("Could not extract .zip file " + pathToZip, e);
        }
    }

    private void convertZipStreamToFiles(Path tempDirectory, ZipInputStream zipStream) throws IOException {
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

    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        var destFile = new File(destinationDir, zipEntry.getName());

        var destDirPath = destinationDir.getCanonicalPath();
        var destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
