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
package edu.umd.cs.piccolox;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import edu.umd.cs.piccolo.PCanvas;

/**
 * <b>PApplet</b> is meant to be subclassed by applications that just need a PCanvas
 * embedded in a web page.
 *
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PApplet extends JApplet {

    private PCanvas canvas;

    public void init() {
        setBackground(null);

        canvas = createCanvas();
        getContentPane().add(canvas);
        validate();
        canvas.requestFocus();
        beforeInitialize();
        
        // Manipulation of Piccolo's scene graph should be done from Swings
        // event dispatch thread since Piccolo is not thread safe. This code calls
        // initialize() from that thread once the PFrame is initialized, so you are 
        // safe to start working with Piccolo in the initialize() method.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                PApplet.this.initialize();
                repaint();
            }
        });
    }
    
    public PCanvas getCanvas() {
        return canvas;
    }
    
    public PCanvas createCanvas() {
        return new PCanvas();
    }
    
    //****************************************************************
    // Initialize
    //****************************************************************

    /**
     * This method will be called before the initialize() method and will be
     * called on the thread that is constructing this object.
     */
    public void beforeInitialize() {
    }

    /**
     * Subclasses should override this method and add their 
     * Piccolo initialization code there. This method will be called on the
     * swing event dispatch thread. Note that the constructors of PFrame
     * subclasses may not be complete when this method is called. If you need to
     * initailize some things in your class before this method is called place
     * that code in beforeInitialize();
     */
    public void initialize() {
    }    
}