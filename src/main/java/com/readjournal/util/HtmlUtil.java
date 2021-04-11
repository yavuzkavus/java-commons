package com.readjournal.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtil {
	public static String readTitle(File htmlFile) {
		String html = readHtml(htmlFile);
		int ind = html.indexOf("<title"),
			ind2 = -1;
		if( ind>0 ) {
			ind = html.indexOf('>', ind);
			if( ind!=-1 ) {
				ind2 = html.indexOf("</", ind);
			}
		}
		if( ind!=-1 && ind2!=-1 )
			return html.substring(ind+1, ind2);
		else
			return "";
	}

	public static String readHtml(File htmlFile) {
		return readHtml(htmlFile, false);
	}
	
	public static String readHtml(File htmlFile, boolean forceUtf8) {
		try {
			byte[] bytes = Files.readAllBytes(htmlFile.toPath());
			return readHtml(bytes, forceUtf8);
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	
	public static String readHtml(String url) {
		return readHtml(url, false);
	}
	
	public static String readHtml(String url, int timeout) {
		return readHtml(url, timeout, false);
	}
	
	public static String readHtml(String url, boolean forceUtf8) {
		return readHtml(url, 10_000, forceUtf8);
	}
	
	public static String readHtml(String url, int timeout, boolean forceUtf8) {
		try {
			byte[] data = HttpUtil.downloadUrl(url, timeout);
			return readHtml(data, forceUtf8);
		} 
		catch (IOException e) {
			throw Utils.runtime(e);
		}
	}
	
	public static String readHtml(String url, String... params) {
		return readHtml(url, 10_000, false, params);
	}
	
	public static String readHtml(String url, int timeout, String... params) {
		return readHtml(url, timeout, false, params);
	}
	
	public static String readHtml(String url, boolean forceUtf8, String... params) {
		return readHtml(url, 10_000, forceUtf8, params);
	}
	
	public static String readHtml(String url, int timeout, boolean forceUtf8, String... params) {
		try {
			byte[] data = HttpUtil.downloadUrl(url.toString(), timeout, params);
			return readHtml(data, forceUtf8);
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	
	public static String readHtml(byte[] bytes) {
		return readHtml(bytes, false);
	}
	
	public static String readHtml(byte[] bytes, boolean forceUtf8) {
		try {
			String html = new String(bytes, "UTF-8");
			Pattern pattern = Pattern.compile(
					"<meta\\s[^>]*charset=['\"]?([^';\"]+);?['\"][^>]*>|" +
							"^<\\?xml\\s[^>]*encoding=['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(html);
			if( !matcher.find() ) {
				html = new String(bytes, "UTF-8");
				if( forceUtf8 ) {
					int pos = html.indexOf("<head");
					pos +=5;
					char nextChar = html.length()<=pos ? '\0' : html.charAt(pos);
					if( nextChar=='>' ) {
						pos++;
					}
					else if( Character.isWhitespace(nextChar) ) {
						pos = html.indexOf('>', pos);
						if( pos>0 )
							pos++;
					}
					else {
						pos = -1;
					}
					if( pos!=-1 ) {
						html = html.substring(0, pos)
								+ "<meta charset=\"UTF-8\" />"
								+ html.substring(pos);
					}
				}
			}
			else {
				String encoding = matcher.group(1);
				if( encoding==null )
					encoding = matcher.group(2);
				encoding = encoding.trim().toUpperCase();
				Charset charset;
				try {
					charset = Charset.forName(encoding);
				}
				catch(Exception ex) {
					charset = Charset.defaultCharset();
				}
				html = new String(bytes, charset);
				if( forceUtf8 && !encoding.equals("UTF-8") ) {
					StringBuilder sb = new StringBuilder();
					//replace meta tag's charset
					matcher = pattern.matcher(html);
					int last = 0;
					while( matcher.find() ) {
						int start = matcher.group(1)!=null ? matcher.start(1) : matcher.start(2);
						sb.append( html.substring(last, start) );
						sb.append("UTF-8");
						last = matcher.group(1)!=null ? matcher.end(1) : matcher.end(2);
					}
					sb.append( html.substring(last) );
					html = sb.toString();
				}
			}
			return html;
		} 
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	
	public static String createEmptyHtml() {
		return createHtml("Index", "");
	}
	
	public static String createHtml(String title, String body) {
		return	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
				"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
				"<head>\n" +
					"\t<meta charset=\"UTF-8\"/>\n" +
					"\t<title>"+title+"</title>\n" +
				"</head>\n" +
				"<body>\n" + 
				body + "\n" +
				"</body>\n" + 
				"</html>";
	}
	
	public static String encryptHTML(String html, String key) {
		if( html==null || html.indexOf("<body")>=html.indexOf("</body") )
			return html;
		try {
			StringBuilder result = new StringBuilder();
			int i, j;
			
			i = html.indexOf("<body");
			if( (j=html.indexOf('>', i+1))<0 )
				return html;
			result.append( html.substring(0, ++j) );
			i = j;
			
			
			if( (j=html.indexOf("</body", i+1))<0 )
				return html;
			String body = html.substring(i, j);
			i = j;
			
			byte[] bytes = body.getBytes("UTF-8");
			byte[] cipher = key.getBytes("UTF-8");
			int keyLen = cipher.length;
			int charLen = bytes.length;
			int k;
			for(k=0; k<charLen; k++)
				bytes[k] ^= cipher[k%keyLen];
			result.append("<div style=\"display: none\" id=\"encryptedContent\">");
			result.append(Base64.encode(bytes));
			result.append("</div>");
			result.append( html.substring(i) );
			return result.toString();
		}
		catch(Exception ex) {
			//RJLogger.getLogger().severe(Developer.YAVUZ, html, ex);
			return html;
		}
	}
	
	public static String fixCommentedXmlHeader(String html) {
		//jsoup comments out xml header, fix it.
		if( html.startsWith("<!--?xml") ) {
			int ind = html.indexOf("?-->") ;
			if( ind>0 ) {
				html = "<?xml" + html.substring(8, ind) + "?>" + html.substring(ind + 4);
			}
		}
		return html;
	}
	
	public static void main(String[] args) throws Exception {
		String html = HtmlUtil.readHtml("http://yarinhaber.net/");
		System.out.println(html);
	}
}
