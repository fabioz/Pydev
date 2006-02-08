package org.python.pydev.editor.refactoring;

public class TooManyMatchesException extends RuntimeException {

	private int matches;

	public TooManyMatchesException(String msg, int matches) {
		super(msg);
		this.setMatches(matches);
	}

	public void setMatches(int matches) {
		this.matches = matches;
	}

	public int getMatches() {
		return matches;
	}

}
