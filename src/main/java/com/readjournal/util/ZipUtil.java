package com.readjournal.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtil {	
	public static void unzipFile(File zipFile, File outDir) {
		if( !outDir.exists() )
			outDir.mkdirs();
		BufferedOutputStream bos = null;
		InputStream in = null;
		ZipInputStream zipIn = null;
		try {
			in = new BufferedInputStream(new FileInputStream(zipFile));
			zipIn = new ZipInputStream( in );
			ZipEntry entry;
			File outFile = null;
			byte buff[] = new byte[8192];
			int len;
			while( (entry=zipIn.getNextEntry())!=null ) {
				if( entry.isDirectory() )
					continue;
				outFile = new File(outDir, entry.getName());
				outFile.getParentFile().mkdirs();
				bos = new BufferedOutputStream( new FileOutputStream(outFile) );
				while( (len=zipIn.read(buff))!=-1 )
					bos.write(buff, 0, len);
				bos.close();
			}
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
		finally {
			Utils.closeSilently(in, bos, zipIn);
		}
	}
	
	public static void zipFolder(File inDir, File zipFile) {
		if( !inDir.isDirectory() ) {
			throw new IllegalArgumentException(inDir + " : is not directory!");
		}
		ZipOutputStream zout = null;
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream( new FileOutputStream(zipFile) );
			zout = new ZipOutputStream(bos);
			addRecursive(inDir, "", zout);
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
		finally {
			Utils.closeSilently(zout);
			Utils.closeSilently(bos);
		}
	}
	
	public static final void addFile(File file, String entryPath, ZipOutputStream zop) throws IOException {
		zop.putNextEntry( new ZipEntry(entryPath) );
		zop.write( Files.readAllBytes(file.toPath()) );
		zop.closeEntry();
	}
	
	public static final void addBinaryData(byte[] data, String entryPath, ZipOutputStream zop) throws IOException {
		zop.putNextEntry( new ZipEntry(entryPath) );
		zop.write( data );
		zop.closeEntry();
	}
	
	public static final void addRecursive(File file, String parentEntry, ZipOutputStream zop) throws IOException {
		if( parentEntry==null )
			parentEntry = "";
		else if( !StringUtil.empty(parentEntry) && !parentEntry.endsWith("/") )
			parentEntry += "/"; 
		if( file.isFile() ) {
			addFile(file, parentEntry + file.getName(), zop);
			return;
		}
		File files[] = file.listFiles();
		if( files!=null ) {
			for(File subFile : files) {
				if( subFile.isFile() ) {
					zop.putNextEntry( new ZipEntry(parentEntry + subFile.getName()) );
					zop.write( Files.readAllBytes(subFile.toPath()) );
					zop.closeEntry();
				}
				else {
					addRecursive(subFile, parentEntry + subFile.getName() + "/", zop);
				}
			}
		}
	}
	
	public static final void addFileToZip(File zipFile, File extraFile) {
		addFileToZip(zipFile, extraFile, extraFile.getName());
	}
	
	public static final void addFileToZip(File zipFile, File extraFile, String entryPath) {
		BufferedOutputStream bos = null;
		ZipOutputStream zipOut = null;
		BufferedInputStream bis = null;
		ZipInputStream zipIn = null;
		try {
			String name = zipFile.getName();
			String ext = FileUtil.getExtension(name);
			if( !StringUtil.empty(ext) ) {
				name = name.substring(0, name.lastIndexOf('.'));
				ext = "." + ext;
			}
			File newZip = new File( name + "1" + ext );
			int i = 2;
			while( newZip.exists() )
				newZip = new File(name+(i++)+ext);
			ZipEntry entryIn;
			bis = new BufferedInputStream( new FileInputStream(zipFile) );
			zipIn = new ZipInputStream( bis );
			bos = new BufferedOutputStream( new FileOutputStream(newZip) );
			zipOut = new ZipOutputStream( bos );
			while( (entryIn=zipIn.getNextEntry())!=null ) {
				zipOut.putNextEntry( new ZipEntry(entryIn.getName()) );
				FileUtil.copy(zipIn, zipOut);
				zipOut.closeEntry();
			}
			bis.close();
			zipIn.close();
			zipOut.putNextEntry( new ZipEntry(entryPath) );
			bis = new BufferedInputStream( new FileInputStream(extraFile) );
			FileUtil.copy(bis, zipOut);
			zipOut.closeEntry();
			
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
		finally {
			Utils.closeSilently(bos, bis, zipIn, zipOut);
		}
	}
	
	public static final void addDirectoryToZip(File zipFile, File dirFile) {
		BufferedOutputStream bos = null;
		ZipOutputStream zipOut = null;
		BufferedInputStream bis = null;
		ZipInputStream zipIn = null;
		try {
			String name = zipFile.getName();
			String ext = FileUtil.getExtension(name);
			if( !StringUtil.empty(ext) ) {
				name = name.substring(0, name.lastIndexOf('.'));
				ext = "." + ext;
			}
			File newZip = new File( name + "1" + ext );
			int i = 2;
			while( newZip.exists() )
				newZip = new File(name+(i++)+ext);
			ZipEntry entryIn;
			bis = new BufferedInputStream( new FileInputStream(zipFile) );
			zipIn = new ZipInputStream( bis );
			bos = new BufferedOutputStream( new FileOutputStream(newZip) );
			zipOut = new ZipOutputStream( bos );
			while( (entryIn=zipIn.getNextEntry())!=null ) {
				zipOut.putNextEntry( new ZipEntry(entryIn.getName()) );
				FileUtil.copy(zipIn, zipOut);
				zipOut.closeEntry();
			}
			bis.close();
			zipIn.close();
			addRecursive(dirFile, null, zipOut);
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
		finally {
			Utils.closeSilently(bos, bis, zipIn, zipOut);
		}
	}
	
	public static final byte[] encodeZip(byte[] data) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length/3);
			ZipOutputStream gos = new ZipOutputStream(baos);
			gos.write(data);
			gos.close();
			return baos.toByteArray();
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}
	
	public static final byte[] decodeZip(byte[] data) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			ZipInputStream gip = new ZipInputStream(bais);
			byte[] content = FileUtil.toByteArray(gip);
			gip.close();
			return content;
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}
	
	public static final byte[] encodeGzip(byte[] data) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length/3);
			GZIPOutputStream gos = new GZIPOutputStream(baos);
			gos.write(data);
			gos.close();
			return baos.toByteArray();
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}
	
	public static final byte[] decodeGzip(byte[] data) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			GZIPInputStream gip = new GZIPInputStream(bais);
			byte[] content = FileUtil.toByteArray(gip);
			gip.close();
			return content;
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}
}
