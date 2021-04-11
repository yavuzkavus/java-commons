package com.readjournal.util;

import static com.readjournal.util.StringUtil.NEW_LINE;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class HttpUtil {
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:80.0) Gecko/20100101 Firefox/80.0";

	public static final Set<String> RESOURCE_EXTENSIONS = CollectionUtil.asSet(
						"jpeg",
						"jpg",
						"png",
						"gif",
						"bmp",
						"svg",
						"class",
						"cgi",
						"js",
						"json",
						"css",
						"xcss",
						"swf",
						"pdf",
						"ico");

	public final static SSLContext insecureSslContext;
	public final static SSLSocketFactory insecureSslSocketFactory;
	public final static HostnameVerifier insecureVerifier = new HostnameVerifier() {
		@Override
		public boolean verify(String urlHostName, SSLSession session) {
			return true;
		}
	};

	// Create a trust manager that does not validate certificate chains
	public static final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
		@Override
		public void checkClientTrusted(final X509Certificate[] chain, final String authType) { }

		@Override
		public void checkServerTrusted(final X509Certificate[] chain, final String authType) { }

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}};

	static {
		/**
		 * Initialise Trust manager that does not validate certificate chains and
		 * add it to current SSLContext.
		 * <p/>
		 * please not that this method will only perform action if sslSocketFactory is not yet
		 * instantiated.
		 *
		 * @throws IOException
		 */
		// Install the all-trusting trust manager
		try {
			insecureSslContext = SSLContext.getInstance("SSL");
			insecureSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			// Create an ssl socket factory with our all-trusting manager
			insecureSslSocketFactory = insecureSslContext.getSocketFactory();
		}
		catch (NoSuchAlgorithmException|KeyManagementException e) {
			throw Utils.runtime(e);
		}
	}

	public static String getDomainPath(String url) {
		int slashCount = url.indexOf("://")>=0  ? 3 : 1;
		int x = -1;
		for(int i=0; i<slashCount; i++) {
			int k = url.indexOf("/", x+1);
			if(k==-1) {
				x = url.length();
				break;
			}
			x = k;
		}
		if( x == -1 )
			return url;
		else
			return url.substring(0, x);
	}

	public static String extractDomain(String url) {
		StringBuilder sb = new StringBuilder(url);
		int ind;
		if( (ind=sb.indexOf("://"))>=0 )
			sb.delete(0, ind+3);
		if( (ind=sb.indexOf("/"))>0 )
			sb.delete(ind, sb.length());
		return sb.toString();
	}

	public static String encodeParam(final String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (Exception e) {}
		return str;
	}

	public static String decodeParam(final String str) {
		try {
			return URLDecoder.decode(str, "UTF-8");
		} catch (Exception e) {}
		return str;
	}

	public static boolean isResource(String path) {
		String ext = getExtension(path);
		return !StringUtil.empty(ext) && RESOURCE_EXTENSIONS.contains(ext);
	}

	public static String getExtension(String path) {
		int ind = path.indexOf('?');
		if( ind>=0 )
			path = path.substring(0, ind);
		return FileUtil.getExtension(path);
	}

	public static HttpURLConnection openConnection(String method, String url) throws MalformedURLException, IOException {
		return openConnection(method, url, 10_000);
	}

	public static HttpURLConnection openConnection(String method, String url, int timeout) throws MalformedURLException, IOException {
		HttpURLConnection con = (HttpURLConnection)new URL(url).openConnection();
		con.setRequestMethod(method);
		con.setRequestProperty("user-agent", USER_AGENT);
		con.setRequestProperty("host", extractDomain(url));
		con.setRequestProperty("Referer", getDomainPath(url));
		//con.setRequestProperty("Connection", "keep-alive");
		//con.setRequestProperty("Connection", "close");
		//con.setRequestProperty("Accept-Encoding", "identity");
		con.setRequestProperty("Accept-Encoding", "gzip, deflate");
		con.setConnectTimeout(timeout);
		con.setReadTimeout(timeout);
		if (con instanceof HttpsURLConnection)
			disableSslVerification((HttpsURLConnection)con); //from jsoup
		return con;
	}

	public static InputStream getInputStream(HttpURLConnection con) throws IOException {
		InputStream in = null;
		try {
			in = con.getInputStream();
		}
		catch(Exception ex) {
			in = con.getErrorStream();
		}
		String contentEncoding = con.getHeaderField("Content-Encoding");
		if( contentEncoding!=null && "gzip".equals( contentEncoding.toLowerCase(Locale.ENGLISH) ) )
			in = new GZIPInputStream(in);
		else if( contentEncoding!=null && "deflate".equals( contentEncoding.toLowerCase(Locale.ENGLISH) ) )
			in = new DeflaterInputStream(in);
		return in;
	}

	public static byte[] readConnection(HttpURLConnection con) {
		try( InputStream in = getInputStream(con) ) {
			//closed, because some response contains less bytes than content-length header
			/*
			int len = con.getContentLength();
			if( len>0 ) {
				byte[] data = new byte[len];
				int next = 0, n;
				while( (n = in.read(data, next, Math.min(4096, len-next)))!=-1 ) {
					next += n;
				}
				return data;
			}
			else {
				return FileUtil.toByteArray(in);
			}
			*/
			return FileUtil.toByteArray(in);
		}
		catch(Exception e) {
			throw Utils.runtime(e);
		}
	}

	public static byte[] downloadUrl(String url) throws IOException {
		return downloadUrl(url, 10_000);
	}

	public static byte[] downloadUrl(String url, int timeout) throws IOException {
		HttpURLConnection con = openConnection("GET", url, timeout);
		switch( con.getResponseCode() ) {
			case HttpURLConnection.HTTP_MOVED_PERM:
			case HttpURLConnection.HTTP_MOVED_TEMP:
				String newUrl = con.getHeaderField("Location");
				if( !StringUtil.empty(newUrl) ) {
					if( !newUrl.startsWith("http://") && !newUrl.startsWith("https://") ) {
						if( newUrl.charAt(0)=='/' ) {
							String domain = getDomainPath(url);
							newUrl = domain.substring(0, domain.length()-1) + newUrl;
						}
						else {
							newUrl = (url.charAt(url.length()-1)=='/' ? "" : "/") + newUrl;
						}
					}
					return downloadUrl(newUrl);
				}
		}

		return readConnection(con);
	}

	public static byte[] downloadUrl(String url, String... params) throws IOException {
		return downloadUrl(url, 10_000, params);
	}

	public static byte[] downloadUrl(String url, int timeout, String... params) throws IOException {
		if( params.length%2!=0 )
			throw new IllegalArgumentException("params length must be two folds");
		HttpURLConnection con = openConnection("POST", url, timeout);
		if( params.length>0 ) {
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<params.length; i+=2)
				sb.append(URLEncoder.encode(params[i], "UTF-8")).append('=').append(URLEncoder.encode(params[i+1], "UTF-8")).append('&');
			sb.setLength(sb.length()-1);
			byte[] data = sb.toString().getBytes("UTF-8");
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
			con.setRequestProperty("Content-Length", "" + data.length);
			con.setDoOutput(true);
			try( BufferedOutputStream out = new BufferedOutputStream(con.getOutputStream()) ) {
				out.write( data );
				//out.flush();
				//out.close();
			}
		}
		switch( con.getResponseCode() ) {
			case HttpURLConnection.HTTP_MOVED_PERM:
			case HttpURLConnection.HTTP_MOVED_TEMP:
				String newUrl = con.getHeaderField("Location");
				if( !StringUtil.empty(newUrl) ) {
					if( !newUrl.startsWith("http://") && !newUrl.startsWith("https://") ) {
						if( newUrl.charAt(0)=='/' ) {
							String domain = getDomainPath(url);
							newUrl = domain.substring(0, domain.length()-1) + newUrl;
						}
						else {
							newUrl = (url.charAt(url.length()-1)=='/' ? "" : "/") + newUrl;
						}
					}
					return downloadUrl(newUrl, params);
				}
		}

		return readConnection(con);
	}

	public static String getLastUrl(String url) {
		return getLastUrl(url, 5_000, 5);
	}

	public static String getLastUrl(String url, int timeout) {
		return getLastUrl(url, timeout, 5);
	}

	public static String getLastUrl(String url, int timeout, int remainingReq) {
		try {
			HttpURLConnection con = openConnection("HEAD", url, timeout);
			con.setInstanceFollowRedirects(false);
			switch( con.getResponseCode() ) {
				case HttpURLConnection.HTTP_MOVED_PERM:
				case HttpURLConnection.HTTP_MOVED_TEMP:
					String newUrl = con.getHeaderField("Location");
					if( newUrl.equals(url) || remainingReq <= 0)
						return url;
					if( !StringUtil.empty(newUrl) ) {
						if( newUrl.startsWith("//") ) {
							newUrl = url.substring(0, url.indexOf(':')+1) + newUrl;
							if( newUrl.equals(url))
								return url;
						}
						else if( !newUrl.startsWith("http://") && !newUrl.startsWith("https://") ) {
							if( newUrl.charAt(0)=='/' ) {
								String domain = getDomainPath(url);
								newUrl = domain.substring(0, domain.length()-1) + newUrl;
							}
							else {
								newUrl = (url.charAt(url.length()-1)=='/' ? "" : "/") + newUrl;
							}
						}
						remainingReq--;
						return getLastUrl(newUrl, timeout, remainingReq);
					}
			}
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
		return url;
	}

	public static boolean urlExists(String url) throws IOException {
		return urlExists(url, 5_000);
	}

	public static boolean urlExists(String url, int timeout) throws IOException {
		HttpURLConnection con = null;
		try {
			con = openConnection("HEAD", url, timeout);
			return con.getResponseCode()/100 == 2;
		}
		catch(Exception ex) {
			try {
				if( con.getResponseCode()/100 == 4 )
					return false;
			}
			catch(Exception ex2) { }
			throw ex;
		}
	}

	public static void disableSslVerification(HttpsURLConnection con) {
		if( HttpsURLConnection.getDefaultHostnameVerifier()!=insecureVerifier )
			con.setHostnameVerifier(insecureVerifier);
		if( HttpsURLConnection.getDefaultSSLSocketFactory()!=insecureSslSocketFactory )
			con.setSSLSocketFactory(insecureSslSocketFactory);
	}


	public static void disableSslVerification() {
		if( HttpsURLConnection.getDefaultHostnameVerifier()!=insecureVerifier )
			HttpsURLConnection.setDefaultHostnameVerifier(insecureVerifier);
		if( HttpsURLConnection.getDefaultSSLSocketFactory()!=insecureSslSocketFactory )
			HttpsURLConnection.setDefaultSSLSocketFactory(insecureSslSocketFactory);
	}

	public static boolean isThisMyIpAddress(String ip) {
		try {
			// Check if the address is a valid special local or loop back
			InetAddress addr = InetAddress.getByName(ip);
			if( addr.isAnyLocalAddress() || addr.isLoopbackAddress() )
				return true;

			//Check if the address is defined on any interface
			if( NetworkInterface.getByInetAddress(addr)!=null )
				return true;

			return false;
		}
		catch (Exception e) {
			return false;
		}
	}

	public static boolean matchIps(String ip1, String ip2) {
		if( "*".equals(ip1) )
			return true;
		String ipParts1[] = ip1.split("\\."),
			   ipParts2[] = ip2.split("\\.");
		if( ipParts1.length!=4 || ipParts2.length!=4 )
			return false;
		for(int i=0; i<4; i++) {
			String ipPart1 = ipParts1[i].trim(),
				   ipPart2 = ipParts2[i].trim();
			if( !ipPart1.equals("*") && Utils.toInt(ipPart1, -1)<0 ||
					!ipPart2.equals("*") && Utils.toInt(ipPart2, -1)<0 ||
					!ipPart1.equals("*") && !ipPart2.equals("*") && Integer.parseInt(ipPart1)!=Integer.parseInt(ipPart2) )
				return false;
		}
		return true;
	}

	public static String forScript(String str) {
		if(str==null)
			return "";
		StringBuilder sb = new StringBuilder( (int)(1.3*str.length()) );
		char ch;
		for(int i=0; i<str.length(); i++) {
			ch = str.charAt(i);
			switch (ch) {
			case 10 :
				if( i>0 && str.charAt(i-1)==13 ) {
					break;
				}
				sb.append("\\n"); break;
			case 13 : sb.append("\\n"); break;
			case '"' :
			case '\'' :
			case '\\' : sb.append("\\"+ch); break;
			default : sb.append( ch );
			}
		}
		return sb.toString();
	}

	public static String forHtml(String str) {
		if(str==null)
			return "";
		StringBuilder sb = new StringBuilder( 2*str.length() );
		char ch;
		for(int i=0; i<str.length(); i++) {
			ch = str.charAt(i);
			if( ch==10 || ch==13 || ch=='\'' || ch=='"' || ch=='\\' || ch=='<' || ch=='>') {
				sb.append("&#"+ ((int)ch)+";" );
			}
			else {
				sb.append( ch );
			}
		}
		return sb.toString();
	}

	public static void printRequestHeaders(URLConnection con) {
		printRequestHeaders(con, System.out );
	}

	public static void printRequestHeaders(URLConnection con, OutputStream out) {
		if( out==System.out ) {
			System.out.print( getRequestHeaders(con) );
			return;
		}
		printRequestHeaders(con, new OutputStreamWriter(out));
	}

	public static void printRequestHeaders(URLConnection con, Writer writer) {
		try {
			writer.write(  getRequestHeaders(con) );
			writer.flush();
		}
		catch (Exception e) { e.printStackTrace(); }
	}

	public static String getRequestHeaders(URLConnection con) {
		StringBuilder sb = new StringBuilder();
		for(Entry<String, List<String>> entry : con.getRequestProperties().entrySet())
			sb.append( entry.getKey() ).append(" = ").append(entry.getValue()).append( NEW_LINE );
		return sb.toString();
	}

	public static void printResponseHeaders(URLConnection con) {
		printResponseHeaders(con, System.out );
	}

	public static void printResponseHeaders(URLConnection con, OutputStream out) {
		if( out==System.out ) {
			System.out.print( getResponseHeaders(con) );
			return;
		}
		printRequestHeaders(con, new OutputStreamWriter(out));
	}

	public static void printResponseHeaders(URLConnection con, Writer writer) {
		try {
			writer.write(  getResponseHeaders(con) );
		}
		catch (Exception e) { e.printStackTrace(); }
	}

	public static String getResponseHeaders(URLConnection con) {
		StringBuilder sb = new StringBuilder();
		for(Entry<String, List<String>> entry : con.getHeaderFields().entrySet())
			sb.append( entry.getKey() ).append(" = ").append(entry.getValue()).append( NEW_LINE );
		return sb.toString();
	}
}
