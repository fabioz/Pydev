/*
 * Author: atotic
 * Created: July 10, 2003
 * License: Common Public License v1.0
 */
 
 package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.graphics.Color;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.ui.ColorCache;


/**
 * Adds simple partitioner, and specific behaviors like double-click actions to the TextWidget.
 * 
 * <p>Implements a simple partitioner that does syntax highlighting.
 * 
 * <p>TODO: doubleClickStragegy?
 * 
 */
public class PyEditConfiguration extends SourceViewerConfiguration {
	private ColorCache colorCache;

	public PyEditConfiguration(ColorCache colorManager) {
		colorCache = colorManager;
	}

	/**
	  * @return PyAutoIndentStrategy which deals with spaces/tabs
	  */
	 public IAutoIndentStrategy getAutoIndentStrategy(ISourceViewer sourceViewer,String contentType) {
		 return new PyAutoIndentStrategy();
	 }

	/**
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getTabWidth(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public int getTabWidth(ISourceViewer sourceViewer) {
		return PydevPlugin.getDefault().getPluginPreferences().getInt(PydevPrefs.TAB_WIDTH);
	}
    
	/**
	 * @param color - default return color of this scanner
	 * @return scanner with no rules, that colors all text with default color
	 */
	private RuleBasedScanner getColoredScanner(Color color) {
		RuleBasedScanner scanner = new RuleBasedScanner();
		TextAttribute style = new TextAttribute(color);
		scanner.setDefaultReturnToken(new Token(style));
		return scanner;
	}
	
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
				"try","while","yield","None" };

		public GreatKeywordDetector() {
		}
		public boolean isWordStart(char c) {
			return Character.isJavaIdentifierStart(c);
		}
		public boolean isWordPart(char c) {
			return Character.isJavaIdentifierPart(c);
		}
	}
	
	/** 
	 * Code scanner colors keywords
	 * 
	 * TODO: something for spaces at the beginning of the line?
	 */
	private RuleBasedScanner getCodeScanner() {
		RuleBasedScanner codeScanner = new RuleBasedScanner();
		IToken keywordToken = new Token(
				new TextAttribute(colorCache.getNamedColor(PydevPrefs.KEYWORD_COLOR)));
		IToken defaultToken = new Token(
				new TextAttribute(colorCache.getNamedColor("BLACK")));
		IToken errorToken = new Token(
				new TextAttribute(colorCache.getNamedColor("BLACK"))); // You can make it RED for fun display
		codeScanner.setDefaultReturnToken(errorToken);
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
		codeScanner.setRules(result);
		return codeScanner;
	}
 
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr;

		// DefaultDamagerRepairer implements both IPresentationDamager, IPresentationRepairer 
		// IPresentationDamager::getDamageRegion does not scan, just 
		// returns the intersection of document event, and partition region
		// IPresentationRepairer::createPresentation scans
		// gets each token, and sets text attributes according to token
		
		// We need to cover all the content types from PyPartitionScanner

		// Comments have uniform color
		dr = new DefaultDamagerRepairer(
				getColoredScanner((colorCache.getNamedColor(PydevPrefs.COMMENT_COLOR))));
		reconciler.setDamager(dr, PyPartitionScanner.PY_COMMENT);
		reconciler.setRepairer(dr, PyPartitionScanner.PY_COMMENT);

		// Strings have uniform color
		dr = new DefaultDamagerRepairer(
				getColoredScanner((colorCache.getNamedColor(PydevPrefs.STRING_COLOR))));
		reconciler.setDamager(dr, PyPartitionScanner.PY_SINGLELINE_STRING);
		reconciler.setRepairer(dr, PyPartitionScanner.PY_SINGLELINE_STRING);
		reconciler.setDamager(dr, PyPartitionScanner.PY_MULTILINE_STRING);
		reconciler.setRepairer(dr, PyPartitionScanner.PY_MULTILINE_STRING);
	
		// Default content is code, we need syntax highlighting
		dr = new DefaultDamagerRepairer(
			getCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		return reconciler;
	}
}