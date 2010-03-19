package org.python.pydev.django.launching;

import java.util.Map;

import org.eclipse.core.expressions.PropertyTester;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.customizations.common.CustomizationCommons;

/**
 * Test to check if a given file is from a django project.
 * 
 * @author Fabio
 */
public class DjangoPropertyTester extends PropertyTester{


    /**
     * Expected value is ignored.
     * 
     * Considers as available for being a django project if there's a DJANGO_MANAGE_LOCATION variable
     * defined.
     */
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        IPythonPathNature nature = CustomizationCommons.getPythonPathNatureFromObject(receiver);
        if(nature == null){
            return false;
        }
        
        try{
            Map<String, String> variableSubstitution = nature.getVariableSubstitution();
            //Only consider a django project if a django_manage_variable is defined
            if(variableSubstitution != null && variableSubstitution.containsKey(DjangoConstants.DJANGO_MANAGE_VARIABLE)){
                return true;
            }
        }catch(Exception e){
            Log.log(e);
        }
     
        return false;
    }

}
