/*
 * Created on Jul 1, 2006
 * @author Fabio
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.parser.visitors.NodeUtils;

public class PyCalltipsContextInformationFromIToken implements IPyCalltipsContextInformation{

    private final IToken token;

    /** 
     * The arguments to be displayed. 
     */
    private String argumentsWithParens;
    
    /** 
     * The information to be displayed (calculated when requested)
     */
    private String argumentsWithoutParens;
    
    /** 
     * The image to be displayed.
     */
    private final Image fImage;
    
    /**
     * The place where the replacement started.
     */
    private final int fReplacementOffset;
    
    private final String defaultArguments;

    /**
     * Creates a new context information without an image.
     *
     * @param argumentsWithParens the arguments available.
     * @param replacementOffset the offset where the replacement for the arguments started (the place right after the
     * parenthesis start)
     * @param i 
     */
    public PyCalltipsContextInformationFromIToken(IToken token, String defaultArguments, int replacementOffset) {
        Assert.isNotNull(token);
        fImage= null;
        fReplacementOffset = replacementOffset;
        this.defaultArguments = defaultArguments;
        this.token = token;
    }


    /*
     * @see IContextInformation#equals(Object)
     */
    public boolean equals(Object object) {
        if (object instanceof PyCalltipsContextInformationFromIToken) {
            PyCalltipsContextInformationFromIToken contextInformation= (PyCalltipsContextInformationFromIToken) object;
            contextInformation.calculateArgumentsWithParens();
            this.calculateArgumentsWithParens();
            return argumentsWithParens.equalsIgnoreCase(contextInformation.argumentsWithParens);
        }
        return false;
    }

    private void calculateArgumentsWithParens() {
        if(argumentsWithParens == null){
            if(token instanceof SourceToken){
                SourceToken sourceToken = (SourceToken) token;
                SimpleNode ast = sourceToken.getAst();
                if(ast != null){
                    if(ast instanceof ClassDef){
                        ast = NodeUtils.getClassDefInit((ClassDef) ast);
                    }
                    if(ast instanceof FunctionDef){
                        FunctionDef functionDef = (FunctionDef) ast;
                        if(functionDef.args != null){
                            String printed = PrettyPrinterV2.printArguments(new IGrammarVersionProvider() {
                                
                                @Override
                                public int getGrammarVersion() throws MisconfigurationException {
                                    return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0;
                                }
                            }, functionDef.args);
                            if(printed != null){
                                if(!printed.startsWith("(")){
                                    printed = "("+printed+")";
                                }
                                argumentsWithParens = printed;
                            }
                        }
                    }
                }
            }
            if(argumentsWithParens == null){
                //still not found: use default
                argumentsWithParens = defaultArguments;
            }
        }
        
    }


    /*
     * @see java.lang.Object#hashCode()
     * @since 3.1
     */
    public int hashCode() {
        calculateArgumentsWithParens();
        return argumentsWithParens.hashCode();
    }

    /*
     * @see IContextInformation#getInformationDisplayString()
     */
    public String getInformationDisplayString() {
        if(argumentsWithoutParens == null){
            calculateArgumentsWithParens();
            argumentsWithoutParens = argumentsWithParens.substring(1, argumentsWithParens.length()-1); //remove the parenthesis
        }
        return argumentsWithoutParens;
    }

    /*
     * @see IContextInformation#getImage()
     */
    public Image getImage() {
        return fImage;
    }

    /*
     * @see IContextInformation#getContextDisplayString()
     */
    public String getContextDisplayString() {
        return getInformationDisplayString();
    }


    public int getShowCalltipsOffset() {
        return this.fReplacementOffset;
    }



}
