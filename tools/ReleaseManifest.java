import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;

/** Sign release metadata using the existing APK identity; never print credentials. */
class ReleaseManifest {
    public static void main(String[] args) throws Exception {
        if(args.length==3 && args[0].equals("sign")) {
            Path manifest=Path.of(args[1]),directory=Path.of(args[2]);
            char[] storePassword=required("BITNFC_RELEASE_STORE_PASSWORD").toCharArray();
            char[] keyPassword=required("BITNFC_RELEASE_KEY_PASSWORD").toCharArray();
            try {
                KeyStore store=KeyStore.getInstance(Path.of(required("BITNFC_RELEASE_STORE_FILE")).toFile(),storePassword);
                String alias=required("BITNFC_RELEASE_KEY_ALIAS");
                PrivateKey key=(PrivateKey)store.getKey(alias,keyPassword);
                java.security.cert.Certificate certificate=store.getCertificate(alias);
                if(key==null || certificate==null || !key.getAlgorithm().equals("RSA"))
                    throw new GeneralSecurityException("Expected existing RSA APK signing identity");
                Signature signature=Signature.getInstance("SHA256withRSA");
                signature.initSign(key);signature.update(Files.readAllBytes(manifest));
                Files.createDirectories(directory);
                Files.write(directory.resolve("release-manifest.json.sig"),signature.sign());
                Files.write(directory.resolve("release-certificate.der"),certificate.getEncoded());
            } finally { Arrays.fill(storePassword,'\0');Arrays.fill(keyPassword,'\0'); }
        } else if(args.length==5 && args[0].equals("verify")) {
            byte[] der=Files.readAllBytes(Path.of(args[2]));
            String fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(der));
            if(!fingerprint.equalsIgnoreCase(args[4].replace(":","")))
                throw new GeneralSecurityException("Certificate does not match the independently trusted fingerprint");
            java.security.cert.Certificate certificate=CertificateFactory.getInstance("X.509")
                .generateCertificate(new java.io.ByteArrayInputStream(der));
            Signature signature=Signature.getInstance("SHA256withRSA");signature.initVerify(certificate);
            signature.update(Files.readAllBytes(Path.of(args[1])));
            if(!signature.verify(Files.readAllBytes(Path.of(args[3]))))
                throw new GeneralSecurityException("Release manifest signature is invalid");
            System.out.println("Release manifest signature and pinned certificate verified");
        } else {
            throw new IllegalArgumentException("sign MANIFEST OUTPUT_DIR | verify MANIFEST CERT_DER SIGNATURE TRUSTED_CERT_SHA256");
        }
    }
    private static String required(String name) {
        String value=System.getenv(name);
        if(value==null || value.isEmpty())throw new IllegalArgumentException("Missing signing configuration: "+name);
        return value;
    }
}
