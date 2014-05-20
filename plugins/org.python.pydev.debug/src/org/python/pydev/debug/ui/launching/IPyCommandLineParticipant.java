package org.python.pydev.debug.ui.launching;

import org.python.pydev.debug.ui.launching.PythonRunnerCallbacks.CreatedCommandLineParams;

public interface IPyCommandLineParticipant {

    CreatedCommandLineParams updateCommandLine(CreatedCommandLineParams createdCommandLineParams);

}
