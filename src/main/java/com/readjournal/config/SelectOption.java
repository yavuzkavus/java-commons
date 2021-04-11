package com.readjournal.config;

import com.readjournal.util.Utils;

public class SelectOption {
	public static SelectOption[] EMPTY_ARRAY = new SelectOption[0];
	
	private String value;
	private String label;

	public SelectOption(String value) {
		this(value, value);
	}

	public SelectOption(String value, String label) {
		super();
		this.value = value;
		this.label = label;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	@Override
	public boolean equals(Object obj) {
		if( obj==null || !SelectOption.class.isAssignableFrom(obj.getClass()) )
			return false;
		String otherValue = obj==null ? null : ((SelectOption)obj).getValue();
		return Utils.equals(value, otherValue);
	}
	@Override
	public String toString() {
		return label;
	}
}
