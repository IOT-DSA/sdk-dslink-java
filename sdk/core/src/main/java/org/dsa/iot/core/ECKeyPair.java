package org.dsa.iot.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;

/**
 * @author Samuel Grenier
 */
@Getter
@AllArgsConstructor
public class ECKeyPair {

    private final BCECPrivateKey privKey;
    private final BCECPublicKey pubKey;

}
