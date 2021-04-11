package com.readjournal.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

final public class Base64 {
	private static final int MAX_LINE_LENGTH = 76;
	private static final char[] CHARS =  {
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 
		'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 
		'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
		'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
	};
//	private static final Map<Character, Byte> INVERSED_CHARS = new HashMap<Character, Byte>(64);
	private static byte[] INVERSED_CHARS = new byte[256];
	static {
		Arrays.fill(INVERSED_CHARS, (byte)-1);
		for(int i=0; i<CHARS.length; i++) {
			INVERSED_CHARS[ CHARS[i] ] = (byte)i;
		}
	}
	
	public static final String encode(byte[] byteData) {
		int left = byteData.length % 3;
		int count = byteData.length - left;
		int cLengt = ( left==0 ? (count/3)*4 : (count/3+1)*4); 
		char[] charData = new char[cLengt];
		int bIndex=0, cIndex=0;
		byte b1, b2, b3, c1, c2, c3, c4;
		while(bIndex<count) {
			b1 = byteData[bIndex++];
			b2 = byteData[bIndex++];
			b3 = byteData[bIndex++];
			c1 = (byte)( b1>>2 & 0x3f );
			c2 = (byte)( (b1&0x3)<<4 | b2>>4&0x0f );
			c3 = (byte)( (b2&0x0f)<<2 | b3>>6&0x03 );
			c4 = (byte)( b3&0x3f );
			charData[cIndex++] = CHARS[c1];
			charData[cIndex++] = CHARS[c2]; 
			charData[cIndex++] = CHARS[c3];
			charData[cIndex++] = CHARS[c4]; 
		}
		if( left==1 ) {
			b1 = byteData[bIndex++];
			c1 = (byte)( b1>>2 & 0x3f );
			c2 = (byte)( (b1&0x3)<<4 );
			charData[cIndex++] = CHARS[c1];
			charData[cIndex++] = CHARS[c2]; 
			charData[cIndex++] = '=';
			charData[cIndex++] = '='; 
		}
		else if(left==2 ) {
			b1 = byteData[bIndex++];
			b2 = byteData[bIndex++];
			c1 = (byte)( b1>>2 & 0x3f );
			c2 = (byte)( (b1&0x3)<<4 | b2>>4&0x0f );
			c3 = (byte)( (b2&0x0f)<<2 );
			charData[cIndex++] = CHARS[c1];
			charData[cIndex++] = CHARS[c2]; 
			charData[cIndex++] = CHARS[c3];
			charData[cIndex++] = '='; 
		}
		StringBuilder sb = new StringBuilder( new String(charData) );
		int j=MAX_LINE_LENGTH;
		while( j < sb.length() ) {
			sb.insert(j, "\r\n");
			j +=MAX_LINE_LENGTH+2; // addition of 2 for length of "\r\n"
		}
		return sb.toString();
	}
	
	public static byte[] decode(final String base64str) throws UnsupportedEncodingException{
		String str = base64str.replaceAll("\r\n", "");
		int numOfEquals = str.endsWith("==") ? 2 : str.endsWith("=") ? 1 : 0;
		if( str.length()==numOfEquals )
			return new byte[0];
		int bLength = (str.length()/4)*3 - numOfEquals;
		byte[] byteData = new byte[ bLength ];
		int bIndex=0, cIndex=0;
		int last = numOfEquals==0 ? str.length() : str.length()-4;
		byte cb1, cb2, cb3, cb4, b1, b2, b3;
		while( cIndex<last ) {
			cb1 = INVERSED_CHARS[ str.charAt(cIndex++) ]; 
			cb2 = INVERSED_CHARS[ str.charAt(cIndex++) ]; 
			cb3 = INVERSED_CHARS[ str.charAt(cIndex++) ]; 
			cb4 = INVERSED_CHARS[ str.charAt(cIndex++) ];
			b1 = (byte)(cb1<<2 | cb2>>4&0x03);
			b2 = (byte)( (cb2&0x0f)<<4 | cb3>>2&0x0f);
			b3 = (byte)( (cb3&0x03)<<6 | cb4);
			byteData[bIndex++] = b1;
			byteData[bIndex++] = b2;
			byteData[bIndex++] = b3;
		}
		if(numOfEquals==1) {
			cb1 = INVERSED_CHARS[ str.charAt(cIndex++) ]; 
			cb2 = INVERSED_CHARS[ str.charAt(cIndex++) ]; 
			cb3 = INVERSED_CHARS[ str.charAt(cIndex++) ];
			b1 = (byte)(cb1<<2 | cb2>>4&0x03);
			b2 = (byte)( (cb2&0x0f)<<4 | cb3>>2&0x0f);
			byteData[bIndex++] = b1;
			byteData[bIndex++] = b2;
		}
		else if(numOfEquals==2) {
			cb1 = INVERSED_CHARS[ str.charAt(cIndex++) ]; 
			cb2 = INVERSED_CHARS[ str.charAt(cIndex++) ]; 
			b1 = (byte)(cb1<<2 | cb2>>4&0x03);
			b2 = (byte)( (cb2&0x0f)<<4);
			byteData[bIndex++] = b1;
			byteData[bIndex++] = b2;
		}
		return byteData;
	}
	
	public static void main(String[] args) throws IOException {
		byte data[] = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.".getBytes();
		int repeat = 100000;
		System.out.println( new String(decode( encode("bakalım türkçe karakterlerde nasıl bir sorun yaşayacağız. BİRDE AYNI TÜRKÇE KARAKTERLERİ BİRDE BÜYÜK HARFLE YAZALIM BÖYLE DENEYİNCE NASIL OLDUĞUNU GÖRMEK İSTİYORUM. bu adamında şiiri baya bir uzunumuş ama ben onu geçerim, bakalım nereye kadar götürecek bizi".getBytes()) ) ));
		String encoded = encode(data);
//		for(int i=0; i<repeat; i++) {
//			java.util.Base64.getEncoder().encode(data);
//		}
//		System.gc();
//		tt.addTrace("after new BASE64Encoder().encode");
		for(int i=0; i<repeat;i++) {
			encode(data);
		}
		System.gc();
//		tt.addTrace("after Base64.encode");
//		for(int i=0; i<repeat;i++) {
//			new String(org.apache.commons.codec.binary.Base64.encodeBase64(data, true));
//		}
//		System.gc();
//		tt.addTrace("after commons Base64.encode");
//		for(int i=0; i<repeat; i++) {
//			java.util.Base64.getDecoder().decode(encoded);
//		}
		System.gc();
		for(int i=0; i<repeat;i++) {
			decode(encoded);
		}
		System.gc();
	}
}
