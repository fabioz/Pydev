package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.python.pydev.core.IToken;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;

/**
 * This class defines a module that represents a given java class within a java project 
 * (that's referenced from a jython project).
 *
 * @author Fabio
 */
public class JavaClassModuleInProject extends AbstractJavaClassModule {

    private static final boolean DEBUG_CLASS_MODULE_IN_PROJECT = false;
    
    private IJavaProject javaProject;

    /**
     * @param name that's the name of the module for jython
     * @param javaProject that's the project where it exists.
     */
    protected JavaClassModuleInProject(String name, IJavaProject javaProject) {
        super(name);
        this.javaProject = javaProject;
        
        if(DEBUG_CLASS_MODULE_IN_PROJECT){
            System.out.println("Created JavaClassModuleInProject: "+name);
        }

        this.tokens = createTokens(name);
        if(DEBUG_CLASS_MODULE_IN_PROJECT){
            System.out.println("JavaClassModuleInProject tokens:");
            for(IToken t:this.tokens){
                System.out.println(t.getRepresentation());
            }
        }

    }

    /**
     * TODO: It doesn't have any file available?
     */
    @Override
    public File getFile() {
        return null;
    }
    
    
    /**
     * @see AbstractJavaClassModule#getJavaCompletionProposals(String, String)
     */
    @Override
    protected List<Tuple<IJavaElement, CompletionProposal>> getJavaCompletionProposals(String completeClassDesc, String filterCompletionName)
            throws Exception {
        String contents;
        if(filterCompletionName != null){
            //pre-filter it a bit if we already know the completion name
            contents = "new %s().%s";
            contents = StringUtils.format(contents, completeClassDesc, completeClassDesc, filterCompletionName);
            
        }else{
            contents = "new %s().";
            contents = StringUtils.format(contents, completeClassDesc, completeClassDesc);
        }
        
        return getJavaCompletionProposals(contents, contents.length(), filterCompletionName);
    }

    /**
     * @see AbstractJavaClassModule#getJavaCompletionProposals(String, int, String)
     * 
     * @note: the completionOffset is ignored (we find the type and go for the completions on that type).
     */
    @Override
    protected List<Tuple<IJavaElement, CompletionProposal>> getJavaCompletionProposals(String contents, int completionOffset,
            String filterCompletionName) throws Exception {
        try {
            IType type = this.javaProject.findType(name);
            
            final List<Tuple<IJavaElement, CompletionProposal>> ret = new ArrayList<Tuple<IJavaElement, CompletionProposal>>();
            ICompilationUnit unit = type.getCompilationUnit();
            CompletionProposalCollector collector = createCollector(filterCompletionName, ret, unit);
            type.codeComplete(StringUtils.format(contents, name).toCharArray(), -1, 0, new char[0][0], new char[0][0], new int[0], false, collector);
            return ret;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }   
        
        

    }

}
