package org.dsa.iot.broker.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.dsa.iot.broker.Broker;
import org.dsa.iot.broker.client.Client;
import org.dsa.iot.broker.client.ClientManager;
import org.dsa.iot.dslink.handshake.LocalKeys;
import org.dsa.iot.dslink.handshake.RemoteKey;
import org.dsa.iot.dslink.util.UrlBase64;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * @author Samuel Grenier
 */
public class DsaHandshake {

    private static final SecureRandom RANDOM = new SecureRandom();

    private String dsId;
    private String publicKey;
    private boolean isRequester;
    private boolean isResponder;
    private JsonObject linkData;

    private String name;
    private String salt;
    private LocalKeys tempKey;

    public DsaHandshake(JsonObject handshake, String dsId) {
        this.dsId = dsId;
        this.publicKey = handshake.get("publicKey");
        this.isRequester = getBool(handshake, "isRequester");
        this.isResponder = getBool(handshake, "isResponder");
        this.linkData = handshake.get("linkData");

        this.name = dsIdToName(dsId);
        this.salt = generateSalt();
        this.tempKey = LocalKeys.generate();
    }

    public String name() {
        return name;
    }

    public String dsId() {
        return dsId;
    }

    public JsonObject linkData() {
        return linkData;
    }

    public boolean isRequester() {
        return isRequester;
    }

    public boolean isResponder() {
        return isResponder;
    }

    public ByteBuf initialize(Broker broker) {
        Client client = new Client(broker, this);

        ClientManager manager = broker.getClientManager();
        manager.clientConnecting(client);

        JsonObject obj = new JsonObject();
        obj.put("tempKey", tempKey.encodedPublicKey());
        obj.put("salt", salt);

        if (isResponder) {
            this.name = broker.getTree().initResponder(name, dsId);
            obj.put("path", "/" + broker.getDownstreamName() + "/" + name);
        }
        obj.put("wsUri", "/ws");

        byte[] bytes = obj.encode().getBytes(CharsetUtil.UTF_8);
        return Unpooled.wrappedBuffer(bytes);
    }

    public boolean validate(String auth) {
        if (auth == null) {
            return false;
        }
        byte[] origHash = UrlBase64.decode(auth);
        byte[] validHash;
        {
            byte[] salt = this.salt.getBytes(CharsetUtil.UTF_8);
            byte[] ss = RemoteKey.generate(tempKey, publicKey).getSharedSecret();
            byte[] bytes = new byte[salt.length + ss.length];
            System.arraycopy(salt, 0, bytes, 0, salt.length);
            System.arraycopy(ss, 0, bytes, salt.length, ss.length);

            SHA256.Digest sha = new SHA256.Digest();
            validHash = sha.digest(bytes);
        }
        return MessageDigest.isEqual(origHash, validHash);
    }

    private static boolean getBool(JsonObject obj, String name) {
        Boolean b = obj.get(name);
        return b != null ? b : false;
    }

    private static String generateSalt() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return new String(b, CharsetUtil.UTF_8);
    }

    private static String dsIdToName(String dsId) {
        String tmp = dsId.substring(0, dsId.length() - 43);
        if (tmp.lastIndexOf('-') > -1) {
            tmp = tmp.substring(0, tmp.length() - 1);
        }
        if (tmp.isEmpty()) {
            throw new IllegalStateException("Invalid DsId name from " + dsId);
        }
        return tmp;
    }
}
