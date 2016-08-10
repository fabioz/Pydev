package org.python.pydev.parser.visitors;

import org.python.pydev.core.ITypeInfo;
import org.python.pydev.core.UnpackInfo;

public class TypeInfo implements ITypeInfo {

    private String rep;

    public TypeInfo(String rep) {
        this.rep = rep;
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
