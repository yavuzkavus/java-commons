package com.readjournal.util;

import java.io.IOException;
import java.io.StringReader;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class HtmlStripper extends HTMLEditorKit.ParserCallback {
	private StringBuilder sb;
	private boolean keepNewLine;

	public HtmlStripper() {
		this(false);
	}

	public HtmlStripper(boolean keepNewLine) {
		this.keepNewLine = keepNewLine;
	}

	public String parse(String html) {
		if( StringUtil.empty(html) )
			return html;
		sb = new StringBuilder();
		ParserDelegator delegator = new ParserDelegator();
		try {
			StringReader reader = new StringReader(html);
			delegator.parse(reader, this, true);
			reader.close();
			if( sb.length()>0 )
				sb.setLength( sb.length()-1 );
			String text = sb.toString().replace('\u00A0', ' ');
			sb = null;
			return text;
		}
		catch (IOException e) {
			return null;
		}
	}

	public boolean isKeepNewLine() {
		return keepNewLine;
	}

	public void setKeepNewLine(boolean keepNewLine) {
		this.keepNewLine = keepNewLine;
	}

	@Override
	public void handleText(char[] text, int pos) {
		sb.append(text).append(' ');
	}

	@Override
	public void handleSimpleTag(Tag t, MutableAttributeSet a, int pos) {
		if( isKeepNewLine() && (t==Tag.BR || t==Tag.HR) )
			sb.append('\n');
	}

	@Override
	public void handleStartTag(Tag t, MutableAttributeSet a, int pos) {
		if( keepNewLine &&(	t==Tag.DIV	||
							t==Tag.P	||
							t==Tag.H1	||
							t==Tag.H2	||
							t==Tag.H3	||
							t==Tag.H4	||
							t==Tag.H5	||
							t==Tag.H6	||
							t==Tag.TR	||
							t==Tag.FRAME ) )
			sb.append('\n');
	}

	@Override
	public void handleEndTag(Tag t, int pos) {
		if( keepNewLine && (sb.length()>0 ? sb.charAt(sb.length()-1) : '\0')!='\n' &&
						   (t==Tag.DIV	||
							t==Tag.P	||
							t==Tag.H1	||
							t==Tag.H2	||
							t==Tag.H3	||
							t==Tag.H4	||
							t==Tag.H5	||
							t==Tag.H6	||
							t==Tag.TR	||
							t==Tag.FRAME||
							t==Tag.LI ) )
				sb.append('\n');
	}

	public static void main(String[] args) throws Exception {
		String text = 	"<html>" +
							"<body>" +
								"&#160;&#160;&#160;deneme" +
								"<hr/>" +
								"<h1>dfakldjflka</h2>"+
								"<br /><br></br>" +
								"<div>dfakldjflka</div>" +
								"<p><strong>strong</strong></p>" +
								"<ul><li>1</li><li>2<li>3</li></ul>" +
								"<a href='#' style=\"background-color: red;\">link</a>"+
							"</body>" +
						"</html>";
		System.out.println(StringUtil.stripHtml(text));
		System.out.println( "***********************************************" );
		System.out.println(StringUtil.stripHtml(text, true));
		System.out.println( "***********************************************" );
	}
}