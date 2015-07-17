package org.dsa.iot.dslink.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @author Samuel Grenier
 */
public class FileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    /**
     * Reads all the bytes from the source file.
     *
     * @param src Source file to read
     * @return All the bytes of the file
     * @throws IOException IO Exception occurred
     */
    public static byte[] readAllBytes(File src) throws IOException {
        try (InputStream stream = new FileInputStream(src)) {
            int length = (int) src.length();
            if (length < 0) {
                // Handle overflow
                length = Integer.MAX_VALUE;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] bytes = new byte[length];
            int read = stream.read(bytes);
            while (read != -1) {
                baos.write(bytes, 0, read);
                read = stream.read(bytes);
            }
            baos.flush();
            return baos.toByteArray();
        }
    }

    /**
     * If the file already exists, it will be overwritten with the new
     * bytes designated here.
     *
     * @param path Path to write to.
     * @param bytes Bytes to write into the new file.
     * @throws IOException IO Exception occurred
     */
    public static void write(File path, byte[] bytes) throws IOException {
        if (path.delete()) {
            LOGGER.debug("Removed " + path.getPath() + " during writing");
        }
        try (OutputStream stream = new FileOutputStream(path)) {
            stream.write(bytes);
        }
    }

    /**
     * Copying files will delete the destination file if it already exists.
     *
     * @param src Source file to copy
     * @param dest Destination file to copy to
     * @throws IOException IO Exception occurred
     */
    public static void copy(File src, File dest) throws IOException {
        if (dest.delete()) {
            LOGGER.debug("Removed " + dest.getPath() + " during copying");
        }
        InputStream input = null;
        OutputStream output = null;

        try {
            input = new FileInputStream(src);
            output = new FileOutputStream(dest);

            byte[] buf = new byte[1024];
            int read;
            while ((read = input.read(buf)) > 0) {
                output.write(buf, 0, read);
            }
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                LOGGER.debug(writer.toString());
            }

            try {
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                LOGGER.debug(writer.toString());
            }
        }
    }
}
