package com.readjournal.config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.readjournal.util.StringUtil;

public class Setting implements Cloneable {
	public enum InputType { TEXT, PASSWORD, SELECT, CHECKBOX, RADIO,TEXTAREA }
	public enum ValidationType {LONG, DOUBLE, HOST, URL, BOOLEAN, DIRECTORY, FILE, PASSWORD}

	public static final String PASSWORD_PATTERN = "^[a-zA-Z0-9_\\-.\"'!#$@^+%&/{}()\\[\\]:;~\\\\?*ğĞüÜiİşŞçÇöÖ]+$";
	public static final String IP_PATTERN =
			"^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."+
			"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."+
			"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."+
			"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
	public static final String DOMAIN_PATTERN = "^(?:(?:[a-zA-Z]|[a-zA-Z][a-zA-Z0-9\\-_]*[a-zA-Z0-9])\\.)*(?:[A-Za-z]|[A-Za-z][A-Za-z0-9\\-_]*[A-Za-z0-9])$";

	private String name;
	private String label;
	private String value;
	private Number max, min;
	private InputType type;
	private ValidationType validationType;
	private SelectOption[] options;
	private boolean required;
	private boolean requireRestart;
	
	public Setting(	String name, 
					String label, 
					String value, 
					InputType type ) {
		this(name, label, value, type, null, true, false, null, null, null);
	}
	
	public Setting(	String name, 
					String label, 
					String value, 
					InputType type, 
					ValidationType validationType ) {
		this(name, label, value, type, validationType, true, false, null, null, null);
	}
	
	public Setting(	String name, 
					String label, 
					String value, 
					InputType type, 
					boolean required,
					boolean requireRestart) {
		this(name, label, value, type, null, required, requireRestart, null, null, null);
	}
	
	public Setting(	String name, 
					String label, 
					String value, 
					InputType type, 
					ValidationType validationType,
					boolean required,
					boolean requireRestart) {
		this(name, label, value, type, validationType, required, requireRestart, null, null, null);
	}
	
	public Setting(	String name, 
					String label, 
					String value, 
					InputType type, 
					ValidationType validationType,
					SelectOption[] options) {
		this(name, label, value, type, validationType, true, false, options, null, null);
	}
	
	public Setting(	String name, 
					String label, 
					String value, 
					InputType type, 
					ValidationType validationType,
					boolean required,
					boolean requireRestart,
					SelectOption[] options) {
		this(name, label, value, type, validationType, required, required, options, null, null);
	}
	
	public Setting(	String name, 
					String label, 
					String value, 
					InputType type, 
					ValidationType validationType,
					Number min,
					Number max) {
		this(name, label, value, type, validationType, true, false, null, min, max);
	}
	
	public Setting(	String name, 
					String label, 
					String value, 
					InputType type, 
					ValidationType validationType,
					boolean required, 
					boolean requireRestart,
					Number min,
					Number max) {
		this(name, label, value, type, validationType, required, requireRestart, null, min, max);
	}

	public Setting(	String name, 
					String label, 
					String value, 
					InputType type, 
					ValidationType validationType,
					boolean required, 
					boolean requireRestart,
					SelectOption[] options, 
					Number min,
					Number max) {
		this.name = name;
		this.label = label;
		this.value = value;
		this.type = type;
		this.validationType = validationType;
		this.required = required;
		this.requireRestart = requireRestart;
		this.options = options;
		this.min = min;
		this.max = max;
	}

	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getName() {
		return name;
	}
	public InputType getType() {
		return type;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public SelectOption[] getOptions() {
		return options;
	}
	public Number getMin() {
		return min;
	}
	public Number getMax() {
		return max;
	}

	public boolean isText() {
		return this.type == InputType.TEXT;
	}

	public boolean isTextArea() {
		return this.type == InputType.TEXTAREA;
	}

	public boolean isPassword() {
		return this.type == InputType.PASSWORD;
	}

	public boolean isSelect() {
		return this.type == InputType.SELECT;
	}

	public boolean isCheckBox() {
		return this.type == InputType.CHECKBOX;
	}

	public boolean isRadio() {
		return this.type == InputType.RADIO;
	}

	public boolean isRequired() {
		return required;
	}
	
	public boolean isRequireRestart() {
		return requireRestart;
	}

	public String validate() {
		return validate(this, value);
	}

	public static String validate(Setting setting, String value) {
		boolean required = setting.required;
		if( StringUtil.empty(value) ) {
			if( required )
				return "Değer boş olamaz!";
			else
				return null;
		}
		ValidationType validationType = setting.validationType;
		if(validationType==null)
			return null;
		Number	min = setting.min,
				max = setting.max;
		if(validationType==ValidationType.LONG) {
			if( !isValidLong(value) )
				return "Değer tam sayı olmalı!";
			long ival = Long.parseLong(value);
			if( max!=null && max.longValue()<ival )
				return "Girilebilecek en büyük değer " + max + "!";
			if( min!=null && min.longValue()>ival )
				return "Girilebilecek en küçük değer " + min + "!";
			return null;
		}
		if(validationType==ValidationType.DOUBLE) {
			if( !isValidDouble(value) )
				return "Değer kesirli sayı olmalı!";
			double fval = Double.parseDouble(value);
			if( max!=null && max.doubleValue()<fval )
				return "Girilebilecek en büyük değer " + max + "!";
			if( min!=null && min.doubleValue()>fval )
				return "Girilebilecek en küçük değer " + min + "!";
			return null;
		}
		if(validationType==ValidationType.URL) {
			if( !isValidUrl(value) )
				return "Değer geçerli url olmalı!";
			return null;
		}
		if(validationType==ValidationType.BOOLEAN) {
			if( !isValidBoolean(value))
				return "Değer true/false olmalı!";
			return null;
		}
		if(validationType==ValidationType.PASSWORD) {
			if( !value.matches(PASSWORD_PATTERN) )
				return "Geçerli bir şifre giriniz!";
			return null;
		}
		if(validationType==ValidationType.HOST) {
			if( Character.isDigit(value.trim().charAt(0)) ) {
				if( !value.matches(IP_PATTERN) )
					return "Geçerli bir ip veya domain adı giriniz!";
			}
			else if( !value.matches(DOMAIN_PATTERN) )
				return "Geçerli bir ip veya domain adı giriniz!";
			return null;
		}
		if(validationType==ValidationType.DIRECTORY) {
			try {
				File file = new File(value);
				if( !file.exists() )
					return "Dizin bulunamadı!";
				else if( !file.isDirectory() )
					return "Girilen değer dizin olmalı!";
			}
			catch(Exception ex) {
				return "Değer geçerli bir dizin olmalı!";
			}
			return null;
		}
		if(validationType==ValidationType.FILE) {
			try {
				File file = new File(value);
				if( !file.exists() )
					return "Dosya bulunamadı!";
				if( !file.isFile() )
					return "Girilen değer dosya olmalı!";
			}
			catch(Exception ex) {
				return "Değer geçerli bir dosya olmalı!";
			}
			return null;
		}
		return "Geçerli bir değer giriniz";
	}
	
	public static boolean isValidLong(String str) {
		try {
			Long.parseLong(str);
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	public static boolean isValidDouble(String str) {
		try {
			Double.parseDouble(str);
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	public static boolean isValidUrl(String url) {
		try {
			new URL(url);
		}
		catch (MalformedURLException e) {
			return false;
		}
		return true;
	}

	public static boolean isValidBoolean(String str) {
		try {
			Boolean.parseBoolean(str);
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public Setting clone() {
		try {
			return (Setting)super.clone();
		} catch (CloneNotSupportedException e) { }
		return null;
	}
}