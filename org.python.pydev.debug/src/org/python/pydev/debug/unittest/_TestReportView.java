package org.python.pydev.debug.unittest;
///*
// * Created on Nov 10, 2004
// *
// * TODO To change the template for this generated file go to
// * Window - Preferences - Java - Code Style - Code Templates
// */
//package org.python.pydev.debug.unittest;
//
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//
//import org.eclipse.jface.viewers.DoubleClickEvent;
//import org.eclipse.jface.viewers.IDoubleClickListener;
//import org.eclipse.jface.viewers.ISelection;
//import org.eclipse.jface.viewers.IStructuredSelection;
//import org.eclipse.jface.viewers.TableViewer;
//import org.eclipse.swt.SWT;
//import org.eclipse.swt.graphics.Color;
//import org.eclipse.swt.graphics.Image;
//import org.eclipse.swt.layout.GridData;
//import org.eclipse.swt.layout.GridLayout;
//import org.eclipse.swt.layout.RowLayout;
//import org.eclipse.swt.widgets.Composite;
//import org.eclipse.swt.widgets.Control;
//import org.eclipse.swt.widgets.Display;
//import org.eclipse.swt.widgets.Label;
//import org.eclipse.swt.widgets.Table;
//import org.eclipse.swt.widgets.TableColumn;
//import org.eclipse.ui.IWorkbenchPage;
//import org.eclipse.ui.IWorkbenchPart;
//import org.eclipse.ui.IWorkbenchWindow;
//import org.eclipse.ui.PartInitException;
//import org.eclipse.ui.part.ViewPart;
//import org.python.pydev.debug.core.PydevDebugPlugin;
//import org.python.pydev.editor.actions.PyOpenAction;
//import org.python.pydev.editor.model.ItemPointer;
//import org.python.pydev.editor.model.Location;
//import org.python.pydev.utils.ProgressAction;
//
//
///**
// * @author ggheorg
// *
// * TODO To change the template for this generated type comment go to
// * Window - Preferences - Java - Code Style - Code Templates
// */
//public class TestReportView extends ViewPart {
//
//	private TableViewer viewer;
//	private DoubleClickTestAction doubleClickAction = new DoubleClickTestAction();
//	private static Control colorLabel;
//	private static Control failuresCountLabel;
//	private static Control errorsCountLabel;
//	private ITestRunListener listener;
//	private boolean fIsDisposed= false;
//	
//	public class LabelListener implements ITestRunListener {
//
//		private boolean success;
//		private int testsExecuted;
//		private int errCount;
//		private int failCount;
//		
//		private void setLabelTextColor(){
//			Color green = colorLabel.getDisplay().getSystemColor(SWT.COLOR_GREEN);
//			Color red = colorLabel.getDisplay().getSystemColor(SWT.COLOR_RED);
//			Color gray = colorLabel.getDisplay().getSystemColor(SWT.COLOR_GRAY);
//			Color color;
//			if (testsExecuted == 0)
//				color = gray;
//			else
//				color = success? green: red;
//			colorLabel.setBackground(color);
//			colorLabel.redraw();
//
//			String strErrCount = Integer.toString(errCount);
//			String errText = "Errors: " + strErrCount;
//			((Label)errorsCountLabel).setText(errText);
//			errorsCountLabel.redraw();
//			
//			String strFailCount = Integer.toString(failCount);
//			String failText = "Failures: " + strFailCount;
//			((Label)failuresCountLabel).setText(failText);
//			failuresCountLabel.redraw();
//		}
//
//		public void testsStarted(int testCount, String testFile) {
//			success = true;
//			testsExecuted = 0;
//			errCount = 0;
//			failCount = 0;
//			updateLabels();
//			//postShowTestReportView();
//		}
//
//		public void testsFinished(String summary) {
//			//if (colorLabel == null || colorLabel.isDisposed())
//			//	return;
//			updateLabels();
//		}
//
//		private void updateLabels() {
//			colorLabel.getDisplay().syncExec(new Runnable() {
//				public void run() {
//					if (colorLabel.isDisposed())
//						return;
//					setLabelTextColor();
//				}
//			});
//		}
//
//		public void testStarted(String klass, String method) {
//			testsExecuted++;
//		}
//
//		public void testOK(String klass, String method) {
//
//		}
//
//		public void testFailed(String klass, String method, String failureType, String trace) {
//			success = false;
//			if (failureType.equals("ERROR"))
//				errCount++;
//			else if (failureType.equals("FAIL"))
//				failCount++;
//			if (colorLabel == null || colorLabel.isDisposed())
//				return;
//			updateLabels();
//		}
//	}
//
//    private final class DoubleClickTestAction extends ProgressAction {
//
//        public void run() {
//            run(viewer.getSelection());
//        }
//
//        /**
//         * @param event
//         */
//        public void runWithEvent(DoubleClickEvent event) {
//            run(event.getSelection());
//        }
//
//        public void run(ISelection selection) {
//            try {
//                Object obj = ((IStructuredSelection) selection).getFirstElement();
//
//                File realFile = new File(((TestResult)obj).testFile);
//                String klass = ((TestResult)obj).klass;
//                String method = ((TestResult)obj).method;
//                if (realFile.exists() && !realFile.isDirectory()) {
//                	Location loc = navigateToClassMethod(realFile, klass, method);
//                    //ItemPointer p = new ItemPointer(realFile, new Location(-1, -1), null);
//                	ItemPointer p = new ItemPointer(realFile, loc, null);
//                    PyOpenAction act = new PyOpenAction();
//                    act.run(p);
///*
//                    if (act.editor instanceof PyEdit) {
//                        PyEdit e = (PyEdit) act.editor;
//                        IEditorInput input = e.getEditorInput();
//                        IFile original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
//                        if (original == null)
//                            return;
//                        IDocument document = e.getDocumentProvider().getDocument(e.getEditorInput());
//                    }
//*/
//                }
//            } catch (Exception e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        
//        private Location navigateToClassMethod(File file, String klass, String method) {
//        	Location loc = new Location();
//        	int lineCnt = 0;
//            //StringBuffer contents = new StringBuffer();
//            BufferedReader input = null;
//            boolean foundClass = false;
//            boolean foundMethod = false;
//            // klass is of the form module.class; we only need class
//    		String new_klass = klass.substring(klass.indexOf(".") + 1);
//    		klass = new_klass;
//            try {
//              //use buffering
//              //this implementation reads one line at a time
//              input = new BufferedReader( new FileReader(file) );
//              String line = null;
//              while (( line = input.readLine()) != null){
//              	lineCnt++;
//              	if (line.indexOf(klass) > 0) {
//              		foundClass = true;
//              	}
//              	if (foundClass && (line.indexOf(method) > 0)){
//              		foundMethod = true;
//              		break;
//              	}
//                //contents.append(line);
//                //contents.append(System.getProperty("line.separator"));
//              }
//            }
//            catch (FileNotFoundException ex) {
//              ex.printStackTrace();
//            }
//            catch (IOException ex){
//              ex.printStackTrace();
//            }
//            finally {
//              try {
//                if (input!= null) {
//                  //flush and close both "input" and its underlying FileReader
//                  input.close();
//                }
//              }
//              catch (IOException ex) {
//                ex.printStackTrace();
//              }
//            }
//            if (foundClass && foundMethod)
//            	loc.line = lineCnt;
//            return loc;
//        }
//    }
//	
//	public TableViewer getViewer(){
//		return viewer;
//	}
//	/* (non-Javadoc)
//	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
//	 */
//	public void createPartControl(Composite parent) {
//        GridLayout layout = new GridLayout();
//        layout.numColumns = 1;
//        layout.makeColumnsEqualWidth = true;
//        layout.marginWidth = 0;
//        layout.marginHeight = 2;
//        parent.setLayout(layout);
//
//        colorLabel = new Label(parent, SWT.NONE);
//        GridData labelData = new GridData();
//        labelData.heightHint = 10;
//        labelData.grabExcessHorizontalSpace = true;
//        labelData.horizontalAlignment = GridData.FILL;
//		Color gray = colorLabel.getDisplay().getSystemColor(SWT.COLOR_GRAY);
//		colorLabel.setBackground(gray);
//        colorLabel.setLayoutData(labelData);
//
//        Image imageFailures = PydevDebugPlugin.getDefault().imageCache.get("icons/testfailures_ovr.gif");
//		Image imageErrors   = PydevDebugPlugin.getDefault().imageCache.get("icons/testerrors_ovr.gif");
//
//        Composite labelComposite = new Composite(parent, SWT.MULTI);
//        RowLayout labelLayout = new RowLayout();
//        labelComposite.setLayout(labelLayout);
//
//		Label failuresIconLabel = new Label(labelComposite, SWT.NONE);
//        failuresIconLabel.setImage(imageFailures);
//        failuresCountLabel = new Label(labelComposite, SWT.NONE);
//        ((Label)failuresCountLabel).setText("Failures:           ");
//        //failuresCountLabel.setSize(20,10);
//        
//        Label errorsIconLabel = new Label(labelComposite, SWT.NONE);
//        errorsIconLabel.setImage(imageErrors);
//        errorsCountLabel = new Label(labelComposite, SWT.NONE);
//        ((Label)errorsCountLabel).setText("Errors:           ");
//        //errorsCountLabel.setSize(20,10);
//        		
//        Table table = new Table(parent, SWT.SINGLE | SWT.H_SCROLL 
//				| SWT.V_SCROLL | SWT.FULL_SELECTION);
//		table.setHeaderVisible(true);
//		table.setLinesVisible(true);
//		
//        GridData layoutData = new GridData();
//        layoutData.grabExcessHorizontalSpace = true;
//        layoutData.grabExcessVerticalSpace = true;
//        layoutData.horizontalAlignment = GridData.FILL;
//        layoutData.verticalAlignment = GridData.FILL;
//        table.setLayoutData(layoutData);
//		
//		TableColumn column = new TableColumn(table, SWT.NONE, 0);
//		column.setText("Test");
//		column.setWidth(300);
//		column.setAlignment(SWT.LEFT);
//
//		column = new TableColumn(table, SWT.NONE, 1);
//		column.setText("Time (ms)");
//		column.setWidth(100);
//		column.setAlignment(SWT.RIGHT);
//
//		viewer = new TableViewer(table);
//		viewer.setLabelProvider(new TestReportLabelProvider());
//		viewer.setContentProvider(new TestReportContentProvider());
//		viewer.setInput(new ArrayList());
//
//		hookViewerActions();
//		
//		// Register listener for colorLabel
//		listener = new LabelListener();
//		PydevDebugPlugin.getDefault().addTestListener(listener);
//		
//	}
//
//	public void dispose() {
//		fIsDisposed = true;
//		if (listener != null) {
//			PydevDebugPlugin.getDefault().removeTestListener(listener);
//		}
//	}
//
//	/* (non-Javadoc)
//	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
//	 */
//	public void setFocus() {
//		viewer.getControl().setFocus();
//
//	}
//
//    private void hookViewerActions() {
//        viewer.addDoubleClickListener(new IDoubleClickListener() {
//            public void doubleClick(DoubleClickEvent event) {
//                doubleClickAction.runWithEvent(event);
//            }
//        });
//    }
//
//	
//	private boolean isDisposed() {
//		return fIsDisposed;
//	}
//	
//	private void postSyncRunnable(Runnable r) {
//		if (!isDisposed())
//			getDisplay().syncExec(r);
//	}
//	
//	private Display getDisplay() {
//		return getViewSite().getShell().getDisplay();
//	}
//	
//	protected void postShowTestReportView() {
//		postSyncRunnable(new Runnable() {
//			public void run() {
//				if (isDisposed()) 
//					return;
//				showTestReportView();
//			}
//		});
//	}
//	public void showTestReportView() {
//		String viewName = "org.python.pydev.debug.unittest.TestReportView";
//		IWorkbenchWindow window= getSite().getWorkbenchWindow();
//		IWorkbenchPage page= window.getActivePage();
//		TestReportView testRunner= null;
//		
//		if (page != null) {
//			try { // show the result view
//				testRunner= (TestReportView)page.findView(viewName);
//				if(testRunner == null) {
//					IWorkbenchPart activePart= page.getActivePart();
//					testRunner= (TestReportView)page.showView(viewName);
//					//restore focus 
//					page.activate(activePart);
//				} else {
//					page.bringToTop(testRunner);
//				}
//			} catch (PartInitException pie) {
//				pie.printStackTrace();
//			}
//		}
//	}
//
//}
