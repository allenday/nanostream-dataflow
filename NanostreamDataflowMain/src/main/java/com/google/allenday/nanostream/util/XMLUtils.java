package com.google.allenday.nanostream.util;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

public class XMLUtils {

    public static Document getDocumentFromXMLString(String xmlString)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xmlString));
        return builder.parse(is);
    }

    public static String getElementTextFromDocument(Document document, String elementName) {
        if (document != null && document.getDocumentElement().getElementsByTagName(elementName).getLength() > 0) {
            return document.getDocumentElement().getElementsByTagName(elementName).item(0).getTextContent();
        } else {
            return null;
        }
    }
}
