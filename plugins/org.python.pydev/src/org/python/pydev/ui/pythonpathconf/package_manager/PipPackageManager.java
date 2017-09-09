package org.python.pydev.ui.pythonpathconf.package_manager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class PipPackageManager extends AbstractPackageManager {

    public PipPackageManager(InterpreterInfo interpreterInfo) {
        super(interpreterInfo);
    }

    /**
     * To be called from any thread
     */
    @Override
    public List<String[]> list() {
        List<String[]> listed = new ArrayList<String[]>();
        File pipExecutable;
        try {
            pipExecutable = interpreterInfo.searchExecutableForInterpreter("pip", false);
        } catch (UnableToFindExecutableException e) {
            return errorToList(listed, e);
        }

        String encoding = null; // use system encoding
        Tuple<String, String> output = new SimpleRunner().runAndGetOutput(
                new String[] { pipExecutable.toString(), "list", "--format=columns" }, null, null, null,
                encoding);

        List<String> splitInLines = StringUtils.splitInLines(output.o1, false);
        for (String line : splitInLines) {
            line = line.trim();
            List<String> split = StringUtils.split(line, ' ');
            if (split.size() == 2) {
                String p0 = split.get(0).trim();
                String p1 = split.get(1).trim();

                if (p0.toLowerCase().equals("package")
                        && p1.toLowerCase().equals("version")) {
                    continue;
                }
                if (p0.toLowerCase().startsWith("--")
                        && p1.toLowerCase().startsWith("--")) {
                    continue;
                }
                listed.add(new String[] { p0.trim(), p1.trim(), "<pip>" });
            }
        }
        return listed;
    }

    @Override
    protected String getPackageManagerName() {
        return "pip";
    }

    @Override
    public void manage() {
        // TODO Auto-generated method stub

    }

}
