package com.python.pydev.analysis.scopeanalysis;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.commentType;

import junit.framework.TestCase;

public class ScopeAnalysisCommentsTest extends TestCase{

    public static void main(String[] args) {
    	try {
			ScopeAnalysisCommentsTest test = new ScopeAnalysisCommentsTest();
			test.setUp();
			test.tearDown();
			junit.textui.TestRunner.run(ScopeAnalysisCommentsTest.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public void test1() throws Exception {
    	ArrayList<Object> c = new ArrayList<Object>();
		commentType comment = new commentType("#this is foo");
		comment.beginLine=10;
		comment.beginColumn=20;
		c.add(comment);
		
		List<Name> names = ScopeAnalysis.checkComments(c, "this");
		assertEquals(1, names.size());
		
		assertEquals(10, names.get(0).beginLine);
		assertEquals(20+1, names.get(0).beginColumn);
	}
}
