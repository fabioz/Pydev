/*
 * Created on 28/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import org.python.pydev.core.Tuple;

/**
 * Adds dependency information to the interpreter information. This should be used only for
 * classes that are part of a project (this info will not be gotten for the system interpreter) 
 * 
 * (Basically, it will index all the names that are found in a module so that we can easily know all the
 * places where some name exists)
 * 
 * 
 * @author Fabio
 */
public abstract class AbstractAdditionalDependencyInfo extends AbstractAdditionalInterpreterInfo{
    
    public static boolean TESTING = false;
    
    /**
     * default constructor
     */
    public AbstractAdditionalDependencyInfo() {
	}
    
    @Override
    public void removeInfoFromModule(String moduleName, boolean generateDelta) {
    	if(moduleName == null){
    		throw new AssertionError("The module name may not be null.");
    	}
        super.removeInfoFromModule(moduleName, generateDelta);
    }

    @Override
    protected Object getInfoToSave() {
        return new Tuple<Object, Object>(super.getInfoToSave(), null);
    }
    
    @Override
    protected void restoreSavedInfo(Object o){
        Tuple readFromFile = (Tuple) o;
        super.restoreSavedInfo(readFromFile.o1);
    }




}
