package org.python.pydev.debug.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.python.pydev.core.StringMatcher;

public class PyConfigureExceptionDialog extends ListSelectionDialog {

	protected DefaultFilterMatcher fFilterMatcher = new DefaultFilterMatcher();
	protected boolean updateInThread = true;

	// the visual selection widget group
	private Text filterPatternField;
	private Text addNewExceptionField;

	// providers for populating this dialog
	private ILabelProvider labelProvider;
	private IStructuredContentProvider contentProvider;
	private String filterPattern;

	// the root element to populate the viewer with
	private Object inputElement;

	private FilterJob filterJob;

	public PyConfigureExceptionDialog(Shell parentShell, Object input,
			IStructuredContentProvider contentProvider,
			ILabelProvider labelProvider, String message) {
		super(parentShell, input, contentProvider, labelProvider, message);
		setTitle(WorkbenchMessages.ListSelection_title);
		this.inputElement = input;
		this.contentProvider = contentProvider;
		this.labelProvider = labelProvider;
		if (message != null) {
			setMessage(message);
		} else {
			setMessage(WorkbenchMessages.ListSelection_message);
		}
	}

	/**
	 * 
	 * @param composite
	 *            the parent composite
	 * @return the message label
	 */
	protected Label createMessageArea(Composite composite) {
		Label filterLabel = new Label(composite, SWT.NONE);
		filterLabel.setLayoutData(new GridData(GridData.BEGINNING,
				GridData.CENTER, false, false, 2, 1));
		filterLabel.setText("Enter a filter (* = any number of "
				+ "characters, ? = any single character)"
				+ "\nor an empty string for no filtering:");

		filterPatternField = new Text(composite, SWT.BORDER);
		filterPatternField.setLayoutData(new GridData(GridData.FILL,
				GridData.CENTER, true, false));

		return filterLabel;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite top = (Composite) super.createDialogArea(parent);

		addNewExceptionField = new Text(top, SWT.BORDER);
		addNewExceptionField.setLayoutData(new GridData(GridData.FILL,
				GridData.BEGINNING, true, false));

		Button buttonAdd = new Button(top, SWT.PUSH);
		buttonAdd.setLayoutData(new GridData(GridData.END, GridData.END, true,
				false));
		buttonAdd.setText("Add Exception");
		initContent();

		getViewer().addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				if (getCheckBoxTableViewer().getChecked(element)){
					addToSelectedElements(element);
				}
				return matchExceptionToShowInList(element);
			}
		});

		getCheckBoxTableViewer().addCheckStateListener(
				new ICheckStateListener() {
					public void checkStateChanged(CheckStateChangedEvent event) {
						if (event.getChecked()){
							addToSelectedElements(event.getElement());
						} else{
							removeFromSelectedElements(event.getElement());
						}
					}
				});

		return top;
	}

	/**
	 * Returns the viewer cast to the correct instance. Possibly
	 * <code>null</code> if the viewer has not been created yet.
	 * 
	 * @return the viewer cast to CheckboxTableViewer
	 */
	protected CheckboxTableViewer getCheckBoxTableViewer() {
		return (CheckboxTableViewer) getViewer();
	}

	/**
	 * Updates the current filter with the text field text.
	 */
	protected void doFilterUpdate(IProgressMonitor monitor) {
		setFilter(filterPatternField.getText(), monitor, true);
	}

	// filtering things...
	protected void setFilter(String text, IProgressMonitor monitor,
			boolean updateFilterMatcher) {
		if (monitor.isCanceled())
			return;

		if (updateFilterMatcher) {
			// just so that subclasses may already treat it.
			if (fFilterMatcher.lastPattern.equals(text)) {
				// no actual change...
				return;
			}
			fFilterMatcher.setFilter(text);
			if (monitor.isCanceled())
				return;
		}
		
		getViewer().refresh();
		setElementChecked();
	}

	protected boolean matchExceptionToShowInList(Object element) {
		return fFilterMatcher.match(element);
	}

	private void initContent() {
		Listener listener = new Listener() {
			public void handleEvent(Event e) {
				if (updateInThread) {
					if (filterJob != null) {
						// cancel it if it was already in progress
						filterJob.cancel();
					}
					filterJob = new FilterJob();
					filterJob.start();
				} else {
					doFilterUpdate(new NullProgressMonitor());
				}
			}
		};

		filterPatternField.setText(filterPattern != null ? filterPattern : "");
		filterPatternField.addListener(SWT.Modify, listener);
	}

	private void setElementChecked(){
		for (Object element: getSelectedElements()){
			getViewer().setChecked(element, true);
		}
	}
	
	private List<Object> selectedElements;

	private List<Object> getSelectedElements(){
		return selectedElements;
	}
	
	private void addToSelectedElements(Object element) {
		if (selectedElements == null)
			selectedElements = new ArrayList<Object>();
		if (!selectedElements.contains(element))
			selectedElements.add(element);
	}

	private void removeFromSelectedElements(Object element) {
		if (selectedElements != null && selectedElements.contains(element))
			selectedElements.remove(element);
	}

	class FilterJob extends Thread {
		// only thing it implements is the cancelled
		IProgressMonitor monitor = new NullProgressMonitor();

		public FilterJob() {
			setPriority(Thread.MIN_PRIORITY);
			setName("PyConfigureExceptionDialog: FilterJob");
		}

		@Override
		public void run() {
			try {
				sleep(300);
			} catch (InterruptedException e) {
				// ignore
			}
			if (!monitor.isCanceled()) {
				Display display = Display.getDefault();
				display.asyncExec(new Runnable() {

					public void run() {
						if (!monitor.isCanceled()) {
							doFilterUpdate(monitor);
						}
					}

				});
			}
		}

		public void cancel() {
			this.monitor.setCanceled(true);
		}
	}

	protected class DefaultFilterMatcher {
		public StringMatcher fMatcher;
		public String lastPattern;

		public DefaultFilterMatcher() {
			setFilter("");

		}

		public void setFilter(String pattern) {
			setFilter(pattern, true, false);
		}

		private void setFilter(String pattern, boolean ignoreCase,
				boolean ignoreWildCards) {
			fMatcher = new StringMatcher(pattern + '*', ignoreCase,
					ignoreWildCards);
			this.lastPattern = pattern;
		}

		public boolean match(Object element) {
			boolean match = fMatcher.match(labelProvider.getText(element));
			if (match) {
				return true;
			}
			return false;
		}
	}
}
