package com.readjournal.util;

import java.io.Serializable;

@SuppressWarnings("serial")
public class IntStr implements Comparable<IntStr>, Serializable {
	public static final IntStr[] EMPTY_ARRAY = new IntStr[0];
	
	private int integer;
	private String string;

	public IntStr() { }

	public IntStr(int integer, String string) {
		this.integer = integer;
		this.string = string;
	}
	public int getInt() {
		return integer;
	}
	public void setInt(int integer) {
		this.integer = integer;
	}
	public void increaseInt() {
		this.integer++;
	}
	public void increaseInt(int i) {
		this.integer+=i;
	}
	public void decreaseInt() {
		this.integer--;
	}
	public void decreaseInt(int i) {
		this.integer-=i;
	}
	public String getStr() {
		return string;
	}
	public void setStr(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}

	@Override
	public boolean equals(Object _obj) {
		if( !(_obj instanceof IntStr) )
			return false;
		IntStr obj = (IntStr)_obj;
		return obj.getInt()==getInt() && Utils.equals(obj.getStr(), getStr());
	}

	@Override
	public int compareTo(IntStr o) {
		if( o==null )
			return 1;
		if( string==null && o.string==null )
			return 0;
		return string.compareTo(o==null ? null : o.string);
	}
}
