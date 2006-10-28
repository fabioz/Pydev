/*
 * Created on Oct 28, 2006
 * @author Fabio
 */
package org.python.pydev.plugin.nature;

public interface IPythonNatureListener {

    void pythonPathChanged(String projectPythonpath, String externalPythonpath);
}
