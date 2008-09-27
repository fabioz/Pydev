/*
 * @author: atotic, Scott Schlesier
 * Created: March 5, 2005
 * License: Common Public License v1.0
 */
package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.ui.ColorCache;

/**
 * PyCodeScanner - A scanner that looks for python keywords and code
 * and supports the updating of named colors through the colorCache
 * 
 * GreatWhite, GreatKeywordDetector came from PyEditConfiguration
 */
public class PyCodeScanner extends RuleBasedScanner {
    
    // keywords list has to be alphabetized for the keyword detector to work properly
    static final public String[] KEYWORDS = {
        "and","as","assert","break","class","continue",
        "def","del","elif","else","except","exec",
        "finally","for","from","global",
        "if","import","in","is","lambda","not",
        "or","pass","print","raise","return",
        "self", "try","while","with","yield","False", "None", "True" };

    private ColorCache colorCache;

    private IToken keywordToken  ; 
    private IToken selfToken     ; 
    private IToken defaultToken  ; 
    private IToken decoratorToken; 
    private IToken numberToken   ; 
    private IToken classNameToken; 
    private IToken funcNameToken ; 
    
    
    
    
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
    }
    
    /**
     * Python keyword detector
     */
    static private class GreatKeywordDetector implements IWordDetector {

        public GreatKeywordDetector() {
        }
        public boolean isWordStart(char c) {
            return Character.isJavaIdentifierStart(c);
        }
        public boolean isWordPart(char c) {
            return Character.isJavaIdentifierPart(c);
        }
    }
    
    static private class DecoratorDetector implements IWordDetector{

        /**
         * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
         */
        public boolean isWordStart(char c) {
            return c == '@';
        }

        /**
         * @see org.eclipse.jface.text.rules.IWordDetector#isWordPart(char)
         */
        public boolean isWordPart(char c) {
            return c != '\n' && c != '\r' && c != '(';
        }
        
    }
    
    static public class NumberDetector implements IWordDetector{

        /**
         * Used to keep the state of the token
         */
        private FastStringBuffer buffer = new FastStringBuffer();
        
        /**
         * Defines if we are at an hexa number
         */
        private boolean isInHexa;
        
        /**
         * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
         */
        public boolean isWordStart(char c) {
            isInHexa = false;
            buffer.clear();
            buffer.append(c);
            return Character.isDigit(c);
        }

        /**
         * Check if we are still in the number
         */
        public boolean isWordPart(char c) {
            //ok, we have to test for scientific notation e.g.: 10.9e10
            
            if((c == 'x' || c == 'X') && buffer.length() == 1 && buffer.charAt(0) == '0'){
                //it is an hexadecimal
                buffer.append(c);
                isInHexa = true;
                return true;
            }else{
                buffer.append(c);
            }

            if(isInHexa){
                return Character.isDigit(c) || c == 'a'  || c == 'A'
                                            || c == 'b'  || c == 'B'
                                            || c == 'c'  || c == 'C'
                                            || c == 'd'  || c == 'D'
                                            || c == 'e'  || c == 'E'
                                            || c == 'f'  || c == 'F';
                
            }else{
                return Character.isDigit(c) || c == 'e'  || c == '.';
            }
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
        IPreferenceStore preferences = PydevPlugin.getChainedPrefStore();
        keywordToken   = new Token( new TextAttribute(colorCache.getNamedColor(PydevPrefs.KEYWORD_COLOR), null, preferences.getInt(PydevPrefs.KEYWORD_STYLE)));
        selfToken      = new Token( new TextAttribute(colorCache.getNamedColor(PydevPrefs.SELF_COLOR), null, preferences.getInt(PydevPrefs.SELF_STYLE)));
        defaultToken   = new Token( new TextAttribute(colorCache.getNamedColor(PydevPrefs.CODE_COLOR), null, preferences.getInt(PydevPrefs.CODE_STYLE)));
        decoratorToken = new Token( new TextAttribute(colorCache.getNamedColor(PydevPrefs.DECORATOR_COLOR), null, preferences.getInt(PydevPrefs.DECORATOR_STYLE)));
        numberToken    = new Token( new TextAttribute(colorCache.getNamedColor(PydevPrefs.NUMBER_COLOR), null, preferences.getInt(PydevPrefs.NUMBER_STYLE)));
        classNameToken = new Token( new TextAttribute(colorCache.getNamedColor(PydevPrefs.CLASS_NAME_COLOR), null, preferences.getInt(PydevPrefs.CLASS_NAME_STYLE)));
        funcNameToken  = new Token( new TextAttribute(colorCache.getNamedColor(PydevPrefs.FUNC_NAME_COLOR), null, preferences.getInt(PydevPrefs.FUNC_NAME_STYLE)));
        
        setDefaultReturnToken(defaultToken);
        List<IRule> rules = new ArrayList<IRule>();
        
        // Scanning strategy:
        // 1) whitespace
        // 2) code
        // 3) regular words?
        
        rules.add(new WhitespaceRule(new GreatWhite()));
        
        Map<String,IToken> defaults = new HashMap<String, IToken>();
        defaults.put("self", selfToken);
        
        PyWordRule wordRule = new PyWordRule(new GreatKeywordDetector(), defaultToken, classNameToken, funcNameToken);
        for (String keyword : KEYWORDS) {
            IToken token = defaults.get(keyword);
            if(token == null){
                token = keywordToken;
            }
            wordRule.addWord( keyword, token);
        }
        rules.add(wordRule);

        
        rules.add(new WordRule(new DecoratorDetector(), decoratorToken));
        rules.add(new WordRule(new NumberDetector(), numberToken));
        
        setRules(rules.toArray(new IRule[0]));
    }
}
