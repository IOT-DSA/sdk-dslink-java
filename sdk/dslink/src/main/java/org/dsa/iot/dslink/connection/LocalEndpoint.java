package org.dsa.iot.dslink.connection;

/**
 * Endpoint for managing servers.
 * @author Samuel Grenier
 */
public abstract class LocalEndpoint implements Endpoint {

    private String bindAddress = "0.0.0.0";
    private int port = 8080;

    private boolean ssl;
    private String keyStorePath;
    private char[] keyStorePass;

    /**
     * Sets the bind address the server will bind to.
     * @param bindAddress Bind address
     */
    public void setBindAddress(String bindAddress) {
        if (bindAddress == null)
            throw new NullPointerException("bindAddress");
        this.bindAddress = bindAddress;
    }

    /**
     * @return Bind address the server will use.
     */
    public String getBindAddress() {
        return bindAddress;
    }

    /**
     * @param port Port the server will bind to.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return Port the server will bind to.
     */
    public int getPort() {
        return port;
    }

    /**
     * If {@code ssl} is {@code true} then a key store path must be set as well
     * to properly configure the SSL handler.
     * @param ssl Whether to enable SSL or not in the server.
     */
    public void setSSL(boolean ssl) {
        this.ssl = ssl;
    }

    /**
     * @return Whether the server is operating in secure mode or not
     */
    public boolean isSSL() {
        return ssl;
    }

    /**
     * @param keyStorePath Key store path for Java certificates
     */
    public void setKeyStorePath(String keyStorePath) {
        if (keyStorePath == null)
            throw new NullPointerException("keyStorePath");
        this.keyStorePath = keyStorePath;
    }

    /**
     * @return Key store path for Java certificates.
     */
    public String getKeyStorePath() {
        return keyStorePath;
    }

    /**
     * Only needs to be set if the key store has a password.
     * @param password Password for the key store
     */
    public void setKeyStorePassword(String password) {
        if (password == null)
            throw new NullPointerException("password");
        this.keyStorePass = password.toCharArray();
    }

    /**
     * @return Password for the key store
     */
    public char[] getKeyStorePassword() {
        return keyStorePass.clone();
    }
}
