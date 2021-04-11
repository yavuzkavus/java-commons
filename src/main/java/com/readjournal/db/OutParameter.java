package com.readjournal.db;

public class OutParameter<T> {
	private T value;

	public OutParameter() { }

	public OutParameter(T value) {
		this.value = value;
	}

	public void setValue(T wrapped) {
		this.value = wrapped;
	}

	public T getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value==null ? super.toString() : value.toString();
	}
}
