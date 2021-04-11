package com.readjournal.xml;

import java.util.Collections;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.readjournal.util.CollectionUtil;
import com.readjournal.util.Utils;

public class XPath {
	private static final NamespaceContext EMPTY_NS_CONTEXT	= new NamespaceContextMap(Collections.emptyMap());
	private static final NamespaceContext SVG_NS_CONTEXT	= new NamespaceContextMap(CollectionUtil.toUnmodifiableMap("s", XMLDocument.SVG_NS, "x", XMLDocument.XLINK_NS));
	
	private Document document;
	private javax.xml.xpath.XPath xpath;
	private NamespaceContext namespaceContext;
	
	public XPath(Document document) {
		this.document = document;
		xpath = XPathFactory.newInstance().newXPath();
		if( namespaceContext!=null )
			xpath.setNamespaceContext(namespaceContext);
	}
	
	public void setNamespaceContext(String... prefixesAndUrls) {
		if( prefixesAndUrls==null || prefixesAndUrls.length==0 )
			throw new IllegalArgumentException("There should be at least two arguments");
		if( prefixesAndUrls.length%2!=0 )
			throw new IllegalArgumentException("arguments size should be folds of two");
		Map<String, String> map = CollectionUtil.toMap((Object[])prefixesAndUrls);
		setNamespaceContext(new NamespaceContextMap(map));
	}
	
	public void setSvgNamespaceContext() {
		setNamespaceContext(SVG_NS_CONTEXT);
	}
	
	public void setNamespaceContext(NamespaceContext namespaceContext) {
		this.namespaceContext = namespaceContext;
		if( xpath!=null )
			xpath.setNamespaceContext(namespaceContext!=null ? namespaceContext : EMPTY_NS_CONTEXT);
	}
	
	public NamespaceContext getNamespaceContext() {
		return namespaceContext;
	}
	
	public NodeList getNodeList(String path) {
		return getNodeList(document, path);
	}
	
	public NodeList getNodeList(Node node, String path) {
		try {
			return (NodeList)xpath.evaluate(path, node, XPathConstants.NODESET);
		}
		catch (XPathExpressionException e) {
			throw Utils.runtime(e);
		}
	}
	
	public Node getNode(String path) {
		return getNode(document, path);
	}
	
	public Node getNode(Node node, String path) {
		try {
			return (Node)xpath.evaluate(path, node, XPathConstants.NODE);
		}
		catch (XPathExpressionException e) {
			throw Utils.runtime(e);
		}
	}

	public String getString(Node node, String path, String def) {
		try {
			return (String)xpath.evaluate(path, node, XPathConstants.STRING);
		}
		catch (XPathExpressionException e) {
			return def;
		}		
	}

	public String getString(String path, String def) {
		return getString(document, path, def);		
	}

	public String getString(Node node, String path) {
		return getString(node, path, null);
	}

	public String getString(String path) {
		return getString(document, path, null);		
	}

	public Number getNumber(Node node, String path, Number def) {
		try {
			return (Number)xpath.evaluate(path, node, XPathConstants.NUMBER);
		}
		catch (XPathExpressionException e) {
			return def;
		}		
	}

	public Number getNumber(String path, Number def) {
		return getNumber(document, path, def);
	}

	public Boolean getBoolean(Node node, String path, boolean def) {
		try {
			return (Boolean)xpath.evaluate(path, node, XPathConstants.BOOLEAN);
		}
		catch (XPathExpressionException e) {
			return def;
		}		
	}

	public Boolean getBoolean(String path, boolean def) {
		return getBoolean(document, path, def);
	}
}
