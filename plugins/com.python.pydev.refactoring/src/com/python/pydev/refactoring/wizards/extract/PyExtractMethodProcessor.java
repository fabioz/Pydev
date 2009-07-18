package com.python.pydev.refactoring.wizards.extract;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.refactoring.ast.GetSelectedStmtsVisitor;
import com.python.pydev.refactoring.ast.PyASTChanger;
import com.python.pydev.refactoring.ast.PyASTFactory;

public class PyExtractMethodProcessor extends RefactoringProcessor{

    private RefactoringRequest request;
    private CompositeChange fChange;
    private List<SimpleNode> selectedStmt;

    public PyExtractMethodProcessor (RefactoringRequest request) {
        this.request = request;
    }

    @Override
    public Object[] getElements() {
        return new Object[] { this.request };
    }

    public static final String IDENTIFIER = "org.python.pydev.pyExtractMethod";
    
    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getProcessorName() {
        return "Pydev PyExtractMethodProcessor";
    }

    @Override
    public boolean isApplicable() throws CoreException {
        return true;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();
        
        this.selectedStmt = getSelectedStmt();
        System.out.println("selectedStmt:"+selectedStmt);
        
        return status;
    }

    private List<SimpleNode> getSelectedStmt() {
        GetSelectedStmtsVisitor visitor = new GetSelectedStmtsVisitor(request.ps);
        try {
            request.getAST().accept(visitor);
            return visitor.getSelectedStmts();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();
        
        final SimpleNode ast = request.getAST();
        PyASTChanger astChanger;
        try{
            astChanger = new PyASTChanger(request.getDoc(), ast, request.nature.getGrammarVersion());
        }catch(MisconfigurationException e){
            RefactoringStatusContext cxt = new RefactoringStatusContext(){
            
                @Override
                public Object getCorrespondingElement(){
                    return ast;
                }
            };;
            status.addEntry(RefactoringStatus.ERROR, e.getMessage(), cxt, PydevPlugin.getPluginID(), 1111);
            return status;
        }
        
        fChange = new CompositeChange("ExtractMethodChange: "+request.inputName);
        
        TextChange docChange;
        if(request.pyEdit == null){
            //used in tests
            docChange = new DocumentChange("ExtractMethodChange: ", request.ps.getDoc());
        }else{
            docChange = new TextFileChange("ExtractMethodChange: ", request.getIFile());
        }
        
        MultiTextEdit rootEdit = new MultiTextEdit();
        docChange.setEdit(rootEdit);
        docChange.setKeepPreviewEdits(true);
        Expr expr = new Expr(PyASTFactory.makeCall(request.inputName));
        astChanger.addStmtToNode(ast, "body", 0, expr, false);
        
        Tuple<TextChange, MultiTextEdit> tup = new Tuple<TextChange, MultiTextEdit>(docChange, rootEdit);
        astChanger.getChange(tup);
        
        ITextSelection selection = request.ps.getTextSelection();
        ReplaceEdit edit = new ReplaceEdit(selection.getOffset(),selection.getLength(),"");
        rootEdit.addChild(edit);
        docChange.addTextEditGroup(new TextEditGroup("extracting: foo", edit));
        
        ReplaceEdit edit2 = new ReplaceEdit(4,0,"def m1():\n    a=1\n");
        rootEdit.addChild(edit2);
        docChange.addTextEditGroup(new TextEditGroup("extracting: foo", edit2));
        
        fChange.add(docChange);
        
        return status;
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        return fChange; 
    }

    @Override
    public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
        return null; // no participants are loaded
    }

}
