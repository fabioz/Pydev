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

        for(int i = 0; i < node.targets.length; i++){
            exprType target = node.targets[i];
            if(i >= 1){ //more than one assign
                doc.add(lastNode.beginLine, lastNode.beginColumn + 1, this.prefs.getAssignPunctuation(), node);
            }
            target.accept(this);
        }
        doc.add(lastNode.beginLine, lastNode.beginColumn + 1, this.prefs.getAssignPunctuation(), node);

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
            LinePart lastPart = doc.getLastPart();
            doc.add(doc.getLastLineKey(), lastPart.beginCol + 1, this.prefs.getBoolOperatorMapping(node.op), lastNode);
        }
        node.values[node.values.length - 1].accept(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitCompare(Compare node) throws Exception {
        beforeNode(node);
        node.left.accept(this);

        for(int i = 0; i < node.comparators.length; i++){
            doc.add(node.comparators[i].beginLine, node.comparators[i].beginColumn - 1, this.prefs.getCmpOp(node.ops[i]), node.comparators[i]);
            node.comparators[i].accept(this);
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
        indent(node);
        for(int i = 0; i < values.length; i++){
            keys[i].accept(this);
            values[i].accept(this);
        }
        dedent();
        afterNode(node);
        return null;
    }
    
    
    @Override
    public Object visitList(List node) throws Exception {
        beforeNode(node);
        indent(node);
        node.traverse(this);
        afterNode(node);
        dedent();
        return null;
    }

    @Override
    public Object visitListComp(ListComp node) throws Exception {
        beforeNode(node);
        this.doc.pushRecordChanges();
        node.elt.accept(this);
        for(comprehensionType c:node.generators){
            c.accept(this);
        }
        java.util.List<LinePart> recordedChanges = this.doc.popRecordChanges();
        this.doc.replaceRecorded(recordedChanges, "for", " for ");
        this.doc.replaceRecorded(recordedChanges, "if", " if ");
        afterNode(node);
        return null;
    }

    @Override
    public Object visitSetComp(SetComp node) throws Exception {
        beforeNode(node);
        node.elt.accept(this);
        for(comprehensionType c:node.generators){
            c.accept(this);
        }
        afterNode(node);
        return null;
    }

    @Override
    public Object visitDictComp(DictComp node) throws Exception {
        beforeNode(node);
        node.key.accept(this);
        node.value.accept(this);
        for(comprehensionType c:node.generators){
            c.accept(this);
        }
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
        indent(node);
        node.traverse(this);
        afterNode(node);
        dedent();
        return null;
    }

    @Override
    public Object visitWhile(While node) throws Exception {
        beforeNode(node);
        node.test.accept(this);
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
        
        if (node.context_expr != null)
            node.context_expr.accept(this);
        
        if (node.optional_vars != null)
            node.optional_vars.accept(this);
        
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
        doc.pushRecordChanges();

        beforeNode(node);
        //a
        node.target.accept(this);
        
        doc.replaceRecorded(doc.popRecordChanges(), "for", "for ");

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
        doc.pushRecordChanges();
        Object ret = super.visitReturn(node);
        java.util.List<LinePart> changes = doc.popRecordChanges();
        if(node.value != null){
            doc.replaceRecorded(changes, "return", "return ");
        }
        return ret;
    }

    public void visitTryPart(SimpleNode node, stmtType[] body) throws Exception {
        //try:
        beforeNode(node);

        indent(node);
        for(stmtType st:body){
            st.accept(this);
        }
        dedent();
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
            LinePart foundExcept = null;
            for(LinePart l:line.getSortedParts()){
                if(foundExcept != null && !l.string.equals(":")){
                    foundExcept.string += " ";
                    break;
                }
                if(l.string.equals("except")){
                    foundExcept = l;
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
        node.value.accept(this);
        doc.add(node.value.beginLine, node.value.beginColumn + 1, ".", node.value);
        node.attr.accept(this);
        afterNode(node);
        return null;
    }
    

    @Override
    public Object visitCall(Call node) throws Exception {
        beforeNode(node);
        indent(node);
        
        if (node.func != null)
            node.func.accept(this);
        
        handleArguments(node.args, node.keywords, node.starargs, node.kwargs);
        
        
        dedent();
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

    
    @Override
    public Object visitExec(Exec node) throws Exception {
        doc.pushRecordChanges();
        Object ret = super.visitExec(node);
        doc.replaceRecorded(doc.popRecordChanges(), "exec", "exec ");
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
    
    
        if(node.bases != null && node.bases.length > 0){
            for (exprType expr : node.bases) {
                expr.accept(this);
            }
        }
        
        if (node.keywords != null) {
            for (int i = 0; i < node.keywords.length; i++) {
                if (node.keywords[i] != null)
                    node.keywords[i].accept(this);
            }
        }
        if (node.starargs != null)
            node.starargs.accept(this);
        if (node.kwargs != null)
            node.kwargs.accept(this);
        
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

        if(node.args != null)
            node.args.accept(this);
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
        this.doc.pushRecordChanges();

        
        beforeNode(node);
        node.body.accept(this);
        node.test.accept(this);
        if(node.orelse != null){
            node.orelse.accept(this);
        }
        afterNode(node);
        java.util.List<LinePart> recordedChanges = this.doc.popRecordChanges();
        this.doc.replaceRecorded(recordedChanges, "if", " if ");
        this.doc.replaceRecorded(recordedChanges, "else", " else ");
        
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
        this.doc.pushRecordChanges();
        beforeNode(node);
        
        for (aliasType name : node.names){
            beforeNode(name);
            name.accept(this);
            afterNode(name);
        }
        
        afterNode(node);
        
        this.doc.replaceRecorded(this.doc.popRecordChanges(), "import", "import ");
        return null;
    }
    
    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        this.doc.pushRecordChanges();
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
        
        java.util.List<LinePart> recordChanges = this.doc.popRecordChanges();
        this.doc.replaceRecorded(recordChanges, "import", " import ");
        
        for(LinePart linePart:recordChanges){
            if(linePart.string.equals("from")){
                
                if(node.level > 0){
                    String s = new FastStringBuffer(node.level).appendN('.', node.level).toString();
                    doc.add(linePart.getLine(), linePart.beginCol+1, s, linePart.token);
                }

                linePart.string = "from ";
                break;
            }
        }

        return null;
    }
    

    @Override
    public Object visitRaise(Raise node) throws Exception {
        this.doc.pushRecordChanges();
        Object ret = super.visitRaise(node);
        java.util.List<LinePart> recordChanges = this.doc.popRecordChanges();
        if(node.type != null){
            this.doc.replaceRecorded(recordChanges, "raise", "raise ");
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
        node.traverse(this);
        afterNode(node);
        return null;
    }

    public Object visitIf(If node) throws Exception {
        beforeNode(node);
        node.test.accept(this);
        indent(node.test);

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
