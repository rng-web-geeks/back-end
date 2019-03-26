package com.ringcentral.demo.xml.utils;

import com.ringcentral.demo.utils.IOHelper;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

public final class XMLHelper {

    public static Document loadXML(String location) throws IOException, ParserConfigurationException, SAXException {
        try(InputStream is = IOHelper.loadFromResource(location)) {
            if(null != is) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                Document doc = dbf.newDocumentBuilder().parse(is);
                doc.getDocumentElement().setIdAttribute("ID", true);
                return doc;
            }
        }
        return null;
    }

    public static Element getNextElement(Node el) {
        return XMLUtils.getNextElement(el);
    }

    public static String getFullTextChildrenFromElement(Element el) {
        return XMLUtils.getFullTextChildrenFromElement(el);
    }

    public static Element[] selectDsNodes(Node sibling, String nodeName) {
        return XMLUtils.selectDsNodes(sibling, nodeName);
    }

    public static boolean isDescendantOrSelf(Node current, Node target) {
       return XMLUtils.isDescendantOrSelf(current, target);
    }
}
