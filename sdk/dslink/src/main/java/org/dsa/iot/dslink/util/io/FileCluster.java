package org.dsa.iot.dslink.util.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;

/**
 * Cluster used for handling multiple backups. The lower the copy number
 * the older the backup is.
 *
 * @author Samuel Grenier
 */
public class FileCluster {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCluster.class);
    private final String path;
    private final int copies;

    public FileCluster(String path, int copies) {
        this.path = new File(path).getAbsolutePath();
        this.copies = copies;
    }

    /**
     *
     * @return Pair of the read file and the bytes.
     * @throws IOException Reading the file failed.
     */
    public synchronized Map.Entry<File, byte[]> read() throws IOException {
        for (int i = copies; i >= 1; --i) {
            File file = new File(path + "." + i);
            if (file.exists()) {
                byte[] data = FileUtils.readAllBytes(file);
                return new AbstractMap.SimpleEntry<>(file, data);
            }
        }
        return null;
    }

    /**
     *
     * @param src Source file to move
     * @return Destination file that has been moved to.
     * @throws IOException Moving the file failed.
     */
    public synchronized File move(File src) throws IOException {
        File[] files = new File[copies];
        for (int i = 1; i <= copies; ++i) {
            files[i - 1] = new File(path + "." + i);
        }

        for (File file : files) {
            if (file.exists()) {
                continue;
            } else if (!src.renameTo(file)) {
                throw new IOException("Failed to rename file to destination");
            }
            return file;
        }

        shiftFiles(files);
        File f = files[copies - 1];
        if (!src.renameTo(f)) {
            throw new IOException("Failed to rename file to destination");
        }
        return f;
    }

    /**
     * Shifts file down by one.
     * @param files Files to shift down
     * @throws IOException Shifting process failed
     */
    protected synchronized void shiftFiles(File[] files) throws IOException {
        if (!files[0].delete()) {
            throw new IOException("Failed to delete oldest file");
        }

        for (int i = 2; i <= copies; ++i) {
            File a = files[i - 1];
            File b = files[i - 2];
            if (!a.renameTo(b)) {
                String err = "Failed to rename file during shifting process";
                throw new IOException(err);
            } else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Moved {} to {}", a.getPath(), b.getPath());
            }
        }
    }
}
