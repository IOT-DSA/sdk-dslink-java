package org.dsa.iot.dslink.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;

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
            return readAllBytes(stream);
        }
    }

    /**
     * Reads all the bytes from the input stream.
     *
     * @param input The stream input to read
     * @return Bytes of the stream
     * @throws IOException IO Exception occurred
     */
    public static byte[] readAllBytes(InputStream input) throws IOException {
        Buffer buffer = new Buffer();
        byte[] buf = new byte[1024];
        int read;
        while ((read = input.read(buf)) > 0) {
            buffer.appendBytes(buf, 0, read);
        }
        return buffer.getBytes();
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
     * Copying files will delete the destination file.
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
