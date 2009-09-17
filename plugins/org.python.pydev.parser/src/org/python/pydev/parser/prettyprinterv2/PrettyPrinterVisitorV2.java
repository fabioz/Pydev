package org.python.pydev.parser.prettyprinterv2;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.BinOp;
import org.python.pydev.parser.jython.ast.BoolOp;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.DictComp;
import org.python.pydev.parser.jython.ast.Ellipsis;
import org.python.pydev.parser.jython.ast.Exec;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.IfExp;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Lambda;
import org.python.pydev.parser.jython.ast.List;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Print;
import org.python.pydev.parser.jython.ast.Raise;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.SetComp;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.UnaryOp;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.comprehensionType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;
import org.python.pydev.parser.prettyprinter.IPrettyPrinterPrefs;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * statements that 'need' to be on a new line:
 * print
 * del
 * pass
 * flow
 * import
 * global
 * exec
 * assert
 * 
 * 
 * flow:
 * return
 * yield
 * raise
 * 
 * 
 * compound:
 * if
 * while
 * for
 * try
 * func
 * class
 * 
 * @author Fabio
 */
public class PrettyPrinterVisitorV2 extends PrettyPrinterUtilsV2 {

    public PrettyPrinterVisitorV2(IPrettyPrinterPrefs prefs, PrettyPrinterDocV2 doc) {
        super(prefs, doc);
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
        beforeNode(node);

        int id=0;
        java.util.List<ILinePart> recordChanges = null;
        org.python.pydev.core.Tuple<ILinePart, ILinePart> lowerAndHigher = null;

        
        for(int i = 0; i < node.targets.length; i++){
            exprType target = node.targets[i];
            if(i >= 1){ //more than one assign
                doc.add(lowerAndHigher.o2.getLine(), lowerAndHigher.o2.getBeginCol(), this.prefs.getAssignPunctuation(), node);
            }
            id = this.doc.pushRecordChanges();
            target.accept(this);
            recordChanges = this.doc.popRecordChanges(id);
            lowerAndHigher = doc.getLowerAndHigerFound(recordChanges);
        }
        doc.add(lowerAndHigher.o2.getLine(), lowerAndHigher.o2.getBeginCol(), this.prefs.getAssignPunctuation(), node);

        node.value.accept(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitAugAssign(AugAssign node) throws Exception {
        beforeNode(node);
        node.target.accept(this);
        doc.add(lastNode.beginLine, lastNode.beginColumn+1, this.prefs.getAugOperatorMapping(node.op), node);
        node.value.accept(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitBinOp(BinOp node) throws Exception {
        beforeNode(node);
        node.left.accept(this);
        doc.add(node.beginLine, node.beginColumn, this.prefs.getOperatorMapping(node.op), node);
        node.right.accept(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitUnaryOp(UnaryOp node) throws Exception {
        beforeNode(node);
        doc.add(node.beginLine, node.beginColumn, this.prefs.getUnaryopOperatorMapping(node.op), node);
        node.operand.accept(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitBoolOp(BoolOp node) throws Exception {
        beforeNode(node);
        for(int i = 0; i < node.values.length - 1; i++){
            node.values[i].accept(this);
            ILinePart lastPart = doc.getLastPart();
            doc.add(doc.getLastLineKey(), lastPart.getBeginCol() + 1, this.prefs.getBoolOperatorMapping(node.op), lastNode);
        }
        node.values[node.values.length - 1].accept(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitCompare(Compare node) throws Exception {
        beforeNode(node);
        int id = this.doc.pushRecordChanges();
        node.left.accept(this);
        java.util.List<ILinePart> recordChanges = this.doc.popRecordChanges(id);
        org.python.pydev.core.Tuple<ILinePart, ILinePart> lowerAndHigher = doc.getLowerAndHigerFound(recordChanges);

        for(int i = 0; i < node.comparators.length; i++){
            ILinePart lastPart = lowerAndHigher.o2; //higher
            doc.add(lastPart.getLine(), lastPart.getBeginCol(), this.prefs.getCmpOp(node.ops[i]), lastNode);
            
            
            id = this.doc.pushRecordChanges();
            node.comparators[i].accept(this);
            recordChanges = this.doc.popRecordChanges(id);
            lowerAndHigher = doc.getLowerAndHigerFound(recordChanges);
        }

        afterNode(node);
        return null;
    }

    @Override
    public Object visitEllipsis(Ellipsis node) throws Exception {
        beforeNode(node);
        //        this.state.write("...");
        afterNode(node);
        return null;
    }

    @Override
    public Object visitDict(Dict node) throws Exception {
        beforeNode(node);
        exprType[] keys = node.keys;
        exprType[] values = node.values;
        for(int i = 0; i < values.length; i++){
            keys[i].accept(this);
            values[i].accept(this);
        }
        afterNode(node);
        return null;
    }
    
    
    @Override
    public Object visitList(List node) throws Exception {
        beforeNode(node);
        node.traverse(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitListComp(ListComp node) throws Exception {
        beforeNode(node);
        int id = this.doc.pushRecordChanges();
        node.elt.accept(this);
        for(comprehensionType c:node.generators){
            c.accept(this);
        }
        java.util.List<ILinePart> recordedChanges = this.doc.popRecordChanges(id);
        this.doc.replaceRecorded(recordedChanges, "for", " for ", "if", " if ");
        afterNode(node);
        return null;
    }

    @Override
    public Object visitSetComp(SetComp node) throws Exception {
        beforeNode(node);
        int id = doc.pushRecordChanges();
        node.elt.accept(this);
        for(comprehensionType c:node.generators){
            c.accept(this);
        }
        doc.replaceRecorded(doc.popRecordChanges(id), "for", " for ", "if", " if ");
        afterNode(node);
        return null;
    }

    @Override
    public Object visitDictComp(DictComp node) throws Exception {
        beforeNode(node);
        int id = doc.pushRecordChanges();
        node.key.accept(this);
        node.value.accept(this);
        for(comprehensionType c:node.generators){
            c.accept(this);
        }
        doc.replaceRecorded(doc.popRecordChanges(id), "for", " for ", "if", " if ");
        afterNode(node);
        return null;
    }

    private SimpleNode[] reverseNodeArray(SimpleNode[] expressions) {
        java.util.List<SimpleNode> ifs = Arrays.asList(expressions);
        Collections.reverse(ifs);
        SimpleNode[] ifsInOrder = ifs.toArray(new SimpleNode[0]);
        return ifsInOrder;
    }

    @Override
    public Object visitComprehension(Comprehension node) throws Exception {
        beforeNode(node);
        node.target.accept(this);
        node.iter.accept(this);
        for(SimpleNode s:reverseNodeArray(node.ifs)){
            s.accept(this);
        }
        afterNode(node);
        return null;
    }
    
    @Override
    public Object visitTuple(Tuple node) throws Exception {
        beforeNode(node);
        node.traverse(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitWhile(While node) throws Exception {
        beforeNode(node);
        startStatementPart();
        node.test.accept(this);
        endStatementPart(node);
        indent(node.test);
        for(SimpleNode n:node.body){
            n.accept(this);
        }
        dedent();

        if(node.orelse != null){
            visitOrElsePart(node.orelse);
        }
        afterNode(node);
        return null;
    }
    
    @Override
    public Object visitWith(With node) throws Exception {
        beforeNode(node);
        startStatementPart();
        if (node.context_expr != null)
            node.context_expr.accept(this);
        
        if (node.optional_vars != null)
            node.optional_vars.accept(this);
        endStatementPart(node);
        
        indent(node);
        if (node.body != null)
            node.body.accept(this);
        dedent();
        
        afterNode(node);
        return null;
    }

    @Override
    public Object visitFor(For node) throws Exception {
        //for a in b: xxx else: yyy
        int id = doc.pushRecordChanges();

        beforeNode(node);
        //a
        startStatementPart();
        node.target.accept(this);
        endStatementPart(node);
        
        doc.replaceRecorded(doc.popRecordChanges(id), "for", "for ");

        //in b
        node.iter.accept(this);
        
        indent(node.iter);
        for(SimpleNode n:node.body){
            n.accept(this);
        }
        dedent();
        visitOrElsePart(node.orelse);

        afterNode(node);
        return null;
    }
    
    @Override
    public Object visitReturn(Return node) throws Exception {
        int id=0;
        if(node.value != null){
            id = doc.pushRecordChanges();
        }
        
        Object ret = super.visitReturn(node);
        if(node.value != null){
            java.util.List<ILinePart> changes = doc.popRecordChanges(id);
            doc.replaceRecorded(changes, "return", "return ");
        }
        return ret;
    }

    public void visitTryPart(SimpleNode node, stmtType[] body) throws Exception {
        //try:
        beforeNode(node);

        boolean indent = false;
        if(node.specialsBefore != null && node.specialsBefore.size() > 0){
            for(Object o:node.specialsBefore){
                if(o.toString().equals("try")){
                    indent = true;
                    break;
                }
            }
        }

        if(indent){
            indent(node);
        }
        for(stmtType st:body){
            st.accept(this);
        }
        if(indent){
            dedent();
        }
        afterNode(node);

    }

    public void visitOrElsePart(suiteType orelse) throws Exception {
        if(orelse != null){
            beforeNode(orelse);
            indent(orelse);
            for(stmtType st:orelse.body){
                st.accept(this);
            }
            dedent();
            afterNode(orelse);
        }
    }

    @Override
    public Object visitTryFinally(TryFinally node) throws Exception {
        visitTryPart(node, node.body);
        visitOrElsePart(node.finalbody);
        return null;
    }

    @Override
    public Object visitTryExcept(TryExcept node) throws Exception {
        visitTryPart(node, node.body);
        for(excepthandlerType h:node.handlers){

            //            if(h.type != null || h.name != null){
            //                state.write(" ");
            //            }

            beforeNode(h);
            if(h.type != null){
                h.type.accept(this);
            }
            if(h.name != null){
                h.name.accept(this);
            }
            afterNode(h);
            
            //at this point, we may have written an except clause that should have a space after it,
            //so, let's get what we've written and see if a space should be added.
            PrettyPrinterDocLineEntry line = this.doc.getLine(h.beginLine);
            ILinePart2 foundExcept = null;
            for(ILinePart l:line.getSortedParts()){
                if(!(l instanceof ILinePart2)){
                    continue;
                }
                ILinePart2 iLinePart2 = (ILinePart2) l;
                if(foundExcept != null && !iLinePart2.getString().equals(":")){
                    foundExcept.setString(foundExcept.getString() + " ");
                    break;
                }
                if(iLinePart2.getString().equals("except")){
                    foundExcept = iLinePart2;
                }else{
                    foundExcept = null;
                }
            }
            
            indent(h);
            for(stmtType st:h.body){
                st.accept(this);
            }
            dedent();
        }
        visitOrElsePart(node.orelse);
        return null;
    }


    @Override
    public Object visitPrint(Print node) throws Exception {
        beforeNode(node);
        
        doc.add(node.beginLine, node.beginColumn, "print ", node);
        
        if (node.dest != null){
            doc.add(node.beginLine, node.beginColumn, ">> ", node);
            node.dest.accept(this);
        }
        
        if (node.values != null) {
            for (int i = 0; i < node.values.length; i++) {
                exprType value = node.values[i];
                if (value != null){
                    value.accept(this);
                }
            }
        }

        afterNode(node);
        return null;
    }
    
    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        beforeNode(node);
        doc.startWriteSequentialOnSameLine();
        node.value.accept(this);
        doc.add(lastNode.beginLine, lastNode.beginColumn, ".", node.value);
        node.attr.accept(this);
        doc.endWriteSequentialOnSameLine();
        afterNode(node);
        return null;
    }
    

    @Override
    public Object visitCall(Call node) throws Exception {
        beforeNode(node);
        if (node.func != null)
            node.func.accept(this);
        
        handleArguments(node.args, node.keywords, node.starargs, node.kwargs);
        
        afterNode(node);
        return null;
    }


    
    @Override
    public Object visitExec(Exec node) throws Exception {
        int id = doc.pushRecordChanges();
        Object ret = super.visitExec(node);
        doc.replaceRecorded(doc.popRecordChanges(id), "exec", "exec ");
        return ret;
    }
    
    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        if(node.decs != null){
            for(decoratorsType n:node.decs){
                if(n != null){
                    handleDecorator(n);
                }
            }
        }
        
        beforeNode(node);
        doc.add(node.name.beginLine, node.beginColumn, "class", node);
        node.name.accept(this);
        indent(node.name);
    
    
        handleArguments(node.bases, node.keywords, node.starargs, node.kwargs);
        
        for(SimpleNode n: node.body){
            n.accept(this);
        }
    
        dedent();
        afterNode(node);
        return null;
    }

    
    public boolean isFilled(SimpleNode[] nodes) {
        return (nodes != null) && (nodes.length > 0);
    }

    
    private void handleDecorator(decoratorsType node) throws Exception {
        beforeNode(node);
        if (node.func != null)
            node.func.accept(this);
        
        handleArguments(node.args, node.keywords, node.starargs, node.kwargs);
        afterNode(node);
    }
    
    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        if(node.decs != null){
            for(decoratorsType n:node.decs){
                if(n != null){
                    handleDecorator(n);
                }
            }
        }
        beforeNode(node);
        doc.add(node.name.beginLine, node.beginColumn, "def", node);
        node.name.accept(this);
        indent(node.name);

        if(node.args != null){
            handleArguments(node.args);
            node.args.accept(this);
        }
        if(node.body != null){
            for(int i = 0; i < node.body.length; i++){
                if(node.body[i] != null)
                    node.body[i].accept(this);
            }
        }
        if(node.returns != null)
            node.returns.accept(this);

        dedent();
        afterNode(node);
        return null;
    }

    @Override
    public Object visitIfExp(IfExp node) throws Exception {
        //we have to change the order a bit...
        int id = this.doc.pushRecordChanges();

        
        beforeNode(node);
        node.body.accept(this);
        node.test.accept(this);
        if(node.orelse != null){
            node.orelse.accept(this);
        }
        afterNode(node);
        java.util.List<ILinePart> recordedChanges = this.doc.popRecordChanges(id);
        this.doc.replaceRecorded(recordedChanges, "if", " if ", "else", " else ");
        
        return null;
    }

    @Override
    public Object visitName(Name node) throws Exception {
        beforeNode(node);
        doc.add(node.beginLine, node.beginColumn, node.id, node);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        beforeNode(node);
        doc.add(node.beginLine, node.beginColumn, node.id, node);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitNum(Num node) throws Exception {
        beforeNode(node);
        doc.add(node.beginLine, node.beginColumn, node.num, node);
        afterNode(node);
        return null;
    }
    
    
    @Override
    public Object visitStr(Str node) throws Exception {
        beforeNode(node);
        doc.add(node.beginLine, node.beginColumn, NodeUtils.getStringToPrint(node), node);
        afterNode(node);
        return null;
    }
    
    @Override
    public Object visitImport(Import node) throws Exception {
        int id = this.doc.pushRecordChanges();
        beforeNode(node);
        
        for (aliasType name : node.names){
            beforeNode(name);
            name.accept(this);
            afterNode(name);
        }
        
        afterNode(node);
        
        this.doc.replaceRecorded(this.doc.popRecordChanges(id), "import", "import ");
        return null;
    }
    
    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        int id = this.doc.pushRecordChanges();
        beforeNode(node);
        
        if(!((NameTok)node.module).id.equals("")){
            node.module.accept(this); //no need to add an empty module
        }else{
            //empty
            beforeNode(node.module);
            afterNode(node.module);
        }
        
        for (aliasType name : node.names){
            beforeNode(name);
            name.accept(this);
            afterNode(name);
        }
        
        afterNode(node);
        
        java.util.List<ILinePart> recordChanges = this.doc.popRecordChanges(id);
        this.doc.replaceRecorded(recordChanges, "import", " import ");
        
        for(ILinePart linePart:recordChanges){
            if(!(linePart instanceof ILinePart2)){
                continue;
            }
            ILinePart2 iLinePart2 = (ILinePart2) linePart;
            if(iLinePart2.getString().equals("from")){
                
                if(node.level > 0){
                    String s = new FastStringBuffer(node.level).appendN('.', node.level).toString();
                    doc.add(iLinePart2.getLine(), linePart.getBeginCol()+1, s, linePart.getToken());
                }

                iLinePart2.setString("from ");
                break;
            }
        }

        return null;
    }
    

    @Override
    public Object visitRaise(Raise node) throws Exception {
        int id = this.doc.pushRecordChanges();
        Object ret = super.visitRaise(node);
        java.util.List<ILinePart> recordChanges = this.doc.popRecordChanges(id);
        if(node.type != null){
            this.doc.replaceRecorded(recordChanges, "raise", "raise ", "from", " from ");
        }
        return ret;
    }


    @Override
    public Object visitLambda(Lambda node) throws Exception {
        beforeNode(node);

        argumentsType arguments = node.args;
        String str;
        if(arguments == null || arguments.args == null || arguments.args.length == 0){
            str = "lambda";
        }else{
            str = "lambda ";
        }

        doc.add(node.beginLine, node.beginColumn, str, node);
        this.handleArguments(node.args);
        node.traverse(this);
        afterNode(node);
        return null;
    }
    
    
    private void handleArguments(exprType[] args, keywordType[] keywords, exprType starargs, exprType kwargs) throws Exception, IOException {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null)
                    args[i].accept(this);
            }
        }
        if (keywords != null) {
            for (int i = 0; i < keywords.length; i++) {
                keywordType keyword = keywords[i];
                if (keyword != null){
                    beforeNode(keyword);
                    keyword.accept(this);
                    afterNode(keyword);
                }
            }
        }
        if (starargs != null)
            starargs.accept(this);
        if (kwargs != null)
            kwargs.accept(this);
    }


     /**
     * Prints the arguments.
     */
    protected void handleArguments(argumentsType completeArgs) throws Exception {
        if(completeArgs.vararg == null && completeArgs.kwonlyargs != null && completeArgs.kwonlyargs.length > 0 && completeArgs.kwonlyargs[0] != null){
            //we must add a '*,' to print it if we have a keyword arg after the varargs but don't really have an expression for it
            doc.add(completeArgs.kwonlyargs[0].beginLine, completeArgs.kwonlyargs[0].beginColumn, "*", completeArgs.kwonlyargs[0]);
            doc.add(completeArgs.kwonlyargs[0].beginLine, completeArgs.kwonlyargs[0].beginColumn, ",", completeArgs.kwonlyargs[0]);
        }
        
    }
    
    
    public Object visitIf(If node) throws Exception {
        beforeNode(node);
        startStatementPart();
        node.test.accept(this);
        endStatementPart(node);
        indent(lastNode);

        //write the body and dedent
        for(SimpleNode n:node.body){
            n.accept(this);
        }
        dedent();
        afterNode(node);

        if(node.orelse != null && node.orelse.length > 0){
            boolean inElse = false;

            //now, if it is an elif, it will end up calling the 'visitIf' again,
            //but if it is an 'else:' we will have to make the indent again
            if(node.specialsAfter != null){
                for(Object o:node.specialsAfter){
                    if(o.toString().equals("else")){
                        inElse = true;
                        indent((Token) o);
                    }
                }
            }
            for(SimpleNode n:node.orelse){
                n.accept(this);
            }
            if(inElse){
                dedent();
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "PrettyPrinterVisitorV2{\n" + this.doc + "\n}";
    }
}
