package org.python.pydev.refactoring.coderefactoring.inlinelocal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.shared_core.string.CoreTextSelection;
import org.python.pydev.shared_core.string.ICoreTextSelection;

import junit.framework.TestCase;

public class InlineLocalTest extends TestCase {
    private RefactoringInfo getInfo(IDocument document, ICoreTextSelection selection) {
        RefactoringInfo info = new RefactoringInfo(document, selection, new IGrammarVersionProvider() {
            @Override
            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5;
            }

            @Override
            public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions() throws MisconfigurationException {
                return null;
            }
        });
        return info;
    }

    private void check(String contents, String expected) throws CoreException {
        IDocument document = new Document(contents);
        ICoreTextSelection selection = new CoreTextSelection(document, document.getLength() - 1,
                0);
        RefactoringInfo info = getInfo(document, selection);
        InlineLocalRefactoring refactoring = new InlineLocalRefactoring(info);
        NullProgressMonitor monitor = new NullProgressMonitor();
        RefactoringStatus result = refactoring.checkAllConditions(monitor);
        assertTrue("Refactoring is not ok: " + result.getMessageMatchingSeverity(RefactoringStatus.WARNING),
                result.isOK());
        Change change = refactoring.createChange(monitor);
        change.perform(monitor);
        assertEquals(expected, document.get());
    }

    public void testSemicolonOnString() throws OperationCanceledException, CoreException {
        String contents = "s = \"A;B\"\n" +
                "print(s)";
        String expected = "print(\"A;B\")";
        check(contents, expected);
    }

    public void testMultiLineCall1() throws OperationCanceledException, CoreException {
        String contents = "new_elem = styling(\n"
                + "x\n"
                + ")\n" +
                "print(new_elem)";
        String expected = "print(styling(\n"
                + "    x\n"
                + "))";
        check(contents, expected);
    }

    public void testMultiLineCall2() throws OperationCanceledException, CoreException {
        String contents = "new_elem = styling(\n" +
                "    *self.convert_children(styling_in, dataset)\n" +
                ")\n"
                + "print(new_elem)";
        String expected = "print(styling(\n" +
                "    *self.convert_children(styling_in, dataset)\n))";
        check(contents, expected);
    }

    public void testMultiLineCall3() throws OperationCanceledException, CoreException {
        String contents = "new_elem = styling[\n"
                + "x\n"
                + "]\n" +
                "print(new_elem)";
        String expected = "print(styling[\n"
                + "    x\n"
                + "])";
        check(contents, expected);
    }

    public void testMultiLineCall4() throws OperationCanceledException, CoreException {
        String contents = "new_elem = {\n"
                + "x\n"
                + "}\n" +
                "print(new_elem)";
        String expected = "print({\n"
                + "    x\n"
                + "})";
        check(contents, expected);
    }
}
