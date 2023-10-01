/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 18, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion.revisited;

import org.python.pydev.ast.codecompletion.PyCodeCompletionImages;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ITypeInfo;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.FullRepIterable;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractToken implements IToken {

    protected String rep;
    protected String originalRep;
    protected String doc;
    protected String args;
    protected String parentPackage;
    public int type;
    private boolean originalHasRep;
    private ITypeInfo generatorType;
    public final IPythonNature nature;
    protected ICallback<String, IToken> computeDocstring;

    public AbstractToken(String rep, String doc, String args, String parentPackage, int type, String originalRep,
            boolean originalHasRep, IPythonNature nature) {
        this(rep, doc, args, parentPackage, type, nature);
        this.originalRep = originalRep;
        this.originalHasRep = originalHasRep;
    }

    @Override
    public void setGeneratorType(ITypeInfo type) {
        this.generatorType = type;

    }

    @Override
    public ITypeInfo getGeneratorType() {
        return this.generatorType;
    }

    @Override
    public IPythonNature getNature() {
        return this.nature;
    }

    public AbstractToken(String rep, String doc, String args, String parentPackage, int type, IPythonNature nature) {
        if (rep != null) {
            this.rep = rep;
        } else {
            this.rep = "";
        }

        if (args != null) {
            this.args = args;
        } else {
            this.args = "";
        }

        this.originalRep = this.rep;

        if (doc != null) {
            this.doc = doc;
        } else {
            this.doc = "";
        }

        if (parentPackage != null) {
            this.parentPackage = parentPackage;
        } else {
            this.parentPackage = "";
        }

        this.type = type;
        this.nature = nature;
    }

    /**
     * @see org.python.pydev.core.IToken#getArgs()
     */
    @Override
    public String getArgs() {
        return args;
    }

    /**
     * @see org.python.pydev.core.IToken#setArgs(java.lang.String)
     */
    @Override
    public void setArgs(String args) {
        this.args = args;
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.IToken#getRepresentation()
     */
    @Override
    public String getRepresentation() {
        return rep;
    }

    /**
     * @see org.python.pydev.core.IToken#setDocStr(java.lang.String)
     */
    @Override
    public void setDocStr(String docStr) {
        this.doc = docStr;
    }

    public void setDocStrCallback(ICallback<String, IToken> computeDocstring) {
        this.computeDocstring = computeDocstring;
    }

    @Override
    public String getDocStr() {
        if (computeDocstring != null) {
            ICallback<String, IToken> temp = this.computeDocstring;
            this.computeDocstring = null;
            String found = temp.call(this);
            if (found != null && found.length() > 0) {
                doc = found;
            }
        }
        return doc;
    }

    /**
     * @see org.python.pydev.core.IToken#getParentPackage()
     */
    @Override
    public String getParentPackage() {
        return parentPackage;
    }

    /**
     * @see org.python.pydev.core.IToken#getType()
     */
    @Override
    public int getType() {
        return type;
    }

    @Override
    public IImageHandle getImage() {
        return PyCodeCompletionImages.getImageForType(type);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractToken)) {
            return false;
        }

        AbstractToken c = (AbstractToken) obj;

        if (c.getRepresentation().equals(getRepresentation()) == false) {
            return false;
        }

        if (c.getParentPackage().equals(getParentPackage()) == false) {
            return false;
        }

        if (c.getType() != getType()) {
            return false;
        }

        return true;

    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getRepresentation().hashCode() * getType();
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Object o) {
        AbstractToken comp = (AbstractToken) o;

        int thisT = getType();
        int otherT = comp.getType();

        if (thisT != otherT) {
            if (thisT == IToken.TYPE_PARAM || thisT == IToken.TYPE_LOCAL
                    || thisT == IToken.TYPE_OBJECT_FOUND_INTERFACE) {
                return -1;
            }

            if (otherT == IToken.TYPE_PARAM || otherT == IToken.TYPE_LOCAL
                    || otherT == IToken.TYPE_OBJECT_FOUND_INTERFACE) {
                return 1;
            }

            if (thisT == IToken.TYPE_IMPORT) {
                return -1;
            }

            if (otherT == IToken.TYPE_IMPORT) {
                return 1;
            }
        }

        int c = getRepresentation().compareTo(comp.getRepresentation());
        if (c != 0) {
            return c;
        }

        c = getParentPackage().compareTo(comp.getParentPackage());
        if (c != 0) {
            return c;
        }

        return c;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        if (getParentPackage() != null && getParentPackage().length() > 0) {
            return new FastStringBuffer(getRepresentation(), 64).append(" - ").append(getParentPackage()).toString();
        } else {
            return getRepresentation();
        }
    }

    /**
     * @see org.python.pydev.core.IToken#getOriginalRep(boolean)
     */
    private String getOriginalRep(boolean decorateWithModule) {
        if (!decorateWithModule) {
            return originalRep;
        }

        String p = getParentPackage();
        if (p != null && p.length() > 0) {
            return p + "." + originalRep;
        }
        return originalRep;
    }

    /**
     * Make our complete path relative to the base module.
     *
     * @see org.python.pydev.core.IToken#getAsRelativeImport(java.lang.String)
     */
    @Override
    public String getAsRelativeImport(String baseModule) {
        String completePath = getOriginalRep(true);

        return makeRelative(baseModule, completePath);
    }

    @Override
    public String getAsAbsoluteImport() {
        return getAsRelativeImport(".");
    }

    /**
     * @param baseModule this is the 'parent package'. The path passed will be made relative to it
     * @param completePath this is the path that we want to make relative
     * @return the relative path.
     *
     * e.g.: if the baseModule is aa.xx and the completePath is aa.xx.foo.bar, this
     * funcion would return aa.foo.bar
     */
    public static String makeRelative(String baseModule, String completePath) {
        if (baseModule == null) {
            return completePath;
        }

        if (completePath.startsWith(baseModule)) {
            String relative = completePath.substring(baseModule.length());

            baseModule = FullRepIterable.headAndTail(baseModule)[0];

            if (baseModule.length() == 0) {
                if (relative.length() > 0 && relative.charAt(0) == '.') {
                    return relative.substring(1);
                }
            }
            if (relative.length() > 0 && relative.charAt(0) == '.') {
                return baseModule + relative;
            } else {
                return baseModule + '.' + relative;
            }
        }
        return completePath;
    }

    /**
     * @return the original representation (useful for imports)
     * e.g.: if it was import coilib.Exceptions as Exceptions, would return coilib.Exceptions
     */
    @Override
    public String getOriginalRep() {
        return originalRep;
    }

    /**
     * @return the original representation without the actual representation (useful for imports, because
     * we have to look within __init__ to check if the token is defined before trying to gather modules, if
     * we have a name clash).
     *
     * e.g.: if it was from coilib.test import Exceptions, it would return coilib.test
     *
     * @note: if the rep is not a part of the original representation, this function will return an empty string.
     */
    @Override
    public String getOriginalWithoutRep() {
        int i = originalRep.length() - rep.length() - 1;
        if (!originalHasRep) {
            return "";
        }
        return i > 0 ? originalRep.substring(0, i) : "";
    }

    @Override
    public int getLineDefinition() {
        return UNDEFINED;
    }

    @Override
    public int getColDefinition() {
        return UNDEFINED;
    }

    @Override
    public boolean isImport() {
        return false;
    }

    @Override
    public boolean isImportFrom() {
        return false;
    }

    @Override
    public boolean isWildImport() {
        return false;
    }

    @Override
    public boolean isString() {
        return false;
    }

    /**
     * This representation may not be accurate depending on which tokens we are dealing with.
     */
    @Override
    public int[] getLineColEnd() {
        return new int[] { UNDEFINED, UNDEFINED };
    }

    public static boolean isClassDef(IToken element) {
        if (element instanceof SourceToken) {
            SourceToken token = (SourceToken) element;
            SimpleNode ast = token.getAst();
            if (ast instanceof ClassDef) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFunctionDefProperty(IToken element) {
        if (element instanceof SourceToken) {
            SourceToken token = (SourceToken) element;
            SimpleNode ast = token.getAst();
            if (ast instanceof FunctionDef) {
                FunctionDef functionDef = (FunctionDef) ast;
                decoratorsType[] decs = functionDef.decs;
                if (decs != null) {
                    for (int i = 0; i < decs.length; i++) {
                        decoratorsType dec = decs[i];
                        if (dec != null && dec.func != null) {
                            final String representationString = NodeUtils.getRepresentationString(dec.func);
                            if ("property".equals(representationString)
                                    // The ones below are used for enums (depending on the python version).
                                    || "DynamicClassAttribute".equals(representationString)
                                    || "_magic_enum_attr".equals(representationString)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

}
