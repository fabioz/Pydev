package org.python.pydev.parser.grammarcommon;

import org.python.pydev.core.structure.FastStack;
import org.python.pydev.parser.jython.Node;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;

public abstract class AbstractJJTPythonGrammarState implements IJJTPythonGrammarState{

    protected FastStack<SimpleNode> nodes;
    protected IntStack marks;
    protected IntStack lines;
    protected IntStack columns;

    protected int sp; // number of nodes on stack
    protected int mk; // current mark
    protected boolean node_created;

    public ITreeBuilder builder;

    protected AbstractJJTPythonGrammarState() {
        nodes = new FastStack<SimpleNode>();
        marks = new IntStack();
        lines = new IntStack();
        columns = new IntStack();
        sp = 0;
        mk = 0;
        this.builder = createBuilder();
    }

    protected abstract ITreeBuilder createBuilder();

    /* Determines whether the current node was actually closed and
       pushed.  This should only be called in the final user action of a
       node scope.  */
    public boolean nodeCreated() {
        return node_created;
    }

    /* Call this to reinitialize the node stack.  It is called
    automatically by the parser's ReInit() method. */
    public void reset() {
        nodes.removeAllElements();
        marks.removeAllElements();
        sp = 0;
        mk = 0;
    }

    /* Returns the root node of the AST.  It only makes sense to call
       this after a successful parse. */
    public Node rootNode() {
        return nodes.getFirst();
    }

    /* Pushes a node on to the stack. */
    public void pushNode(SimpleNode n) {
        nodes.push(n);
        ++sp;
    }

    /* Returns the node on the top of the stack, and remove it from the
       stack.  */
    public SimpleNode popNode() {
        if (--sp < mk) {
            mk = marks.pop();
        }
        return nodes.pop();
    }
    
    
    /* Returns the node currently on the top of the stack. */
    public SimpleNode peekNode() {
        return nodes.peek();
    }
    
    /* Returns the node currently on the top of the stack. */
    public SimpleNode peekNode(int i) {
        return nodes.peek(i);
    }

    /* Returns the number of children on the stack in the current node
       scope. */
    public int nodeArity() {
        return sp - mk;
    }

    public void pushNodePos(int line, int col) {
        lines.push(line);
        columns.push(col);
    }
    
    
    public SimpleNode setNodePos() {
        SimpleNode n = (SimpleNode) peekNode();
        
        int popLine = lines.pop();
        if(n.beginLine == 0)
            n.beginLine = popLine;
        
        int popCol = columns.pop();
        if(n.beginColumn == 0)
            n.beginColumn = popCol;
        return n;
    }


    public void clearNodeScope(Node n) {
        while (sp > mk) {
            popNode();
        }
        mk = marks.pop();
    }


    public void openNodeScope(Node n) {
        marks.push(mk);
        mk = sp;
    }

    

    
    /* A definite node is constructed from a specified number of
       children.  That number of nodes are popped from the stack and
       made the children of the definite node.  Then the definite node
       is pushed on to the stack. */
    public void closeNodeScope(Node n, int num) throws ParseException {
        SimpleNode sn = (SimpleNode) n;
        mk = marks.pop();
        SimpleNode newNode = null;
        try {
            newNode = builder.closeNode(sn, num);
            if (ITreeBuilder.DEBUG_TREE_BUILDER) {
                System.out.println("Created node: " + newNode);
            }
        } catch (ParseException exc) {
            throw exc;
        } catch (Exception exc) {
            exc.printStackTrace();
            throw new ParseException("Internal error:" + exc);
        }
        if (newNode == null) {
            throw new ParseException("Internal AST builder error");
        }
        pushNode(newNode);
        node_created = true;
    }

    /* A conditional node is constructed if its condition is true.  All
    the nodes that have been pushed since the node was opened are
    made children of the the conditional node, which is then pushed
    on to the stack.  If the condition is false the node is not
    constructed and they are left on the stack. */
    public void closeNodeScope(Node n, boolean condition) throws ParseException {
        SimpleNode sn = (SimpleNode) n;
        if (condition) {
            SimpleNode newNode = null;
            try {
                newNode = builder.closeNode(sn, nodeArity());
                if (ITreeBuilder.DEBUG_TREE_BUILDER) {
                    System.out.println("Created node: " + newNode);
                }
            } catch (ParseException exc) {
                throw exc;
            } catch (ClassCastException exc) {
                exc.printStackTrace();
                throw new ParseException("Internal error:" + exc);
            } catch (Exception exc) {
                exc.printStackTrace();
                throw new ParseException("Internal error:" + exc);
            }
            if (newNode == null) {
                throw new ParseException("Internal AST builder error when closing node:" + sn);
            }
            if (marks.size() > 0) {
                mk = marks.pop();
            } else {
                mk = 0;
            }
            pushNode(newNode);
            node_created = true;
        } else {
            mk = marks.pop();
            node_created = false;
        }
    }


}


/**
 * IntStack implementation. During all the tests, it didn't have it's size raised,
 * so, 50 is probably a good overall size... (max on python lib was 40)
 */
class IntStack {
    int[] stack;
    int sp = 0;

    public IntStack() {
        stack = new int[50];
    }


    public void removeAllElements() {
        sp = 0;
    }

    public int size() {
        return sp;
    }

    public int elementAt(int idx) {
        return stack[idx];
    }

    public void push(int val) {
        if (sp >= stack.length) {
            int[] newstack = new int[sp*2];
            System.arraycopy(stack, 0, newstack, 0, sp);
            stack = newstack;
        }
        stack[sp++] = val;
    }

    public int pop() {
        return stack[--sp];
    }
}
