package io.coingift.core;

import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;

/** Standard secp256k1 parameters, now private inside bitcoinj's ECKey API. */
final class CurveParameters {
    static final X9ECParameters SECP256K1 = CustomNamedCurves.getByName("secp256k1");
    private CurveParameters() {}
}
