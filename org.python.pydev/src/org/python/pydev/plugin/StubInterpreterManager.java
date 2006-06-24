package org.python.pydev.plugin;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;

public class StubInterpreterManager implements IInterpreterManager {
	private boolean isPython;

	StubInterpreterManager(boolean isPython){
		this.isPython = isPython;
	}

	public String getDefaultInterpreter() {
		return null;
	}

	public String[] getInterpreters() {
		return new String[]{};
	}

	public IInterpreterInfo getInterpreterInfo(String executable, IProgressMonitor monitor) {
		return null;
	}

	public IInterpreterInfo getDefaultInterpreterInfo(IProgressMonitor monitor) {
		return null;
	}

	public String addInterpreter(String executable, IProgressMonitor monitor) {
		return null;
	}

	public String[] getInterpretersFromPersistedString(String persisted) {
		return null;
	}

	public String getStringToPersist(String[] executables) {
		return null;
	}

	public boolean hasInfoOnDefaultInterpreter(IPythonNature nature) {
		return false;
	}

	public void clearAllBut(List<String> allButTheseInterpreters) {
	}

	public boolean isJython() {
		return !isPython;
	}

	public boolean isPython() {
		return isPython;
	}

	public void restorePythopathFor(String defaultSelectedInterpreter, IProgressMonitor monitor) {
	}

	public String getManagerRelatedName() {
		return null;
	}

	public String getPersistedString() {
		return null;
	}

	public void setPersistedString(String s) {
	}

    public boolean isConfigured() {
        return false;
    }

    public int getRelatedId() {
        if(isPython()){
            return IPythonNature.PYTHON_RELATED;
        }else if(isJython()){
            return IPythonNature.JYTHON_RELATED;
        }else{
            throw new RuntimeException("Expected Python or Jython");
        }
    }

    public boolean hasInfoOnInterpreter(String interpreter) {
        return false;
    }

}
