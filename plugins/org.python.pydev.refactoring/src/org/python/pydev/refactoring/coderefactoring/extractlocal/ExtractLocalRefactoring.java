/******************************************************************************
* Copyright (C) 2006-2013  IFS Institute for Software and others
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
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial implementation
*     Jonah Graham <jonah@kichwacoders.com> - ongoing maintenance
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractlocal;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.TokenMgrError;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.core.base.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.change.IChangeProcessor;
import org.python.pydev.refactoring.core.validator.NameValidator;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.refactoring.utils.ListUtils;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;
import org.python.pydev.shared_core.structure.Tuple;

public class ExtractLocalRefactoring extends AbstractPythonRefactoring {
    private ExtractLocalRequestProcessor requestProcessor;

    public ExtractLocalRefactoring(RefactoringInfo info) {
        super(info);

        this.requestProcessor = new ExtractLocalRequestProcessor(info);
    }

    @Override
    protected List<IChangeProcessor> getChangeProcessors() {
        IChangeProcessor changeProcessor = new ExtractLocalChangeProcessor(getName(), this.info, this.requestProcessor);
        return ListUtils.wrap(changeProcessor);
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
        List<Tuple<ITextSelection, ModuleAdapter>> selections = new LinkedListWarningOnSlowOperations<Tuple<ITextSelection, ModuleAdapter>>();

        /* Use different approaches to find a valid selection */
        selections
                .add(new Tuple<ITextSelection, ModuleAdapter>(info.getUserSelection(), info.getParsedUserSelection()));

        selections.add(new Tuple<ITextSelection, ModuleAdapter>(info.getExtendedSelection(), info
                .getParsedExtendedSelection()));

        selections.add(new Tuple<ITextSelection, ModuleAdapter>(info.getUserSelection(),
                getParsedMultilineSelection(info.getUserSelection())));

        /* Find a valid selection */
        ITextSelection selection = null;
        exprType expression = null;
        for (Tuple<ITextSelection, ModuleAdapter> s : selections) {
            /* Is selection valid? */
            if (s != null) {
                expression = extractExpression(s.o2);
                selection = s.o1;
                if (expression != null) {
                    break;
                }
            }
        }

        /* No valid selections found, report error */
        if (expression == null) {
            status.addFatalError(Messages.extractLocalNoExpressionSelected);
        }

        AbstractScopeNode<?> scopeAdapter = info.getModuleAdapter().getScopeAdapter(selection);
        requestProcessor.setDuplicates(scopeAdapter.getDuplicates(selection, expression));

        requestProcessor.setSelection(selection);
        requestProcessor.setExpression(expression);

        return status;
    }

    private ModuleAdapter getParsedMultilineSelection(ITextSelection selection) {
        String source = selection.getText();
        source = source.replaceAll("\n", "");
        source = source.replaceAll("\r", "");

        try {
            ModuleAdapter node = VisitorFactory.createModuleAdapter(null, null, new Document(source), null,
                    info.getVersionProvider());
            return node;
        } catch (TokenMgrError e) {
            return null;
        } catch (ParseException e) {
            return null;
        } catch (Throwable e) {
            Log.log(e);
            return null;
        }
    }

    private exprType extractExpression(ModuleAdapter node) {
        if (node == null) {
            return null;
        }
        Module astNode = node.getASTNode();
        if (astNode == null) {
            return null;
        }
        stmtType[] body = astNode.body;

        if (body.length > 0 && body[0] instanceof Expr) {
            Expr expr = (Expr) body[0];
            return expr.value;
        }
        return null;
    }

    @Override
    public String getName() {
        return Messages.extractLocalLabel;
    }

    public ExtractLocalRequestProcessor getRequestProcessor() {
        return requestProcessor;
    }

    /**
     * Checks if a given variable name is valid or not. Using invalid
     * identifiers (e.g. special chars) or already used variable names
     * aren't allowed and would result in a fatal status
     * 
     * @param variableName
     * @return status
     */
    public RefactoringStatus checkVarName(String variableName) {
        RefactoringStatus status = new RefactoringStatus();

        NameValidator nameValidator = new NameValidator(status, info.getScopeAdapter());
        nameValidator.validateVariableName(variableName);
        nameValidator.validateUniqueVariable(variableName);

        return status;
    }

}
