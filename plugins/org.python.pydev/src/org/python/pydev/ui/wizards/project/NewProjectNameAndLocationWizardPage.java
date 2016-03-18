/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *    Sebastian Davids <sdavids@gmx.de> - Fix for bug 19346 - Dialog font should be
 *        activated and used by other components.
 *******************************************************************************/
package org.python.pydev.ui.wizards.project;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.WorkingSetConfigurationBlock;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.PyStructureConfigHelpers;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.ui.PyProjectPythonDetails;
import org.python.pydev.ui.wizards.gettingstarted.AbstractNewProjectPage;
import org.python.pydev.utils.ICallback;
import org.python.pydev.utils.PyFileListing;
import org.python.pydev.utils.PyFileListing.PyFileInfo;

/**
 * First page for the new project creation wizard. This page
 * collects the name and location of the new project.
 * 
 * NOTE: COPIED FROM org.eclipse.ui.internal.ide.dialogs.WizardNewProjectNameAndLocationPage 
 * Changed to add the details for the python project type 
 */

public class NewProjectNameAndLocationWizardPage extends AbstractNewProjectPage implements
        IWizardNewProjectNameAndLocationPage {

    // Whether to use default or custom project location
    private boolean useDefaults = true;

    // initial value stores
    private String initialProjectFieldValue;

    private IPath initialLocationFieldValue;

    // the value the user has entered
    private String customLocationFieldValue;

    // widgets
    private Text projectNameField;

    private Text locationPathField;

    private Label locationLabel;

    private Button browseButton;

    private PyProjectPythonDetails.ProjectInterpreterAndGrammarConfig details;

    /**
     * @return a string as specified in the constants in IPythonNature
     * @see IPythonNature#PYTHON_VERSION_XXX 
     * @see IPythonNature#JYTHON_VERSION_XXX
     * @see IPythonNature#IRONPYTHON_VERSION_XXX
     */
    @Override
    public String getProjectType() {
        return details.getSelectedPythonOrJythonAndGrammarVersion();
    }

    @Override
    public String getProjectInterpreter() {
        return details.getProjectInterpreter();
    }

    private Listener nameModifyListener = new Listener() {
        @Override
        public void handleEvent(Event e) {
            setLocationForSelection();
            setPageComplete(validatePage());
        }
    };

    private Listener locationModifyListener = new Listener() {
        @Override
        public void handleEvent(Event e) {
            setPageComplete(validatePage());
        }
    };

    protected Button checkSrcFolder;
    protected Button projectAsSrcFolder;
    protected Button exSrcFolder;
    protected Button noSrcFolder;

    private final static String RESOURCE = "org.eclipse.ui.resourceWorkingSetPage"; //$NON-NLS-1$

    private final class WorkingSetGroup {

        private WorkingSetConfigurationBlock fWorkingSetBlock;

        public WorkingSetGroup() {
            String[] workingSetIds = new String[] { RESOURCE };
            fWorkingSetBlock = new WorkingSetConfigurationBlock(workingSetIds, PydevPlugin.getDefault()
                    .getDialogSettings());
        }

        public Control createControl(Composite composite) {
            Group workingSetGroup = new Group(composite, SWT.NONE);
            workingSetGroup.setFont(composite.getFont());
            workingSetGroup.setText("Working sets");
            workingSetGroup.setLayout(new GridLayout(1, false));

            fWorkingSetBlock.createContent(workingSetGroup);

            return workingSetGroup;
        }

        public void setWorkingSets(IWorkingSet[] workingSets) {
            fWorkingSetBlock.setWorkingSets(workingSets);
        }

        public IWorkingSet[] getSelectedWorkingSets() {
            return fWorkingSetBlock.getSelectedWorkingSets();
        }
    }

    // constants
    private static final int SIZING_TEXT_FIELD_WIDTH = 250;

    private final WorkingSetGroup fWorkingSetGroup;

    /**
     * Creates a new project creation wizard page.
     *
     * @param pageName the name of this page
     */
    public NewProjectNameAndLocationWizardPage(String pageName) {
        super(pageName);
        setTitle("PyDev Project");
        setDescription("Create a new PyDev Project.");
        setPageComplete(false);
        initialLocationFieldValue = Platform.getLocation();
        customLocationFieldValue = ""; //$NON-NLS-1$

        fWorkingSetGroup = new WorkingSetGroup();
        setWorkingSets(new IWorkingSet[0]);
    }

    /* (non-Javadoc)
     * Method declared on IDialogPage.
     */
    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setFont(parent.getFont());

        createProjectNameGroup(composite);
        createProjectLocationGroup(composite);
        createProjectDetails(composite);

        projectAsSrcFolder = new Button(composite, SWT.RADIO);
        projectAsSrcFolder.setText("&Add project directory to the PYTHONPATH");

        checkSrcFolder = new Button(composite, SWT.RADIO);
        checkSrcFolder.setText("Cr&eate 'src' folder and add it to the PYTHONPATH");

        exSrcFolder = new Button(composite, SWT.RADIO);
        exSrcFolder.setText("Create links to e&xisting sources (select them on the next page)");

        noSrcFolder = new Button(composite, SWT.RADIO);
        noSrcFolder.setText("Don't configure PYTHONPATH (to be done &manually later on)");

        IPreferenceStore preferences = PydevPrefs.getPreferences();
        int srcFolderCreate = preferences
                .getInt(IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PREFERENCES);
        switch (srcFolderCreate) {
            case PYDEV_NEW_PROJECT_CREATE_PROJECT_AS_SRC_FOLDER:
                projectAsSrcFolder.setSelection(true);
                break;

            case PYDEV_NEW_PROJECT_EXISTING_SOURCES:
                exSrcFolder.setSelection(true);
                break;

            case PYDEV_NEW_PROJECT_NO_PYTHONPATH:
                noSrcFolder.setSelection(true);
                break;

            default:
                //default is src folder...
                checkSrcFolder.setSelection(true);
        }

        checkSrcFolder.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.widget == checkSrcFolder) {
                    IPreferenceStore preferences = PydevPrefs.getPreferences();
                    if (checkSrcFolder.getSelection()) {
                        preferences.setValue(IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PREFERENCES,
                                PYDEV_NEW_PROJECT_CREATE_SRC_FOLDER);
                        setPageComplete(validatePage());
                    }
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        projectAsSrcFolder.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.widget == projectAsSrcFolder) {
                    IPreferenceStore preferences = PydevPrefs.getPreferences();
                    if (projectAsSrcFolder.getSelection()) {
                        preferences.setValue(IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PREFERENCES,
                                PYDEV_NEW_PROJECT_CREATE_PROJECT_AS_SRC_FOLDER);
                        setPageComplete(validatePage());
                    }

                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        exSrcFolder.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.widget == exSrcFolder) {
                    IPreferenceStore preferences = PydevPrefs.getPreferences();
                    if (exSrcFolder.getSelection()) {
                        preferences.setValue(IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PREFERENCES,
                                PYDEV_NEW_PROJECT_EXISTING_SOURCES);
                        setPageComplete(validatePage());
                    }
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        noSrcFolder.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.widget == noSrcFolder) {
                    IPreferenceStore preferences = PydevPrefs.getPreferences();
                    if (noSrcFolder.getSelection()) {
                        preferences.setValue(IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PREFERENCES,
                                PYDEV_NEW_PROJECT_NO_PYTHONPATH);
                        setPageComplete(validatePage());
                    }
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        Control workingSetControl = createWorkingSetControl(composite);
        workingSetControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        validatePage();

        // Show description on opening
        setErrorMessage(null);
        setMessage(null);
        setControl(composite);
    }

    /**
     * Creates the controls for the working set selection.
     *
     * @param composite the parent composite
     * @return the created control
     */
    protected Control createWorkingSetControl(Composite composite) {
        return fWorkingSetGroup.createControl(composite);
    }

    /**
     * Returns the working sets to which the new project should be added.
     *
     * @return the selected working sets to which the new project should be added
     */
    @Override
    public IWorkingSet[] getWorkingSets() {
        return fWorkingSetGroup.getSelectedWorkingSets();
    }

    /**
     * Sets the working sets to which the new project should be added.
     *
     * @param workingSets the initial selected working sets
     */
    public void setWorkingSets(IWorkingSet[] workingSets) {
        if (workingSets == null) {
            throw new IllegalArgumentException();
        }
        fWorkingSetGroup.setWorkingSets(workingSets);
    }

    /**
     * @param composite
     */
    private void createProjectDetails(Composite parent) {
        Font font = parent.getFont();
        Composite projectDetails = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        projectDetails.setLayout(layout);
        projectDetails.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        projectDetails.setFont(font);

        Label projectTypeLabel = new Label(projectDetails, SWT.NONE);
        projectTypeLabel.setFont(font);
        projectTypeLabel.setText("Project type");
        //let him choose the type of the project
        details = new PyProjectPythonDetails.ProjectInterpreterAndGrammarConfig(new ICallback() {

            //Whenever the configuration changes there, we must evaluate whether the page is complete
            @Override
            public Object call(Object args) throws Exception {
                setPageComplete(NewProjectNameAndLocationWizardPage.this.validatePage());
                return null;
            }
        });

        Control createdOn = details.doCreateContents(projectDetails);
        details.setDefaultSelection();
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        createdOn.setLayoutData(data);
    }

    /**
     * Creates the project location specification controls.
     *
     * @param parent the parent composite
     */
    private final void createProjectLocationGroup(Composite parent) {
        Font font = parent.getFont();
        // project specification group
        Composite projectGroup = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        projectGroup.setLayout(layout);
        projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        projectGroup.setFont(font);

        // new project label
        Label projectContentsLabel = new Label(projectGroup, SWT.NONE);
        projectContentsLabel.setFont(font);

        projectContentsLabel.setText("Project contents:");

        GridData labelData = new GridData();
        labelData.horizontalSpan = 3;
        projectContentsLabel.setLayoutData(labelData);

        final Button useDefaultsButton = new Button(projectGroup, SWT.CHECK | SWT.RIGHT);
        useDefaultsButton.setText("Use &default");
        useDefaultsButton.setSelection(useDefaults);
        useDefaultsButton.setFont(font);

        GridData buttonData = new GridData();
        buttonData.horizontalSpan = 3;
        useDefaultsButton.setLayoutData(buttonData);

        createUserSpecifiedProjectLocationGroup(projectGroup, !useDefaults);

        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                useDefaults = useDefaultsButton.getSelection();
                browseButton.setEnabled(!useDefaults);
                locationPathField.setEnabled(!useDefaults);
                locationLabel.setEnabled(!useDefaults);
                if (useDefaults) {
                    customLocationFieldValue = locationPathField.getText();
                    setLocationForSelection();
                } else {
                    locationPathField.setText(customLocationFieldValue);
                }
            }
        };
        useDefaultsButton.addSelectionListener(listener);
    }

    /**
     * Creates the project name specification controls.
     *
     * @param parent the parent composite
     */
    private final void createProjectNameGroup(Composite parent) {
        Font font = parent.getFont();
        // project specification group
        Composite projectGroup = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        projectGroup.setLayout(layout);
        projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // new project label
        Label projectLabel = new Label(projectGroup, SWT.NONE);
        projectLabel.setFont(font);

        projectLabel.setText("&Project name:");

        // new project name entry field
        projectNameField = new Text(projectGroup, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = SIZING_TEXT_FIELD_WIDTH;
        projectNameField.setLayoutData(data);
        projectNameField.setFont(font);

        // Set the initial value first before listener
        // to avoid handling an event during the creation.
        if (initialProjectFieldValue != null) {
            projectNameField.setText(initialProjectFieldValue);
        }
        projectNameField.addListener(SWT.Modify, nameModifyListener);
    }

    /**
     * Creates the project location specification controls.
     *
     * @param projectGroup the parent composite
     * @param enabled the initial enabled state of the widgets created
     */
    private void createUserSpecifiedProjectLocationGroup(Composite projectGroup, boolean enabled) {
        Font font = projectGroup.getFont();
        // location label
        locationLabel = new Label(projectGroup, SWT.NONE);
        locationLabel.setFont(font);
        locationLabel.setText("Director&y");
        locationLabel.setEnabled(enabled);

        // project location entry field
        locationPathField = new Text(projectGroup, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = SIZING_TEXT_FIELD_WIDTH;
        locationPathField.setLayoutData(data);
        locationPathField.setFont(font);
        locationPathField.setEnabled(enabled);

        // browse button
        browseButton = new Button(projectGroup, SWT.PUSH);
        browseButton.setFont(font);
        browseButton.setText("B&rowse");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleLocationBrowseButtonPressed();
            }
        });

        browseButton.setEnabled(enabled);

        // Set the initial value first before listener
        // to avoid handling an event during the creation.
        if (initialLocationFieldValue != null) {
            locationPathField.setText(initialLocationFieldValue.toOSString());
        }
        locationPathField.addListener(SWT.Modify, locationModifyListener);
    }

    /**
     * Returns the current project location path as entered by 
     * the user, or its anticipated initial value.
     *
     * @return the project location path, its anticipated initial value, or <code>null</code>
     *   if no project location path is known
     */
    @Override
    public IPath getLocationPath() {
        if (useDefaults) {
            return initialLocationFieldValue;
        }

        return new Path(getProjectLocationFieldValue());
    }

    /**
     * Creates a project resource handle for the current project name field value.
     * <p>
     * This method does not create the project resource; this is the responsibility
     * of <code>IProject::create</code> invoked by the new project resource wizard.
     * </p>
     *
     * @return the new project resource handle
     */
    @Override
    public IProject getProjectHandle() {
        return PyStructureConfigHelpers.getProjectHandle(getProjectName());
    }

    /**
     * Returns the current project name as entered by the user, or its anticipated
     * initial value.
     *
     * @return the project name, its anticipated initial value, or <code>null</code>
     *   if no project name is known
     */
    @Override
    public String getProjectName() {
        if (projectNameField == null) {
            return initialProjectFieldValue;
        }

        return getProjectNameFieldValue();
    }

    /**
     * Returns the value of the project name field
     * with leading and trailing spaces removed.
     * 
     * @return the project name in the field
     */
    private String getProjectNameFieldValue() {
        if (projectNameField == null) {
            return ""; //$NON-NLS-1$
        } else {
            return projectNameField.getText().trim();
        }
    }

    /**
     * Returns the value of the project location field
     * with leading and trailing spaces removed.
     * 
     * @return the project location directory in the field
     */
    private String getProjectLocationFieldValue() {
        if (locationPathField == null) {
            return ""; //$NON-NLS-1$
        } else {
            return locationPathField.getText().trim();
        }
    }

    /**
     *  Open an appropriate directory browser
     */
    private void handleLocationBrowseButtonPressed() {
        DirectoryDialog dialog = new DirectoryDialog(locationPathField.getShell());
        dialog.setMessage("Select the project contents directory.");

        String dirName = getProjectLocationFieldValue();
        if (!dirName.equals("")) { //$NON-NLS-1$
            File path = new File(dirName);
            if (path.exists()) {
                dialog.setFilterPath(new Path(dirName).toOSString());
            }
        }

        String selectedDirectory = dialog.open();
        if (selectedDirectory != null) {
            customLocationFieldValue = selectedDirectory;
            locationPathField.setText(customLocationFieldValue);
        }
    }

    /**
     * Returns whether the currently specified project
     * content directory points to an existing project
     */
    private boolean isDotProjectFileInLocation() {
        // Want to get the path of the containing folder, even if workspace location is used
        IPath path = new Path(getProjectLocationFieldValue());
        path = path.append(IProjectDescription.DESCRIPTION_FILE_NAME);
        return path.toFile().exists();
    }

    /**
     * Sets the initial project name that this page will use when
     * created. The name is ignored if the createControl(Composite)
     * method has already been called. Leading and trailing spaces
     * in the name are ignored.
     * 
     * @param name initial project name for this page
     */
    /* package */void setInitialProjectName(String name) {
        if (name == null) {
            initialProjectFieldValue = null;
        } else {
            initialProjectFieldValue = name.trim();
        }
    }

    /**
     * Set the location to the default location if we are set to useDefaults.
     */
    private void setLocationForSelection() {
        if (useDefaults) {
            IPath defaultPath = Platform.getLocation().append(getProjectNameFieldValue());
            locationPathField.setText(defaultPath.toOSString());
        }
    }

    /**
     * Returns whether this page's controls currently all contain valid 
     * values.
     *
     * @return <code>true</code> if all controls are valid, and
     *   <code>false</code> if at least one is invalid
     */
    protected boolean validatePage() {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        String projectFieldContents = getProjectNameFieldValue();
        if (projectFieldContents.equals("")) { //$NON-NLS-1$
            setErrorMessage(null);
            setMessage("Project name is empty");
            return false;
        }

        IStatus nameStatus = workspace.validateName(projectFieldContents, IResource.PROJECT);
        if (!nameStatus.isOK()) {
            setErrorMessage(nameStatus.getMessage());
            return false;
        }

        String locationFieldContents = getProjectLocationFieldValue();

        if (locationFieldContents.equals("")) { //$NON-NLS-1$
            setErrorMessage(null);
            setMessage("Project location is empty");
            return false;
        }

        IPath path = new Path(""); //$NON-NLS-1$
        if (!path.isValidPath(locationFieldContents)) {
            setErrorMessage("Project location is not valid");
            return false;
        }

        //commented out. See comments on https://sourceforge.net/tracker/?func=detail&atid=577329&aid=1798364&group_id=85796
        //        if (!useDefaults
        //                && Platform.getLocation().isPrefixOf(
        //                        new Path(locationFieldContents))) {
        //            setErrorMessage("Default location error");
        //            return false;
        //        }

        IProject projectHandle = getProjectHandle();
        if (projectHandle.exists()) {
            setErrorMessage("Project already exists");
            return false;
        }

        if (!useDefaults) {
            path = getLocationPath();
            if (path.equals(workspace.getRoot().getLocation())) {
                setErrorMessage("Project location cannot be the workspace location.");
                return false;
            }
        }

        if (isDotProjectFileInLocation()) {
            setErrorMessage(".project found in: " + getLocationPath().toOSString()
                    + " (use the Import Project wizard instead).");
            return false;
        }

        if (getProjectInterpreter() == null) {
            setErrorMessage("Project interpreter not specified");
            return false;
        }

        setErrorMessage(null);
        setMessage(null);

        // Look for existing Python files in the destination folder.
        File locFile = (!useDefaults ? getLocationPath() : getLocationPath().append(projectFieldContents)).toFile();
        PyFileListing pyFileListing = PythonPathHelper.getModulesBelow(locFile, null);
        if (pyFileListing != null) {
            boolean foundInit = false;
            Collection<PyFileInfo> modulesBelow = pyFileListing.getFoundPyFileInfos();
            for (PyFileInfo fileInfo : modulesBelow) {
                // Only notify existence of init files in the top-level directory.
                if (PythonPathHelper.isValidInitFile(fileInfo.getFile().getPath())
                        && fileInfo.getFile().getParentFile().equals(locFile)) {
                    setMessage("Project location contains an __init__.py file. Consider using the location's parent folder instead.");
                    foundInit = true;
                    break;
                }
            }
            if (!foundInit && modulesBelow.size() > 0) {
                setMessage("Project location contains existing Python files. The created project will include them.");
            }
        }

        return true;
    }

    /*
     * see @DialogPage.setVisible(boolean)
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            projectNameField.setFocus();
        }
    }

    @Override
    public int getSourceFolderConfigurationStyle() {
        IPreferenceStore preferences = PydevPrefs.getPreferences();
        int srcFolderCreate = preferences
                .getInt(IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PREFERENCES);
        switch (srcFolderCreate) {

            case PYDEV_NEW_PROJECT_CREATE_PROJECT_AS_SRC_FOLDER:
                return PYDEV_NEW_PROJECT_CREATE_PROJECT_AS_SRC_FOLDER;
            case PYDEV_NEW_PROJECT_EXISTING_SOURCES:
                return PYDEV_NEW_PROJECT_EXISTING_SOURCES;
            case PYDEV_NEW_PROJECT_NO_PYTHONPATH:
                return PYDEV_NEW_PROJECT_NO_PYTHONPATH;

            default:
                return PYDEV_NEW_PROJECT_CREATE_SRC_FOLDER;
        }
    }

    public void setProjectName(String projectName) {
        this.projectNameField.setText(projectName);
    }

    @Override
    public IWizardPage getNextPage() {
        PythonProjectWizard wizard = (PythonProjectWizard) getWizard();
        if (getSourceFolderConfigurationStyle() == IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_EXISTING_SOURCES) {
            return wizard.getSourcesPage();
        }
        return wizard.getPageAfterSourcesPage();
    }

}
