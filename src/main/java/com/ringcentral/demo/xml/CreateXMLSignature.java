package com.ringcentral.demo.xml;

import com.ringcentral.demo.utils.SecurityHelper;
import com.ringcentral.demo.xml.utils.XMLHelper;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.w3c.dom.Document;

import java.io.*;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class CreateXMLSignature {
    public static void main(String[] args) throws Exception {
        System.setProperty("org.apache.xml.security.ignoreLineBreaks", "true");
        org.apache.xml.security.Init.init();

        Document doc = XMLHelper.loadXML("xml/source-doc-to-be-signed.xml");
        X509Certificate certificate = SecurityHelper.loadCert("certs/test-public-key.pem");
        PrivateKey signKey = SecurityHelper.getPrivateKey("certs/test-sign-key.pem", "ringcentral");

        XMLSignature sig = new XMLSignature(doc, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);

        doc.getDocumentElement().appendChild(sig.getElement());

        Transforms transforms = new Transforms(doc);

        transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);

        transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);

        sig.addDocument(String.format("#%s", doc.getDocumentElement().getAttribute("ID")), transforms, "http://www.w3.org/2001/04/xmlenc#sha256");

        sig.addKeyInfo(certificate);
        sig.sign(signKey);

        XMLHelper.toXML(doc.getDocumentElement(), System.out);
    }
}
