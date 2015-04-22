package org.dsa.iot.dslink.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Samuel Grenier
 */
public class FileUtilsTest {

    @Before
    public void setup() {
        // Coverage
        new FileUtils();
    }

    @Test
    @SuppressWarnings("unused")
    public void write() throws IOException {
        File file = File.createTempFile("test", "tmp");
        Assert.assertTrue(file.isFile());
        try {
            FileUtils.write(file, "Hello world".getBytes("UTF-8"));

            byte[] data = FileUtils.readAllBytes(file);
            Assert.assertNotNull(data);

            String s = new String(data, "UTF-8");
            Assert.assertEquals("Hello world", s);
        } finally {
            boolean ignored = file.delete();
        }
    }

    @Test
    @SuppressWarnings({"unused", "UnusedAssignment"})
    public void copy() throws IOException {
        File a = File.createTempFile("file-1", "tmp");
        File b = File.createTempFile("file-2", "tmp");

        Assert.assertTrue(a.isFile());
        Assert.assertTrue(b.isFile());

        try {
            FileUtils.write(a, "Hello world".getBytes("UTF-8"));
            FileUtils.copy(a, b);

            byte[] data = FileUtils.readAllBytes(b);
            Assert.assertNotNull(data);

            String s = new String(data, "UTF-8");
            Assert.assertEquals("Hello world", s);
        } finally {
            boolean ignored = a.delete();
            ignored = b.delete();
        }
    }
}
