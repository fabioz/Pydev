package org.python.pydev.compare;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.editor.PyEditConfigurationWithoutEditor;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.ui.ColorAndStyleCache;

public class PyContentViewer extends Viewer {

    private SourceViewer fSourceViewer;
    private Object fInput;

    PyContentViewer(Composite parent, CompareConfiguration mp) {
        fSourceViewer = new SourceViewer(parent, null, SWT.LEFT_TO_RIGHT | SWT.H_SCROLL | SWT.V_SCROLL);
        IPreferenceStore store = PydevPrefs.getChainedPrefStore();

        final ColorAndStyleCache c = new ColorAndStyleCache(store);

        // Ideally we wouldn't pass null for the grammarVersionProvider... although
        // I haven't been able to get to this code at all (is this something still needed?)
        // It seems that Eclipse (in 4.5m5 at least) never gets to use the org.eclipse.compare.contentViewers
        // as it seems to use what's provided by org.eclipse.compare.contentMergeViewers or the
        // editor directly... if that's not the case, first we need to discover how that's still needed.
        fSourceViewer.configure(new PyEditConfigurationWithoutEditor(c, store, null));

        fSourceViewer.setEditable(false);
        parent.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(DisposeEvent e) {
                c.dispose();
            }
        });
    }

    @Override
    public Control getControl() {
        return fSourceViewer.getControl();
    }

    @Override
    public void setInput(Object input) {
        if (input instanceof IStreamContentAccessor) {
            Document document = new Document(getString(input));
            PyPartitionScanner.addPartitionScanner(document, null);
        }
        fInput = input;
    }

    @Override
    public Object getInput() {
        return fInput;
    }

    @Override
    public ISelection getSelection() {
        return null;
    }

    @Override
    public void setSelection(ISelection s, boolean reveal) {
    }

    @Override
    public void refresh() {
    }

    /**
     * A helper method to retrieve the contents of the given object
     * if it implements the IStreamContentAccessor interface.
     */
    private static String getString(Object input) {

        if (input instanceof IStreamContentAccessor) {
            IStreamContentAccessor sca = (IStreamContentAccessor) input;
            try {
                return readString(sca);
            } catch (CoreException ex) {
                Log.log(ex);
            }
        }
        return ""; //$NON-NLS-1$
    }

    public static String readString(IStreamContentAccessor sa) throws CoreException {
        InputStream is = sa.getContents();
        if (is != null) {
            String encoding = null;
            if (sa instanceof IEncodedStreamContentAccessor) {
                try {
                    encoding = ((IEncodedStreamContentAccessor) sa).getCharset();
                } catch (Exception e) {
                }
            }
            if (encoding == null) {
                encoding = ResourcesPlugin.getEncoding();
            }
            return readString(is, encoding);
        }
        return null;
    }

    /**
     * Reads the contents of the given input stream into a string.
     * The function assumes that the input stream uses the platform's default encoding
     * (<code>ResourcesPlugin.getEncoding()</code>).
     * Returns null if an error occurred.
     */
    private static String readString(InputStream is, String encoding) {
        if (is == null) {
            return null;
        }
        BufferedReader reader = null;
        try {
            StringBuffer buffer = new StringBuffer();
            char[] part = new char[2048];
            int read = 0;
            reader = new BufferedReader(new InputStreamReader(is, encoding));

            while ((read = reader.read(part)) != -1) {
                buffer.append(part, 0, read);
            }

            return buffer.toString();

        } catch (IOException ex) {
            // NeedWork
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    // silently ignored
                }
            }
        }
        return null;
    }
}
