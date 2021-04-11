package com.readjournal.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class DownloadClient {
	private String method;
	private String url;
	private int timeout;
	private KeyValuePairs params;
	private KeyValuePairs headers;
	private int responseCode;

	public DownloadClient(String method, String url) {
		this.method = method;
		this.url = url;
	}

	public DownloadClient method(String method) {
		this.method = method;
		return this;
	}

	public DownloadClient timeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	public DownloadClient addParam(String key, String value) {
		if( params==null )
			params = new KeyValuePairs();
		params.addPair(key, value);
		return this;
	}

	public DownloadClient addHeader(String key, String value) {
		if( headers==null )
			headers = new KeyValuePairs();
		headers.addPair(key, value);
		return this;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public byte[] download() throws IOException {
		String url = this.url;
		if( method.equals("GET") && params!=null )
			url += (url.indexOf('?')>=0 ? "&" : "?") + params.encode();
		HttpURLConnection con = HttpUtil.openConnection(method, url, timeout>0 ? timeout: 60_000);
		if( headers!=null )
			headers.writeHeaders(con);
		if( !method.equals("GET") && params!=null ) {
			byte[] data = params.encode().getBytes(StandardCharsets.UTF_8);
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
			con.setRequestProperty("Content-Length", "" + data.length);
			con.setDoOutput(true);
			try( BufferedOutputStream out = new BufferedOutputStream(con.getOutputStream()) ) {
				out.write( data );
			}
		}

		switch( responseCode = con.getResponseCode() ) {
			case HttpURLConnection.HTTP_MOVED_PERM:
			case HttpURLConnection.HTTP_MOVED_TEMP:
				String newUrl = con.getHeaderField("Location");
				if( !StringUtil.empty(newUrl) ) {
					if( !newUrl.startsWith("http://") && !newUrl.startsWith("https://") ) {
						if( newUrl.charAt(0)=='/' ) {
							String domain = HttpUtil.getDomainPath(url);
							newUrl = domain.substring(0, domain.length()-1) + newUrl;
						}
						else {
							newUrl = (url.charAt(url.length()-1)=='/' ? "" : "/") + newUrl;
						}
					}
					return HttpUtil.downloadUrl(newUrl);
				}
		}

		return HttpUtil.readConnection(con);
	}

	public static class KeyValuePairs {
		private List<KeyValuePair> pairs = new ArrayList<>(3);

		public KeyValuePairs addPair(String key, String value) {
			Objects.requireNonNull(key);
			Objects.requireNonNull(value);
			pairs.add(new KeyValuePair(key, value));
			return this;
		}

		public String encode() {
			try {
				StringBuilder sb = new StringBuilder();
				for(KeyValuePair pair : pairs) {
					sb.append(URLEncoder.encode(pair.key, "UTF-8")).append('=').append(URLEncoder.encode(pair.value, "UTF-8")).append('&');
				}
				if( sb.length()>0 )
					sb.setLength(sb.length()-1);
				return sb.toString();
			}
			catch(Exception ex) {
				throw Utils.runtime(ex);
			}
		}

		public void writeHeaders(URLConnection con) {
			try {
				Set<String> addeds = new HashSet<>();
				for(KeyValuePair pair : pairs) {
					if( addeds.contains( pair.key.toLowerCase(Locale.ENGLISH) ) )
						con.addRequestProperty(pair.key, pair.value);
					else {
						addeds.add( pair.key.toLowerCase(Locale.ENGLISH) );
						con.setRequestProperty(pair.key, pair.value);
					}
				}
			}
			catch(Exception ex) {
				throw Utils.runtime(ex);
			}
		}
	}

	private static class KeyValuePair {
		String key;
		String value;
		public KeyValuePair(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}
}
