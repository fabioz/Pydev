/*
 * @author: atotic, Scott Schlesier
 * Created: March 5, 2005
 * License: Common Public License v1.0
 */
package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.ui.ColorCache;

/**
 * PyCodeScanner - A scanner that looks for python keywords and code
 * and supports the updating of named colors through the colorCache
 * 
 * GreatWhite, GreatKeywordDetector came from PyEditConfiguration
 */
public class PyCodeScanner extends RuleBasedScanner {
	
	private ColorCache colorCache;
	
	/**
	 * Whitespace detector.
	 * 
	 * I know, naming the class after a band that burned
	 * is not funny, but I've got to get my brain off my
	 * annoyance with the abstractions of JFace.
	 * So many classes and interfaces for a single method?
	 * f$%@#$!!
	 */
	static private class GreatWhite implements IWhitespaceDetector {
		public boolean isWhitespace(char c) {return Character.isWhitespace(c);}
	};
	
	/**
	 * Python keyword detector
	 */
	static private class GreatKeywordDetector implements IWordDetector {
		// keywords list has to be alphabetized for this to work properly
		static public String[] keywords = {
				"and","as","assert","break","class","continue",
				"def","del","elif","else","except","exec",
				"finally","for","from","global",
				"if","import","in","is","lambda","not",
				"or","pass","print","raise","return",
				"try","while","yield","False", "None", "True" };

		public GreatKeywordDetector() {
		}
		public boolean isWordStart(char c) {
			return Character.isJavaIdentifierStart(c);
		}
		public boolean isWordPart(char c) {
			return Character.isJavaIdentifierPart(c);
		}
	}
	
	public PyCodeScanner(ColorCache colorCache) {
		super();
		this.colorCache = colorCache;
		
		setupRules();
	}
	
	public void updateColors() {
		setupRules();
	}
	
	private void setupRules() {
		IToken keywordToken = new Token(
				new TextAttribute(colorCache.getNamedColor(PydevPrefs.KEYWORD_COLOR)));
		IToken defaultToken = new Token(
				new TextAttribute(colorCache.getNamedColor(PydevPrefs.CODE_COLOR)));
		IToken errorToken = new Token(
				new TextAttribute(colorCache.getNamedColor(PydevPrefs.CODE_COLOR))); // Includes operators, brackets, numbers etc.
		setDefaultReturnToken(errorToken);
		List rules = new ArrayList();
		
		// Scanning strategy:
		// 1) whitespace
		// 2) code
		// 3) regular words?
		
		rules.add(new WhitespaceRule(new GreatWhite()));
		
		WordRule wordRule = new WordRule(new GreatKeywordDetector(), defaultToken);
		for (int i=0; i<GreatKeywordDetector.keywords.length;i++) {
			wordRule.addWord(GreatKeywordDetector.keywords[i], keywordToken);
		}
		rules.add(wordRule);

		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}
}
