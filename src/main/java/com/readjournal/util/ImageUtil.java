package com.readjournal.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Objects;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ImageUtil {
	public static final float INCH_2_MM = 25.4f;

	public static void saveJpeg(BufferedImage image, int density, File out) {
		saveJpeg(image, density, 0, out);
	}

	public static void saveJpeg(BufferedImage image, int density, OutputStream out) {
		saveJpeg(image, density, 0, out);
	}

	public static void saveJpeg(BufferedImage image, int density, ImageOutputStream out) {
		saveJpeg(image, density, 0, out);
	}

	public static void saveJpeg(BufferedImage image, float quality, File out) {
		saveJpeg(image, 0, quality, out);
	}

	public static void saveJpeg(BufferedImage image, float quality, OutputStream out) {
		saveJpeg(image, 0, quality, out);
	}

	public static void saveJpeg(BufferedImage image, float quality, ImageOutputStream out) {
		saveJpeg(image, 0, quality, out);
	}

	// 0<=quality<=1
	public static void saveJpeg(BufferedImage image, int density, float quality, File out) {
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
			saveJpeg(image, density, quality, ios);
		} catch (IOException ex) {
			throw Utils.runtime(ex);
		}
	}

	public static void saveJpeg(BufferedImage image, int density, float quality, OutputStream out) {
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
			saveJpeg(image, density, quality, ios);
		} catch (IOException ex) {
			throw Utils.runtime(ex);
		}
	}

	// 0<=quality<=1
	public static void saveJpeg(BufferedImage image, int density, float quality, ImageOutputStream out) {
		Objects.requireNonNull(image, "image is null");
		try {
			ImageWriter jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next();

			ImageWriteParam jpegParams = new JPEGImageWriteParam(StringUtil.trLocale);
			//quality
			if( quality>0 ) {
				jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				jpegParams.setCompressionQuality(quality);
			}
			if( density>0 ) {
				//density
				IIOMetadata metadata = null;
				try {
					ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromRenderedImage(image);
					metadata = jpegWriter.getDefaultImageMetadata(typeSpecifier, jpegParams);
					setDensity(metadata, density);
				}
				catch(Exception ex) {
					//for some bogus color spaces, density cant be set. The convert to a proper image
					image = toRGB(image);
					ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromRenderedImage(image);
					metadata = jpegWriter.getDefaultImageMetadata(typeSpecifier, jpegParams);
					setDensity(metadata, density);
				}
				//save
				jpegWriter.setOutput(out);
				jpegWriter.write(metadata, new IIOImage(image, null, metadata), jpegParams);
				jpegWriter.dispose();
			}
			else {
				jpegWriter.setOutput(out);
				try {
					jpegWriter.write(null, new IIOImage(image, null, null), jpegParams);
				}
				catch(Exception ex) {
					//bogus color space
					jpegWriter.reset();
					out.reset();
					jpegWriter.setOutput(out);
					image = toRGB(image);
					jpegWriter.write(null, new IIOImage(image, null, null), jpegParams);
				}
				jpegWriter.dispose();
			}
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	public static BufferedImage toRGB(Image image) {
		BufferedImage rgbImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = rgbImage.createGraphics();
		g2.drawImage(image, 0, 0, Color.WHITE, null);
		g2.dispose();
		return rgbImage;
	}

	private static void setDensity(IIOMetadata metadata, int dpi) throws IIOInvalidTreeException {
		String metadataFormat = "javax_imageio_jpeg_image_1.0";
		IIOMetadataNode tree = (IIOMetadataNode)metadata.getAsTree(metadataFormat);
		IIOMetadataNode jfif = (IIOMetadataNode)tree.getElementsByTagName("app0JFIF").item(0);
		jfif.setAttribute("Xdensity", Integer.toString(dpi));
		jfif.setAttribute("Ydensity", Integer.toString(dpi));
		jfif.setAttribute("resUnits", "1"); //density is dots per inch
		metadata.setFromTree(metadataFormat, tree);
		/*
		String metadataFormat = "javax_imageio_jpeg_image_1.0";
		IIOMetadataNode root = new IIOMetadataNode(metadataFormat);
		IIOMetadataNode jpegVariety = new IIOMetadataNode("JPEGvariety");
		IIOMetadataNode markerSequence = new IIOMetadataNode("markerSequence");

		IIOMetadataNode app0JFIF = new IIOMetadataNode("app0JFIF");
		app0JFIF.setAttribute("majorVersion", "1");
		app0JFIF.setAttribute("minorVersion", "2");
		app0JFIF.setAttribute("thumbWidth", "0");
		app0JFIF.setAttribute("thumbHeight", "0");
		app0JFIF.setAttribute("resUnits", "01");
		app0JFIF.setAttribute("Xdensity", Integer.toString(dpi));
		app0JFIF.setAttribute("Ydensity", Integer.toString(dpi));

		root.appendChild(jpegVariety);
		root.appendChild(markerSequence);
		jpegVariety.appendChild(app0JFIF);

		metadata.mergeTree(metadataFormat, root);
		*/
	 }

	public static BufferedImage readPpm(File file) {
		return readPpm(FileUtil.readFileToByteArray(file));
	}

	public static BufferedImage readPpm(byte[] buff) {
		return readPpm(ByteBuffer.wrap(buff));
	}

	public static BufferedImage readPpm(ByteBuffer byteBuff) {
		try {
			byte[] buff = new byte[20];
			String format = readTillWs(byteBuff, buff);
			if( !"P6".equals(format) )
				throw new RuntimeException("Unknown format " + format);

			int width = Integer.parseInt(readTillWs(byteBuff, buff)),
				height = Integer.parseInt(readTillWs(byteBuff, buff)),
				maxColor = Integer.parseInt(readTillWs(byteBuff, buff));

			if( maxColor>=256 )
				throw new UnsupportedOperationException("maxColor should be less than 256");
			//System.out.printf("%s %dx%d, %d%n", format, width, height, maxColor);
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

			for(int y=0; y<height; y++) {
				for(int x=0; x<width; x++) {
					int r = (byteBuff.get() & 0xFF)<<16,
						g = (byteBuff.get() & 0xFF)<<8,
						b = byteBuff.get() & 0xFF;
					image.setRGB(x, y, r|g|b);
				}
				//System.out.println(byteBuff.remaining());
			}
			//System.out.println(byteBuff.remaining());
			return image;
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	private static String readTillWs(ByteBuffer byteBuff, byte[] buff) throws IOException {
		byte b;
		int i = 0;
		while( !Character.isWhitespace( (char)(b = byteBuff.get()) ) ) {
			buff[i++] = b;
		}
		return new String(buff, 0, i);
	}

	public static class ImageDimensions {
		private int width;
		private int height;
		private int hdpi;
		private int vdpi;

		public ImageDimensions() { }

		public ImageDimensions(int width, int height, int dpi) {
			this(width, height, dpi, dpi);
		}

		public ImageDimensions(int width, int height, int hdpi, int vdpi) {
			super();
			this.width = width;
			this.height = height;
			this.hdpi = hdpi;
			this.vdpi = vdpi;
		}

		public int getHeight() {
			return height;
		}
		public void setWidth(int width) {
			this.width = width;
		}
		public int getWidth() {
			return width;
		}
		public void setHeight(int height) {
			this.height = height;
		}
		public int getVdpi() {
			return vdpi;
		}
		public void setVdpi(int vdpi) {
			this.vdpi = vdpi;
		}
		public int getHdpi() {
			return hdpi;
		}
		public void setHdpi(int hdpi) {
			this.hdpi = hdpi;
		}
		@Override
		public String toString() {
			return "ImageDimensions [width=" + width + ", height=" + height + ", hdpi=" + hdpi + ", vdpi=" + vdpi + "]";
		}
	}

	public static final ImageDimensions getImageDimensions(File imageFile) {
		ImageDimensions dims = new ImageDimensions();
		dims.setHdpi(72); //default
		dims.setVdpi(72); //default
		try(ImageInputStream in = ImageIO.createImageInputStream(imageFile)) {
			if( in!=null ) {
				final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
				if (readers.hasNext()) {
					ImageReader reader = readers.next();
					try {
						reader.setInput(in);
						dims.setWidth(reader.getWidth(0));
						dims.setHeight(reader.getHeight(0));

						int hdpi=96, vdpi=96;

						Element node = (Element)reader.getImageMetadata(0).getAsTree("javax_imageio_1.0");
						NodeList nodeList = node.getElementsByTagName("HorizontalPixelSize");
						if(nodeList != null && nodeList.getLength() == 1)
							hdpi = Math.round(INCH_2_MM/Float.parseFloat(((Element)nodeList.item(0)).getAttribute("value")));

						nodeList = node.getElementsByTagName("VerticalPixelSize");
						if(nodeList != null && nodeList.getLength() == 1)
							vdpi = Math.round(INCH_2_MM/Float.parseFloat(((Element)nodeList.item(0)).getAttribute("value")));

						dims.setHdpi(hdpi);
						dims.setVdpi(vdpi);
					}
					catch(Exception ex) { }
					finally {
						reader.dispose();
					}
				}
			}
		}
		catch (Exception e) {
			//RJLogger.getLogger().severe(Developer.YAVUZ, e);
		}
		if( dims.getWidth()==0 && dims.getHeight()==0 && imageFile.length()>0 ) {
			//if could not read from metadata, then read full image
			try {
				BufferedImage image = ImageIO.read(imageFile);
				dims.setWidth( image.getWidth() );
				dims.setHeight( image.getHeight() );
			}
			catch(Exception ex) { }
		}
		return dims;
	}
}
