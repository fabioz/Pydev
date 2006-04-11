package edu.umd.cs.piccolox.swt;

import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

/**
 * Overridden to wrap an SWT KeyEvent
 * 
 * @author Lance Good
 */
public class PSWTKeyEvent extends KeyEvent {

	static Component fakeSrc = new Component() {};

	org.eclipse.swt.events.KeyEvent swtEvent;
	
	public PSWTKeyEvent(org.eclipse.swt.events.KeyEvent ke, int eventType) {
		super(fakeSrc, eventType, ke.time, 0, ke.keyCode, ke.character, KeyEvent.KEY_LOCATION_STANDARD);
		
		swtEvent = ke;
	}

	public Object getSource() {
		return swtEvent.getSource();	
	}

	public boolean isShiftDown() {
		return (swtEvent.stateMask & SWT.SHIFT) != 0;
	}

	public boolean isControlDown() {
		return (swtEvent.stateMask & SWT.CONTROL) != 0;
	}
	
	public boolean isAltDown() {
		return (swtEvent.stateMask & SWT.ALT) != 0;
	}
	
	public int getModifiers() {
		int modifiers = 0;
		
		if (swtEvent != null) {
			if ((swtEvent.stateMask & SWT.ALT) != 0) {
				modifiers = modifiers | InputEvent.ALT_MASK;	
			}
			if ((swtEvent.stateMask & SWT.CONTROL) != 0) {
				modifiers = modifiers | InputEvent.CTRL_MASK;
			}
			if ((swtEvent.stateMask & SWT.SHIFT) != 0) {
				modifiers = modifiers | InputEvent.SHIFT_MASK;	
			}
		}

		return modifiers;
	}
	
	public int getModifiersEx() {
		int modifiers = 0;
		
		if (swtEvent != null) {
			if ((swtEvent.stateMask & SWT.ALT) != 0) {
				modifiers = modifiers | InputEvent.ALT_DOWN_MASK;	
			}
			if ((swtEvent.stateMask & SWT.CONTROL) != 0) {
				modifiers = modifiers | InputEvent.CTRL_DOWN_MASK;
			}
			if ((swtEvent.stateMask & SWT.SHIFT) != 0) {
				modifiers = modifiers | InputEvent.SHIFT_DOWN_MASK;	
			}
		}
		
		return modifiers;
	}	

	public boolean isActionKey() {
		return false;	
	}

	///////////////////////////
	// THE SWT SPECIFIC EVENTS
	///////////////////////////

	public Widget getWidget() {
		return swtEvent.widget;	
	}
	
	public Display getDisplay() {
		return swtEvent.display;	
	}
	
	public Object getData() {
		return swtEvent.data;	
	}	
}
