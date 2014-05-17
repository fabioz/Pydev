/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.PyCodeCompletionImages;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This is an abstract class for modules based on java classes.
 *
 * @author Fabio
 */
public abstract class AbstractJavaClassModule extends AbstractModule {

    public static final boolean DEBUG_JAVA_COMPLETIONS = false;

    protected static final CompiledToken[] EMPTY_ITOKEN = new CompiledToken[0];

    protected CompiledToken[] tokens;

    public static HashMap<String, String> replacementMap = new HashMap<String, String>();

    static {
        replacementMap.put("object", "obj");
        replacementMap.put("class", "class_");
        replacementMap.put("[QString", "str");
        replacementMap.put("[I", "int");
    }

    public boolean hasFutureImportAbsoluteImportDeclared() {
        return false;
    }

    protected AbstractJavaClassModule(String name) {
        super(name);
        checkJavaImageDescriptorCreated();
    }

    /**
     * Variable just to see if the java image descriptor was created.
     */
    private static boolean imageDescriptorCreated = false;

    /**
     * This method must be called to make sure that the java image descriptor is already initialized.
     */
    @SuppressWarnings("restriction")
    protected void checkJavaImageDescriptorCreated() {
        if (imageDescriptorCreated) {
            return;
        }
        //that's because if the JavaPlugin is not initialized, we'll have errors because it will try to create the
        //image descriptor registry from a non-display owner when making the completions (and in this way, we'll 
        //guarantee that its cache is already created).
        try {
            JavaPlugin.getImageDescriptorRegistry();
        } catch (Throwable e) {
            Display.getDefault().syncExec(new Runnable() {

                public void run() {
                    try {
                        JavaPlugin.getImageDescriptorRegistry();
                    } catch (Throwable e) {
                        //ignore it at this point
                    }
                }
            });
        }
        imageDescriptorCreated = true;
    }

    /**
     * This method will create the tokens for a given package.
     */
    protected CompiledToken[] createTokens(String packagePlusactTok) {
        ArrayList<CompiledToken> lst = new ArrayList<CompiledToken>();

        try {

            //TODO: if we don't want to depend on jdt inner classes, we should create a org.eclipse.jdt.core.CompletionRequestor
            //(it's not currently done because its API is not as easy to handle).
            //we should be able to check the CompletionProposalCollector to see how we can transform the info we want...
            //also, making that change, it should be faster, because we won't need to 1st create a java proposal to then
            //create a pydev token (it would be a single step to transform it from a Completion Proposal to an IToken).

            List<Tuple<IJavaElement, CompletionProposal>> elementsFound = getJavaCompletionProposals(packagePlusactTok,
                    null);

            HashMap<String, IJavaElement> generatedProperties = new HashMap<String, IJavaElement>();

            FastStringBuffer tempBuffer = new FastStringBuffer(128);
            for (Tuple<IJavaElement, CompletionProposal> element : elementsFound) {
                IJavaElement javaElement = element.o1;
                String args = "";
                if (javaElement instanceof IMethod) {
                    tempBuffer.clear();
                    tempBuffer.append("()");
                    IMethod method = (IMethod) javaElement;
                    for (String param : method.getParameterTypes()) {
                        if (tempBuffer.length() > 2) {
                            tempBuffer.insert(1, ", ");
                        }

                        //now, let's make the parameter 'pretty'
                        String lastPart = FullRepIterable.getLastPart(param);
                        if (lastPart.length() > 0) {
                            lastPart = PyAction.lowerChar(lastPart, 0);
                            if (lastPart.charAt(lastPart.length() - 1) == ';') {
                                lastPart = lastPart.substring(0, lastPart.length() - 1);
                            }
                        }

                        //we may have to replace it for some other word
                        String replacement = replacementMap.get(lastPart);
                        if (replacement != null) {
                            lastPart = replacement;
                        }
                        tempBuffer.insert(1, lastPart);
                    }
                    args = tempBuffer.toString();

                    String elementName = method.getElementName();
                    if (elementName.startsWith("get") || elementName.startsWith("set")) {
                        //Create a property for it
                        tempBuffer.clear();
                        elementName = elementName.substring(3);
                        if (elementName.length() > 0) {
                            tempBuffer.append(Character.toLowerCase(elementName.charAt(0)));
                            tempBuffer.append(elementName.substring(1));

                            String propertyName = tempBuffer.toString();
                            IJavaElement existing = generatedProperties.get(propertyName);
                            if (existing != null) {
                                if (existing.getElementName().startsWith("set")) {
                                    //getXXX has precedence over the setXXX.
                                    generatedProperties.put(propertyName, javaElement);
                                }
                            } else {
                                generatedProperties.put(propertyName, javaElement);
                            }
                        }
                    }

                }
                if (DEBUG_JAVA_COMPLETIONS) {
                    System.out.println("Element: " + javaElement);
                }

                lst.add(new JavaElementToken(javaElement.getElementName(), "", args, this.name, getType(javaElement
                        .getElementType()), javaElement, element.o2));

            }

            //Fill our generated properties.
            for (Entry<String, IJavaElement> entry : generatedProperties.entrySet()) {
                IJavaElement javaElement = entry.getValue();
                lst.add(new JavaElementToken(entry.getKey(), "", "", this.name, IToken.TYPE_ATTR, javaElement,
                        PyCodeCompletionImages.getImageForType(IToken.TYPE_ATTR)));
            }
        } catch (Exception e) {
            Log.log(e);
        }

        return lst.toArray(new CompiledToken[lst.size()]);
    }

    /**
     * Stores the mapping from the java type to the IToken type
     */
    private static HashMap<Integer, Integer> typesMapping = new HashMap<Integer, Integer>();
    static {
        typesMapping.put(IJavaElement.CLASS_FILE, IToken.TYPE_CLASS);
        typesMapping.put(IJavaElement.COMPILATION_UNIT, IToken.TYPE_CLASS);
        typesMapping.put(IJavaElement.PACKAGE_DECLARATION, IToken.TYPE_PACKAGE);
        typesMapping.put(IJavaElement.PACKAGE_FRAGMENT, IToken.TYPE_PACKAGE);
        typesMapping.put(IJavaElement.PACKAGE_FRAGMENT_ROOT, IToken.TYPE_PACKAGE);
    }

    /**
     * @param elementType the java element type we're interested in
     * @return the IToken type which is correspondent to the java type
     */
    private int getType(int elementType) {
        Integer found = typesMapping.get(elementType);
        if (found != null) {
            return found;
        }
        return IToken.TYPE_ATTR;
    }

    /**
     * Compiled modules do not have imports to be seen
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getWildImportedModules()
     */
    @Override
    public IToken[] getWildImportedModules() {
        return EMPTY_ITOKEN;
    }

    /**
     * Compiled modules do not have imports to be seen
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getTokenImportedModules()
     */
    @Override
    public IToken[] getTokenImportedModules() {
        return EMPTY_ITOKEN;
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getGlobalTokens()
     */
    @Override
    public IToken[] getGlobalTokens() {
        if (this.tokens == null) {
            return EMPTY_ITOKEN;
        }
        return this.tokens;
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getDocString()
     */
    @Override
    public String getDocString() {
        return "Java class module extension";
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getGlobalTokens(java.lang.String)
     */
    @Override
    public IToken[] getGlobalTokens(ICompletionState state, ICodeCompletionASTManager manager) {
        String actTok = state.getFullActivationToken();
        if (actTok == null) {
            actTok = state.getActivationToken();
        }
        if (actTok == null) {
            return new IToken[0];
        }
        String act = new FastStringBuffer(name, 2 + actTok.length()).append('.').append(actTok).toString();
        return createTokens(act);
    }

    @Override
    public boolean isInDirectGlobalTokens(String tok, ICompletionCache completionCache) {
        if (this.tokens != null) {
            return binaryHasObject(this.tokens, new CompiledToken(tok, "", "", "", 0));
        }
        return false;
    }

    @Override
    public boolean isInGlobalTokens(String tok, IPythonNature nature, ICompletionCache completionCache) {
        if (tok.indexOf('.') == -1) {
            return isInDirectGlobalTokens(tok, completionCache);
        } else {
            System.err.println("Still no treated isInDirectGlobalTokens with dotted string:" + tok);
            return false;
        }
    }

    /**
     * Gotten from Arrays.binarySearch (but returning boolean if key was found or not).
     * 
     * It also works directly with CompiledToken because we want a custom compare (from the representation)
     */
    private static boolean binaryHasObject(CompiledToken[] a, CompiledToken key) {
        int low = 0;
        int high = a.length - 1;

        while (low <= high) {
            int mid = (low + high) >> 1;
            CompiledToken midVal = a[mid];
            int cmp = midVal.getRepresentation().compareTo(key.getRepresentation());

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            }
            else {
                return true; // key found
            }
        }
        return false; // key not found.
    }

    /**
     * @param findInfo 
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#findDefinition(java.lang.String, int, int)
     */
    @Override
    public Definition[] findDefinition(ICompletionState state, int line, int col, IPythonNature nature)
            throws Exception {

        //try to see if that's a java class from a package... to do that, we must go iterating through the name found
        //to check if we're able to find modules with that name. If a module with that name is found, that means that 
        //we actually have a java class. 
        List<String> splitted = StringUtils.dotSplit(state.getActivationToken());
        FastStringBuffer modNameBuf = new FastStringBuffer(this.getName(), 128);
        IModule validModule = null;
        IModule module = null;
        int i = 0; //so that we know what will result in the tok
        for (String s : splitted) {
            modNameBuf.append(".");
            modNameBuf.append(s);
            module = nature.getAstManager().getModule(modNameBuf.toString(), nature, true, false);
            if (module != null) {
                validModule = module;
            } else {
                break;
            }
        }

        modNameBuf.clear();
        FastStringBuffer pathInJavaClass = modNameBuf; //get it cleared
        if (validModule == null) {
            validModule = this;
            pathInJavaClass.clear();
            pathInJavaClass.append(state.getActivationToken());
        } else {
            //After having found a valid java class, we must also check which was the resulting token within that class 
            //to check if it's some method or something alike (that should be easy after having the class and the path
            //to the method we want to find within it).
            if (!(validModule instanceof AbstractJavaClassModule)) {
                throw new RuntimeException("The module found from a java class module was found as another kind: "
                        + validModule.getClass());
            }
            for (int j = i; j < splitted.size(); j++) {
                if (j != i) {
                    pathInJavaClass.append('.');
                }
                pathInJavaClass.append(splitted.get(j));
            }
        }

        AbstractJavaClassModule javaClassModule = (AbstractJavaClassModule) validModule;

        IJavaElement elementFound = null;
        String foundAs;
        if (pathInJavaClass.length() == 0) {
            //ok, now, if there is no path, the definition is the java class itself.
            foundAs = "";
            elementFound = findJavaElement(javaClassModule.getName());

        } else {
            //ok, it's not the class directly, so, we have to check what it actually is.
            foundAs = pathInJavaClass.toString();
            List<Tuple<IJavaElement, CompletionProposal>> javaCompletionProposals = getJavaCompletionProposals(
                    javaClassModule.getName(), foundAs);
            if (javaCompletionProposals.size() > 0) {
                elementFound = javaCompletionProposals.get(0).o1;

            } else if (javaClassModule.getName().endsWith("." + foundAs)) {
                //This is the following case: we have a reference to the constructor (e.g.: javax.swing.JFrame.JFrame)
                //So, we have to ignore the last JFrame part.
                foundAs = "";
                elementFound = findJavaElement(javaClassModule.getName());
            }

        }

        if (elementFound != null) {
            return new Definition[] { new JavaDefinition(foundAs, javaClassModule, elementFound) };
        }

        //no definitions found
        return new Definition[0];
    }

    /**
     * @return tuple with:
     * - a list of tuples corresponding to the element and the proposal for the gotten elements
     * 
     */
    protected abstract IJavaElement findJavaElement(String javaClassModuleName) throws Exception;

    /**
     * Gets tuples with the java element and the corresponding completion proposal for that element.
     * 
     * @param completeClassDesc the name of the class from where we should get the tokens. E.g. java.lang.Class, javax.swing.JFrame
     * @param filterCompletionName if specified, only return matches from elements that have the name passed (otherwise it should be null)
     * @return a list of tuples corresponding to the element and the proposal for the gotten elements
     * @throws JavaModelException
     */
    protected abstract List<Tuple<IJavaElement, CompletionProposal>> getJavaCompletionProposals(
            String completeClassDesc, String filterCompletionName) throws Exception;

    /**
     * Gets tuples with the java element and the corresponding completion proposal for that element.
     * 
     * @param contents the contents that should be set for doing the code-completion
     * @param completionOffset the offset where the code completion should be requested
     * @param filterCompletionName if specified, only return matches from elements that have the name passed (otherwise it should be null)
     * @return a list of tuples corresponding to the element and the proposal for the gotten elements
     * @throws JavaModelException
     */
    protected abstract List<Tuple<IJavaElement, CompletionProposal>> getJavaCompletionProposals(String contents,
            int completionOffset, final String filterCompletionName) throws Exception;

    /**
     * Create a proposal collector that's able to gather the passed completions/related java elements and adds 
     * them to the passed 'ret' parameter
     * 
     * @param filterCompletionName may be null or a name to which we want to match the java element name (so, only
     * a java element with an exact match of its name to filterCompletionName is added).
     * 
     * @param ret the placeholder for the found java elements and completion proposals
     * @param unit the ICompilationUnit that's used (must be passed in the CompletionProposalCollector constructor).
     * @return the collector that will gather the completions (note that it'll keep the 'ret' placeholder alive unti it's 
     * garbage-collected.
     */
    protected CompletionProposalCollector createCollector(final String filterCompletionName,
            final List<Tuple<IJavaElement, CompletionProposal>> ret, ICompilationUnit unit) {
        CompletionProposalCollector collector = new CompletionProposalCollector(unit) {

            /**
             * Override the java proposal creation to always return null, as we'll keep just what we actually need.
             */
            @SuppressWarnings("restriction")
            @Override
            public IJavaCompletionProposal createJavaCompletionProposal(CompletionProposal proposal) {
                IJavaCompletionProposal javaCompletionProposal = super.createJavaCompletionProposal(proposal);
                if (javaCompletionProposal instanceof AbstractJavaCompletionProposal) {
                    AbstractJavaCompletionProposal prop = (AbstractJavaCompletionProposal) javaCompletionProposal;
                    IJavaElement javaElement = prop.getJavaElement();
                    if (javaElement != null) {

                        if (filterCompletionName == null) {
                            ret.add(new Tuple<IJavaElement, CompletionProposal>(javaElement, proposal));
                            return null;
                        }

                        if (javaElement.getElementName().equals(filterCompletionName)) {
                            ret.add(new Tuple<IJavaElement, CompletionProposal>(javaElement, proposal));
                            return null;
                        }

                    }
                }
                return null;
            }
        };
        return collector;
    }

    /**
     * For java, as we don't have __init__.py, the package folder name is always the actual name of the module
     */
    @Override
    public String getPackageFolderName() {
        return this.name;
    }

}
