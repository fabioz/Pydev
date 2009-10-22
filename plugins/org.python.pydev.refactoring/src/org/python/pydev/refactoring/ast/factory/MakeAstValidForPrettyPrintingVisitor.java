package org.python.pydev.refactoring.ast.factory;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;

public class MakeAstValidForPrettyPrintingVisitor extends VisitorBase{

    int currentLine = 0;
    int currentCol = 0;
    
    private void nextLine() {
        currentLine += 1;
        currentCol = 0;
    }
    
    
    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }
    

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        if(node.specialsBefore != null){
            for(Object o:node.specialsBefore){
                if(o instanceof commentType){
                    unhandled_node((SimpleNode) o);
                }
            }
        }
        
        if(node instanceof stmtType || node instanceof suiteType){
            nextLine();
        }
        if(node.beginLine < currentLine){
            node.beginLine = currentLine;
            node.beginColumn = currentCol;
            
        }else if(node.beginLine == currentLine && node.beginColumn < currentCol){
            node.beginColumn = currentCol;
            
        }else{
            currentLine = node.beginLine;
            currentCol = node.beginColumn;
        }
        if(node.specialsAfter != null){
            for(Object o:node.specialsAfter){
                if(o instanceof commentType){
                    unhandled_node((SimpleNode) o);
                }
            }
        }
        
        node.traverse(this);
        return null;
    }

    

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        nextLine();
        if(node.decs != null){
            for(decoratorsType n:node.decs){
                if(n != null){
                    n.accept(this);
                }
            }
        }
        nextLine();
        node.name.accept(this);

        if(node.args != null){
            handleArguments(node.args);
        }
        
        if(node.returns != null){
            node.returns.accept(this);
        }
        
        nextLine();
        if(node.body == null || node.body.length == 0){
            node.body = new stmtType[]{new Pass()};
        }

        int length = node.body.length;
        for(int i = 0; i < length; i++){
            if(node.body[i] != null){
                node.body[i].accept(this);
            }
        }

        return null;
    }
    
    
    @Override
    public Object visitIf(If node) throws Exception {
        Object ret = unhandled_node(node);
        if (node.test != null){
            node.test.accept(this);
        }
        if (node.body != null) {
            for (int i = 0; i < node.body.length; i++) {
                if (node.body[i] != null){
                    node.body[i].accept(this);
                }
            }
        }
        if (node.orelse != null){
            unhandled_node(node.orelse);
            node.orelse.accept(this);
        }        
        return ret;
    }

    private void handleArguments(argumentsType completeArgs) throws Exception {
        exprType[] args = completeArgs.args;
        exprType[] d = completeArgs.defaults;
        exprType[] anns = completeArgs.annotation;
        int argsLen = args==null?0:args.length;
        int defaultsLen = d==null?0:d.length;
        int diff = argsLen-defaultsLen;
        
        for(int i=0;i<argsLen;i++) {
            exprType argName=args[i];
            
            //handle argument
            argName.accept(this);
            
            
            //handle annotation
            if(anns != null){
                exprType ann = anns[i];
                if(ann != null){
                    ann.accept(this); //right after the '='
                }
            }
            
            //handle defaults
            if(i >= diff){
                exprType defaulArgValue = d[i-diff];
                if(defaulArgValue != null){
                    defaulArgValue.accept(this);
                }
            }
            
        }
        
        
        //varargs
        if(completeArgs.vararg != null){
            completeArgs.vararg.accept(this);
            if(completeArgs.varargannotation != null){
                completeArgs.varargannotation.accept(this);
            }
            
        }
        
        //keyword only arguments (after varargs)
        if(completeArgs.kwonlyargs != null){
            for(int i=0;i<completeArgs.kwonlyargs.length;i++){
                exprType kwonlyarg = completeArgs.kwonlyargs[i];
                if(kwonlyarg != null){
                    
                    kwonlyarg.accept(this);
                    
                    if(completeArgs.kwonlyargannotation != null && completeArgs.kwonlyargannotation[i] != null){
                        completeArgs.kwonlyargannotation[i].accept(this);
                    }
                    if(completeArgs.kw_defaults != null && completeArgs.kw_defaults[i] != null){
                        completeArgs.kw_defaults[i].accept(this);
                    }
                }
            }
        }
        
        
        //keyword arguments
        if(completeArgs.kwarg != null){
            completeArgs.kwarg.accept(this);
            if(completeArgs.kwargannotation != null){
                completeArgs.kwargannotation.accept(this);
            }
        }        
    }
    
}
