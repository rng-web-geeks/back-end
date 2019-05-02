package com.ringcentral.demo.xml;

import com.ringcentral.demo.utils.SecurityHelper;
import com.ringcentral.demo.xml.utils.XMLHelper;
import org.apache.xml.security.signature.XMLSignature;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import java.security.cert.X509Certificate;
import java.util.List;

public class VerifyXMLSignatureFullExample {
    public static void main(String[] args) throws Exception {
        System.setProperty("org.apache.xml.security.ignoreLineBreaks", "true");
        org.apache.xml.security.Init.init();


        Document doc = XMLHelper.loadXML("xml/signed-xml-example.xml");

        X509Certificate verifyCert = SecurityHelper.loadCert("certs/test-okta-public-key.pem");

        verifyBySantuario(doc, verifyCert);
        verifyByNativeJava(doc, verifyCert);
    }

    private static void verifyBySantuario(Document doc, X509Certificate certificate) throws Exception {
        Element signatureNode = getSignatureNode(doc.getDocumentElement());

        XMLSignature signature = new XMLSignature(signatureNode, "", true);
        boolean result = signature.checkSignatureValue(certificate.getPublicKey());

        System.out.println(String.format("Signature validation result is: %s", result));

        for(int i =0; i < signature.getSignedInfo().getLength(); i ++){
            boolean isRefValidated = signature.getSignedInfo().getVerificationResult(i);
            System.out.println(String.format("Reference[%s] validation result is %s", i, isRefValidated));
        }
    }

    private static void verifyByNativeJava(Document doc, X509Certificate certificate) throws Exception {
        Element signatureNode = getSignatureNode(doc.getDocumentElement());

        DOMValidateContext valContext = new DOMValidateContext(certificate.getPublicKey(), signatureNode);
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        javax.xml.crypto.dsig.XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        boolean result = signature.validate(valContext);

        System.out.println(String.format("Signature validation result is: %s", result));

        boolean sv = signature.getSignatureValue().validate(valContext);
        System.out.println(String.format("Signature value validation status: %s", sv));

        List refs = signature.getSignedInfo().getReferences();
        for(int i=0; i<refs.size(); i++) {
            Reference ref = (Reference)refs.get(i);
            boolean refValid = ref.validate(valContext);
            System.out.println(String.format("Reference[%s] validity status is %s", i, refValid));
        }
    }

    private static Element getSignatureNode(Element doc) throws Exception {
        Element[] signatureNodes = XMLHelper.selectDsNodes(doc.getFirstChild(), "Signature");

        if (signatureNodes.length == 0) {
            throw new Exception("Cannot find Signature element");
        }

        return signatureNodes[0];
    }
}
