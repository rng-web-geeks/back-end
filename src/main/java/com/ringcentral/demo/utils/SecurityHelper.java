package com.ringcentral.demo.utils;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;

public final  class SecurityHelper {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static X509Certificate loadCert(String location) throws IOException, CertificateException {
        try(InputStream is = IOHelper.loadFromResource(location)) {
            if (null != is) {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                return (X509Certificate) certFactory.generateCertificate(is);
            }
        }
        return null;
    }

    public static PrivateKey getPrivateKey(String location, String password) throws Exception {
        try (InputStream is = IOHelper.loadFromResource(location)) {
            try (PEMParser parser = new PEMParser(new InputStreamReader(is))) {
                Object keyObject = parser.readObject();
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

                if (keyObject == null) {
                    throw new InvalidKeyException(String.format("Unable to decode PEM key:%s", location));
                } else if (keyObject instanceof PKCS8EncryptedPrivateKeyInfo) {
                    PKCS8EncryptedPrivateKeyInfo keypair = (PKCS8EncryptedPrivateKeyInfo) keyObject;
                    JceOpenSSLPKCS8DecryptorProviderBuilder jce = new JceOpenSSLPKCS8DecryptorProviderBuilder();
                    jce.setProvider("BC");
                    PrivateKeyInfo keyInfo = keypair.decryptPrivateKeyInfo(jce.build(password.toCharArray()));
                    return converter.getPrivateKey(keyInfo);
                } else {
                    return converter.getPrivateKey(((PEMKeyPair) keyObject).getPrivateKeyInfo());
                }
            }
        }
    }
}
