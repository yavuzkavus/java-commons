package com.readjournal.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.readjournal.util.HttpUtil;
import com.readjournal.util.StringUtil;
import com.readjournal.util.Utils;

public class XMLDocument {
	public static final String SVG_NS	= "http://www.w3.org/2000/svg";
	public static final String XLINK_NS	= "http://www.w3.org/1999/xlink";

	private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	static {
		try {
			dbf.setValidating(false);
			dbf.setExpandEntityReferences(true);
			dbf.setIgnoringElementContentWhitespace(true);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private Transformer transformer;
	private Document document;
	private String encoding = "UTF-8";

	public XMLDocument() {
		this(null, null, false);
	}

	public XMLDocument(String rootName) {
		this(rootName, null, false);
	}

	public XMLDocument(boolean namespaceAware) {
		this(null, null, namespaceAware);
	}

	public XMLDocument(String rootName, boolean namespaceAware) {
		this(rootName, null, namespaceAware);
	}

	public XMLDocument(String rootName, String namespaceUri) {
		this(rootName, namespaceUri, true);
	}

	public XMLDocument(String rootName, String namespaceUri, boolean namespaceAware) {
		this(rootName, namespaceUri, namespaceAware, null, null);
	}

	public XMLDocument(String rootName, String namespaceUri, boolean namespaceAware, String publicId, String systemId) {
		try {
			DocumentBuilder documentBuilder;
			synchronized(dbf) {
				dbf.setNamespaceAware(namespaceAware);
				documentBuilder = dbf.newDocumentBuilder();
			}

			DocumentType doctype = null;
			if(systemId!=null || publicId!=null)
				doctype = documentBuilder.getDOMImplementation().createDocumentType(rootName, publicId, systemId);
			document = documentBuilder.getDOMImplementation().createDocument(namespaceUri, rootName, doctype);
			//encoding = document.getXmlEncoding();
		}
		catch (ParserConfigurationException e) {
			throw Utils.runtime(e);
		}
	}

	public XMLDocument(Document document) {
		this.document = document;
		this.encoding = document.getXmlEncoding();
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public Document getDocument() {
		return document;
	}

	public Element getDocumentElement() {
		return document.getDocumentElement();
	}

	public Element createElement(String tagName) {
		return document.createElement(tagName);
	}

	public Element createElement(Element parent, String tagName) {
		return createElement(parent, tagName, null);
	}

	public Element createElement(String tagName, Object tagValue) {
		return createElement(null, tagName, tagValue);
	}

	public Element createElement(Element parent, String tagName, Object tagValue) {
		return createElementNS(null, parent, tagName, tagValue);
	}

	public Element createElementNS(String namespaceURI, String tagName) {
		return createElementNS(namespaceURI, tagName, null);
	}

	public Element createElementNS(String namespaceURI, Element element, String tagName) {
		return createElementNS(namespaceURI, element, tagName, null);
	}

	public Element createElementNS(String namespaceURI, String tagName, Object tagValue) {
		return createElementNS(namespaceURI, null, tagName, tagValue);
	}

	public Element createElementNS(String namespaceURI, Element parent, String tagName, Object tagValue) {
		Objects.requireNonNull(tagName);

		Element element = namespaceURI==null ? document.createElement(tagName) : document.createElementNS(namespaceURI, tagName);
		if( tagValue!=null )
			element.appendChild( document.createTextNode( clearXMLText(tagValue.toString())) );
		if(parent!=null)
			parent.appendChild(element);
		return element;
	}

	public void setAttributes(Element element, Object... props) {
		for(int i=0; i<props.length; i+=2)
			element.setAttribute(props[i].toString(), props[i+1].toString());
	}

	public void clearChildren(Element parent) {
		while( parent.hasChildNodes() )
			parent.removeChild( parent.getFirstChild() );
	}

	public int getIntegerAttr(Element element, String attrName, int def) {
		return Utils.toInt(element.getAttribute(attrName), def);
	}

	public long getLongAttr(Element element, String attrName, long def) {
		return Utils.toLong(element.getAttribute(attrName), def);
	}

	public float getFloatAttr(Element element, String attrName, float def) {
		return Utils.toFloat(element.getAttribute(attrName), def);
	}

	public double getDoubleAttr(Element element, String attrName, double def ) {
		return Utils.toDouble(element.getAttribute(attrName), def);
	}

	public boolean getBooleanAttr(Element element, String attrName, boolean def) {
		return Utils.toBoolean(element.getAttribute(attrName), def);
	}

	public int getIntegerVal(Element element, int def) {
		return Utils.toInt(element.getTextContent().trim(), def);
	}

	public long getLongVal(Element element, long def) {
		return Utils.toLong(element.getTextContent().trim(), def);
	}

	public float getFloatVal(Element element, float def) {
		return Utils.toFloat(element.getTextContent().trim(), def);
	}

	public double getDoubleVal(Element element, double def) {
		return Utils.toDouble(element.getTextContent().trim(), def);
	}

	public boolean getBooleanVal(Element element, boolean def) {
		return Utils.toBoolean(element.getTextContent().trim(), def);
	}

	public Element getDirectChild(Element parent, String tagName) {
		NodeList nodeList = parent.getChildNodes();
		for(int i=0, len=nodeList.getLength(); i<len; i++) {
			Node node = nodeList.item(i);
			if( node instanceof Element && tagName.equals(((Element)node).getTagName()) )
				return (Element)node;
		}
		return null;
	}

	public String getDirectChildText(Element el, String tagName) {
		Element child = getDirectChild(el, tagName);
		if( child==null || child.getFirstChild()==null )
			return "";
		return child.getFirstChild().getTextContent().trim();
	}

	public Element getChild(Element el, String tagName) {
		NodeList nodeList = el.getElementsByTagName(tagName);
		if( nodeList==null || nodeList.getLength()==0 )
			return null;
		return (Element)nodeList.item(0);
	}

	public String getChildText(Element el, String tagName) {
		Element child = getChild(el, tagName);
		if( child==null || child.getFirstChild()==null )
			return "";
		return child.getFirstChild().getTextContent().trim();
	}

	public String getXMLText() throws TransformerFactoryConfigurationError, TransformerException {
		return getXMLText(getDocumentElement());
	}

	public String getXMLText(Node node) throws TransformerFactoryConfigurationError, TransformerException {
		StringWriter writer = new StringWriter();
		transform2(node, writer);
		return writer.toString();
	}

	/*  XML parsing from different sources */
	public static XMLDocument parseFromText(String text){
		return parseFromText(text, false);
	}

	public static XMLDocument parseFromText(String text, boolean namespaceAware){
		StringReader reader = new StringReader(text);
		return parse2( new InputSource(reader), namespaceAware );
	}

	public static XMLDocument parse(File file){
		return parse2(file, false);
	}

	public static XMLDocument parse(File file, boolean namespaceAware){
		return parse2(file, namespaceAware);
	}

	public static XMLDocument parse(String file) {
		return parse2(file, false);
	}

	public static XMLDocument parse(String file, boolean namespaceAware) {
		return parse2(file, namespaceAware);
	}

	public static XMLDocument parse(InputSource is) {
		return parse2(is, false);
	}

	public static XMLDocument parse(InputSource is, boolean namespaceAware) {
		return parse2(is, namespaceAware);
	}

	public static XMLDocument parse(InputStream is) {
		return parse2(is, false);
	}

	public static XMLDocument parse(InputStream is, boolean namespaceAware) {
		return parse2(is, namespaceAware);
	}

	public static XMLDocument parse(URL url) {
		return parse(url, false, 0);
	}

	public static XMLDocument parse(URL url, int timeout) {
		return parse(url, false, timeout);
	}

	public static XMLDocument parse(URL url, boolean namespaceAware) {
		return parse(url, namespaceAware, 0);
	}

	public static XMLDocument parse(URL url, boolean namespaceAware, int timeout) {
		try {
			if( timeout<=0 )
				timeout = 15_000;
			byte data[] = HttpUtil.downloadUrl(url.toString(), timeout);
			XMLDocument xml = parse2( new ByteArrayInputStream(data), namespaceAware );
			return xml;
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	private static XMLDocument parse2(Object source, boolean namespaceAware) {
		try {
			DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
			synchronized(dbf) {
				dbf.setNamespaceAware(namespaceAware);
				documentBuilder = dbf.newDocumentBuilder();
			}
			Document document = null;
			if( source instanceof InputStream )
				document = documentBuilder.parse( (InputStream)source );
			else if( source instanceof InputSource )
				document = documentBuilder.parse( (InputSource)source );
			else if( source instanceof String )
				document = documentBuilder.parse( new File((String)source) );
			else if( source instanceof File )
				document = documentBuilder.parse( (File)source );
			return new XMLDocument( document );
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/* Transforming XML to different destinations */
	public void transform(OutputStream os) throws TransformerFactoryConfigurationError, TransformerException {
		transform2(getDocumentElement(), os);
	}

	public void transform(Writer writer) throws TransformerFactoryConfigurationError, TransformerException {
		transform2(getDocumentElement(), writer);
	}

	public void transform(String filePath) throws TransformerFactoryConfigurationError, TransformerException {
		transform2(getDocumentElement(), filePath);
	}

	public void transform(File file) throws TransformerFactoryConfigurationError, TransformerException {
		transform2(getDocumentElement(), file);
	}

	private void transform2(Node node, Object stream) throws TransformerFactoryConfigurationError, TransformerException {
		Source source = new DOMSource(node);
		Result result;
		if( OutputStream.class.isInstance(stream) )
			result = new StreamResult( (OutputStream)stream );
		else if( Writer.class.isInstance(stream) )
			result = new StreamResult( (Writer)stream );
		else if( File.class.isInstance(stream) )
			result = new StreamResult((File)stream);
		else if( String.class.isInstance(stream) )
			result = new StreamResult( new File((String)stream) );
		else
			throw new IllegalArgumentException("not illegal argument to transform");
		//my be we need synchronize below
		getTransformer().transform(source, result);
	}

	private final Transformer getTransformer() throws TransformerConfigurationException, TransformerFactoryConfigurationError {
		if( transformer==null ) {
			try {
				transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
				transformer.setOutputProperty(OutputKeys.ENCODING, StringUtil.ifEmpty(encoding, "UTF-8"));
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				//transformer.setOutputProperty(OutputKeys.VERSION, StringUtil.ifEmpty(document.getXmlVersion(), "1.1"));
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
				if( document.getDoctype()!=null ) {
					transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, document.getDoctype().getSystemId());
					transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, document.getDoctype().getPublicId());
				}
			}
			catch (Exception e) {
				throw Utils.runtime(e);
			}
		}
		return transformer;
	}

	private static String clearXMLText(String str) {
		if(str==null)
			return null;

		StringBuilder sb = null;
		for(int i=0, len=str.length(); i<len; i++) {
			int code = str.codePointAt(i);
			if( code!=0x9 && code!=0xA && code!=0xD &&
					(code<0x20 || 0xD7FF<code) &&
					(code<0xE000 || 0xFFFD<code) &&
					(code<0x10000 || 0x10FFFF<code) ) {
				if( sb==null ) {
					sb = new StringBuilder(str.substring(0, i));
					sb.append('?');
				}
			}
			else if( sb!=null ) {
				sb.append((char)code);
			}
		}
		return sb==null ? str : sb.toString();
	}
}
