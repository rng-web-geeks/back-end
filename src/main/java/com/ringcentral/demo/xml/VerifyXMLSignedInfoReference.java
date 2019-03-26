package com.ringcentral.demo.xml;

import com.ringcentral.demo.utils.CertificateHelper;
import com.ringcentral.demo.xml.utils.XMLHelper;
import org.apache.xml.security.c14n.implementations.Canonicalizer20010315ExclOmitComments;
import org.apache.xml.security.signature.NodeFilter;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;

public class VerifyXMLSignedInfoReference {

    public static void main(String[] args) throws Exception {
        System.setProperty("org.apache.xml.security.ignoreLineBreaks", "true");
        org.apache.xml.security.Init.init();


        Document doc = XMLHelper.loadXML("xml/signed-xml-example.xml");

        X509Certificate verifyCert = CertificateHelper.loadCert("certs/okta.pem");

        Element signatureNode = getSignatureNode(doc.getDocumentElement());
        Element signInfoNode = XMLHelper.getNextElement(signatureNode.getFirstChild());

        Element[] references = getReferences(signInfoNode);
        for (Element ref: references) {
            verifyReference(ref, doc);
        }
    }

    private static void verifyReference(Element reference, Document doc) throws Exception {
        Element node = getRefNode(reference);

        byte[] c14nData = getCanonicalizedXML(node, doc);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();

        byte[] calcDigest =digest.digest(c14nData);

        byte[] orignalDigest = Base64.getDecoder().decode(getDigestValue(reference));

        System.out.println(String.format("Reference %s validation result: %s", reference.getAttribute("URI"), Arrays.equals(calcDigest, orignalDigest)));
    }

    private static String getDigestValue(Element reference) throws Exception {
        Element[] digestValue = XMLHelper.selectDsNodes(reference.getFirstChild(), "DigestValue");
        if (digestValue.length != 1) {
            throw new Exception("Cannot find DigestValue element");
        }
        return digestValue[0].getTextContent();
    }

    private static byte[] getCanonicalizedXML(Element node, Document doc)  throws Exception  {
        Element signatureNode = getSignatureNode(doc.getDocumentElement());

        XMLSignatureInput input = new XMLSignatureInput(doc.getFirstChild());
        input.setExcludeComments(true);
        input.setExcludeNode(signatureNode);
        input.setSecureValidation(true);
        input.addNodeFilter(new NodeFilter() {
            @Override
            public int isNodeInclude(Node n) {
                if (n == input.getExcludeNode()) {
                    return -1;
                }
                return 1;
            }

            @Override
            public int isNodeIncludeDO(Node n, int level) {
                if (n == input.getExcludeNode() ||
                        XMLHelper.isDescendantOrSelf(input.getExcludeNode(), n)) {
                    return -1;
                }
                return 1;
            }
        });

        Canonicalizer20010315ExclOmitComments c14n = new Canonicalizer20010315ExclOmitComments();
        c14n.setSecureValidation(true);
        return c14n.engineCanonicalize(input, "xs");
    }


    private static Element getRefNode(Element reference) {
        String uri = reference.getAttribute("URI");
        Element result = null;
        if(null == uri || uri.trim().length() ==0) {
            result = reference.getOwnerDocument().getDocumentElement();
        } else {
            result = reference.getOwnerDocument().getElementById(uri.substring(1));
        }
        return result;
    }

    private static Element[] getReferences(Element signInfoNode) {
        return XMLHelper.selectDsNodes(signInfoNode.getFirstChild(), "Reference");
    }

    private static Element getSignatureNode(Element doc) throws Exception {
        Element[] signatureNodes = XMLHelper.selectDsNodes(doc.getFirstChild(), "Signature");

        if (signatureNodes.length == 0) {
            throw new Exception("Cannot find Signature element");
        }

        return signatureNodes[0];
    }
}
