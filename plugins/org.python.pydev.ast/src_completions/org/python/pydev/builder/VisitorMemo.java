package org.python.pydev.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple3;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class VisitorMemo extends HashMap<String, Object> {

    public VisitorMemo() {
        super();
    }

    public VisitorMemo(VisitorMemo memo) {
        super(memo);
        this.setSharedProjectState(memo.getSharedProjectState());
    }

    private static final long serialVersionUID = 9146498000310919785L;

    private List<String> onlyProjectPythonPathStrExternal;
    private List<String> onlyProjectPythonPathStrNonExternal;
    private IPythonNature fCacheNature;

    public List<String> getOnlyProjectPythonPathStr(IPythonNature nature, boolean addExternal) throws CoreException {
        if (fCacheNature != null) {
            if (nature != fCacheNature) {
                onlyProjectPythonPathStrExternal = null;
                onlyProjectPythonPathStrNonExternal = null;
                fCacheNature = nature;
            }
        } else {
            fCacheNature = nature;
        }

        List<String> lst;
        if (addExternal) {
            lst = onlyProjectPythonPathStrExternal;
        } else {
            lst = onlyProjectPythonPathStrNonExternal;
        }
        if (lst == null) {
            String onlyProjectPythonPathStr = nature.getPythonPathNature().getOnlyProjectPythonPathStr(addExternal);
            HashSet<String> projectSourcePath = new HashSet<String>(StringUtils.splitAndRemoveEmptyTrimmed(
                    onlyProjectPythonPathStr, '|'));
            lst = new ArrayList<String>(projectSourcePath);
            if (addExternal) {
                onlyProjectPythonPathStrExternal = lst;
            } else {
                onlyProjectPythonPathStrNonExternal = lst;
            }
        }

        return lst;
    }

    public Object getSharedProjectState() {
        return new Tuple3(fCacheNature, onlyProjectPythonPathStrExternal, onlyProjectPythonPathStrNonExternal);
    }

    public void setSharedProjectState(Object memoSharedProjectState) {
        if (memoSharedProjectState != null) {
            Tuple3 t = (Tuple3) memoSharedProjectState;
            fCacheNature = (IPythonNature) t.o1;
            onlyProjectPythonPathStrExternal = (List<String>) t.o2;
            onlyProjectPythonPathStrNonExternal = (List<String>) t.o3;
        }
    }

}
