package org.dsa.iot.responder.connection;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jcajce.provider.digest.SHA384;
import org.bouncycastle.util.encoders.Base64;
import org.dsa.iot.core.Utils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;

/**
 * A common connection class to handle multiple transport protocols.
 * @author Samuel Grenier
 */
public abstract class Connector {

    private final Handler<Void> dcHandler;

    public final String url;
    public final boolean secure;

    protected final Handshake handshake;
    private boolean authenticated = false;

    /**
     * @param url URL to connect to
     * @param secure Whether or not to use SSL
     * @param dcHandler Disconnection handler, can only be called when the
     *                  remote host closes the connection or there is a network
     *                  error.
     */
    public Connector(String url, boolean secure,
                     Handler<Void> dcHandler) {
        this.url = url;
        this.secure = secure;
        this.dcHandler = dcHandler;
        this.handshake = getHandshake();
    }

    /**
     * @return Whether the connection completed the initial authentication
     *         handshake.
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Connects to the server.
     * @param parser parses the incoming data from the server
     * @throws IOException
     */
    public abstract void connect(Handler<JsonObject> parser) throws IOException;

    /**
     * Closes the connection to the server.
     */
    public abstract void disconnect();

    /**
     * Writes data to the server
     * @param data Writes a response to the server
     */
    public abstract void write(String data);

    /**
     * It is the implementation's responsibility to call this.
     */
    protected final void connected() {
        authenticated = false;
        write(handshake.toJson().encode());
    }

    /**
     * It is the implementation's responsibility to call this. This will
     * finalize authentication. The implementation must check if the connection
     * is authenticated and call this if not.
     * @param obj Returned authentication data from server.
     */
    protected final void finalizeHandshake(JsonObject obj) {
        String dsId = obj.getString("dsId");
        String publicKey = obj.getString("publicKey");
        String wsUri = obj.getString("wsUri");
        String httpUri = obj.getString("httpUri");
        String encryptedNonce = obj.getString("encryptedNonce");
        int updateInterval = obj.getInteger("updateInterval");

        // TODO: store this data into a server information object

        authenticated = true;
    }

    /**
     * Called when a client is disconnected due to network error or the remote
     * host closed the connection.
     */
    protected final void disconnected() {
        if (dcHandler != null) {
            dcHandler.handle(null);
        }
    }

    protected final URLInfo getURL() throws MalformedURLException {
        URL url = new URL(this.url);

        String host = url.getHost();
        int port = url.getPort();
        if (port == -1)
            port = Utils.getDefaultPort(url.getProtocol());

        String path = url.getPath();
        if (path == null || path.isEmpty())
            path = "/";
        return new URLInfo(host, port, path);
    }

    /**
     * Factory method used to create a default connector implementation based
     * on the URL protocol.
     * @return A connector instance
     * @see #Connector
     */
    public static Connector create(String url, Handler<Void> dcHandler) {
        try {
            final URL u = new URL(url);
            final String protocol = u.getProtocol();
            switch (protocol) {
                case "http":
                case "https":
                    throw new UnsupportedOperationException("Not yet implemented");
                case "ws":
                    return new WebSocketConnector(url, false, dcHandler);
                case "wss":
                    return new WebSocketConnector(url, true, dcHandler);
                default:
                    throw new RuntimeException("Unhandled protocol: " + protocol);
            }

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an initialized set of handshake headers ready for use
     * @return Handshake headers in a map
     */
    @SuppressWarnings("ConstantConditions")
    private static Handshake getHandshake() {

        // TODO: ability for dslink implementer to provide a preset public key
        String publicKey;
        String dsId = "dslink-test-"; // TODO: ability to provide custom id prefix
        try {
            RSAKeyPairGenerator gen = new RSAKeyPairGenerator();
            gen.init(new RSAKeyGenerationParameters(Handshake.PUBLIC_EXPONENT,
                    new SecureRandom(),
                    Handshake.KEY_STRENGTH,
                    Handshake.KEY_CERTAINTY));
            AsymmetricCipherKeyPair key = gen.generateKeyPair();

            RSAKeyParameters pubParams = (RSAKeyParameters) key.getPublic();
            publicKey = Base64.toBase64String(pubParams.getModulus().toByteArray());

            SubjectPublicKeyInfo info = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(key.getPublic());
            SHA384.Digest sha = new SHA384.Digest();
            byte[] hash = sha.digest(info.getEncoded());
            dsId += Base64.toBase64String(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String zone = "default";

        boolean isRequester = false;
        boolean isResponder = true;
        return new Handshake(dsId, publicKey, zone, isRequester, isResponder);
    }

    public static class URLInfo {
        public final String host;
        public final int port;
        public final String path;

        public URLInfo(String host, int port, String path) {
            this.host = host;
            this.port = port;
            this.path = path;
        }
    }

    public static class Handshake {

        public static final BigInteger PUBLIC_EXPONENT = BigInteger.valueOf(65537);
        public static final int KEY_STRENGTH = 2048;
        public static final int KEY_CERTAINTY = 32;

        public final String dsId;
        public final String publicKey;
        public final String zone;
        public final boolean isRequester;
        public final boolean isResponder;

        public Handshake(String dsId, String publicKey, String zone,
                         boolean isRequester, boolean isResponder) {
            this.dsId = dsId;
            this.publicKey = publicKey;
            this.zone = zone;
            this.isRequester = isRequester;
            this.isResponder = isResponder;
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.putString("publicKey", publicKey);
            obj.putString("zone", zone);
            obj.putBoolean("isRequester", isRequester);
            obj.putBoolean("isResponder", isResponder);
            return obj;
        }
    }
}
