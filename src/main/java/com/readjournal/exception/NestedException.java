package com.readjournal.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * @author
 *         http://weblogs.java.net/blog/crazybob/archive/2004/02/exception_handl.
 *         html
 */
@SuppressWarnings("serial")
public class NestedException extends RuntimeException {

	private NestedException(Throwable t) {
		super(t);
	}

	public static RuntimeException wrap(Throwable t) {
		if (t instanceof RuntimeException)
			return (RuntimeException) t;
		return new NestedException(t);
	}

	@Override
	public void printStackTrace() {
		this.getCause().printStackTrace();
	}

	@Override
	public String getMessage() {
		return getCause().getMessage();
	}

	@Override
	public String getLocalizedMessage() {
		return getCause().getLocalizedMessage();
	}

	@Override
	public StackTraceElement[] getStackTrace() {
		return getCause().getStackTrace();
	}

	@Override
	public void printStackTrace(PrintStream s) {
		getCause().printStackTrace(s);
	}

	@Override
	public void printStackTrace(PrintWriter s) {
		getCause().printStackTrace(s);
	}

	@Override
	public String toString() {
		return getCause().toString();
	}
}
