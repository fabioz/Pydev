package com.python.pydev.runalltests2;

import junit.framework.Test;

public class AllRefactoringTests {
    
    
    public static Test suite() {
        return org.python.pydev.refactoring.tests.AllTests.suite(); 
    }
}
