/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.codegenerator.overridemethods;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.codegenerator.overridemethods.request.OverrideMethodsRequest;
import org.python.pydev.refactoring.core.request.IRequestProcessor;

public class MockupOverrideMethodsRequestProcessor implements IRequestProcessor<OverrideMethodsRequest> {

    private ModuleAdapter module;

    private int classSelection;

    private int offsetStrategy;

    private List<String> methodSelection;

    private int editClass;

    /**
     * @param module the module where the refactoring should take place
     * @param classSelection the index of the base class that should have the methods overridden 
     * @param methodSelection the indexes of the methods that should be overriden
     * @param offsetStrategy ?
     * @param editClass the class that should be edited
     */
    private MockupOverrideMethodsRequestProcessor(ModuleAdapter module, int classSelection, List<String> methodSelection,
            int offsetStrategy, int editClass) {
        this.module = module;
        this.methodSelection = methodSelection;
        this.classSelection = classSelection;
        this.offsetStrategy = offsetStrategy;
        this.editClass = editClass;
    }

    public MockupOverrideMethodsRequestProcessor(ModuleAdapter module, MockupOverrideMethodsConfig config) {
        this(module, config.getClassSelection(), config.getMethodSelection(), config.getOffsetStrategy(), config.getEditClass());
    }

    public List<OverrideMethodsRequest> getRefactoringRequests() {
        List<IClassDefAdapter> classes = module.getClasses();
        //get the class to be edited
        IClassDefAdapter clazz = classes.get(editClass);
        
        List<IClassDefAdapter> baseClasses = clazz.getBaseClasses();
        //and the one that should be overridden (from the base clasess list)
        IClassDefAdapter clazzSelection = baseClasses.get(classSelection);
        
        String baseClassName = clazzSelection.getName();
        List<FunctionDefAdapter> methods = new ArrayList<FunctionDefAdapter>();

        List<FunctionDefAdapter> functions = clazzSelection.getFunctions();
        
        for(FunctionDefAdapter method: functions){
            if(this.methodSelection.contains(method.getName())){
                if(method.getSignature().equals("")){
                    throw new RuntimeException("Invalid signature!");
                }
                methods.add(method);
            }
        }

        List<OverrideMethodsRequest> requests = new ArrayList<OverrideMethodsRequest>();

        for (FunctionDefAdapter method : methods) {
            OverrideMethodsRequest req = new OverrideMethodsRequest(clazz, this.offsetStrategy, method, false, baseClassName, "\n");
            requests.add(req);
        }

        return requests;

    }
}
