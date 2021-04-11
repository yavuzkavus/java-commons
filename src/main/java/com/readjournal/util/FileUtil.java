package com.readjournal.util;

import static com.readjournal.util.Utils.runtime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileUtil {
	public static String getExtension(String fileName) {
		int len;
		if(fileName==null ||
				(len = fileName.length())==0 ||
				isSep(fileName) || //in the case of a directory
				fileName.charAt(len-1)=='.' ) //in the case of . or ..
			return "";
		int dotInd = fileName.lastIndexOf('.'),
			sepInd = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
		if( dotInd<=sepInd )
			return "";
		else
			return fileName.substring(dotInd+1).toLowerCase();
	}

	// C:\a\b\c.ext => c.ext
	public static String getFileName(String filePath) {
		if( filePath==null || filePath.length()==0 )
			return "";
		filePath = filePath.replaceAll("[/\\\\]+", "/");
		int len = filePath.length(),
			upCount = 0,
			lastInd;
		String fileName;
		while( len>0 ) {
			//remove trailing separator
			if( filePath.charAt(len-1)=='/' ) {
				len--;
				if( len==0 )
					return "";
			}
			lastInd = filePath.lastIndexOf('/', len-1);
			fileName = filePath.substring(lastInd+1, len);
			if( fileName.equals(".") ) {
				len--;
			}
			else if( fileName.equals("..") ) {
				len -= 2;
				upCount++;
			}
			else {
				if( upCount==0 )
					return fileName;
				upCount--;
				len -= fileName.length();
			}
		}
		return "";
	}

	// C:\a\b\c.ext => c
	public static String getBaseName(String filePath) {
		String fileName = getFileName(filePath);
		if( fileName.equals("") )
			return "";
		int dotInd = fileName.lastIndexOf('.');
		return dotInd==-1 ? fileName : dotInd==0 ? "" : fileName.substring(0, dotInd);
	}

	public static String getDirectory(String filePath) {
		if( filePath==null || filePath.length()==0 )
			return "";
		int len = filePath.length(),
			upCount = 1,
			lastInd;
		String fileName;
		while( len>0 ) {
			//remove trailing separator
			while( isSep(filePath, len-1) ) {
				len--;
				if( len==0 ) {
					if( upCount==0 && isSep(filePath, 0) )
						return filePath.substring(0, 1);
					return "";
				}
			}
			lastInd = Math.max(filePath.lastIndexOf('/', len-1), filePath.lastIndexOf('\\', len-1));
			fileName = filePath.substring(lastInd+1, len);
			if( fileName.equals(".") ) {
				len--;
			}
			else if( fileName.equals("..") ) {
				len -= 2;
				upCount++;
			}
			else {
				if( upCount==0 ) {
					while( len>0 && isSep(filePath, len-1) )
						len--;
					return (len>0 ? filePath.substring(0, len) : "") + filePath.charAt(len);
				}
				upCount--;
				len -= fileName.length();
			}
		}
		return "";
	}

	public static String changeExtension(final String fileName, final String newExt) {
		if( StringUtil.empty(fileName) )
			return fileName;
		int lastInd = fileName.lastIndexOf(".");
		String newName = fileName;
		if( lastInd>0 ) {
			newName = fileName.substring(0,lastInd+1)+newExt;
		}
		else {
			newName = fileName+"."+newExt;
		}
		return newName;
	}

	public static String changeFileName(String path, String newName) {
		String newPath = getDirectory(path);
		if( newPath!=null
				&& (newPath=newPath.trim()).length()!=0
				&& !isSep(newPath) )
			newPath += "/";
		newPath = (newPath==null ? "" : newPath) + newName;
		return newPath;
	}

	public static File getUnexistedFile(File file) {
		if( !file.exists() )
			return file;
		String name = file.getName();
		String ext = getExtension(name);
		if( !StringUtil.empty(ext) ) {
			name = getBaseName(name);
			ext = "." + ext;
		}
		File parentFile = file.getParentFile();
		int i = 2;
		do {
			file = new File( parentFile, name + "-" + i + ext );
			i++;
		} while(file.exists());
		return file;
	}

	public static void move(File source, File target) {
		try {
			Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		catch(IOException e) {
			throw runtime(e);
		}
	}

	public static boolean move(File source, File target, int tryCount, int waitTime) {
		int i = 0;
		while( i<tryCount ) {
			try {
				move(source, target);
				return true;
			}
			catch(Exception ex) {
				i++;
				synchronized (source) {
					try {
						source.wait(waitTime);
					} catch (InterruptedException e) { }
				}
			}
		}
		return false;
	}

	public static byte[] readFileToByteArray(File file) {
		try {
			return Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			throw runtime(e);
		}
	}

	public static void writeByteArrayToFile(File file, byte[] data) {
		copy(new ByteArrayInputStream(data), file);
	}

	public static String readFileToString(File file, Charset charset) {
		if( !file.exists() )
			return "";
		return new String( readFileToByteArray(file), charset);
	}

	public static String readFileToString(File file) {
		return readFileToString(file, StandardCharsets.UTF_8);
	}

	public static void writeStringToFile(File file, String str, Charset charset) {
		writeByteArrayToFile(file, str.getBytes(charset));
	}

	public static void writeStringToFile(File file, String str) {
		writeByteArrayToFile(file, str.getBytes(StandardCharsets.UTF_8));
	}

	public static long copy(InputStream input, OutputStream output) {
		byte[] buffer = new byte[4096];
		long count = 0;
		int n = 0;
		try {
			while( (n = input.read(buffer))!=-1 ) {
				output.write(buffer, 0, n);
				count += n;
			}
		}
		catch(IOException e) {
			throw runtime(e);
		}
		return count;
	}

	public static long copy(File inputFile, OutputStream outputStream) {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(inputFile);
			return copy(inputStream, outputStream);
		}
		catch(IOException e) {
			throw runtime(e);
		}
		finally {
			Utils.closeSilently(inputStream);
		}
	}

	public static long copy(InputStream inputStream, File outputFile) {
		OutputStream outputStream = null;
		try {
			outputFile.getParentFile().mkdirs();
			outputStream = new FileOutputStream(outputFile);
			return copy(inputStream, outputStream);
		}
		catch(IOException e) {
			throw runtime(e);
		}
		finally {
			Utils.closeSilently(outputStream);
		}
	}

	public static long copy(File inputFile, File outputFile) {
		OutputStream outputStream = null;
		InputStream inputStream = null;
		try {
			outputFile.getParentFile().mkdirs();
			outputStream = new FileOutputStream(outputFile);
			inputStream = new FileInputStream(inputFile);
			return copy(inputStream, outputStream);
		}
		catch(IOException e) {
			throw runtime(e);
		}
		finally {
			Utils.closeSilently(inputStream, outputStream);
		}
	}

	public static byte[] toByteArray(final InputStream input) {
		try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			copy(input, output);
			return output.toByteArray();
		}
		catch (IOException e) {
			throw Utils.runtime(e);
		}
	}

	public static boolean isSymlink(File file) {
		return Files.isSymbolicLink(file.toPath());
	}

	public static boolean deleteDirectory(File directory) {
		if( !directory.exists() )
			return true;

		boolean deleted = true;
		if( !isSymlink(directory) )
			deleted = cleanDirectory(directory);

		return deleted && directory.delete();
	}

	public static boolean deleteDirectorySilently(File directory) {
		try { return deleteDirectory(directory); } catch(Throwable thr) { return false; }
	}

	public static boolean cleanDirectory(File directory) {
		if( !directory.exists() )
			return true;

		if( !directory.isDirectory() )
			throw new IllegalArgumentException(directory + " is not a directory");

		for(File file : directory.listFiles()) {
			if( file.isDirectory() ) {
				if( !deleteDirectory(file) )
					return false;
			}
			else {
				if( !file.delete() )
					return false;
			}
		}

		return true;
	}

	public static boolean cleanDirectorySilently(File directory) {
		try { return cleanDirectory(directory); } catch(Throwable thr) { return false; }
	}

	public static boolean isSep(String str, int ind) {
		return isSep( str.charAt(ind) );
	}

	public static boolean isSep(String str) {
		if( str==null )
			return false;
		int len = str.length();
		return len>0 && isSep( str.charAt(len-1) );
	}

	public static boolean isSep(char ch) {
		return ch=='/' || ch=='\\';
	}

	public static String normalizePath(String path) {
		return normalizePath(path, false);
	}

	public static String normalizePath(String path, boolean endSeparator) {
		return normalizePath(path, File.separatorChar, endSeparator);
	}

	public static String normalizePath(String path, char separator) {
		int len = path==null ? 0 : path.length();
		boolean endSeparator = 	len>0 && isSep(path) ||
								len>1 && path.charAt(len-1)=='.' && isSep(path, len-2) ||
								len>2 && path.charAt(len-1)=='.' && path.charAt(len-2)=='.' && isSep(path, len-3);
		return normalizePath(path, separator, endSeparator);
	}

	public static String normalizePath(String path, char separator, boolean endSeparator) {
		if( !isSep(separator) )
			throw new IllegalArgumentException(separator + ": Not a valid seperator. Allowed values: / or \\");
		int len;
		if( null==path || (len = (path=path.trim()).length())==0 )
			return path;
		char otherSep = separator=='/' ? '\\' : '/',
			ch1 = 0,
			ch2 = 0;
		boolean wasSep = false;
		int ind = 0;
		StringBuilder sb = new StringBuilder(len+2);

		//finding prefix: begin
		ch1 = path.charAt(0);
		ch2 = len>1 ? path.charAt(1) : 0;
		if( ch2==':' && Character.toUpperCase(ch1)>='A' && Character.toUpperCase(ch1)<='Z' ) {
			sb.append(Character.toUpperCase(ch1)).append(ch2).append(separator);
			ind = 2;
			wasSep = true;
			//emit remaining separators
			while( ind<len && ((ch2 = path.charAt(ind))==separator || ch2==otherSep) )
				ind++;
		}
		else if( len>2 && isSep(path, 0) && Character.toUpperCase(ch2)>='A' && Character.toUpperCase(ch2)<='Z' &&
				path.charAt(2)==':' ) {
			sb.append(Character.toUpperCase(ch2)).append(':').append(separator);
			ind = 3;
			wasSep = true;
			//emit remaining separators
			while( ind<len && ((ch2 = path.charAt(ind))==separator || ch2==otherSep) )
				ind++;
		}
		else if( ch1==separator || ch1==otherSep ) {
			sb.append(separator);
			ind = 1;
			wasSep = true;
			if( ch2==separator || ch2==otherSep ) {
				sb.append(separator);
				ind++;
				//emit remaining separators
				while( ind<len && ((ch2 = path.charAt(ind))==separator || ch2==otherSep) )
					ind++;
				while( ind<len && ((ch2 = path.charAt(ind))!=separator && ch2!=otherSep) ) {
					sb.append(ch2);
					ind++;
				}
				if( sb.length()>2 ) {
					sb.append(separator);
					//emit remaining separators
					while( ind<len && ((ch2 = path.charAt(ind))==separator || ch2==otherSep) )
						ind++;
				}
			}
		}
		else if( ch1=='~' ) { // unix/linux
			sb.append(ch1);
			ind = 1;
			while( ind<len && ((ch2 = path.charAt(ind))!=separator && ch2!=otherSep) ) {
				sb.append(ch2);
				ind++;
			}
			sb.append(separator);
			//emit remaining separators
			while( ind<len && ((ch2 = path.charAt(ind))==separator || ch2==otherSep) )
				ind++;
			wasSep = true;
		}
		int prefixLen = sb.length();
		//finding prefix: end

		while(ind<len) {
			ch1 = path.charAt(ind++);
			if( ch1==separator || ch1==otherSep ) {
				//eleminate duplicate seps
				if( !wasSep ) {
					sb.append(separator);
					wasSep = true;
				}
			}
			else if( (wasSep || ind==1) && ch1=='.' &&
					(ind>=len || (ch2 = path.charAt(ind))==separator || ch2==otherSep) ) {
				//bypass this directory (.)
				ind++;
			}
			else if( wasSep && ch1=='.' &&
					ch2=='.' &&
					(ind+1>=len || (ch2 = path.charAt(ind+1))==separator || ch2==otherSep) ){
				//delete up dir on finding up dir (..)
				if( prefixLen==0 && sb.length()==0 ) {
					sb.append("..").append(separator);
				}
				else {
					int ind2 = sb.length() - 2;
					while( ind2>=prefixLen && (ch2 = sb.charAt(ind2))!=separator )
						ind2--;
					if( ind2+1<prefixLen) {
						return null;
					}
					else if( sb.substring(ind2+1, sb.length()-1).equals("..") ) {
						sb.append("..").append(separator);
					}
					else {
						sb.setLength( ind2 + 1 );
					}
				}
				ind += 2;
			}
			else {
				sb.append(ch1);
				wasSep = false;
			}
		}
		if( endSeparator && !wasSep )
			sb.append(separator);
		else if( !endSeparator && wasSep )
			sb.setLength( sb.length()-1 );
		return sb.toString();
	}

	//paths should been relativized
	public static String relativizePath(String path, String refPath) {
		return relativizePath(path, refPath, File.separatorChar=='/');
	}

	//paths should been relativized
	public static String relativizePath(String path, String refPath, boolean unixSeparator) {
		StringBuilder sb = new StringBuilder();
		int len1 = path.length(),
			len2 = refPath.length(),
			ind=0,
			ind2=0,
			minLen = Math.min(len1, len2);
		while( ind<minLen && path.charAt(ind)==refPath.charAt(ind) ) {
			if( isSep(path, ind) )
				ind2 = ind;
			ind++;
		}
		if( ind2>0 ) {
			path = len1<=ind2 ? "" : path.substring(ind2+1);
			refPath = len2==ind2 ? "" : refPath.substring(ind2+1);
		}
		char sep = unixSeparator ? '/' : '\\';
		while( !(refPath=getDirectory(refPath)).equals("") )
			sb.append("..").append(sep);
		return sb.append(path).toString();
	}

	public static void forceDelete(File file) throws IOException {
		if( file.exists() ) {
			if (!file.delete())
				throw new IOException("Unable to delete file: " + file);
		}
	}

	public static byte[] read(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		copy(input, output);
		return output.toByteArray();
	}
}
