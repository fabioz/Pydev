package org.python.pydev.parser.visitors;

import java.util.List;

import org.python.pydev.core.ITypeInfo;
import org.python.pydev.core.UnpackInfo;
import org.python.pydev.parser.jython.ast.BinOp;
import org.python.pydev.parser.jython.ast.ExtSlice;
import org.python.pydev.parser.jython.ast.Index;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.sliceType;
import org.python.pydev.shared_core.string.StringUtils;

public class TypeInfo implements ITypeInfo {

    public static final String repSeparator = "$!sep!";

    private final String rep;
    private final exprType expr;

    /**
     * Used when the info comes from a docstring.
     */
    public TypeInfo(String rep) {
        this.rep = rep;
        this.expr = null;
    }

    /**
     * Used when the info comes from typing with actual types.
     */
    public TypeInfo(exprType expr) {
        this.expr = NodeUtils.extractOptionalValueSubscript(expr);
        if (this.expr instanceof Subscript) {
            String rep = NodeUtils.getFullRepresentationString(this.expr);
            Subscript subscript = (Subscript) this.expr;
            List<String> repValues = NodeUtils.extractValuesFromSubscriptSlice(subscript.slice);
            if (rep != null) {
                repValues.add(rep);
            }
            this.rep = StringUtils.join(repSeparator, repValues); // use this separator for later values retrieves that do not directly affects Python syntax
        } else if (this.expr instanceof BinOp) {
            BinOp binOp = (BinOp) this.expr;
            if (binOp.op == BinOp.BitOr) {
                List<String> repValues = NodeUtils.extractValuesFromBinOp(this.expr);
                this.rep = StringUtils.join(repSeparator, repValues);
            } else {
                this.rep = "";
            }
        } else {
            String rep = NodeUtils.getFullRepresentationString(this.expr);
            if (rep == null) {
                this.rep = "";
            } else {
                this.rep = rep;
            }
        }
    }

    /* (non-Javadoc)
     * @see org.python.pydev.core.ITypeInfo#getActTok()
     */
    @Override
    public String getActTok() {
        return rep;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.core.ITypeInfo#getPackedType()
     */
    @Override
    public TypeInfo getPackedType() {
        return new TypeInfo(NodeUtils.getPackedTypeFromDocstring(rep));
    }

    @Override
    public ITypeInfo getUnpacked(UnpackInfo unpackInfo) {
        if (expr != null) {
            if (expr instanceof Subscript) {
                Subscript subscript = (Subscript) expr;
                // ExtSlice is something as Dict[int, str]
                if (subscript.slice instanceof ExtSlice) {
                    ExtSlice extSlice = (ExtSlice) subscript.slice;
                    if (extSlice.dims != null) {
                        int i = unpackInfo.getUnpackTuple(extSlice.dims.length);
                        if (i >= 0 && i < extSlice.dims.length) {
                            sliceType sliceType = extSlice.dims[i];
                            if (sliceType instanceof Index) {
                                Index index = (Index) sliceType;
                                exprType valExpr = index.value;
                                if (valExpr != null) {
                                    return new TypeInfo(valExpr);
                                }
                            }
                        }
                    }
                } else if (subscript.slice instanceof Index) {
                    // This is something as: List[str]
                    // As we have only a single entry, it doesn't matter which index
                    // is accessed!
                    Index index = (Index) subscript.slice;
                    exprType valExpr = index.value;
                    if (valExpr != null) {
                        return new TypeInfo(valExpr);
                    }
                }
            }
        }
        return new TypeInfo(NodeUtils.getUnpackedTypeFromTypeDocstring(rep, unpackInfo));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((rep == null) ? 0 : rep.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TypeInfo other = (TypeInfo) obj;
        if (rep == null) {
            if (other.rep != null) {
                return false;
            }
        } else if (!rep.equals(other.rep)) {
            return false;
        }
        return true;
    }
}
