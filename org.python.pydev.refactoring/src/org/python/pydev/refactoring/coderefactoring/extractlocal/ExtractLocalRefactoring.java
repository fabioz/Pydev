/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractlocal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.core.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.change.IChangeProcessor;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.refactoring.ui.pages.extractlocal.ExtractLocalPage;

public class ExtractLocalRefactoring extends AbstractPythonRefactoring {

    private ExtractLocalRequestProcessor requestProcessor;

    private IChangeProcessor changeProcessor;

    public ExtractLocalRefactoring(RefactoringInfo info) {
        super(info);

        try {
            initWizard();
        } catch (Throwable e) {
            status.addInfo(Messages.infoFixCode + " Error-Message: " + e.getLocalizedMessage());
        }
    }

    private void initWizard() throws Throwable {
        this.requestProcessor = new ExtractLocalRequestProcessor(info);
        this.pages.add(new ExtractLocalPage(getName(), this.requestProcessor));
    }

    @Override
    protected List<IChangeProcessor> getChangeProcessors() {
        List<IChangeProcessor> processors = new ArrayList<IChangeProcessor>();
        this.changeProcessor = new ExtractLocalChangeProcessor(getName(), this.info, this.requestProcessor);
        processors.add(changeProcessor);
        return processors;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        List<ModuleAdapter> selections = new LinkedList<ModuleAdapter>();
        
        /* Use different approaches to find a valid selection */
        selections.add(info.getParsedUserSelection());
        selections.add(info.getParsedExtendedSelection());
        selections.add(getParsedMultilineSelection(info.getUserSelection()));
        
        /* Find a valid selection */
        exprType expression = null;
        for (ModuleAdapter selection : selections) {
            /* Is selection valid? */
            if (selection != null) {
                expression = extractExpression(selection);
                if (expression != null) {
                    break;
                }
            }
        }

        /* No valid selections found, report error */
        if (expression == null) {
            status.addFatalError(Messages.extractLocalNoExpressionSelected);
        }
        
        requestProcessor.setExpression(expression);
        
        return status;
    }
    
    private ModuleAdapter getParsedMultilineSelection(ITextSelection selection) {
        String source = selection.getText();
        source = source.replaceAll("\n", "");
        source = source.replaceAll("\r", "");
        
        try {
            ModuleAdapter node = VisitorFactory.createModuleAdapter(null, null, new Document(source), null);
            return node;
        } catch (ParseException e) {
            return null;
        }
    }
    
    private exprType extractExpression(ModuleAdapter node) {
        stmtType[] body = node.getASTNode().body;

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
}
