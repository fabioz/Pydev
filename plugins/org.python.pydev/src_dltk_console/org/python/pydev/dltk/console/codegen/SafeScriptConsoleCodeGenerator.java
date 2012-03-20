package org.python.pydev.dltk.console.codegen;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;

/**
 * Wrapper around an unknown IScriptConsoleCodeGenerator that catches and logs
 * exceptions using {@link SafeRunner}. 
 */
public class SafeScriptConsoleCodeGenerator implements IScriptConsoleCodeGenerator {
	private final IScriptConsoleCodeGenerator unsafeGenerator;

	private boolean hasPyCode;
	private String pyCode;

	private final class HasPyCodeRunnable implements ISafeRunnable {

		public void run() throws Exception {
			hasPyCode = false;
			hasPyCode = unsafeGenerator.hasPyCode();
		}

		public void handleException(Throwable exception) {
			hasPyCode = false;
		}
	}

	private final class GetPyCodeRunnable implements ISafeRunnable {

		public void run() throws Exception {
			pyCode = null;
			pyCode = unsafeGenerator.getPyCode();
		}

		public void handleException(Throwable exception) {
			pyCode = null;
		}
	}

	/**
	 * Create a Safe wrapped generator for a possibly unsafe one.
	 * @param unsafeGenerator generator to wrap
	 */
	public SafeScriptConsoleCodeGenerator(IScriptConsoleCodeGenerator unsafeGenerator) {
		this.unsafeGenerator = unsafeGenerator;
	}

	/**
	 * Calls nested generators getPyCode in a SafeRunner, on any exception
	 * returns null
	 */
	public String getPyCode() {
		try {
			SafeRunner.run(new GetPyCodeRunnable());
			return pyCode;
		} finally {
			pyCode = null;
		}
	}

	/**
	 * Calls nested generators getPyCode in a SafeRunner, on any exception
	 * returns false
	 */
	public boolean hasPyCode() {
		SafeRunner.run(new HasPyCodeRunnable());
		return hasPyCode;
	}

}
