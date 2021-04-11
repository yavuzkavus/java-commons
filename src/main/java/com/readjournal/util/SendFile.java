package com.readjournal.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SendFile {
	private static final String DOWNLOAD_MIMETYPE = "application/x-download";
	
	private static final String COMPRESSIBLE_TYPES[] = {
		"text/html",
		"text/xml",
		"text/plain",
		"text/css",
		"text/javascript",
		"application/javascript",
		"application/json",
		"application/xml",
		"image/svg+xml",
		"image/x-icon"
	};

	private Object resource;
	private String fileName;
	private String mimeType;
	private Boolean useCompression;
	private int compressionMinLength = 2048;
	private boolean useCaches = true;
	private int maxAgeSeconds;
	private boolean forceDownload;
	private boolean cacheNoCache;
	private boolean cachePrivate;
	private boolean cachePublic;
	private boolean cacheMustRevalidate;

	private long lastModified;

	public SendFile(File resource) {
		this.resource = resource;
	}

	public SendFile(byte[] resource) {
		this.resource = resource;
	}

	public Object getResource() {
		return resource;
	}
	public void setResource(File resource) {
		this.resource = resource;
	}
	public void setResource(byte[] resource) {
		this.resource = resource;
	}

	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getMimeType() {
		return mimeType;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType!=null ? mimeType.toLowerCase(Locale.ENGLISH) : mimeType;
	}

	public boolean isUseCompression() {
		return useCompression==null ? false : useCompression.booleanValue();
	}
	public void setUseCompression(boolean useCompression) {
		this.useCompression = useCompression;
	}

	public int getCompressionMinLength() {
		return compressionMinLength;
	}
	public void setCompressionMinLength(int compressionMinLength) {
		this.compressionMinLength = compressionMinLength;
	}

	public boolean getUseCaches() {
		return useCaches;
	}
	public void setUseCaches(boolean useCaches) {
		this.useCaches = useCaches;
	}

	public int getMaxAgeSeconds() {
		return maxAgeSeconds;
	}
	public void setMaxAgeSeconds(int maxAgeSeconds) {
		this.maxAgeSeconds = maxAgeSeconds;
	}

	public boolean isForceDownload() {
		return forceDownload;
	}
	public void setForceDownload(boolean forceDownload) {
		this.forceDownload = forceDownload;
	}

	public boolean isCacheNoCache() {
		return cacheNoCache;
	}
	public void setCacheNoCache(boolean cacheNoCache) {
		this.cacheNoCache = cacheNoCache;
	}

	public boolean isCachePrivate() {
		return cachePrivate;
	}
	public void setCachePrivate(boolean cachePrivate) {
		this.cachePrivate = cachePrivate;
	}

	public boolean isCachePublic() {
		return cachePublic;
	}
	public void setCachePublic(boolean cachePublic) {
		this.cachePublic = cachePublic;
	}

	public boolean isCacheMustRevalidate() {
		return cacheMustRevalidate;
	}
	public void setCacheMustRevalidate(boolean cacheMustRevalidate) {
		this.cacheMustRevalidate = cacheMustRevalidate;
	}

	public boolean isFile() {
		return resource instanceof File;
	}

	public boolean isByteArray() {
		return resource instanceof byte[];
	}

	public long getLastModified() {
		return lastModified;
	}
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public void send(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if( StringUtil.empty(fileName) && isFile() )
			fileName = ((File)resource).getName();

		if( forceDownload )
			mimeType = DOWNLOAD_MIMETYPE;
		if( StringUtil.empty(mimeType) && fileName!=null )
			mimeType = request.getSession().getServletContext().getMimeType(fileName.toLowerCase(Locale.ENGLISH));

		response.setContentType(mimeType);
		response.setHeader("Accept-Ranges", "none");
		StringBuilder sb = new StringBuilder();
		if( cacheNoCache )
			sb.append("no-cache");
		if( cachePrivate )
			sb.append(sb.length()>0 ? ", " : "").append("private");
		if( cachePublic )
			sb.append(sb.length()>0 ? ", " : "").append("public");
		if( cacheMustRevalidate )
			sb.append(sb.length()>0 ? ", " : "").append("must-revalidate");
		if( maxAgeSeconds>0 )
			sb.append(sb.length()>0 ? ", " : "").append("max-age=").append(maxAgeSeconds);

		if( sb.length()>0 )
			response.setHeader("Cache-Control", sb.toString());

		if( useCaches && sendCachedResponse(request, response) )
			return;

		//dont reset, because code calling this method, may append some headers
		//response.reset();
		long lastModified = this.lastModified>0 ? this.lastModified : isFile() ? ((File)resource).lastModified() : 0;
		if( lastModified>0 )
			response.setDateHeader("Last-Modified", lastModified);
		if( DOWNLOAD_MIMETYPE.equals(mimeType) || "application/zip".equals(mimeType) || "application/mp4".equals(mimeType) )
			response.setHeader("Content-Disposition", "attachment; filename="+ StringUtil.ifEmpty(fileName, "file"));
		//else
		//	response.setHeader("Content-Disposition", "inline; filename="+name );

		byte[] data;
		if( isFile() ) {
			data = FileUtil.readFileToByteArray((File)resource);
		}
		else {
			data = (byte[])resource;
		}

		if( data.length>=compressionMinLength && isCompressible(request) ) {
			String acceptEncoding = request.getHeader("Accept-Encoding");
			if( !StringUtil.empty(acceptEncoding) ) {
				String values[] = acceptEncoding.toLowerCase(Locale.ENGLISH).split("\\s*(,|;)\\s*");
				if( ArrayUtil.contains("gzip", (Object[])values) ) {
					response.setHeader("Content-Encoding", "gzip");
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					GZIPOutputStream gos = new GZIPOutputStream(baos);
					gos.write(data);
					gos.close();
					data = baos.toByteArray();
				}
			}
		}
		int dataLen = data.length;
		response.setContentLength(dataLen);
		try( ServletOutputStream out = response.getOutputStream() ) {
			/*
			int buffSize = response.getBufferSize()>0 ? response.getBufferSize() : 2048;
			int next = 0;
			while( next<dataLen ) {
				out.write(data, next, Math.min(buffSize, dataLen-next));
				next += buffSize;
			}
			*/
			out.write(data);
			response.flushBuffer();
		}
		catch(IOException ex) {
			//ignore exception in the case of connection aborted by client
			if( !isClientAbortException(ex) )
				throw ex;
		}
	}


	public static boolean isClientAbortException(Throwable ex) {
		while( ex!=null ) {
			if( ex.getClass().getSimpleName().equals("ClientAbortException") )
				return true;
			String message = StringUtil.ifNull(ex.getMessage()).toLowerCase(Locale.ENGLISH);
			if( message.contains("connection was forcibly closed by the remote host") ||
					message.contains("connection was aborted by the software"))
				return true;
			ex = ex.getCause();
		}
		return false;
	}

	private boolean isCompressible(HttpServletRequest request) {
		if( useCompression!=null )
			return useCompression;
		String mimeType = this.mimeType!=null ? this.mimeType : fileName!=null ? request.getServletContext().getMimeType(fileName) : null;
		if( mimeType==null )
			return false;
		int ind = mimeType.indexOf(';');
		if( ind>0 )
			mimeType = mimeType.substring(0, ind);
		return ArrayUtil.contains(COMPRESSIBLE_TYPES, mimeType);
	}

	private boolean sendCachedResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String cacheControl = request.getHeader("Cache-Control");
		String pragma = request.getHeader("Pragma");
		if( (cacheControl==null || !cacheControl.toLowerCase(Locale.ENGLISH).contains("no-cache") ) &&
				(pragma==null || !pragma.toLowerCase(Locale.ENGLISH).contains("no-cache") ) ) {
			long lastModified = this.lastModified>0 ? this.lastModified : isFile() ? ((File)resource).lastModified() : 0;
			long ifModifiedSince = request.getDateHeader("If-Modified-Since");
			if( ifModifiedSince>-1 && Math.abs(lastModified-ifModifiedSince)<=2000 ) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				response.flushBuffer();
				return true;
			}
		}
		return false;
	}

	/*
	 * send methods
	 */
	public static void sendFile(
			HttpServletRequest request,
			HttpServletResponse response,
			String filePath) throws IOException{
		sendFile(request, response, new File(filePath));
	}

	public static void sendFile(
			HttpServletRequest request,
			HttpServletResponse response,
			File resource) throws IOException{
		sendFile(request, response, resource, null);
	}

	public static void sendFile(
			HttpServletRequest request,
			HttpServletResponse response,
			String filePath,
			String mimeType) throws IOException {
		sendFile(request, response, filePath, mimeType);
	}

	public static void sendFile(
			HttpServletRequest request,
			HttpServletResponse response,
			File resource,
			String mimeType) throws IOException {
		SendFile sendFile = new SendFile(resource);
		sendFile.setMimeType(mimeType);
		sendFile.send(request, response);
	}
	
	

	public static void sendFile(
			HttpServletRequest request,
			HttpServletResponse response,
			byte[] resource,
			String mimeType) throws IOException {
		sendFile(request, response, resource, mimeType, 0);
	}
	
	public static void sendFile(
			HttpServletRequest request,
			HttpServletResponse response,
			byte resource[],
			String mimeType,
			int maxAgeSeconds) throws IOException {
		SendFile sendFile = new SendFile(resource);
		sendFile.setMimeType(mimeType);
		sendFile.setMaxAgeSeconds(maxAgeSeconds);
		sendFile.send(request, response);
	}

	/*
	 * download methods
	 */
	public static void downloadFile(
			HttpServletRequest request,
			HttpServletResponse response,
			String filePath) throws IOException{
		downloadFile(request, response, new File(filePath), null);
	}

	public static void downloadFile(
			HttpServletRequest request,
			HttpServletResponse response,
			File resource ) throws IOException{
		downloadFile(request, response, resource, null);
	}

	public static void downloadFile(
			HttpServletRequest request,
			HttpServletResponse response,
			String filePath,
			String fileName) throws IOException{
		downloadFile(request, response, new File(filePath), fileName);
	}

	public static void downloadFile(
			HttpServletRequest request,
			HttpServletResponse response,
			File resource,
			String fileName ) throws IOException{
		SendFile sendFile = new SendFile(resource);
		sendFile.setFileName(fileName);
		sendFile.setForceDownload(true);
		sendFile.send(request, response);
	}

	public static void downloadFile(
			HttpServletRequest request,
			HttpServletResponse response,
			byte[] resource,
			String fileName ) throws IOException{
		SendFile sendFile = new SendFile(resource);
		sendFile.setFileName(fileName);
		sendFile.setForceDownload(true);
		sendFile.send(request, response);
	}
}
