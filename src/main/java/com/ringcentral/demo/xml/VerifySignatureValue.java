package com.ringcentral.demo.xml;

import com.ringcentral.demo.utils.CertificateHelper;
import com.ringcentral.demo.xml.utils.XMLHelper;
import org.apache.xml.security.c14n.Canonicalizer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class VerifySignatureValue {

    public static void main(String[] args) throws Exception {
        org.apache.xml.security.Init.init();

        X509Certificate verifyCert = CertificateHelper.loadCert("certs/okta.pem");
        Document doc = XMLHelper.loadXML("xml/signed-xml-example.xml");

        Element signatureNode = getSignatureNode(doc.getDocumentElement());
        Element signInfoNode = XMLHelper.getNextElement(signatureNode.getFirstChild());

        String methodUrl = getCanonicalizationMethodURI(signInfoNode);

        byte[] signatureValue = getSignatureValue(signatureNode);


        Canonicalizer canon = Canonicalizer.getInstance(methodUrl);
        canon.setSecureValidation(true);
        byte[] canonSignInfo = canon.canonicalizeSubtree(signInfoNode);

        Signature sig = Signature.getInstance("SHA256withRSA");

        sig.initVerify(verifyCert.getPublicKey());
        sig.update(canonSignInfo);

        boolean test = sig.verify(signatureValue);
        System.out.println(String.format("XML data validation result is %s.", test));
    }

    private static byte[] getSignatureValue(Element signatureNode) {
        Element signInfoNode = XMLHelper.getNextElement(signatureNode.getFirstChild());
        Element signatureValueNode = XMLHelper.getNextElement(signInfoNode.getNextSibling());
        String content = XMLHelper.getFullTextChildrenFromElement(signatureValueNode);
        return Base64.getMimeDecoder().decode(content);
    }

    private static String getCanonicalizationMethodURI(Element signInfoNode) {
        Element canonicalizationMethod = XMLHelper.getNextElement(signInfoNode.getFirstChild());
        return canonicalizationMethod.getAttribute("Algorithm");
    }

    private static Element getSignatureNode(Element doc) throws Exception {
        Element[] signatureNodes = XMLHelper.selectDsNodes(doc.getFirstChild(), "Signature");

        if (signatureNodes.length == 0) {
            throw new Exception("Cannot find Signature element");
        }

        return signatureNodes[0];
    }

}