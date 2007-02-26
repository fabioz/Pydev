package org.python.pydev.refactoring.tests.core;

import junit.framework.Test;

/**
 * @author Dennis Hunziker, Ueli Kistler
 */
public interface IInputOutputTestCase extends Test {

	void setSource(String line);

	void setResult(String line);
	
	void setConfig(String line);	

	String getSource();

	String getResult();

	void setTestGenerated(String string);

}
