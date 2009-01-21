/*
 * Created on Jun 3, 2006
 */
package com.python.pydev.refactoring.ast;

import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

/**
 * This class is as the 'regular' sequential iterator, but will also get information
 * on parents being tuples, assigns and so on (the regular one just gets classes
 * and methods as parents).
 * 
 *  
 * @author Fabio
 */
public class EasyAstIteratorComplete extends SequencialASTIteratorVisitor{

    
}
