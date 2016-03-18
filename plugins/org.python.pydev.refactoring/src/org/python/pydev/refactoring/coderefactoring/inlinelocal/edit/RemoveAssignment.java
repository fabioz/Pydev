/******************************************************************************
* Copyright (C) 2007-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.coderefactoring.inlinelocal.edit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.refactoring.coderefactoring.inlinelocal.request.InlineLocalRequest;
import org.python.pydev.refactoring.core.edit.AbstractRemoveEdit;

public class RemoveAssignment extends AbstractRemoveEdit {
    private InlineLocalRequest request;
    private int beginOffset;
    private int endOffset;
    private exprType node;

    public RemoveAssignment(InlineLocalRequest req) {
        super(req);
        this.request = req;

        if (req.assignment == null) {
            throw new RuntimeException("no assignment");
        }

        this.node = request.assignment.targets[0];
        IDocument document = req.info.getDocument();
        this.beginOffset = org.python.pydev.parser.visitors.NodeUtils.getOffset(document, node);

        this.endOffset = beginOffset + 1;

        /* Check how much has to be deleted and store this amount in deleteLength */
        try {
            /* marks whether or not there is another statement on this line, if 
             * there is we don't have to remove the indentation
             */
            boolean anotherStatementOnThisLine = false;

            /* now find the end */
            while (true) {
                char c = document.getChar(endOffset - 1);

                /* Look for the end of the line or the ; */
                if (c == '\n') {
                    break;
                }

                if (c == ';') {
                    anotherStatementOnThisLine = true;
                    break;
                }

                endOffset++;
            }

            if (anotherStatementOnThisLine) {
                /* there are still some whitespaces after the ';' on this line, we have to remove them */
                while (document.getChar(endOffset) == ' ') {
                    endOffset++;
                }
            } else {
                /* first we look for the beginning of the line (yap, nice, isn't it?) */
                while (beginOffset > 0) {
                    char c = document.getChar(beginOffset - 1);

                    if (c == '\n' || c == ';') {
                        break;
                    }

                    beginOffset--;
                }
            }

        } catch (BadLocationException e) {
            /* FIXME: this could be caused by EOF during aboves walk. This situation should be added
             * to the unit tests and then get fixed somehow (e.g. with doc.getLength() */
            throw new RuntimeException(e);
        }
    }

    @Override
    protected int getDeleteLength() {
        return endOffset - beginOffset;
    }

    @Override
    public int getOffset() {
        return beginOffset;
    }

    @Override
    protected SimpleNode getEditNode() {
        return node;
    }
}
