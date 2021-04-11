package com.readjournal.util;

import static com.readjournal.util.StringUtil.NEW_LINE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.time.FastDateFormat;

public class RequestUtil {
	public static int getIntegerParameter(HttpServletRequest request, String paramName, int defaultVal) {
		try {
			return Integer.parseInt( request.getParameter(paramName) );
		} catch(Exception ex) {
			return defaultVal;
		}
	}

	public static int[] getIntegerParameters(HttpServletRequest request, String paramName) {
		try {
			String strValues[] = getStringParameters(request, paramName);
			if( strValues==null )
				return null;
			int intValues[] = new int[strValues.length];
			int i = 0;
			for(String str : strValues)
				intValues[i++] = Integer.parseInt( str );
			return intValues;
		} catch(Exception ex) {
			return null;
		}
	}

	public static long getLongParameter(HttpServletRequest request, String paramName, long defaultVal) {
		try {
			return Long.parseLong( request.getParameter(paramName) );
		} catch(Exception ex) {
			return defaultVal;
		}
	}

	public static long[] getLongParameters(HttpServletRequest request, String paramName) {
		try {
			String strValues[] = getStringParameters(request, paramName);
			if( strValues==null )
				return null;
			long longValues[] = new long[strValues.length];
			int i = 0;
			for(String str : strValues)
				longValues[i++] = Long.parseLong( str );
			return longValues;
		} catch(Exception ex) {
			return null;
		}
	}

	public static Date getDateParameter(HttpServletRequest request, String paramName, String pattern, Date defaultVal){
		return getDateParameter(request, paramName, FastDateFormat.getInstance(pattern), defaultVal);
	}

	public static Date getDateParameter(HttpServletRequest request, String paramName, FastDateFormat fdf, Date defaultVal){
		try {
			String dateStr = request.getParameter(paramName);
			if(StringUtil.empty(dateStr))
				return defaultVal;
			return fdf.parse(dateStr);
		} catch(Exception ex) {
			return defaultVal;
		}
	}

	public static Date getDateParameter(HttpServletRequest request, String paramName, SimpleDateFormat sdf, Date defaultVal){
		try {
			String dateStr = request.getParameter(paramName);
			if(StringUtil.empty(dateStr))
				return defaultVal;
			return sdf.parse(dateStr);
		} catch(Exception ex) {
			return defaultVal;
		}
	}

	public static String getStringParameter(HttpServletRequest request, String paramName, String defaultVal) {
		String val = request.getParameter(paramName);
		return StringUtil.empty(val) ? defaultVal : val;
	}

	public static String[] getStringParameters(HttpServletRequest request, String paramName) {
		try {
			String strValues[] = request.getParameterValues(paramName);
			if( strValues==null || strValues.length==0 )
				strValues = request.getParameterValues(paramName + "[]");
			if( strValues==null || strValues.length==0 ||
					(strValues.length==1 && StringUtil.empty(strValues[0])))
				return null;
			if( strValues.length==1 && strValues[0].startsWith("[") && strValues[0].endsWith("]") ) {
				strValues = strValues[0].substring(1, strValues[0].length()-1).split("\\s*,\\s*");
			}
			return strValues;
		} catch(Exception ex) {
			return null;
		}
	}

	public static boolean getBooleanParameter(HttpServletRequest request, String paramName, boolean defaultVal) {
		try {
			String paramVal = request.getParameter(paramName);
			if( paramVal==null )
				return defaultVal;
			return Boolean.parseBoolean( paramVal );
		} catch(Exception ex) {
			return defaultVal;
		}
	}

	public static String getCookie( final HttpServletRequest request, final String cookieName) {
		Cookie[] cookies = request.getCookies();
		if(cookies==null || cookies.length<1)
			return null;
		for (Cookie element : cookies)
			if(element.getName().equalsIgnoreCase(cookieName))
				return element.getValue().replaceAll("\\_", "\\=");
		return null;
	}

	/**
	 * set cookie to response
	 * @param response
	 * @param cookieName
	 * @param cookieValue
	 * @param seconds
	 * 				0   :delete right now,
	 * 				0 > : expire after browser closed,
	 * 				0 < : delete after a time as value in seconds
	 */
	public static void setCookie(HttpServletResponse response, String cookieName, String cookieValue, int seconds) {
		Cookie cookie = new Cookie(cookieName, cookieValue.replaceAll("\\=", "\\_") );
		cookie.setPath( "/" );
		cookie.setMaxAge(seconds);
		response.addCookie(cookie);
	}
	
	public static void setCookieInDays(HttpServletResponse response, String cookieName, String cookieValue, int days) {
		setCookie(response, cookieName, cookieValue, days*24*60*60);
	}
	
	public static void setCookieInHours(HttpServletResponse response, String cookieName, String cookieValue, int hours) {
		setCookie(response, cookieName, cookieValue, hours*60*60);
	}
	
	public static void setCookieInMinutes(HttpServletResponse response, String cookieName, String cookieValue, int minutes) {
		setCookie(response, cookieName, cookieValue, minutes*60);
	}
	
	public static void deleteCookie(HttpServletResponse response, String cookieName) {
		setCookie(response, cookieName, "", 0);
	}
	
	public static final boolean isMultipartRequest(HttpServletRequest request) {
		if( !"POST".equalsIgnoreCase(request.getMethod()) )
			return false;
		String contentType = request.getContentType();
		if( contentType!=null && contentType.toLowerCase(Locale.ENGLISH).startsWith("multipart/"))
			return true;
		return false;
	}
	public static String getFormattedRequestParameters( final HttpServletRequest request ) {
		return getFormattedRequestParameters(request, NEW_LINE);
	}

	public static String getFormattedRequestParameters( final HttpServletRequest request, String seperator ) {
		if( isMultipartRequest(request) )
			return "";

		StringBuilder buf = new StringBuilder();
		Enumeration<String> paramNames = request.getParameterNames();
		while( paramNames.hasMoreElements() ) {
			String name = paramNames.nextElement();
			String[] values = request.getParameterValues(name);
			if( values==null || values.length==0 )
				continue;
			if ( buf.length() > 0) {
				buf.append(seperator);
			}
			buf.append( name ).append('=');
			for( int i=0; i<values.length; i++ ) {
				String value = values[i];
				if( name.indexOf("password")==-1 )
					buf.append( value );
				else
					buf.append("*****");
				if( i<values.length-1 )
					buf.append("; ");
			}
		}
		return buf.toString();
	}

	public static String getRequestParameters( final HttpServletRequest request ) {
		StringBuilder buf = new StringBuilder();
		Enumeration<String> paramNames = request.getParameterNames();
		while( paramNames.hasMoreElements() ) {
			if ( buf.length() > 0) {
				buf.append("&");
			}
			String name = paramNames.nextElement();
			String value = request.getParameter(name);
			buf.append( HttpUtil.encodeParam(name) );
			buf.append('=');
			buf.append( HttpUtil.encodeParam(value) );
		}
		return buf.toString();
	}

	public static String getDomainPath(final HttpServletRequest request) {
		return HttpUtil.getDomainPath(request.getRequestURL().toString());
	}

	public static String getFullContext(final HttpServletRequest request) {
		return 	RequestUtil.getDomainPath(request) + request.getContextPath();
	}

	public static void printRequestHeaders(HttpServletRequest request) {
		printRequestHeaders(request, System.out );
	}

	public static void printRequestHeaders(HttpServletRequest request, OutputStream out) {
		if( out==System.out )
			System.out.print(getRequestHeaders(request));
		else {
			printRequestHeaders(request, new OutputStreamWriter(out));
		}
	}

	public static void printRequestHeaders(HttpServletRequest request, Writer writer) {
		try {
			writer.write( getRequestHeaders(request) );
			writer.flush();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static String getFormattedRequestHeaders( final HttpServletRequest request, String seperator ) {
		StringBuilder buf = new StringBuilder();
		Enumeration<String> headerNames = request.getHeaderNames();
		while( headerNames.hasMoreElements() ) {
			if ( buf.length() > 0)
				buf.append(seperator);
			String name = headerNames.nextElement();
			Enumeration<String> values = request.getHeaders(name);
			buf.append( name ).append('=');
			while(values.hasMoreElements()) {
				buf.append( values.nextElement() );
				if( values.hasMoreElements() )
					buf.append("; ");
			}
		}
		return buf.toString();
	}

	public static String getRequestHeaders(HttpServletRequest request) {
		StringBuilder sb = new StringBuilder();
		Enumeration<String> headerNames = request.getHeaderNames();
		while(headerNames.hasMoreElements()) {
			String name = headerNames.nextElement();
			Enumeration<String> values = request.getHeaders(name);
			sb.append( name ).append(" = ");
			while( values.hasMoreElements() ) {
				sb.append(values.nextElement());
				if( values.hasMoreElements() )
					sb.append("; ");
			}
			sb.append(NEW_LINE);
		}
		return sb.toString();
	}

	public static String getResponseHeaders(HttpServletResponse response) {
		StringBuilder sb = new StringBuilder();
		for(String name : response.getHeaderNames()) {
			Collection<String> values = response.getHeaders(name);
			sb.append( name ).append(" = ");
			for(String value : values) {
				sb.append(value).append("; ");
			}
			sb.append(NEW_LINE);
		}
		return sb.toString();
	}

	public static void printResponseHeaders(HttpServletResponse response) {
		printResponseHeaders(response, System.out );
	}

	public static void printResponseHeaders(HttpServletResponse response, OutputStream out) {
		if( out==System.out )
			System.out.print(getResponseHeaders(response));
		else {
			printResponseHeaders(response, new OutputStreamWriter(out));
		}
	}

	public static void printResponseHeaders(HttpServletResponse response, Writer writer) {
		try {
			writer.write( getResponseHeaders(response) );
			writer.flush();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void printParams(HttpServletRequest request) {
		printParams(request, System.out );
	}

	public static void printParams(HttpServletRequest request, OutputStream out) {
		if( out==System.out ) {
			System.out.print( getFormattedRequestParameters(request) );
			return;
		}
		printParams(request, new OutputStreamWriter(out));
	}

	public static void printParams(HttpServletRequest request, Writer writer) {
		try {
			writer.write( getFormattedRequestParameters(request) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendIllegalArgumentError(HttpServletResponse response) {
		//String params = getRequestParameters(request);
		try {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or  Malformed Argument(s)");
		} catch (IOException e) {}
	}

	public static void removeBean(HttpServletRequest request, String beanName) {
		request.getSession().removeAttribute(beanName);
		request.removeAttribute(beanName);
		request.getSession().getServletContext().removeAttribute(beanName);
	}

	public static void dontCache(HttpServletResponse response) {
		response.addHeader("Pragma", "no-cache");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Expires", "0");
		response.setHeader("Max-Age", "0");
	}

	protected static boolean startsWithPathPrefix(String path, String pathPrefix) {
		return path.equals(pathPrefix) || path.startsWith(pathPrefix + "/");
	}

	public static boolean isResource(HttpServletRequest request) {
		return HttpUtil.isResource( getRequestURI(request) );
	}

	public static boolean isRestful(HttpServletRequest request) {
		return isRestful( getRequestURI(request) );
	}

	public static boolean isRestful(String path) {
		return startsWithPathPrefix(path, "/restful_service") ||
				startsWithPathPrefix(path, "/rest");
	}

	public static String getRequestURI(HttpServletRequest request) {
		return StringUtil.ifEmpty((String)request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI), request.getRequestURI());
	}

	private static final String IP_HEADERS[] = {
			"X-Forwarded-For",
			"HTTP_CLIENT_IP",
			"Proxy-Client-IP",
			"WL-Proxy-Client-IP",
			"client-ip",
			"HTTP_X_FORWARDED_FOR"
	};
	
	public static String getIpAddress(HttpServletRequest request) {
		for(String headerName : IP_HEADERS) {
			String ip = request.getHeader(headerName);
			if( !StringUtil.empty(ip) && "unknown".equalsIgnoreCase(ip))
				return ip;
		}
		return request.getRemoteAddr();
	}

}
