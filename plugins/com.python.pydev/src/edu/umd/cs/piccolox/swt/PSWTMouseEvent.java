/*
 * Copyright (c) 2002-@year@, University of Maryland
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the University of Maryland nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Piccolo was written at the Human-Computer Interaction Laboratory www.cs.umd.edu/hcil by Jesse Grosjean
 * under the supervision of Ben Bederson. The Piccolo website is www.cs.umd.edu/hcil/piccolo.
 */
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
