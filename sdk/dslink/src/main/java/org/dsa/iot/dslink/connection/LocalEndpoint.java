package org.dsa.iot.dslink.connection;

/**
 * @author Samuel Grenier
 */
public abstract class LocalEndpoint implements Endpoint {

    private String bindAddress = "0.0.0.0";
    private int port = 8080;

    private boolean ssl;
    private String keyStorePath;
    private char[] keyStorePass;

    public void setBindAddress(String bindAddress) {
        if (bindAddress == null)
            throw new NullPointerException("bindAddress");
        this.bindAddress = bindAddress;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setSSL(boolean ssl) {
        this.ssl = ssl;
    }

    public boolean isSSL() {
        return ssl;
    }

    public void setKeyStorePath(String keyStorePath) {
        if (keyStorePath == null)
            throw new NullPointerException("keyStorePath");
        this.keyStorePath = keyStorePath;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePassword(String password) {
        if (password == null)
            throw new NullPointerException("password");
        this.keyStorePass = password.toCharArray();
    }

    public char[] getKeyStorePassword() {
        return keyStorePass.clone();
    }
}
