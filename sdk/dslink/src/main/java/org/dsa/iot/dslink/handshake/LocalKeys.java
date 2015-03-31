package org.dsa.iot.dslink.handshake;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi;
import org.bouncycastle.jcajce.provider.config.ProviderConfiguration;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.dsa.iot.dslink.util.FileUtils;
import org.dsa.iot.dslink.util.UrlBase64;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.util.Arrays;

/**
 * Manages a local key ring.
 *
 * @author Samuel Grenier
 */
public class LocalKeys {

    /**
     * The elliptic-curve algorithm used.
     */
    private static final String EC_CURVE = "SECP256R1";

    private final BCECPrivateKey privateKey;
    private final BCECPublicKey publicKey;

    private String encodedPublicKey;
    private String hashedPublicKey;

    /**
     * Populates a local key ring with a key pair.
     *
     * @param privKey Private key
     * @param pubKey  Public key
     */
    LocalKeys(BCECPrivateKey privKey, BCECPublicKey pubKey) {
        this.privateKey = privKey;
        this.publicKey = pubKey;
    }

    /**
     * @return Private key
     */
    public BCECPrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * @return Public key
     */
    public BCECPublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * The key is first sent through a SHA256 hash which is then encoded into
     * base64.
     *
     * @return Hashed key
     */
    public String encodedHashPublicKey() {
        if (hashedPublicKey != null) {
            return hashedPublicKey;
        }
        SHA256.Digest sha = new SHA256.Digest();
        byte[] encodedQ = publicKey.getQ().getEncoded(false);
        byte[] digested = sha.digest(encodedQ);
        return hashedPublicKey = UrlBase64.encode(digested);
    }

    /**
     * Encodes the Q of the public key. The encoding is uncompressed.
     *
     * @return base64 encoded public key Q
     */
    public String encodedPublicKey() {
        if (encodedPublicKey != null) {
            return encodedPublicKey;
        }
        byte[] encodedQ = publicKey.getQ().getEncoded(false);
        return encodedPublicKey = UrlBase64.encode(encodedQ);
    }

    /**
     * Serializes the public and private keys in a standard form. The format is
     * base64 encoded D and base64 encoded Q separated by a space.
     *
     * @return Standard serialized key pair
     */
    public String serialize() {
        // Serialize D
        BigInteger D = privateKey.getD();
        String encodedD = UrlBase64.encode(D.toByteArray());

        // Serialize Q
        ECPoint point = publicKey.getQ();
        byte[] encodedPoint = point.getEncoded(false);
        String encodedQ = UrlBase64.encode(encodedPoint);

        return encodedD + " " + encodedQ;
    }

    /**
     * Computes a hash code value for this object.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        int result = privateKey.getD().hashCode();

        byte[] encodedQ = publicKey.getQ().getEncoded(false);
        result = 31 * result + Arrays.hashCode(encodedQ);

        return result;
    }

    /**
     * Checks whether the keys are the same or not
     *
     * @param o Object to test
     * @return Whether the objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof LocalKeys) {
            LocalKeys other = (LocalKeys) o;

            byte[] localQ = publicKey.getQ().getEncoded(false);
            byte[] otherQ = other.publicKey.getQ().getEncoded(false);
            if (privateKey.getD().equals(other.privateKey.getD())
                    && Arrays.equals(localQ, otherQ)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves and deserializes local keys from the file system. If the file
     * doesn't exist then keys will be generated and stored at the designated
     * path.
     *
     * @param file Path of the keys
     * @return Keys
     */
    public static LocalKeys getFromFileSystem(File file) {
        try {
            if (!file.exists()) {
                LocalKeys generated = generate();
                FileUtils.write(file, generated.serialize().getBytes("UTF-8"));
                return generated;
            } else {
                byte[] bytes = FileUtils.readAllBytes(file);
                String serialized = new String(bytes, "UTF-8");
                return deserialize(serialized);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a key pair as necessary to perform a handshake.
     *
     * @return Generated keys
     */
    public static LocalKeys generate() {
        try {
            KeyPairGeneratorSpi gen = new KeyPairGeneratorSpi.ECDH();
            ECGenParameterSpec spec = new ECGenParameterSpec(EC_CURVE);
            gen.initialize(spec);

            KeyPair pair = gen.generateKeyPair();
            BCECPrivateKey privKey = (BCECPrivateKey) pair.getPrivate();
            BCECPublicKey publicKey = (BCECPublicKey) pair.getPublic();

            return new LocalKeys(privKey, publicKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserializes the serialized data into usable keys.
     *
     * @param serialized Serialized data in standard form.
     * @return The deserialized keys.
     */
    public static LocalKeys deserialize(String serialized) {
        if (serialized == null)
            throw new NullPointerException("serialized");

        String[] split = serialized.split(" ");
        if (split.length != 2) {
            throw new RuntimeException("Serialized data is invalid");
        }

        // Setup basic information
        ECDomainParameters params = getParams();
        ProviderConfiguration conf = BouncyCastleProvider.CONFIGURATION;

        // Decode Q
        byte[] decodedQ = UrlBase64.decode(split[1]);
        ECPoint point = params.getCurve().decodePoint(decodedQ);
        ECPublicKeyParameters pubParams = new ECPublicKeyParameters(point, params);
        BCECPublicKey pubKey = new BCECPublicKey(EC_CURVE, pubParams, conf);

        // Decode D
        BigInteger D = new BigInteger(UrlBase64.decode(split[0]));
        ECPrivateKeyParameters privParams = new ECPrivateKeyParameters(D, params);
        ECParameterSpec spec = getParamSpec();
        BCECPrivateKey privKey = new BCECPrivateKey(EC_CURVE, privParams, pubKey, spec, conf);

        return new LocalKeys(privKey, pubKey);
    }

    /**
     * Gets the parameters of the curve type.
     *
     * @return domain parameters
     */
    private static ECDomainParameters getParams() {
        X9ECParameters ecp = SECNamedCurves.getByName("SECP256R1");
        ECCurve curve = ecp.getCurve();
        ECPoint g = ecp.getG();
        BigInteger n = ecp.getN();
        BigInteger h = ecp.getH();
        byte[] s = ecp.getSeed();
        return new ECDomainParameters(curve, g, n, h, s);
    }

    /**
     * Gets the parameter spec of the algorithm.
     *
     * @return Parameter spec
     */
    private static ECParameterSpec getParamSpec() {
        ECDomainParameters params = getParams();
        ECCurve curve = params.getCurve();
        ECPoint g = params.getG();
        BigInteger n = params.getN();
        BigInteger h = params.getH();
        byte[] s = params.getSeed();
        return new ECNamedCurveSpec(EC_CURVE, curve, g, n, h, s);
    }
}
