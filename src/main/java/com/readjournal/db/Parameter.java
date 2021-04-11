package com.readjournal.db;

import java.sql.Types;

public class Parameter {
	public static final Parameter[] EMPTY_ARRAY = new Parameter[0];
	
	public enum Mode { OUT, IN, INOUT };

	private Object object;
	private int argType = Types.INTEGER;
	private Mode argMode = Mode.OUT ;

	public Parameter(Object object, int argType, Mode argMode){
		this.object = object;
		this.argType = argType;
		this.argMode = argMode;
	}

	public Parameter(int argType){
		this(null, argType, Mode.OUT);
	}

	public Parameter(Object object, int argType){
		this(object, argType, Mode.IN);
	}

	public boolean isInput() {
		return argMode==Mode.IN || argMode==Mode.INOUT;
	}

	public boolean isOutput() {
		return argMode==Mode.OUT || argMode==Mode.INOUT;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object){
		this.object = object;
	}
	
	public int getArgType() {
		return argType;
	}
	
	public Mode getArgMode() {
		return argMode;
	}

	@Override
	public String toString() {
		return 	"[mode:" + (isInput() ? " input" : isOutput() ? "output" : "inout") +
			 	", arg type : " + argType + ", object : " + object + "]";
	}
}
