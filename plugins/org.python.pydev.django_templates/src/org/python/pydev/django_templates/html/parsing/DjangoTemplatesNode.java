package org.python.pydev.django_templates.html.parsing;

import org.python.pydev.django_templates.IDjConstants;

import com.aptana.parsing.ast.ParseNode;

public class DjangoTemplatesNode extends ParseNode {

    private ParseNode fScript;
    private String fStartTag;
    private String fEndTag;

    public DjangoTemplatesNode(ParseNode parseNode, String startTag, String endTag) {
        super(IDjConstants.LANGUAGE_DJANGO_TEMPLATES);
        fScript = parseNode;
        fStartTag = startTag;
        fEndTag = endTag;

        setChildren(fScript.getChildren());
        setLocation(parseNode.getStartingOffset(), parseNode.getEndingOffset());
    }

    public String getStartTag() {
        return fStartTag;
    }

    public String getEndTag() {
        return fEndTag;
    }

    public ParseNode getNode() {
        return fScript;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof DjangoTemplatesNode)) {
            return false;
        }
        DjangoTemplatesNode other = (DjangoTemplatesNode) obj;
        return start == other.start && end == other.end && fScript.equals(other.fScript);
    }

    @Override
    public int hashCode() {
        int hash = start * 31 + end;
        hash = hash * 31 + fScript.hashCode();
        return hash;
    }
}
