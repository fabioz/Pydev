package org.python.pydev.core;

import java.util.HashMap;
import java.util.Map;

import org.python.pydev.shared_core.IMiscConstants;

public class CheckAnalysisErrors {

    private final static Map<String, String> flake8MessageIdToCodeAnalysisError = new HashMap<>();
    private final static Map<String, String> codeAnalysisErrorToFlake8MessageId = new HashMap<>();
    private final static Map<Integer, String> codeAnalysisTypeFlake8MessageId = new HashMap<>();

    private final static Map<String, String> pyLintMessageIdToCodeAnalysisError = new HashMap<>();
    private final static Map<String, String> codeAnalysisErrorToPyLintMessageId = new HashMap<>();
    private final static Map<Integer, String> codeAnalysisTypePyLintMessageId = new HashMap<>();

    // PyLint && PyDev analysis messages.
    // See: com.python.pydev.analysis.IAnalysisPreferences

    static {

        pyLintMessageIdToCodeAnalysisError.put("unused-import", "@UnusedImport");
        flake8MessageIdToCodeAnalysisError.put("F401", "@UnusedImport");

        pyLintMessageIdToCodeAnalysisError.put("unused-variable", "@UnusedVariable");

        pyLintMessageIdToCodeAnalysisError.put("undefined-variable", "@UndefinedVariable");
        flake8MessageIdToCodeAnalysisError.put("F821", "@UndefinedVariable");

        pyLintMessageIdToCodeAnalysisError.put("no-self-argument", "@NoSelf");
        pyLintMessageIdToCodeAnalysisError.put("redefined-builtin", "@ReservedAssignment");
        pyLintMessageIdToCodeAnalysisError.put("pointless-statement", "@NoEffect");
        pyLintMessageIdToCodeAnalysisError.put("function-redefined", "@DuplicatedSignature");
        pyLintMessageIdToCodeAnalysisError.put("unused-wildcard-import", "@UnusedWildImport");
        pyLintMessageIdToCodeAnalysisError.put("reimported", "@Reimport");

        pyLintMessageIdToCodeAnalysisError.put("no-name-in-module", "@UnresolvedImport");

        pyLintMessageIdToCodeAnalysisError.put("arguments-differ", "@ArgumentMismatch");
        pyLintMessageIdToCodeAnalysisError.put("bad-indentation", "@IndentOk");

        for (Map.Entry<String, String> entry : pyLintMessageIdToCodeAnalysisError.entrySet()) {
            codeAnalysisErrorToPyLintMessageId.put(entry.getValue(), entry.getKey());
        }

        for (Map.Entry<String, String> entry : flake8MessageIdToCodeAnalysisError.entrySet()) {
            codeAnalysisErrorToFlake8MessageId.put(entry.getValue(), entry.getKey());
        }

        codeAnalysisTypePyLintMessageId.put(IMiscConstants.TYPE_UNUSED_IMPORT, "unused-import");
        codeAnalysisTypeFlake8MessageId.put(IMiscConstants.TYPE_UNUSED_IMPORT, "F401");

        codeAnalysisTypePyLintMessageId.put(IMiscConstants.TYPE_UNUSED_VARIABLE, "unused-variable");

        codeAnalysisTypePyLintMessageId.put(IMiscConstants.TYPE_UNDEFINED_VARIABLE, "undefined-variable");
        codeAnalysisTypeFlake8MessageId.put(IMiscConstants.TYPE_UNDEFINED_VARIABLE, "F821");

        codeAnalysisTypePyLintMessageId.put(IMiscConstants.TYPE_NO_SELF, "no-self-argument");
        codeAnalysisTypePyLintMessageId.put(IMiscConstants.TYPE_ASSIGNMENT_TO_BUILT_IN_SYMBOL, "redefined-builtin");
        codeAnalysisTypePyLintMessageId.put(IMiscConstants.TYPE_NO_EFFECT_STMT, "pointless-statement");
        codeAnalysisTypePyLintMessageId.put(IMiscConstants.TYPE_DUPLICATED_SIGNATURE, "function-redefined");
        codeAnalysisTypePyLintMessageId.put(IMiscConstants.TYPE_UNUSED_WILD_IMPORT, "unused-wildcard-import");
        codeAnalysisTypePyLintMessageId.put(IMiscConstants.TYPE_REIMPORT, "reimported");

        codeAnalysisTypePyLintMessageId.put(IMiscConstants.TYPE_UNRESOLVED_IMPORT, "no-name-in-module");

        codeAnalysisTypePyLintMessageId.put(IMiscConstants.TYPE_ARGUMENTS_MISATCH, "arguments-differ");
        codeAnalysisTypePyLintMessageId.put(IMiscConstants.TYPE_INDENTATION_PROBLEM, "bad-indentation");
    }

    public static boolean isPyLintErrorHandledAtLine(String line, String pyLintMessageId) {
        String codeAnalysisIgnoreMessage = pyLintMessageIdToCodeAnalysisError.get(pyLintMessageId);
        String flake8Code = null;
        if (codeAnalysisIgnoreMessage != null) {
            flake8Code = codeAnalysisErrorToFlake8MessageId.get(codeAnalysisIgnoreMessage);
        }
        return isErrorHandledAtLine(line, pyLintMessageId, codeAnalysisIgnoreMessage, flake8Code);
    }

    public static boolean isFlake8ErrorHandledAtLine(String line, String flake8Code) {
        String codeAnalysisIgnoreMessage = flake8MessageIdToCodeAnalysisError.get(flake8Code);
        String pyLintMessageId = null;
        if (codeAnalysisIgnoreMessage != null) {
            pyLintMessageId = codeAnalysisErrorToPyLintMessageId.get(codeAnalysisIgnoreMessage);
        }
        return isErrorHandledAtLine(line, pyLintMessageId, codeAnalysisIgnoreMessage, flake8Code);
    }

    private static boolean isErrorHandledAtLine(String line, String pyLintMessageId, String codeAnalysisIgnoreMessage,
            String flake8Code) {
        int i = line.indexOf('#');
        if (i == -1) {
            return false;
        }
        // Get only the comments and all in lowercase.
        line = line.substring(i).toLowerCase();
        if (pyLintMessageId != null) {
            pyLintMessageId = pyLintMessageId.toLowerCase();
            int pos = -1;
            // Old format.
            if ((pos = line.indexOf("ignore:")) != -1) {
                String lintW = line.substring(pos + "ignore:".length());
                if (lintW.startsWith(pyLintMessageId)) {
                    return true;
                }
            }

            // The message is actually something as "# pylint: disable=" + messageId or "noqa: "+messageId.
            if (line.contains(pyLintMessageId)
                    && (line.contains("disable=") || line.contains("pylint:") || line.contains("noqa:"))) {
                return true;
            }
        }

        if (flake8Code != null) {
            flake8Code = flake8Code.toLowerCase();

            // The message is actually something as"noqa: "+messageId.
            if (line.contains(flake8Code) && line.contains("noqa:")) {
                return true;
            }
        }

        if (codeAnalysisIgnoreMessage != null) {
            codeAnalysisIgnoreMessage = codeAnalysisIgnoreMessage.toLowerCase();
            if (line.contains(codeAnalysisIgnoreMessage)) {
                return true;
            }
        }

        int noqaWithErr = line.indexOf("noqa:");
        if (noqaWithErr != -1) {
            // With id must be same as pylint
        } else if (line.contains("noqa")) { // noqa is a catch all.
            return true;
        }
        return false;
    }

    public static boolean isCodeAnalysisErrorHandled(String line, String messageToIgnore) {
        String pyLintMessageId = codeAnalysisErrorToPyLintMessageId.get(messageToIgnore);
        String flake8Code = codeAnalysisErrorToFlake8MessageId.get(messageToIgnore);
        return isErrorHandledAtLine(line, pyLintMessageId, messageToIgnore, flake8Code);
    }

    public static String getPyLintMessageIdForPyDevAnalysisType(int analysisType) {
        return codeAnalysisTypePyLintMessageId.get(analysisType);
    }

}
