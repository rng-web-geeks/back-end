package com.ringcentral.demo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public final  class CertificateHelper {

    public static X509Certificate loadCert(String location) throws IOException, CertificateException {
        try(InputStream is = IOHelper.loadFromResource(location)) {
            if (null != is) {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                return (X509Certificate) certFactory.generateCertificate(is);
            }
        }
        return null;
    }
}
