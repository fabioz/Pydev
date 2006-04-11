/*
 * Created on Mar 4, 2005
 */
package edu.umd.cs.piccolox.swing;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PRoot;
import edu.umd.cs.piccolox.nodes.PCacheCamera;

/**
 * An extension of PCanvas that automatically installs a PCacheCamera
 * @author Lance Good
 */
public class PCacheCanvas extends PCanvas {
    protected PCamera createDefaultCamera() {
		PRoot r = new PRoot();
		PLayer l = new PLayer();
		PCamera c = new PCacheCamera();
		
		r.addChild(c); 
		r.addChild(l); 
		c.addLayer(l);
		
		return c;		
    }
}
