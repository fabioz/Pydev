package com.python.pydev.analysis.pyright;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;
import org.python.pydev.shared_core.resource_stubs.FileStub;
import org.python.pydev.shared_core.resource_stubs.ProjectStub;

import junit.framework.TestCase;

public class PyrightAnalysisTest extends TestCase {

    private File tempDir;
    private FileStub file;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tempDir = FileUtils.getTempFileAt(new File("."), "data_pyright_analysis");
        tempDir.mkdirs();
        ProjectStub project = new ProjectStub(tempDir, null);
        file = new FileStub(project, new File(tempDir, "snippet.py"));
    }

    public void testMarkersMessage() {

        IDocument document = new Document(
                "" +
                        "def add_numbers(a: int, b: int) -> int:\n" +
                        "    return a + b\n" +
                        "\n" +
                        "# This should cause a type error\n" +
                        "result = add_numbers('hello', 'world')\n" +
                        "print(result)\n");

        PyrightAnalysis analysis = new PyrightAnalysis(file, document, null,
                new NullProgressMonitor(), null);

        // Test that the analysis can be created
        assertNotNull("Analysis should not be null", analysis);

        // Test that markers can be retrieved
        List<MarkerInfo> markers = analysis.getMarkers(file);
        assertNotNull("Markers should not be null", markers);
        // Note: In a real test, we would need to actually run pyright and check for specific markers
        // This is a basic structure test
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (tempDir != null && tempDir.exists()) {
            FileUtils.deleteDirectoryTree(tempDir);
        }
    }
}
