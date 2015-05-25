/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.coderefactoring.inlinelocal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.expr_contextType;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.visitors.LocalVariablesVisitor;
import org.python.pydev.refactoring.core.base.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.change.IChangeProcessor;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;

public class InlineLocalRefactoring extends AbstractPythonRefactoring {
    private InlineLocalRequestProcessor requestProcessor;
    private IChangeProcessor changeProcessor;

    public InlineLocalRefactoring(RefactoringInfo info) {
        super(info);

        initWizard();
    }

    private void initWizard() {
        this.requestProcessor = new InlineLocalRequestProcessor(info);
    }

    @Override
    protected List<IChangeProcessor> getChangeProcessors() {
        List<IChangeProcessor> processors = new ArrayList<IChangeProcessor>();
        this.changeProcessor = new InlineLocalChangeProcessor(getName(), this.info, this.requestProcessor);
        processors.add(changeProcessor);
        return processors;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
        AbstractScopeNode<?> scope = info.getScopeAdapter();
        ITextSelection selection = info.getUserSelection();

        SimpleNode node = scope.getASTNode();

        LocalVariablesVisitor visitor = new LocalVariablesVisitor();
        try {
            node.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<Name> variables = visitor.getVariables();

        Name selectedVariable = findSelectedVariable(selection, variables);

        if (selectedVariable == null) {
            status.addFatalError(Messages.validationNoNameSelected);
            return status;
        }

        List<Name> relatedVariables = findAllRelatedVariables(variables, selectedVariable);

        Assign assignment = findAssignment(relatedVariables);

        if (assignment == null) {
            String id = selectedVariable.id;
            status.addFatalError(Messages.format(Messages.inlinelocalNoAssignment, id));
            return status;
        }

        if (!isValid(relatedVariables)) {
            return status;
        }

        requestProcessor.setAssign(assignment);
        requestProcessor.setVariables(relatedVariables);

        return status;
    }

    private boolean isValid(List<Name> variables) {
        int assignCounter = 0;
        for (Name variable : variables) {
            if (variable.parent instanceof Assign) {
                Assign assign = (Assign) variable.parent;
                /* The given name has to be one of the targets of this assignment */
                for (exprType x : assign.targets) {
                    if (x == variable) {
                        assignCounter++;
                        break;
                    }
                }
            }

            if (variable.ctx == expr_contextType.Param || variable.ctx == expr_contextType.KwOnlyParam) {
                status.addFatalError(Messages.format(Messages.inlineLocalParameter, variable.getId()));
                return false;
            }
        }

        if (assignCounter > 1) {
            status.addFatalError(Messages.format(Messages.inlineLocalMultipleAssignments, variables.get(0).getId()));
            return false;
        }

        return true;
    }

    private Assign findAssignment(List<Name> relatedVariables) {
        /* Search for the assignment */
        for (Name variable : relatedVariables) {

            SimpleNode parent = variable.parent;

            if (parent instanceof Assign) {
                Assign assign = (Assign) parent;

                /* The given name has to be one of the targets of this assignment */
                for (exprType x : assign.targets) {
                    if (x == variable) {
                        return assign;
                    }
                }

            }
        }

        return null;
    }

    private List<Name> findAllRelatedVariables(List<Name> variables, Name selectedVariable) {
        List<Name> relatedVariables = new LinkedListWarningOnSlowOperations<Name>();

        for (Name variable : variables) {
            if (variable.id.equals(selectedVariable.id)) {
                relatedVariables.add(variable);
            }
        }
        return relatedVariables;
    }

    private Name findSelectedVariable(ITextSelection selection, List<Name> variables) {
        int selectionOffset = selection.getOffset();

        for (Name variable : variables) {
            int nodeLength = variable.id.length();
            int nodeOffsetBegin = org.python.pydev.parser.visitors.NodeUtils.getOffset(info.getDocument(), variable);
            int nodeOffsetEnd = nodeOffsetBegin + nodeLength;

            if (selectionOffset >= nodeOffsetBegin && selectionOffset <= nodeOffsetEnd) {
                return variable;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return Messages.inlineLocalLabel;
    }

    public InlineLocalRequestProcessor getRequestProcessor() {
        return requestProcessor;
    }
}
