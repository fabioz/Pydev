package org.python.pydev.ui.wizards.files;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;


/**
 * Python module creation wizard
 * 
 * TODO: Create initial file content from a comment templates
 * 
 * @author Mikko Ohtamaa
 * 
 */
public class PythonModuleWizard extends AbstractPythonWizard {

    public static final String WIZARD_ID = "org.python.pydev.ui.wizards.files.PythonModuleWizard";

    @Override
    protected PythonAbstractPathPage createPathPage() {
        return new PythonAbstractPathPage("Create a new Python module", selection){

            @Override
            protected boolean shouldCreatePackageSelect() {
                return true;
            }
            
        };
    }

    /**
     * We will create a new module (file) here given the source folder and the package specified (which
     * are currently validated in the page) 
     * @param monitor 
     * @throws CoreException 
     */
    @Override
    protected IFile doCreateNew(IProgressMonitor monitor) throws CoreException {
        IContainer validatedSourceFolder = filePage.getValidatedSourceFolder();
        if(validatedSourceFolder == null){
            return null;
        }
        IContainer validatedPackage = filePage.getValidatedPackage();
        if(validatedPackage == null){
            return null;
        }
        String validatedName = filePage.getValidatedName()+".py";
        
        IFile file = validatedPackage.getFile(new Path(validatedName));
        if(!file.exists()){
            file.create(new ByteArrayInputStream(new byte[0]), true, monitor);
        }
        
        return file;
    }




}
