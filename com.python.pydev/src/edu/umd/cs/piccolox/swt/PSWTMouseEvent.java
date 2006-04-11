package edu.umd.cs.piccolox.swt;

import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

/**
 * Overridden to wrap an SWT MouseEvent
 * 
 * @author Lance Good
 */
public class PSWTMouseEvent extends MouseEvent {

	static Component fakeSrc = new Component() {};

	protected org.eclipse.swt.events.MouseEvent swtEvent;
	
	protected int clickCount;

	public PSWTMouseEvent(org.eclipse.swt.events.MouseEvent me, int type, int clickCount) {
		super(fakeSrc,type, me.time, 0, me.x, me.y, clickCount, (me.button == 3), me.button);
		
		this.swtEvent = me;
		this.clickCount = clickCount;
	}

	public Object getSource() {
		return swtEvent.getSource();	
	}

	public int getClickCount() {
		return clickCount;	
	}	

	public int getButton() {
		switch (swtEvent.button) {
			case 1:
				return MouseEvent.BUTTON1;	
			case 2:
				return MouseEvent.BUTTON2;
			case 3:
				return MouseEvent.BUTTON3;
			default:
				return MouseEvent.NOBUTTON;
		}
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
			if (swtEvent.button == 1 ||
				(swtEvent.stateMask & SWT.BUTTON1) != 0) {
				modifiers = modifiers | InputEvent.BUTTON1_MASK;
			}
			if (swtEvent.button == 2 ||
				(swtEvent.stateMask & SWT.BUTTON2) != 0) {
				modifiers = modifiers | InputEvent.BUTTON2_MASK;
			}
			if (swtEvent.button == 3 ||
				(swtEvent.stateMask & SWT.BUTTON3) != 0) {
				modifiers = modifiers | InputEvent.BUTTON3_MASK;
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
			if (swtEvent.button == 1 ||
				(swtEvent.stateMask & SWT.BUTTON1) != 0) {
				modifiers = modifiers | InputEvent.BUTTON1_DOWN_MASK;
			}
			if (swtEvent.button == 2 ||
				(swtEvent.stateMask & SWT.BUTTON2) != 0) {
				modifiers = modifiers | InputEvent.BUTTON2_DOWN_MASK;
			}
			if (swtEvent.button == 3 ||
				(swtEvent.stateMask & SWT.BUTTON3) != 0) {
				modifiers = modifiers | InputEvent.BUTTON3_DOWN_MASK;
			}
		}
		
		return modifiers;
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
