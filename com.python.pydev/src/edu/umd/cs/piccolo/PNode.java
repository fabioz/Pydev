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

package edu.umd.cs.piccolo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.event.EventListenerList;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;

import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.activities.PColorActivity;
import edu.umd.cs.piccolo.activities.PInterpolatingActivity;
import edu.umd.cs.piccolo.activities.PTransformActivity;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PNodeFilter;
import edu.umd.cs.piccolo.util.PObjectOutputStream;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PPickPath;
import edu.umd.cs.piccolo.util.PUtil;

/**
 * <b>PNode</b> is the central abstraction in Piccolo. All objects that are
 * visible on the screen are instances of the node class. All nodes may have 
 * other "child" nodes added to them.
 * <p>
 * See edu.umd.piccolo.examples.NodeExample.java for demonstrations of how nodes
 * can be used and how new types of nodes can be created.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PNode implements Cloneable, Serializable, Printable {
		
	/** 
	 * The property name that identifies a change in this node's client
	 * propertie (see {@link #getClientProperty getClientProperty}). In
	 * an property change event the new value will be a reference to the map of
	 * client properties but old value will always be null. 
	 */
    public static final String PROPERTY_CLIENT_PROPERTIES = "clientProperties";
    public static final int PROPERTY_CODE_CLIENT_PROPERTIES = 1 << 0;
    
	/** 
	 * The property name that identifies a change of this node's bounds (see
	 * {@link #getBounds getBounds}, {@link #getBoundsReference
	 * getBoundsReference}). In any property change event the new value will be
	 * a reference to this node's bounds, but old value will always be null.
	 */
    public static final String PROPERTY_BOUNDS = "bounds";
    public static final int PROPERTY_CODE_BOUNDS = 1 << 1;
    
	/** 
	 * The property name that identifies a change of this node's full bounds
	 * (see {@link #getFullBounds getFullBounds}, {@link #getFullBoundsReference
	 * getFullBoundsReference}). In any property change event the new value will
	 * be a reference to this node's full bounds cache, but old value will
	 * always be null.
	 */
    public static final String PROPERTY_FULL_BOUNDS = "fullBounds";
    public static final int PROPERTY_CODE_FULL_BOUNDS = 1 << 2;
    
	/** 
	 * The property name that identifies a change of this node's transform (see
	 * {@link #getTransform getTransform}, {@link #getTransformReference
	 * getTransformReference}). In any property change event the new value will
	 * be a reference to this node's transform, but old value will always be
	 * null.
	 */
    public static final String PROPERTY_TRANSFORM = "transform";
    public static final int PROPERTY_CODE_TRANSFORM = 1 << 3;
    
	/** 
	 * The property name that identifies a change of this node's visibility (see
	 * {@link #getVisible getVisible}). Both old value and new value will be
	 * null in any property change event.
	 */
    public static final String PROPERTY_VISIBLE = "visible";
    public static final int PROPERTY_CODE_VISIBLE = 1 << 4;
    
	/** 
	 * The property name that identifies a change of this node's paint (see
	 * {@link #getPaint getPaint}). Both old value and new value will be set
	 * correctly in any property change event.
	 */
	public static final String PROPERTY_PAINT = "paint";
    public static final int PROPERTY_CODE_PAINT = 1 << 5;
	
	/** 
	 * The property name that identifies a change of this node's transparency
	 * (see {@link #getTransparency getTransparency}). Both old value and new
	 * value will be null in any property change event.
	 */
	public static final String PROPERTY_TRANSPARENCY = "transparency";
    public static final int PROPERTY_CODE_TRANSPARENCY = 1 << 6;
	
	/** 
	 * The property name that identifies a change of this node's pickable status
	 * (see {@link #getPickable getPickable}). Both old value and new value will
	 * be null in any property change event.
	 */
	public static final String PROPERTY_PICKABLE = "pickable";
    public static final int PROPERTY_CODE_PICKABLE = 1 << 7;
	
	/** 
	 * The property name that identifies a change of this node's children
	 * pickable status (see {@link #getChildrenPickable getChildrenPickable}).
	 * Both old value and new value will be null in any property change event.
	 */
	public static final String PROPERTY_CHILDREN_PICKABLE = "childrenPickable";
    public static final int PROPERTY_CODE_CHILDREN_PICKABLE = 1 << 8;

	/** 
	 * The property name that identifies a change in the set of this node's direct children
	 * (see {@link #getChildrenReference getChildrenReference}, {@link #getChildrenIterator getChildrenIterator}).
	 * In any property change event the new value will be a reference to this node's children,
	 * but  old value will always be null. */
    public static final String PROPERTY_CHILDREN = "children";
    public static final int PROPERTY_CODE_CHILDREN = 1 << 9;
 
	/** 
	 * The property name that identifies a change of this node's parent
	 * (see {@link #getParent getParent}). 
	 * Both old value and new value will be set correctly in any property change event. 
	 */
    public static final String PROPERTY_PARENT = "parent";
    public static final int PROPERTY_CODE_PARENT = 1 << 10;
		
	private static final PBounds TEMP_REPAINT_BOUNDS = new PBounds();

	/**
	 * The single scene graph delegate that recives low level node events.
	 */
	public static PSceneGraphDelegate SCENE_GRAPH_DELEGATE = null;

	/**
	 * <b>PSceneGraphDelegate</b> is an interface to recive low level node events. It together
	 * with PNode.SCENE_GRAPH_DELEGATE gives Piccolo users an efficient way to learn about
	 * low level changes in Piccolo's scene graph. Most users will not need to use this.
	 */
	public interface PSceneGraphDelegate {
		public void nodePaintInvalidated(PNode node);
		public void nodeFullBoundsInvalidated(PNode node);
	}
	
	private transient PNode parent;
	private List children;
	private PBounds bounds;
	private PAffineTransform transform;
	private Paint paint;
	private float transparency;
	private MutableAttributeSet clientProperties;
	private PBounds fullBoundsCache;

	private int propertyChangeParentMask = 0;
	private transient SwingPropertyChangeSupport changeSupport;
	private transient EventListenerList listenerList;
	
	private boolean pickable;
	private boolean childrenPickable;
	private boolean visible;
	private boolean childBoundsVolatile;
	private boolean paintInvalid;
	private boolean childPaintInvalid;
	private boolean boundsChanged;
	private boolean fullBoundsInvalid;
	private boolean childBoundsInvalid;
	private boolean occluded;

    /**
	 * Constructs a new PNode.
	 * <P>
	 * By default a node's paint is null, and bounds are empty. These values
	 * must be set for the node to show up on the screen once it's added to
	 * a scene graph. 
	 */
	public PNode() {
		bounds = new PBounds();
		fullBoundsCache = new PBounds();
		transparency = 1.0f;
		pickable = true;
		childrenPickable = true;
		visible = true;
	}
	
	//****************************************************************
	// Animation - Methods to animate this node.
	// 
	// Note that animation is implemented by activities (PActivity), 
	// so if you need more control over your animation look at the 
	// activities package. Each animate method creates an animation that
	// will animate the node from its current state to the new state
	// specified over the given duration. These methods will try to 
	// automatically schedule the new activity, but if the node does not
	// descend from the root node when the method is called then the 
	// activity will not be scheduled and you must schedule it manually.
	//****************************************************************

	/**
	 * Animate this node's bounds from their current location when the activity
	 * starts to the specified bounds. If this node descends from the root then
	 * the activity will be scheduled, else the returned activity should be
	 * scheduled manually. If two different transform activities are scheduled
	 * for the same node at the same time, they will both be applied to the
	 * node, but the last one scheduled will be applied last on each frame, so
	 * it will appear to have replaced the original. Generally you will not want
	 * to do that. Note this method animates the node's bounds, but does not change
	 * the node's transform. Use animateTransformToBounds() to animate the node's
	 * transform instead.
	 * 
	 * @param duration amount of time that the animation should take
	 * @return the newly scheduled activity
	 */
	public PInterpolatingActivity animateToBounds(double x, double y, double width, double height, long duration) {
		if (duration == 0) {
			setBounds(x, y, width, height);
			return null;
		} else {
			final PBounds dst = new PBounds(x, y, width, height);
			
			PInterpolatingActivity ta = new PInterpolatingActivity(duration, PUtil.DEFAULT_ACTIVITY_STEP_RATE) {
				private PBounds src;

				protected void activityStarted() {
					src = getBounds();
					startResizeBounds();
					super.activityStarted();
				}
				
				public void setRelativeTargetValue(float zeroToOne) {
					PNode.this.setBounds(src.x + (zeroToOne * (dst.x - src.x)),
										 src.y + (zeroToOne * (dst.y - src.y)),
										 src.width + (zeroToOne * (dst.width - src.width)),
										 src.height + (zeroToOne * (dst.height - src.height)));
				}
				
				protected void activityFinished() {
					super.activityFinished();
					endResizeBounds();
				}
			};

			addActivity(ta);
			return ta;
		}
	}

	/**
	 * Animate this node from it's current transform when the activity starts 
	 * a new transform that will fit the node into the given bounds. If this 
	 * node descends from the root then the activity will be scheduled, else 
	 * the returned activity should be scheduled manually. If two different 
	 * transform activities are scheduled for the same node at the same time, 
	 * they will both be applied to the node, but the last one scheduled will be 
	 * applied last on each frame, so it will appear to have replaced the original.
	 * Generally you will not want to do that. Note this method animates the node's
	 * transform, but does not directly change the node's bounds rectangle. Use 
	 * animateToBounds() to animate the node's bounds rectangle instead.
	 * 
	 * @param duration amount of time that the animation should take
	 * @return the newly scheduled activity
	 */
	public PTransformActivity animateTransformToBounds(double x, double y, double width, double height, long duration) {
		PAffineTransform t = new PAffineTransform();
		t.setToScale(width / getWidth(), height / getHeight());
		t.setOffset(x, y);
		return animateToTransform(t, duration);
	}

	/**
	 * Animate this node's transform from its current location when the
	 * activity starts to the specified location, scale, and rotation. If this
	 * node descends from the root then the activity will be scheduled, else the
	 * returned activity should be scheduled manually. If two different
	 * transform activities are scheduled for the same node at the same time,
	 * they will both be applied to the node, but the last one scheduled will be
	 * applied last on each frame, so it will appear to have replaced the
	 * original. Generally you will not want to do that.
	 * 
	 * @param duration amount of time that the animation should take
	 * @param theta final theta value (in radians) for the animation
	 * @return the newly scheduled activity
	 */
	public PTransformActivity animateToPositionScaleRotation(double x, double y, double scale, double theta, long duration) {
		PAffineTransform t = getTransform();
		t.setOffset(x, y);
		t.setScale(scale);
		t.setRotation(theta);
		return animateToTransform(t, duration);
	}

	/**
	 * Animate this node's transform from its current values when the activity
	 * starts to the new values specified in the given transform. If this node
	 * descends from the root then the activity will be scheduled, else the
	 * returned activity should be scheduled manually. If two different
	 * transform activities are scheduled for the same node at the same time,
	 * they will both be applied to the node, but the last one scheduled will be
	 * applied last on each frame, so it will appear to have replaced the
	 * original. Generally you will not want to do that.
	 *
	 * @param destTransform the final transform value
	 * @param duration amount of time that the animation should take
	 * @return the newly scheduled activity
	 */
	public PTransformActivity animateToTransform(AffineTransform destTransform, long duration) {
		if (duration == 0) {
			setTransform(destTransform);
			return null;
		} else {
			PTransformActivity.Target t = new PTransformActivity.Target() {
				public void setTransform(AffineTransform aTransform) {
					PNode.this.setTransform(aTransform);
				}
				public void getSourceMatrix(double[] aSource) {
					PNode.this.getTransformReference(true).getMatrix(aSource);
				}
			};		
			
			PTransformActivity ta = new PTransformActivity(duration, PUtil.DEFAULT_ACTIVITY_STEP_RATE, t, destTransform);
			addActivity(ta);		
			return ta;
		}
	}

	/**
	 * Animate this node's color from its current value to the new value
	 * specified. This meathod assumes that this nodes paint property is of
	 * type color. If this node descends from the root then the activity will be
	 * scheduled, else the returned activity should be scheduled manually. If
	 * two different color activities are scheduled for the same node at the
	 * same time, they will both be applied to the node, but the last one
	 * scheduled will be applied last on each frame, so it will appear to have
	 * replaced the original. Generally you will not want to do that.
	 *
	 * @param destColor final color value.
	 * @param duration amount of time that the animation should take
	 * @return the newly scheduled activity
	 */
	public PInterpolatingActivity animateToColor(Color destColor, long duration) {
		if (duration == 0) {
			setPaint(destColor);
			return null;
		} else {
			PColorActivity.Target t = new PColorActivity.Target() {
				public Color getColor() {
					return (Color) getPaint();
				}
				public void setColor(Color color) {
					setPaint(color);
				}
			};
			
			PColorActivity ca = new PColorActivity(duration, PUtil.DEFAULT_ACTIVITY_STEP_RATE, t, destColor);
			addActivity(ca);
			return ca;
		}
	}
	
	/**
	 * Animate this node's transparency from its current value to the 
	 * new value specified. Transparency values must range from zero to one. 
	 * If this node descends from the root then the activity will be 
	 * scheduled, else the returned activity should be scheduled manually.
	 * If two different transparency activities are scheduled for the same
	 * node at the same time, they will both be applied to the node, but the 
	 * last one scheduled will be applied last on each frame, so it will appear 
	 * to have replaced the original. Generally you will not want to do that.
	 *
	 * @param zeroToOne final transparency value.
	 * @param duration amount of time that the animation should take
	 * @return the newly scheduled activity
	 */
	public PInterpolatingActivity animateToTransparency(float zeroToOne, long duration) {
		if (duration == 0) {
			setTransparency(zeroToOne);
			return null;
		} else {
			final float dest = zeroToOne;
			
			PInterpolatingActivity ta = new PInterpolatingActivity(duration, PUtil.DEFAULT_ACTIVITY_STEP_RATE) {
				private float source;
							
				protected void activityStarted() {
					source = getTransparency();
					super.activityStarted();
				}
				
				public void setRelativeTargetValue(float zeroToOne) {
					PNode.this.setTransparency(source + (zeroToOne * (dest - source)));
				}							
			};

			addActivity(ta);
			return ta;
		}
	}
	
	/**
	 * Schedule the given activity with the root, note that only scheduled
	 * activities will be stepped. If the activity is successfully added true is
	 * returned, else false. 
	 * 
	 * @param activity new activity to schedule
	 * @return true if the activity is successfully scheduled.
	 */
	public boolean addActivity(PActivity activity) {
		PRoot r = getRoot();
		if (r != null) {
			return r.addActivity(activity);
		}
		return false;
	}
			
	// ****************************************************************
	// Client Properties - Methods for managing client properties for
	// this node.
	//
	// Client properties provide a way for programmers to attach
	// extra information to a node without having to subclass it and
	// add new instance variables.
	//****************************************************************

	/**
	 * Return mutable attributed set of client properites associated with
	 * this node.
	 */
    public MutableAttributeSet getClientProperties() {
        if (clientProperties == null) {
            clientProperties = new SimpleAttributeSet();
        }
        return clientProperties;
    }

	/**
	 * Returns the value of the client attribute with the specified key. Only
	 * attributes added with <code>addAttribute</code> will return
	 * a non-null value.
	 *
	 * @return the value of this attribute or null
	 */
    public Object getAttribute(Object key) {
        if (clientProperties == null || key == null) {
            return null;
        } else {
            return clientProperties.getAttribute(key);
        }
    }

	/**
	 * Add an arbitrary key/value to this node.
	 * <p>
	 * The <code>get/add attribute<code> methods provide access to
	 * a small per-instance attribute set. Callers can use get/add attribute
	 * to annotate nodes that were created by another module.
	 * <p>
	 * If value is null this method will remove the attribute.
	 */
	public void addAttribute(Object key, Object value) {
		if (value == null && clientProperties == null) return;

		Object oldValue = getAttribute(key);

		if (value != oldValue) {
			if (clientProperties == null) {
				clientProperties = new SimpleAttributeSet();
			}
	
			if (value == null) {
				clientProperties.removeAttribute(key);
			} else {
				clientProperties.addAttribute(key, value);
			}
	
			if (clientProperties.getAttributeCount() == 0 && clientProperties.getResolveParent() == null) {
				clientProperties = null;
			}
	
			firePropertyChange(PROPERTY_CODE_CLIENT_PROPERTIES, PROPERTY_CLIENT_PROPERTIES, null, clientProperties);
			firePropertyChange(PROPERTY_CODE_CLIENT_PROPERTIES, key.toString(), oldValue, value);
		}
	}

	/**
	 * Returns an enumeration of all keys maped to attribute values values.
	 *
	 * @return an Enumeration over attribute keys
	 */
	public Enumeration getClientPropertyKeysEnumeration() {
		if (clientProperties == null) {
			return PUtil.NULL_ENUMERATION;
		} else {
			return clientProperties.getAttributeNames();
		}
	}

    // convenience methods for attributes

    public Object getAttribute(Object key, Object def) {
        Object o = getAttribute(key);
        return (o == null ? def : o);
    }

    public boolean getBooleanAttribute(Object key, boolean def) {
        Boolean b = (Boolean)getAttribute(key);
        return (b == null ? def : b.booleanValue());
    }

    public int getIntegerAttribute(Object key, int def) {
        Number n = (Number)getAttribute(key);
        return (n == null ? def : n.intValue());
    }

    public double getDoubleAttribute(Object key, double def) {
        Number n = (Number)getAttribute(key);
        return (n == null ? def : n.doubleValue());
    }

	/**
	 * @deprecated use getAttribute(Object key)instead.
	 */
	public Object getClientProperty(Object key) {
        return getAttribute(key);
	}

	/**
	 * @deprecated use addAttribute(Object key, Object value)instead.
	 */
	public void addClientProperty(Object key, Object value) {
        addAttribute(key, value);
	}

	/**
	 * @deprecated use getClientPropertyKeysEnumerator() instead.
	 */
	public Iterator getClientPropertyKeysIterator() {
		final Enumeration enumeration = getClientPropertyKeysEnumeration();
		return new Iterator() {
			public boolean hasNext() {
				return enumeration.hasMoreElements();
			}
			public Object next() {
				return enumeration.nextElement();
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	//****************************************************************
	// Copying - Methods for copying this node and its descendants.
	// Copying is implemened in terms of serialization.
	//****************************************************************
	
	/**
	 * The copy method copies this node and all of its descendents. Note
	 * that copying is implemented in terms of java serialization. See
	 * the serialization notes for more information.
	 * 
	 * @return new copy of this node or null if the node was not serializable
	 */ 
	public Object clone() {
		try {
			byte[] ser = PObjectOutputStream.toByteArray(this);
			return (PNode) new ObjectInputStream(new ByteArrayInputStream(ser)).readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	//****************************************************************
	// Coordinate System Conversions - Methods for converting 
	// geometry between this nodes local coordinates and the other 
	// major coordinate systems.
	// 
	// Each nodes has an affine transform that it uses to define its
	// own coordinate system. For example if you create a new node and 
	// add it to the canvas it will appear in the upper right corner. Its
	// coordinate system matches the coordinate system of its parent
	// (the root node) at this point. But if you move this node by calling
	// node.translate() the nodes affine transform will be modified and the
	// node will appear at a different location on the screen. The node
	// coordinate system no longer matches the coordinate system of its
	// parent. 
	//
	// This is useful because it means that the node's methods for 
	// rendering and picking don't need to worry about the fact that 
	// the node has been moved to another position on the screen, they
	// keep working just like they did when it was in the upper right 
	// hand corner of the screen.
	// 
	// The problem is now that each node defines its own coordinate
	// system it is difficult to compare the positions of two node with
	// each other. These methods are all meant to help solve that problem.
	// 
	// The terms used in the methods are as follows:
	// 
	// local - The local or base coordinate system of a node.
	// parent - The coordinate system of a node's parent
	// global - The topmost coordinate system, above the root node.
	// 
	// Normally when comparing the positions of two nodes you will 
	// convert the local position of each node to the global coordinate
	// system, and then compare the positions in that common coordinate
	// system.
	//***************************************************************

	/**
	 * Transform the given point from this node's local coordinate system to
	 * its parent's local coordinate system. Note that this will modify the point
	 * parameter.
	 * 
	 * @param localPoint point in local coordinate system to be transformed.
	 * @return point in parent's local coordinate system
	 */ 
	public Point2D localToParent(Point2D localPoint) {
		if (transform == null) return localPoint;
		return transform.transform(localPoint, localPoint);
	}

	/**
	 * Transform the given dimension from this node's local coordinate system to
	 * its parent's local coordinate system. Note that this will modify the dimension
	 * parameter.
	 * 
	 * @param localDimension dimension in local coordinate system to be transformed.
	 * @return dimension in parent's local coordinate system
	 */ 
	public Dimension2D localToParent(Dimension2D localDimension) {
		if (transform == null) return localDimension;
		return transform.transform(localDimension, localDimension);
	}

	/**
	 * Transform the given rectangle from this node's local coordinate system to
	 * its parent's local coordinate system. Note that this will modify the rectangle
	 * parameter.
	 * 
	 * @param localRectangle rectangle in local coordinate system to be transformed.
	 * @return rectangle in parent's local coordinate system
	 */ 
	public Rectangle2D localToParent(Rectangle2D localRectangle) {
		if (transform == null) return localRectangle;
		return transform.transform(localRectangle, localRectangle);
	}

	/**
	 * Transform the given point from this node's parent's local coordinate system to
	 * the local coordinate system of this node. Note that this will modify the point
	 * parameter.
	 * 
	 * @param parentPoint point in parent's coordinate system to be transformed.
	 * @return point in this node's local coordinate system
	 */ 
	public Point2D parentToLocal(Point2D parentPoint) {
		if (transform == null) return parentPoint;
		try {
			return transform.inverseTransform(parentPoint, parentPoint);
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Transform the given dimension from this node's parent's local coordinate system to
	 * the local coordinate system of this node. Note that this will modify the dimension
	 * parameter.
	 * 
	 * @param parentDimension dimension in parent's coordinate system to be transformed.
	 * @return dimension in this node's local coordinate system
	 */ 
	public Dimension2D parentToLocal(Dimension2D parentDimension) {
		if (transform == null) return parentDimension;
		return transform.inverseTransform(parentDimension, parentDimension);
	}

	/**
	 * Transform the given rectangle from this node's parent's local coordinate system to
	 * the local coordinate system of this node. Note that this will modify the rectangle
	 * parameter.
	 * 
	 * @param parentRectangle rectangle in parent's coordinate system to be transformed.
	 * @return rectangle in this node's local coordinate system
	 */ 
	public Rectangle2D parentToLocal(Rectangle2D parentRectangle) {
		if (transform == null) return parentRectangle;
		return transform.inverseTransform(parentRectangle, parentRectangle);
	}

	/**
	 * Transform the given point from this node's local coordinate system to
	 * the global coordinate system. Note that this will modify the point
	 * parameter.
	 * 
	 * @param localPoint point in local coordinate system to be transformed.
	 * @return point in global coordinates
	 */ 
	public Point2D localToGlobal(Point2D localPoint) {
		PNode n = this;
		while (n != null) {
			localPoint = n.localToParent(localPoint);
			n = n.parent;
		}
		return localPoint;
	}

	/**
	 * Transform the given dimension from this node's local coordinate system to
	 * the global coordinate system. Note that this will modify the dimension
	 * parameter.
	 * 
	 * @param localDimension dimension in local coordinate system to be transformed.
	 * @return dimension in global coordinates
	 */ 
	public Dimension2D localToGlobal(Dimension2D localDimension) {
		PNode n = this;
		while (n != null) {
			localDimension = n.localToParent(localDimension);
			n = n.parent;
		}
		return localDimension;
	}

	/**
	 * Transform the given rectangle from this node's local coordinate system to
	 * the global coordinate system. Note that this will modify the rectangle
	 * parameter.
	 * 
	 * @param localRectangle rectangle in local coordinate system to be transformed.
	 * @return rectangle in global coordinates
	 */ 
	public Rectangle2D localToGlobal(Rectangle2D localRectangle) {
		PNode n = this; 	
		while (n != null) {
			localRectangle = n.localToParent(localRectangle);
			n = n.parent;
		}
		return localRectangle;
	}

	/**
	 * Transform the given point from global coordinates to this node's 
	 * local coordinate system. Note that this will modify the point
	 * parameter.
	 * 
	 * @param globalPoint point in global coordinates to be transformed.
	 * @return point in this node's local coordinate system.
	 */ 
	public Point2D globalToLocal(Point2D globalPoint) {
		if (parent != null) {
			globalPoint = parent.globalToLocal(globalPoint);
		}
		return parentToLocal(globalPoint);
	}

	/**
	 * Transform the given dimension from global coordinates to this node's 
	 * local coordinate system. Note that this will modify the dimension
	 * parameter.
	 * 
	 * @param globalDimension dimension in global coordinates to be transformed.
	 * @return dimension in this node's local coordinate system.
	 */ 
	public Dimension2D globalToLocal(Dimension2D globalDimension) {
		if (parent != null) {
			globalDimension = parent.globalToLocal(globalDimension);
		}
		return parentToLocal(globalDimension);
	}

	/**
	 * Transform the given rectangle from global coordinates to this node's 
	 * local coordinate system. Note that this will modify the rectangle
	 * parameter.
	 * 
	 * @param globalRectangle rectangle in global coordinates to be transformed.
	 * @return rectangle in this node's local coordinate system.
	 */ 
	public Rectangle2D globalToLocal(Rectangle2D globalRectangle) {
		if (parent != null) {
			globalRectangle = parent.globalToLocal(globalRectangle);
		}
		return parentToLocal(globalRectangle);
	}
				
	/**
	 * Return the transform that converts local coordinates at this node 
	 * to the global coordinate system.
	 * 
	 * @return The concatenation of transforms from the top node down to this node.
	 */
	public PAffineTransform getLocalToGlobalTransform(PAffineTransform dest) {
		if (parent != null) {
			dest = parent.getLocalToGlobalTransform(dest);
			if (transform != null) dest.concatenate(transform);
		} else {
			if (dest == null) {
				dest = getTransform();
			} else {
				if (transform != null) {
					dest.setTransform(transform);
				} else {
					dest.setToIdentity();
				}
			}		
		}
		return dest;
	}

	/**
	 * Return the transform that converts global coordinates 
	 * to local coordinates of this node.
	 * 
	 * @return The inverse of the concatenation of transforms from the root down to this node.
	 */
	public PAffineTransform getGlobalToLocalTransform(PAffineTransform dest) {
		try {
			dest = getLocalToGlobalTransform(dest);
			dest.setTransform(dest.createInverse());
			return dest;
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		return null;
	}			
		
	//****************************************************************
	// Event Listeners - Methods for adding and removing event listeners
	// from a node.
	// 
	// Here methods are provided to add property change listeners and
	// input event listeners. The property change listeners are notified 
	// when certain properties of this node change, and the input event 
	// listeners are notified when the nodes receives new key and mouse 
	// events.
	//****************************************************************

	/**
	 * Return the list of event listeners associated with this node.
	 * 
	 * @return event listener list or null
	 */
	public EventListenerList getListenerList() {
		return listenerList;
	}
	
	/**
	 * Adds the specified input event listener to receive input events 
	 * from this node.
	 *
	 * @param listener the new input listener
	 */
	public void addInputEventListener(PInputEventListener listener) {
		if (listenerList == null) listenerList = new EventListenerList();
		getListenerList().add(PInputEventListener.class, listener);
	}	 

	/**
	 * Removes the specified input event listener so that it no longer 
	 * receives input events from this node.
	 *
	 * @param listener the input listener to remove
	 */
	public void removeInputEventListener(PInputEventListener listener) {
		if (listenerList == null) return;
		getListenerList().remove(PInputEventListener.class, listener);
		if (listenerList.getListenerCount() == 0) {
			listenerList = null;
		}
	}	 
			
	/**
	 * Add a PropertyChangeListener to the listener list.
	 * The listener is registered for all properties.
	 * See the fields in PNode and subclasses that start
	 * with PROPERTY_ to find out which properties exist.
	 * @param listener	The PropertyChangeListener to be added
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (changeSupport == null) {
			changeSupport = new SwingPropertyChangeSupport(this);
		}
		changeSupport.addPropertyChangeListener(listener);
	}

	/**
	 * Add a PropertyChangeListener for a specific property.  The listener
	 * will be invoked only when a call on firePropertyChange names that
	 * specific property. See the fields in PNode and subclasses that start
	 * with PROPERTY_ to find out which properties are supported.
	 * @param propertyName	The name of the property to listen on.
	 * @param listener	The PropertyChangeListener to be added
	 */
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		if (listener == null) {
			return;
		}
		if (changeSupport == null) {
			changeSupport = new SwingPropertyChangeSupport(this);
		}
		changeSupport.addPropertyChangeListener(propertyName, listener);
	}

	/**
	 * Remove a PropertyChangeListener from the listener list.
	 * This removes a PropertyChangeListener that was registered
	 * for all properties.
	 *
	 * @param listener	The PropertyChangeListener to be removed
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		if (changeSupport != null) {
			changeSupport.removePropertyChangeListener(listener);
		}
	}

	/**
	 * Remove a PropertyChangeListener for a specific property.
	 *
	 * @param propertyName	The name of the property that was listened on.
	 * @param listener	The PropertyChangeListener to be removed
	 */
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		if (listener == null) {
			return;
		}
		if (changeSupport == null) {
			return;
		}
		changeSupport.removePropertyChangeListener(propertyName, listener);
	}

	/**
	 * Return the propertyChangeParentMask that determines which property
	 * change events are forwared to this nodes parent so that its property
	 * change listeners will also be notified.
	 */
	public int getPropertyChangeParentMask() {
		return propertyChangeParentMask;
	}
	
	/**
	 * Set the propertyChangeParentMask that determines which property
	 * change events are forwared to this nodes parent so that its property
	 * change listeners will also be notified.
	 */
	public void setPropertyChangeParentMask(int propertyChangeParentMask) {
		this.propertyChangeParentMask = propertyChangeParentMask;
	}

	/**
	 * Report a bound property update to any registered listeners.
	 * No event is fired if old and new are equal and non-null. If the propertyCode
	 * exists in this node's propertyChangeParentMask then a property change event
	 * will also be fired on this nodes parent.
	 *
	 * @param propertyCode	The code of the property changed.
	 * @param propertyName	The programmatic name of the property that was changed.
	 * @param oldValue	The old value of the property.
	 * @param newValue	The new value of the property.
	 */
    protected void firePropertyChange(int propertyCode, String propertyName, Object oldValue, Object newValue) {
    	PropertyChangeEvent event = null;
    	
		if (changeSupport != null) {
			event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
			changeSupport.firePropertyChange(event);
		}
		if (parent != null && (propertyCode & propertyChangeParentMask) != 0) {
			if (event == null) event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
			parent.fireChildPropertyChange(event, propertyCode);
		}
	}

	/**
	 * Called by child node to forward property change events up the node tree
	 * so that property change listeners registered with this node will be notified
	 * of property changes of its children nodes. For performance reason only propertyCodes
	 * listed in the propertyChangeParentMask are forwarded.
	 *
	 * @param event	The property change event containing source node and changed values.
	 * @param propertyCode	The code of the property changed.
	 */
	protected void fireChildPropertyChange(PropertyChangeEvent event, int propertyCode) {
		if (changeSupport != null) {
			changeSupport.firePropertyChange(event);
		}
		if (parent != null && (propertyCode & propertyChangeParentMask) != 0) {
			parent.fireChildPropertyChange(event, propertyCode);
		}
	}
	
	//****************************************************************
	// Bounds Geometry - Methods for setting and querying the bounds
	// of this node.
	// 
	// The bounds of a node store the node's position and size in
	// the nodes local coordinate system. Many node subclasses will need
	// to override the setBounds method so that they can update their
	// internal state appropriately. See PPath for an example.
	// 
	// Since the bounds are stored in the local coordinate system
	// they WILL NOT change if the node is scaled, translated, or rotated.
	// 
	// The bounds may be accessed with either getBounds, or 
	// getBoundsReference. The former returns a copy of the bounds
	// the latter returns a reference to the nodes bounds that should
	// normally not be modified. If a node is marked as volatile then
	// it may modify its bounds before returning them from getBoundsReference,
	// otherwise it may not.
	//****************************************************************

	/**
	 * Return a copy of this node's bounds. These bounds are stored in 
	 * the local coordinate system of this node and do not include the
	 * bounds of any of this node's children.
	 */
	public PBounds getBounds() {
		return (PBounds) getBoundsReference().clone();
	}

	/**
	 * Return a direct reference to this node's bounds. These bounds 
	 * are stored in the local coordinate system of this node and do 
	 * not include the bounds of any of this node's children. The value
	 * returned should not be modified.
	 */
	public PBounds getBoundsReference() {
		return bounds;
	}
	
	/**
	 * Notify this node that you will beging to repeadily call <code>setBounds</code>. 
	 * When you are done call <code>endResizeBounds</code> to let the node know that
	 * you are done.
	 */
	public void startResizeBounds() {
	}
				
	/**
	 * Notify this node that you have finished a resize bounds sequence.
	 */
	public void endResizeBounds() {
	}	

	public boolean setX(double x) {
		return setBounds(x, getY(), getWidth(), getHeight());		
	}
	
	public boolean setY(double y) {
		return setBounds(getX(), y, getWidth(), getHeight());
	}
	
	public boolean setWidth(double width) {
		return setBounds(getX(), getY(), width, getHeight());
	}
	
	public boolean setHeight(double height) {
		return setBounds(getX(), getY(), getWidth(), height);
	}
	
	/**
	 * Set the bounds of this node to the given value. These bounds 
	 * are stored in the local coordinate system of this node.
	 * 
	 * @return true if the bounds changed.
	 */
	public boolean setBounds(Rectangle2D newBounds) {
		return setBounds(newBounds.getX(), newBounds.getY(), newBounds.getWidth(), newBounds.getHeight());
	}

	/**
	 * Set the bounds of this node to the given value. These bounds 
	 * are stored in the local coordinate system of this node.
	 * 
	 * If the width or height is less then or equal to zero then the bound's
	 * emtpy bit will be set to true.
	 * 
	 * Subclasses must call the super.setBounds() method.
	 *	
	 * @return true if the bounds changed.
	 */
	public boolean setBounds(double x, double y, double width, double height) {
		if (bounds.x != x || bounds.y != y || bounds.width != width || bounds.height != height) {
			bounds.setRect(x, y, width, height);

			if (width <= 0 || height <= 0) {
				bounds.reset();
			}

			internalUpdateBounds(x, y, width, height);
			invalidatePaint();
			signalBoundsChanged();			
			return true;
		}
		// Don't put any invalidating code here or else nodes with volatile bounds will
		// create a soft infinite loop (calling Swing.invokeLater()) when they validate
		// their bounds.
		return false;		
	}

	/**
	 * Gives nodes a chance to update their internal structure
	 * before bounds changed notifications are sent. When this message
	 * is recived the nodes bounds field will contain the new value.
	 * 
	 * See PPath for an example that uses this method.
	 */
	protected void internalUpdateBounds(double x, double y, double width, double height) {
	}
	
	/**
	 * Set the empty bit of this bounds to true.
	 */
	public void resetBounds() {
		setBounds(0, 0, 0, 0);
	}

	/**
	 * Return the x position (in local coords) of this node's bounds.
	 */
	public double getX() {
		return getBoundsReference().getX();
	}

	/**
	 * Return the y position (in local coords) of this node's bounds.
	 */
	public double getY() {
		return getBoundsReference().getY();
	}
		
	/**
	 * Return the width (in local coords) of this node's bounds.
	 */
	public double getWidth() {
		return getBoundsReference().getWidth();
	}

	/**
	 * Return the height (in local coords) of this node's bounds.
	 */
	public double getHeight() {
		return getBoundsReference().getHeight();
	}
	
	/**
	 * Return a copy of the bounds of this node in the global 
	 * coordinate system.
	 * 
	 * @return the bounds in global coordinate system.
	 */
	public PBounds getGlobalBounds() {
		return (PBounds) localToGlobal(getBounds());
	}
	
	/**
	 * Center the bounds of this node so that they are centered on the given
	 * point specified on the local coords of this node. Note that this meathod
	 * will modify the nodes bounds, while centerFullBoundsOnPoint will modify
	 * the nodes transform.
	 * 
	 * @return true if the bounds changed.
	 */
	public boolean centerBoundsOnPoint(double localX, double localY) {
		double dx = localX - bounds.getCenterX();
		double dy = localY - bounds.getCenterY();
		return setBounds(bounds.x + dx, bounds.y + dy, bounds.width, bounds.height);
	}

	/**
	 * Center the ffull bounds of this node so that they are centered on the
	 * given point specified on the local coords of this nodes parent. Note that
	 * this meathod will modify the nodes transform, while centerBoundsOnPoint
	 * will modify the nodes bounds.
	 */
	public void centerFullBoundsOnPoint(double parentX, double parentY) {
		double dx = parentX - getFullBoundsReference().getCenterX();
		double dy = parentY - getFullBoundsReference().getCenterY();
		offset(dx, dy);
	}
	
	/**
	 * Return true if this node intersects the given rectangle specified in
	 * local bounds. If the geometry of this node is complex this method can become
	 * expensive, it is therefore recommended that <code>fullIntersects</code> is used
	 * for quick rejects before calling this method.
	 * 
	 * @param localBounds the bounds to test for intersection against
	 * @return true if the given rectangle intersects this nodes geometry.
	 */
	public boolean intersects(Rectangle2D localBounds) {
		if (localBounds == null) return true;
		return getBoundsReference().intersects(localBounds);
	}

	//****************************************************************
	// Full Bounds - Methods for computing and querying the 
	// full bounds of this node.
	// 
	// The full bounds of a node store the nodes bounds 
	// together with the union of the bounds of all the 
	// node's descendents. The full bounds are stored in the parent
	// coordinate system of this node, the full bounds DOES change 
	// when you translate, scale, or rotate this node.
	// 
	// The full bounds may be accessed with either getFullBounds, or 
	// getFullBoundsReference. The former returns a copy of the full bounds
	// the latter returns a reference to the node's full bounds that should
	// not be modified.
	//****************************************************************

	/**
	 * Return a copy of this node's full bounds. These bounds are stored in 
	 * the parent coordinate system of this node and they include the
	 * union of this node's bounds and all the bounds of it's descendents.
	 * 
	 * @return a copy of this node's full bounds. 
	 */
	public PBounds getFullBounds() {
		return (PBounds) getFullBoundsReference().clone();
	}

	/**
	 * Return a reference to this node's full bounds cache. These bounds are 
	 * stored in the parent coordinate system of this node and they include the
	 * union of this node's bounds and all the bounds of it's descendents. The bounds
	 * returned by this method should not be modified.
	 * 
	 * @return a reference to this node's full bounds cache. 
	 */
	public PBounds getFullBoundsReference() {
		validateFullBounds();
		return fullBoundsCache;
	}

	/**
	 * Compute and return the full bounds of this node. If the dstBounds
	 * parameter is not null then it will be used to return the results instead
	 * of creating a new PBounds.
	 * 
	 * @param dstBounds if not null the new bounds will be stored here
	 * @return the full bounds in the parent coordinate system of this node
	 */
	public PBounds computeFullBounds(PBounds dstBounds) {
		PBounds result = getUnionOfChildrenBounds(dstBounds);
		result.add(getBoundsReference());
		localToParent(result);
		return result;
	}
	
	/**
	 * Compute and return the union of the full bounds of all the 
	 * children of this node. If the dstBounds parameter is not null 
	 * then it will be used to return the results instead of creating 
	 * a new PBounds.
	 * 
	 * @param dstBounds if not null the new bounds will be stored here
	 */
	public PBounds getUnionOfChildrenBounds(PBounds dstBounds) {
		if (dstBounds == null) {
			dstBounds = new PBounds();
		} else {
			dstBounds.resetToZero();
		}
		
		int count = getChildrenCount();
		for (int i = 0; i < count; i++) {
			PNode each = (PNode) children.get(i);
			dstBounds.add(each.getFullBoundsReference());
		}
		
		return dstBounds;
	}	
	
	/**
	 * Return a copy of the full bounds of this node in the global 
	 * coordinate system.
	 * 
	 * @return the full bounds in global coordinate system.
	 */
	public PBounds getGlobalFullBounds() {
		PBounds b = getFullBounds();
		if (parent != null) {
			parent.localToGlobal(b);
		}
		return b;
	}	

	/**
	 * Return true if the full bounds of this node intersects with the
	 * specified bounds.
	 * 
	 * @param parentBounds the bounds to test for intersection against (specified in parent's coordinate system)
	 * @return true if this nodes full bounds intersect the given bounds.
	 */
	public boolean fullIntersects(Rectangle2D parentBounds) {
		if (parentBounds == null) return true;
		return getFullBoundsReference().intersects(parentBounds);
	}
	
	//****************************************************************
	// Bounds Damage Management - Methods used to invalidate and validate
	// the bounds of nodes.
	//****************************************************************

	/**
	 * Return true if this nodes bounds may change at any time. The default
	 * behavior is to return false, subclasses that override this method to
	 * return true should also override getBoundsReference() and compute their
	 * volatile bounds there before returning the reference.
	 * 
	 * @return true if this node has volatile bounds
	 */
	protected boolean getBoundsVolatile() {
		return false;
	}
		
	/**
	 * Return true if this node has a child with volatile bounds.
	 * 
	 * @return true if this node has a child with volatile bounds
	 */
	protected boolean getChildBoundsVolatile() {
		return childBoundsVolatile;
	}

	/**
	 * Set if this node has a child with volatile bounds. This should normally
	 * be managed automatically by the bounds validation process.
	 * 
	 * @param childBoundsVolatile true if this node has a descendent with volatile bounds
	 */
	protected void setChildBoundsVolatile(boolean childBoundsVolatile) {
		this.childBoundsVolatile = childBoundsVolatile;
	}

	/**
	 * Return true if this node's bounds have recently changed. This flag
	 * will be reset on the next call of validateFullBounds.
	 * 
	 * @return true if this node's bounds have changed.
	 */ 	
	protected boolean getBoundsChanged() {
		return boundsChanged;
	}

	/**
	 * Set the bounds chnaged flag.  This flag
	 * will be reset on the next call of validateFullBounds.
	 * 
	 * @param boundsChanged true if this nodes bounds have changed.
	 */ 	
	protected void setBoundsChanged(boolean boundsChanged) {
		this.boundsChanged = boundsChanged;
	}
	
	/**
	 * Return true if the full bounds of this node are invalid. This means that
	 * the full bounds of this node have changed and need to be recomputed.
	 * 
	 * @return true if the full bounds of this node are invalid
	 */
	protected boolean getFullBoundsInvalid() {
		return fullBoundsInvalid;
	}

	/**
	 * Set the full bounds invalid flag. This flag is set when the full bounds of
	 * this node need to be recomputed as is the case when this node is transformed
	 * or when one of this node's children changes geometry.
	 */
	protected void setFullBoundsInvalid(boolean fullBoundsInvalid) {
		this.fullBoundsInvalid = fullBoundsInvalid;
	}
	
	/**
	 * Return true if one of this node's descendents has invalid bounds.
	 */
	protected boolean getChildBoundsInvalid() {
		return childBoundsInvalid;
	}

	/**
	 * Set the flag indicating that one of this node's descendents has
	 * invalid bounds.
	 */
	protected void setChildBoundsInvalid(boolean childBoundsInvalid) {		
		this.childBoundsInvalid = childBoundsInvalid;
	}
	
	/**
	 * This method should be called when the bounds of this node are changed.
	 * It invalidates the full bounds of this node, and also notifies each of 
	 * this nodes children that their parent's bounds have changed. As a result 
	 * of this method getting called this nodes layoutChildren will be called.
	 */ 
	public void signalBoundsChanged() {
		invalidateFullBounds();
		setBoundsChanged(true);
		firePropertyChange(PROPERTY_CODE_BOUNDS, PROPERTY_BOUNDS, null, bounds); 	

		int count = getChildrenCount();
		for (int i = 0; i < count; i++) {
			PNode each = (PNode) children.get(i);
			each.parentBoundsChanged();
		}
	}

	/**
	 * Invalidate this node's layout, so that later 
	 * layoutChildren will get called.
	 */
	public void invalidateLayout() {
		invalidateFullBounds();
	}

	/**
	 * A notification that the bounds of this node's parent have changed.
	 */
	protected void parentBoundsChanged() {
	}

	/**
	 * Invalidates the full bounds of this node, and sets the child bounds invalid flag
	 * on each of this node's ancestors.
	 */ 
	public void invalidateFullBounds() {
		setFullBoundsInvalid(true);
		
		PNode n = parent;
		while (n != null && !n.getChildBoundsInvalid()) {
			n.setChildBoundsInvalid(true);
			n = n.parent;
		}
		
		if (SCENE_GRAPH_DELEGATE != null) SCENE_GRAPH_DELEGATE.nodeFullBoundsInvalidated(this);
	}
	
	/**
	 * This method is called to validate the bounds of this node and all of its 
	 * descendents. It returns true if this nodes bounds or the bounds of any of its
	 * descendents are marked as volatile.
	 * 
	 * @return true if this node or any of its descendents have volatile bounds
	 */
	protected boolean validateFullBounds() {
		boolean boundsVolatile = getBoundsVolatile();
		
		// 1. Only compute new bounds if invalid flags are set.
		if (fullBoundsInvalid || childBoundsInvalid || boundsVolatile || childBoundsVolatile) {
			
			// 2. If my bounds are volatile and they have not been changed then signal a change. 
			// For most cases this will
			// do nothing, but if a nodes bounds depend on its model, then validate bounds has the
			// responsibility of making the bounds match the models value. For example PPaths
			// validateBounds method makes sure that the bounds are equal to the bounds of the GeneralPath
			// model.
			if (boundsVolatile && !boundsChanged) {
				signalBoundsChanged();
			}
			
			// 3. If the bounds of on of my decendents are invalidate then validate the bounds of all
			// of my children.
			if (childBoundsInvalid || childBoundsVolatile) {
				childBoundsVolatile = false;
				int count = getChildrenCount();
				for (int i = 0; i < count; i++) {
					PNode each = (PNode) children.get(i);
					childBoundsVolatile |= each.validateFullBounds();
				}
			}

			// 4. Now that my children's bounds are valid and my own bounds are valid run any
			// layout algorithm here. Note that if you try to layout volatile children piccolo
			// will most likely start a "soft" infinite loop. It won't freeze your program, but
			// it will make an infinite number of calls to SwingUtilities invoke later. You don't
			// want to do that.
			layoutChildren();
			
			// 5. If the full bounds cache is invalid then recompute the full bounds cache
			// here after our own bounds and the children's bounds have been computed above.
			if (fullBoundsInvalid) {
				double oldX = fullBoundsCache.x;
				double oldY = fullBoundsCache.y;
				double oldWidth = fullBoundsCache.width;
				double oldHeight = fullBoundsCache.height;
				boolean oldEmpty = fullBoundsCache.isEmpty();
				
				// 6. This will call getFullBoundsReference on all of the children. So if the above
				// layoutChildren method changed the bounds of any of the children they will be
				// validated again here.
				fullBoundsCache = computeFullBounds(fullBoundsCache);
				
				boolean fullBoundsChanged = fullBoundsCache.x != oldX ||
									fullBoundsCache.y != oldY ||
									fullBoundsCache.width != oldWidth ||
									fullBoundsCache.height != oldHeight ||
									fullBoundsCache.isEmpty() != oldEmpty;	 
									
				// 7. If the new full bounds cache differs from the previous cache then
				// tell our parent to invalidate their full bounds. This is how bounds changes
				// deep in the tree percolate up.
				if (fullBoundsChanged) {
					if (parent != null) parent.invalidateFullBounds();
					firePropertyChange(PROPERTY_CODE_FULL_BOUNDS, PROPERTY_FULL_BOUNDS, null, fullBoundsCache);
	
					// 8. If our paint was invalid make sure to repaint our old full bounds. The
					// new bounds will be computed later in the validatePaint pass.
					if (paintInvalid && !oldEmpty) {
						TEMP_REPAINT_BOUNDS.setRect(oldX, oldY, oldWidth, oldHeight);
						repaintFrom(TEMP_REPAINT_BOUNDS, this);
					}
				}
			}

			// 9. Clear the invalid bounds flags.			
			boundsChanged = false;
			fullBoundsInvalid = false;
			childBoundsInvalid = false;
		}
		
		return boundsVolatile || childBoundsVolatile;
	}

	/**
	 * Nodes that apply layout constraints to their children should override
	 * this method and do the layout there. 
	 */
	protected void layoutChildren() {
	}
			
	//****************************************************************
	// Node Transform - Methods to manipulate the node's transform.
	// 
	// Each node has a transform that is used to define the nodes
	// local coordinate system. IE it is applied before picking and 
	// rendering the node.
	// 
	// The usual way to move nodes about on the canvas is to manipulate
	// this transform, as opposed to changing the bounds of the 
	// node.
	// 
	// Since this transform defines the local coordinate system of this
	// node the following methods with affect the global position both 
	// this node and all of its descendents.
	//****************************************************************
		
	/**
	 * Returns the rotation applied by this node's transform in radians. 
	 * This rotation affects this node and all its descendents. The value
	 * returned will be between 0 and 2pi radians.
	 * 
	 * @return rotation in radians.
	 */
	public double getRotation() {
		if (transform == null) return 0;
		return transform.getRotation();
	}

	/**
	 * Sets the rotation of this nodes transform in radians. This will
	 * affect this node and all its descendents.
	 * 
	 * @param theta rotation in radians
	 */
	public void setRotation(double theta) {
		rotate(theta - getRotation());
	}

	/**
	 * Rotates this node by theta (in radians) about the 0,0 point. 
	 * This will affect this node and all its descendents.
	 * 
	 * @param theta the amount to rotate by in radians
	 */
	public void rotate(double theta) {
		rotateAboutPoint(theta, 0, 0);
	}

	/**
	 * Rotates this node by theta (in radians), and then translates the node so
	 * that the x, y position of its fullBounds stays constant.
	 * 
	 * @param theta the amount to rotate by in radians
	 */
	public void rotateInPlace(double theta) {
		PBounds b = getFullBoundsReference();
		double px = b.x;
		double py = b.y;
		rotateAboutPoint(theta, 0, 0);
		b = getFullBoundsReference();
		offset(px - b.x, py - b.y);
	}

	/**
	 * Rotates this node by theta (in radians) about the given 
	 * point. This will affect this node and all its descendents.
	 * 
	 * @param theta the amount to rotate by in radians
	 */
	public void rotateAboutPoint(double theta, Point2D point) {
		rotateAboutPoint(theta, point.getX(), point.getY());
	}

	/**
	 * Rotates this node by theta (in radians) about the given 
	 * point. This will affect this node and all its descendents.
	 * 
	 * @param theta the amount to rotate by in radians
	 */
	public void rotateAboutPoint(double theta, double x, double y) {
		getTransformReference(true).rotate(theta, x, y);
		invalidatePaint();
		invalidateFullBounds();
		firePropertyChange(PROPERTY_CODE_TRANSFORM, PROPERTY_TRANSFORM, null, transform);
	}

	/**
	 * Return the total amount of rotation applied to this node by its own
	 * transform together with the transforms of all its ancestors. The value
	 * returned will be between 0 and 2pi radians.
	 * 
	 * @return the total amount of rotation applied to this node in radians
	 */
	public double getGlobalRotation() {
		return getLocalToGlobalTransform(null).getRotation();
	}

	/**
	 * Set the global rotation (in radians) of this node. This is implemented by
	 * rotating this nodes transform the required amount so that the nodes
	 * global rotation is as requested.
	 * 
	 * @param theta the amount to rotate by in radians relative to the global coord system.
	 */
	public void setGlobalRotation(double theta) {
		if (parent != null) {
			setRotation(theta - parent.getGlobalRotation());
		} else {
			setRotation(theta);
		}
	}

	/**
	 * Return the scale applied by this node's transform. The scale is 
	 * effecting this node and all its descendents.
	 * 
	 * @return scale applied by this nodes transform.
	 */
	public double getScale() {
		if (transform == null) return 1;
		return transform.getScale();
	}

	/**
	 * Set the scale of this node's transform. The scale will 
	 * affect this node and all its descendents.
	 * 
	 * @param scale the scale to set the transform to
	 */
	public void setScale(double scale) {
		if (scale == 0) throw new RuntimeException("Can't set scale to 0");
		scale(scale / getScale());
	}

	/**
	 * Scale this nodes transform by the given amount. This will affect this
	 * node and all of its descendents.
	 * 
	 * @param scale the amount to scale by
	 */
	public void scale(double scale) {
		scaleAboutPoint(scale, 0, 0);
	}

	/**
	 * Scale this nodes transform by the given amount about the specified
	 * point. This will affect this node and all of its descendents.
	 * 
	 * @param scale the amount to scale by
	 * @param point the point to scale about
	 */
	public void scaleAboutPoint(double scale, Point2D point) {
		scaleAboutPoint(scale, point.getX(), point.getY());
	}

	/**
	 * Scale this nodes transform by the given amount about the specified
	 * point. This will affect this node and all of its descendents.
	 * 
	 * @param scale the amount to scale by
	 */
	public void scaleAboutPoint(double scale, double x, double y) {
		getTransformReference(true).scaleAboutPoint(scale, x, y);
		invalidatePaint();
		invalidateFullBounds();
		firePropertyChange(PROPERTY_CODE_TRANSFORM, PROPERTY_TRANSFORM, null, transform);
	}

	/**
	 * Return the global scale that is being applied to this node by its transform
	 * together with the transforms of all its ancestors.
	 */ 
	public double getGlobalScale() {
		return getLocalToGlobalTransform(null).getScale();
	}

	/**
	 * Set the global scale of this node. This is implemented by scaling
	 * this nodes transform the required amount so that the nodes global scale
	 * is as requested.
	 * 
	 * @param scale the desired global scale
	 */
	public void setGlobalScale(double scale) {
		if (parent != null) {
			setScale(scale / parent.getGlobalScale());
		} else {
			setScale(scale);
		}
	}
	
	public double getXOffset() {
		if (transform == null) return 0;
		return transform.getTranslateX();		
	}

	public double getYOffset() {
		if (transform == null) return 0;
		return transform.getTranslateY();
	}
	
	/**
	 * Return the offset that is being applied to this node by its
	 * transform. This offset effects this node and all of its descendents
	 * and is specified in the parent coordinate system. This returns the
	 * values that are in the m02 and m12 positions in the affine transform.
	 * 
	 * @return a point representing the x and y offset
	 */
	public Point2D getOffset() {
		if (transform == null) return new Point2D.Double();
		return new Point2D.Double(transform.getTranslateX(), transform.getTranslateY());
	}

	/**
	 * Set the offset that is being applied to this node by its
	 * transform. This offset effects this node and all of its descendents and
	 * is specified in the nodes parent coordinate system. This directly sets the values
	 * of the m02 and m12 positions in the affine transform. Unlike "PNode.translate()" it
	 * is not effected by the transforms scale.
	 * 
	 * @param point a point representing the x and y offset
	 */
	public void setOffset(Point2D point) {
		setOffset(point.getX(), point.getY());
	}

	/**
	 * Set the offset that is being applied to this node by its
	 * transform. This offset effects this node and all of its descendents and
	 * is specified in the nodes parent coordinate system. This directly sets the values
	 * of the m02 and m12 positions in the affine transform. Unlike "PNode.translate()" it
	 * is not effected by the transforms scale.
	 * 
	 * @param x amount of x offset
	 * @param y amount of y offset
	 */
	public void setOffset(double x, double y) {
		getTransformReference(true).setOffset(x, y);
		invalidatePaint();
		invalidateFullBounds();
		firePropertyChange(PROPERTY_CODE_TRANSFORM, PROPERTY_TRANSFORM, null, transform);
	}

	/**
	 * Offset this node relative to the parents coordinate system, and is NOT
	 * effected by this nodes current scale or rotation. This is implemented
	 * by directly adding dx to the m02 position and dy to the m12 position in the
	 * affine transform.
	 */
	public void offset(double dx, double dy) {
		getTransformReference(true);
		setOffset(transform.getTranslateX() + dx,
				  transform.getTranslateY() + dy);		
	}

	/**
	 * Translate this node's transform by the given amount, using the standard affine
	 * transform translate method. This translation effects this node and all of its 
	 * descendents.
	 */
	public void translate(double dx, double dy) {
		getTransformReference(true).translate(dx, dy);
		invalidatePaint();
		invalidateFullBounds();
		firePropertyChange(PROPERTY_CODE_TRANSFORM, PROPERTY_TRANSFORM, null, transform);
	}

	/**
	 * Return the global translation that is being applied to this node by its transform
	 * together with the transforms of all its ancestors.
	 */ 
	public Point2D getGlobalTranslation() {
		Point2D p = getOffset();
		if (parent != null) {
			parent.localToGlobal(p);
		}
		return p;
	}

	/**
	 * Set the global translation of this node. This is implemented by translating
	 * this nodes transform the required amount so that the nodes global scale
	 * is as requested.
	 * 
	 * @param globalPoint the desired global translation
	 */
	public void setGlobalTranslation(Point2D globalPoint) {
		if (parent != null) {
			parent.getGlobalToLocalTransform(null).transform(globalPoint, globalPoint);
		}
		setOffset(globalPoint);
	}
	
	/**
	 * Transform this nodes transform by the given transform.
	 * 
	 * @param aTransform the transform to apply.
	 */
	public void transformBy(AffineTransform aTransform) {
		getTransformReference(true).concatenate(aTransform);
		invalidatePaint();
		invalidateFullBounds();
		firePropertyChange(PROPERTY_CODE_TRANSFORM, PROPERTY_TRANSFORM, null, transform);
	}

	/**
	 * Linearly interpolates between a and b, based on t.
	 * Specifically, it computes lerp(a, b, t) = a + t*(b - a).
	 * This produces a result that changes from a (when t = 0) to b (when t = 1).
	 *
	 * @param a from point
	 * @param b to Point
	 * @param t variable 'time' parameter
	 */
	static public double lerp(double t, double a, double b) {
		return (a + t * (b - a));
	}
	
	/**
	 * This will calculate the necessary transform in order to make this
	 * node appear at a particular position relative to the
	 * specified bounding box.  The source point specifies a point in the
	 * unit square (0, 0) - (1, 1) that represents an anchor point on the
	 * corresponding node to this transform.  The destination point specifies
	 * an anchor point on the reference node.  The position method then
	 * computes the transform that results in transforming this node so that
	 * the source anchor point coincides with the reference anchor
	 * point. This can be useful for layout algorithms as it is
	 * straightforward to position one object relative to another.
	 * <p>
	 * For example, If you have two nodes, A and B, and you call
	 * <PRE>
	 *     Point2D srcPt = new Point2D.Double(1.0, 0.0);
	 *     Point2D destPt = new Point2D.Double(0.0, 0.0);
	 *     A.position(srcPt, destPt, B.getGlobalBounds(), 750, null);
	 * </PRE>
	 * The result is that A will move so that its upper-right corner is at
	 * the same place as the upper-left corner of B, and the transition will
	 * be smoothly animated over a period of 750 milliseconds.
	 * @param srcPt The anchor point on this transform's node (normalized to a unit square)
	 * @param destPt The anchor point on destination bounds (normalized to a unit square)
	 * @param destBounds The bounds (in global coordinates) used to calculate this transform's node
	 * @param millis Number of milliseconds over which to perform the animation
	 */
	public void position(Point2D srcPt, Point2D destPt, Rectangle2D destBounds, int millis) {
		double srcx, srcy;
		double destx, desty;
		double dx, dy;
		Point2D pt1, pt2;

		if (parent != null) {
								// First compute translation amount in global coordinates
			Rectangle2D srcBounds = getGlobalFullBounds();
			srcx  = lerp(srcPt.getX(),  srcBounds.getX(),  srcBounds.getX() +  srcBounds.getWidth());
			srcy  = lerp(srcPt.getY(),  srcBounds.getY(),  srcBounds.getY() +  srcBounds.getHeight());
			destx = lerp(destPt.getX(), destBounds.getX(), destBounds.getX() + destBounds.getWidth());
			desty = lerp(destPt.getY(), destBounds.getY(), destBounds.getY() + destBounds.getHeight());

								// Convert vector to local coordinates
			pt1 = new Point2D.Double(srcx, srcy);
			globalToLocal(pt1);
			pt2 = new Point2D.Double(destx, desty);
			globalToLocal(pt2);
			dx = (pt2.getX() - pt1.getX());
			dy = (pt2.getY() - pt1.getY());

								// Finally, animate change
			PAffineTransform at = new PAffineTransform(getTransformReference(true));
			at.translate(dx, dy);
			animateToTransform(at, millis);
		}
	}


	/**
	 * Return a copy of the transform associated with this node.
	 * 
	 * @return copy of this node's transform
	 */ 	
	public PAffineTransform getTransform() {
		if (transform == null) {
			return new PAffineTransform();
		} else {
			return (PAffineTransform) transform.clone();
		}
	}

	/**
	 * Return a reference to the transform associated with this node.
	 * This returned transform should not be modified. PNode transforms are
	 * created lazily when needed. If you access the transform reference
	 * before the transform has been created it may return null. The 
	 * createNewTransformIfNull parameter is used to specify that the PNode
	 * should create a new transform (and assign that transform to the nodes
	 * local transform variable) instead of returning null.
	 * 
	 * @return reference to this node's transform
	 */ 	
	public PAffineTransform getTransformReference(boolean createNewTransformIfNull) {
		if (transform == null && createNewTransformIfNull) {
			transform = new PAffineTransform();
		}
		return transform;
	}

	/**
	 * Return an inverted copy of the transform associated with this node.
	 * 
	 * @return inverted copy of this node's transform
	 */ 	
	public PAffineTransform getInverseTransform() {
		if (transform == null) {
			return new PAffineTransform();
		} else {
			try {
				return new PAffineTransform(transform.createInverse());
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	/**
	 * Set the transform applied to this node.
	 * 
	 * @param newTransform the new transform value
	 */
	public void setTransform(AffineTransform newTransform) {
		if (newTransform == null) {
			transform = null;
		} else {
			getTransformReference(true).setTransform(newTransform);
		}
		
		invalidatePaint();
		invalidateFullBounds();
		firePropertyChange(PROPERTY_CODE_TRANSFORM, PROPERTY_TRANSFORM, null, transform);
	}
	
	//****************************************************************
	// Paint Damage Management - Methods used to invalidate the areas of 
	// the screen that this node appears in so that they will later get 
	// painted.
	// 
	// Generally you will not need to call these invalidate methods 
	// when starting out with Piccolo because methods such as setPaint
	// already automatically call them for you. You will need to call
	// them when you start creating your own nodes.
	// 
	// When you do create you own nodes the only method that you will
	// normally need to call is invalidatePaint. This method marks the 
	// nodes as having invalid paint, the root node's UI cycle will then 
	// later discover this damage and report it to the Java repaint manager.
	// 
	// Repainting is normally done with PNode.invalidatePaint() instead of 
	// directly calling PNode.repaint() because PNode.repaint() requires 
	// the nodes bounds to be computed right away. But with invalidatePaint 
	// the bounds computation can be delayed until the end of the root's UI
	// cycle, and this can add up to a bit savings when modifying a
	// large number of nodes all at once.
	// 
	// The other methods here will rarely be called except internally
	// from the framework.
	//****************************************************************
	
	/**
	 * Return true if this nodes paint is invalid, in which case the node 
	 * needs to be repainted.
	 * 
	 * @return true if this node needs to be repainted
	 */
	public boolean getPaintInvalid() {
		return paintInvalid;
	}

	/**
	 * Mark this node as having invalid paint. If this is set the node 
	 * will later be repainted. Node this method is most often 
	 * used internally.
	 * 
	 * @param paintInvalid true if this node should be repainted
	 */
	public void setPaintInvalid(boolean paintInvalid) {
		this.paintInvalid = paintInvalid;
	}
	
	/**
	 * Return true if this node has a child with invalid paint.
	 * 
	 * @return true if this node has a child with invalid paint
	 */
	public boolean getChildPaintInvalid() {
		return childPaintInvalid;
	}

	/**
	 * Mark this node as having a child with invalid paint. 
	 * 
	 * @param childPaintInvalid true if this node has a child with invalid paint
	 */
	public void setChildPaintInvalid(boolean childPaintInvalid) {
		this.childPaintInvalid = childPaintInvalid;
	}
	
	/**
	 * Invalidate this node's paint, and mark all of its ancestors as having a node
	 * with invalid paint.
	 */
	public void invalidatePaint() {
		setPaintInvalid(true);

		PNode n = parent;
		while (n != null && !n.getChildPaintInvalid()) {
			n.setChildPaintInvalid(true);
			n = n.parent;
		}
		
		if (SCENE_GRAPH_DELEGATE != null) SCENE_GRAPH_DELEGATE.nodePaintInvalidated(this);
	}
	
	/**
	 * Repaint this node and any of its descendents if they have invalid paint.
	 */
	public void validateFullPaint() {		
		if (getPaintInvalid()) {
			repaint();
			setPaintInvalid(false);
		}
		
		if (getChildPaintInvalid()) {
			int count = getChildrenCount();
			for (int i = 0; i < count; i++) {
				PNode each = (PNode) children.get(i);
				each.validateFullPaint();
			}			
			setChildPaintInvalid(false);
		}	
	}

	/**
	 * Mark the area on the screen represented by this nodes full bounds 
	 * as needing a repaint.
	 */
	public void repaint() {
		TEMP_REPAINT_BOUNDS.setRect(getFullBoundsReference());
		repaintFrom(TEMP_REPAINT_BOUNDS, this);
	}

	/**
	 * Pass the given repaint request up the tree, so that any cameras
	 * can invalidate that region on their associated canvas.
	 * 
	 * @param localBounds the bounds to repaint
	 * @param childOrThis if childOrThis does not equal this then this nodes transform will be applied to the localBounds param 
	 */
	public void repaintFrom(PBounds localBounds, PNode childOrThis) {
		if (parent != null) {
			if (childOrThis != this) {
				localToParent(localBounds);
			} else if (!getVisible()) {
				return;
			}
			parent.repaintFrom(localBounds, this);
		}
	}

	//****************************************************************
	// Occluding - Methods to suppor occluding optimization. Not yet
	// complete.
	//****************************************************************

	public boolean isOpaque(Rectangle2D boundary) {
		return false;
	}
	
	public boolean getOccluded() {
		return occluded;
	}

	public void setOccluded(boolean isOccluded) {
		occluded = isOccluded;
	}

	//****************************************************************
	// Painting - Methods for painting this node and its children
	// 
	// Painting is how a node defines its visual representation on the
	// screen, and is done in the local coordinate system of the node.
	// 
	// The default painting behavior is to first paint the node, and 
	// then paint the node's children on top of the node. If a node
	// needs wants specialized painting behavior it can override:
	// 
	// paint() - Painting here will happen before the children
	// are painted, so the children will be painted on top of painting done
	// here.
	// paintAfterChildren() - Painting here will happen after the children
	// are painted, so it will paint on top of them.
	// 
	// Note that you should not normally need to override fullPaint().
	// 
	// The visible flag can be used to make a node invisible so that 
	// it will never get painted.
	//****************************************************************

	/**
	 * Return true if this node is visible, that is if it will paint itself
	 * and descendents.
	 * 
	 * @return true if this node and its descendents are visible.
	 */
	public boolean getVisible() {
		return visible;
	}

	/**
	 * Set the visibility of this node and its descendents.
	 * 
	 * @param isVisible true if this node and its descendents are visible
	 */
	public void setVisible(boolean isVisible) {
		if (getVisible() != isVisible) {
			if (!isVisible) repaint();
			visible = isVisible;
			firePropertyChange(PROPERTY_CODE_VISIBLE ,PROPERTY_VISIBLE, null, null);
			invalidatePaint();
		}	
	}

	/**
	 * Return the paint used to paint this node. This value may be null.
	 */
	public Paint getPaint() {
		return paint;
	}

	/**
	 * Set the paint used to paint this node. This value may be set to null.
	 */
	public void setPaint(Paint newPaint) {
		if (paint == newPaint) return;
		
		Paint old = paint;
		paint = newPaint;
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_PAINT ,PROPERTY_PAINT, old, paint);
	}

	/**
	 * Return the transparency used when painting this node. Note that this
	 * transparency is also applied to all of the node's descendents.
	 */
	public float getTransparency() {
		return transparency;
	}

	/**
	 * Set the transparency used to paint this node. Note that this transparency
	 * applies to this node and all of its descendents.
	 */
	public void setTransparency(float zeroToOne) {
		if (transparency == zeroToOne) return;
		
		transparency = zeroToOne;
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_TRANSPARENCY, PROPERTY_TRANSPARENCY, null, null);
	}

	/**
	 * Paint this node behind any of its children nodes. Subclasses that define 
	 * a different appearance should override this method and paint themselves 
	 * there.
	 * 
	 * @param paintContext the paint context to use for painting the node
	 */
	protected void paint(PPaintContext paintContext) {
		if (paint != null) {
			Graphics2D g2 = paintContext.getGraphics();
			g2.setPaint(paint);
			g2.fill(getBoundsReference());
		}
	}

	/**
	 * Paint this node and all of its descendents. Most subclasses do not need to
	 * override this method, they should override <code>paint</code> or
	 * <code>paintAfterChildren</code> instead.
	 * 
	 * @param paintContext the paint context to use for painting this node and its children
	 */
	public void fullPaint(PPaintContext paintContext) {
		if (getVisible() && fullIntersects(paintContext.getLocalClip())) {			
			paintContext.pushTransform(transform);
			paintContext.pushTransparency(transparency);

			if (!getOccluded())
				paint(paintContext);
			
			int count = getChildrenCount();
			for (int i = 0; i < count; i++) {
				PNode each = (PNode) children.get(i);
				each.fullPaint(paintContext);
			}				

			paintAfterChildren(paintContext);
			
			paintContext.popTransparency(transparency);
			paintContext.popTransform(transform);
		}
	}

	/**
	 * Subclasses that wish to do additional painting after their children
	 * are painted should override this method and do that painting here.
	 * 
	 * @param paintContext the paint context to sue for painting after the children are painted
	 */
	protected void paintAfterChildren(PPaintContext paintContext) {
	}

	/**
	 * Return a new Image representing this node and all of its children. The image size will
	 * be equal to the size of this nodes full bounds.
	 * 
	 * @return a new image representing this node and its descendents
	 */
	public Image toImage() {
		PBounds b = getFullBoundsReference();
		return toImage((int) Math.ceil(b.getWidth()), (int) Math.ceil(b.getHeight()), null);
	}

	/**
	 * Return a new Image of the requested size representing this 
	 * node and all of its children. If backGroundPaint is null the resulting
	 * image will have transparent regions, else those regions will be filled
	 * with the backgroundPaint.
	 *	
	 * @param width pixel width of the resulting image
	 * @param height pixel height of the resulting image
	 * @return a new image representing this node and its descendents
	 */
	public Image toImage(int width, int height, Paint backGroundPaint) {
		PBounds imageBounds = getFullBounds();		

		imageBounds.expandNearestIntegerDimensions();	
		
		if(width / imageBounds.width < height / imageBounds.height) {
			double scale = width / imageBounds.width;
			height = (int) (imageBounds.height * scale);
		} else {
			double scale = height / imageBounds.height;
			width = (int) (imageBounds.width * scale);
		}

		GraphicsConfiguration graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		BufferedImage result = graphicsConfiguration.createCompatibleImage(width, height, Transparency.TRANSLUCENT);

		return toImage(result, backGroundPaint);
	}

	/**
	 * Paint a representation of this node into the specified buffered image.  If background,
	 * paint is null, then the image will not be filled with a color prior to rendering  
	 *
	 * @return a rendering of this image and its descendents into the specified image
	 */
	public Image toImage(BufferedImage image, Paint backGroundPaint) {
	    int width = image.getWidth();
	    int height = image.getHeight();	    
		Graphics2D g2 = image.createGraphics();	

		if (backGroundPaint != null) {
			g2.setPaint(backGroundPaint);
			g2.fillRect(0, 0, width, height);
		}
		
		// reuse print method
		Paper paper = new Paper();
		paper.setSize(width, height);
		paper.setImageableArea(0, 0, width, height);
		PageFormat pageFormat = new PageFormat();
		pageFormat.setPaper(paper);
		print(g2, pageFormat, 0);

		return image;
	}
	
	/**
	 * Constructs a new PrinterJob, allows the user to select which printer
	 * to print to, And then prints the node.
	 */
	public void print() {
		PrinterJob printJob = PrinterJob.getPrinterJob();
		PageFormat pageFormat = printJob.defaultPage();
		Book book = new Book();
		book.append(this, pageFormat);
		printJob.setPageable(book);

		if (printJob.printDialog()) {
			try {
				printJob.print();
			} catch (Exception e) {
				System.out.println("Error Printing");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Prints the node into the given Graphics context using the specified
	 * format. The zero based index of the requested page is specified by
	 * pageIndex. If the requested page does not exist then this method returns
	 * NO_SUCH_PAGE; otherwise PAGE_EXISTS is returned. If the printable object
	 * aborts the print job then it throws a PrinterException.
	 * 
	 * @param graphics    the context into which the node is drawn
	 * @param pageFormat  the size and orientation of the page
	 * @param pageIndex   the zero based index of the page to be drawn
	 */
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
		if (pageIndex != 0) {
			return NO_SUCH_PAGE;
		}

		Graphics2D g2 = (Graphics2D)graphics;
		PBounds imageBounds = getFullBounds();

		imageBounds.expandNearestIntegerDimensions();
		
		g2.setClip(0, 0, (int)pageFormat.getWidth(), (int)pageFormat.getHeight());
		g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

		// scale the graphics so node's full bounds fit in the imageable bounds.
		double scale = pageFormat.getImageableWidth() / imageBounds.getWidth();
		if (pageFormat.getImageableHeight() / imageBounds.getHeight() < scale) {
			scale = pageFormat.getImageableHeight() / imageBounds.getHeight();
		}
		
		g2.scale(scale, scale);
		g2.translate(-imageBounds.x, -imageBounds.y);
		
		PPaintContext pc = new PPaintContext(g2);
		pc.setRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
		fullPaint(pc);

		return PAGE_EXISTS;
	}	
	
	//****************************************************************
	// Picking - Methods for picking this node and its children.
	// 
	// Picking is used to determine the node that intersects a point or 
	// rectangle on the screen. It is most frequently used by the 
	// PInputManager to determine the node that the cursor is over.
	// 
	// The intersects() method is used to determine if a node has
	// been picked or not. The default implementation just test to see
	// if the pick bounds intersects the bounds of the node. Subclasses
	// whose geometry (a circle for example) does not match up exactly with
	// the bounds should override the intersects() method.
	// 
	// The default picking behavior is to first try to pick the nodes 
	// children, and then try to pick the nodes own bounds. If a node
	// wants specialized picking behavior it can override:
	// 
	// pick() - Pick nodes here that should be picked before the nodes
	// children are picked.
	// pickAfterChildren() - Pick nodes here that should be picked after the
	// node's children are picked.
	// 
	// Note that fullPick should not normally be overridden.
	// 
	// The pickable and childrenPickable flags can be used to make a
	// node or it children not pickable even if their geometry does 
	// intersect the pick bounds.
	//****************************************************************

	/**
	 * Return true if this node is pickable. Only pickable nodes can
	 * receive input events. Nodes are pickable by default.
	 * 
	 * @return true if this node is pickable
	 */
	public boolean getPickable() {
		return pickable;
	}

	/**
	 * Set the pickable flag for this node. Only pickable nodes can
	 * receive input events.  Nodes are pickable by default.
	 * 
	 * @param isPickable true if this node is pickable
	 */
	public void setPickable(boolean isPickable) {
		if (getPickable() != isPickable) {
			pickable = isPickable;
			firePropertyChange(PROPERTY_CODE_PICKABLE, PROPERTY_PICKABLE, null, null);
		}	
	}

	/**
	 * Return true if the children of this node should be picked. If this flag
	 * is false then this node will not try to pick its children. Children
	 * are pickable by default.
	 * 
	 * @return true if this node tries to pick its children
	 */
	public boolean getChildrenPickable() {
		return childrenPickable;
	}

	/**
	 * Set the children pickable flag. If this flag is false then this 
	 * node will not try to pick its children. Children are pickable by
	 * default.
	 * 
	 * @param areChildrenPickable true if this node tries to pick its children
	 */
	public void setChildrenPickable(boolean areChildrenPickable) {
		if (getChildrenPickable() != areChildrenPickable) {
			childrenPickable = areChildrenPickable;
			firePropertyChange(PROPERTY_CODE_CHILDREN_PICKABLE, PROPERTY_CHILDREN_PICKABLE, null, null);
		}	
	}

	/**
	 * Try to pick this node before its children have had a chance to be 
	 * picked. Nodes that paint on top of their children may want to override 
	 * this method to  if the pick path intersects that paint.
	 * 
	 * @param pickPath the pick path used for the pick operation
	 * @return true if this node was picked
	 */
	protected boolean pick(PPickPath pickPath) {
		return false;
	}
	
	/**
	 * Try to pick this node and all of its descendents. Most subclasses should not
	 * need to override this method. Instead they should override <code>pick</code> or
	 * <code>pickAfterChildren</code>.
	 * 
	 * @param pickPath the pick path to add the node to if its picked
	 * @return true if this node or one of its descendents was picked.
	 */ 
	public boolean fullPick(PPickPath pickPath) {
		if ((getPickable() || getChildrenPickable()) && fullIntersects(pickPath.getPickBounds())) {
			pickPath.pushNode(this);
			pickPath.pushTransform(transform);
			
			boolean thisPickable = getPickable() && pickPath.acceptsNode(this);
				
			if (thisPickable) {
				if (pick(pickPath)) {
					return true;
				}
			}
			
			if (getChildrenPickable()) {
				int count = getChildrenCount();
				for (int i = count - 1; i >= 0; i--) {
					PNode each = (PNode) children.get(i);
					if (each.fullPick(pickPath))
						return true;
				}				
			}

			if (thisPickable) {
				if (pickAfterChildren(pickPath)) {
					return true;
				}
			}

			pickPath.popTransform(transform);
			pickPath.popNode(this);
		}

		return false;
	}

	public void findIntersectingNodes(Rectangle2D fullBounds, ArrayList results) {
		if (fullIntersects(fullBounds)) {
			Rectangle2D localBounds = parentToLocal((Rectangle2D)fullBounds.clone());

			if (intersects(localBounds)) {
				results.add(this);
			}

			int count = getChildrenCount();
			for (int i = count - 1; i >= 0; i--) {
				PNode each = (PNode) children.get(i);
				each.findIntersectingNodes(localBounds, results);
			}
		}
	}

	/**
	 * Try to pick this node after its children have had a chance to be 
	 * picked. Most subclasses the define a different geometry will need to
	 * override this method.
	 * 
	 * @param pickPath the pick path used for the pick operation
	 * @return true if this node was picked
	 */
	protected boolean pickAfterChildren(PPickPath pickPath) {
		if (intersects(pickPath.getPickBounds())) {
			return true;
		}
		return false;
	}
		
	//****************************************************************
	// Structure - Methods for manipulating and traversing the 
	// parent child relationship
	// 
	// Most of these methods won't need to be overridden by subclasses
	// but you will use them frequently to build up your node structures.
	//****************************************************************
	 
	/**
	 * Add a node to be a new child of this node. The new node
	 * is added to the end of the list of this node's children.
	 * If child was previously a child of another node, it is 
	 * removed from that first.
	 * 
	 * @param child the new child to add to this node
	 */
	public void addChild(PNode child) {
		int insertIndex = getChildrenCount();	
		if (child.parent == this)
			insertIndex--;
		addChild(insertIndex, child);
	}

	/**
	 * Add a node to be a new child of this node at the specified index.
	 * If child was previously a child of another node, it is removed 
	 * from that node first.
	 * 
	 * @param child the new child to add to this node
	 */
	public void addChild(int index, PNode child) {
		PNode oldParent = child.getParent();

		if (oldParent != null) {
			oldParent.removeChild(child);
		}
		
		child.setParent(this);
		getChildrenReference().add(index, child);
		child.invalidatePaint();
		invalidateFullBounds();
				
		firePropertyChange(PROPERTY_CODE_CHILDREN, PROPERTY_CHILDREN, null, children); 	
	}

	/**
	 * Add a collection of nodes to be children of this node. If these nodes
	 * already have parents they will first be removed from those parents.
	 * 
	 * @param nodes a collection of nodes to be added to this node
	 */
	public void addChildren(Collection nodes) {
		Iterator i = nodes.iterator();
		while (i.hasNext()) {
			PNode each = (PNode) i.next();
			addChild(each);
		}
	}

	/**
	 * Return true if this node is an ancestor of the parameter node.
	 * 
	 * @param node a possible descendent node
	 * @return true if this node is an ancestor of the given node
	 */
	public boolean isAncestorOf(PNode node) {
		PNode p = node.parent;
		while (p != null) {
			if (p == this) return true;
			p = p.parent;
		}
		return false;
	}


	/**
	 * Return true if this node is a descendent of the parameter node.
	 * 
	 * @param node a possible ancestor node
	 * @return true if this nodes descends from the given node
	 */
	public boolean isDescendentOf(PNode node) {
		PNode p = parent;
		while (p != null) {
			if (p == node) return true;
			p = p.parent;
		}
		return false;
	}
	
	/**
	 * Return true if this node descends from the root.
	 */
	public boolean isDescendentOfRoot() {
		return getRoot() != null;
	}

	/**
	 * Change the order of this node in its parent's children list so that
	 * it will draw in back of all of its other sibling nodes.
	 */
	public void moveToBack() {
		PNode p = parent;
		if (p != null) {
			p.removeChild(this);
			p.addChild(0, this);
		}
	}

	/**
	 * Change the order of this node in its parent's children list so that
	 * it will draw in front of all of its other sibling nodes.
	 */
	public void moveInBackOf(PNode sibling) {
		PNode p = parent;
		if (p != null && p == sibling.getParent()) {
			p.removeChild(this);
			int index = p.indexOfChild(sibling);
			p.addChild(index, this);
		}
	}
		
	/**
	 * Change the order of this node in its parent's children list so that
	 * it will draw after the given sibling node.
	 */
	public void moveToFront() {
		PNode p = parent;
		if (p != null) {
			p.removeChild(this);
			p.addChild(this);
		}
	}

	/**
	 * Change the order of this node in its parent's children list so that
	 * it will draw before the given sibling node.
	 */
	public void moveInFrontOf(PNode sibling) {
		PNode p = parent;
		if (p != null && p == sibling.getParent()) {
			p.removeChild(this);
			int index = p.indexOfChild(sibling);
			p.addChild(index + 1, this);
		}
	}

	/**
	 * Return the parent of this node. This will be null if this node has not been
	 * added to a parent yet.
	 * 
	 * @return this nodes parent or null
	 */
	public PNode getParent() {
		return parent;
	}

	/**
	 * Set the parent of this node. Note this is set automatically when adding and
	 * removing children.
	 */
	public void setParent(PNode newParent) {
		PNode old = parent;
		parent = newParent;
		firePropertyChange(PROPERTY_CODE_PARENT, PROPERTY_PARENT, old, parent);		
	}
	
	/**
	 * Return the index where the given child is stored.
	 */
	public int indexOfChild(PNode child) {
		if (children == null) return -1;
		return children.indexOf(child);
	}
	
	/**
	 * Remove the given child from this node's children list. Any 
	 * subsequent children are shifted to the left (one is subtracted 
	 * from their indices). The removed child's parent is set to null.
	 * 
	 * @param child the child to remove
	 * @return the removed child
	 */
	public PNode removeChild(PNode child) {
		return removeChild(indexOfChild(child));
	}

	/**
	 * Remove the child at the specified position of this group node's children.
	 * Any subsequent children are shifted to the left (one is subtracted from
	 * their indices).	The removed child's parent is set to null.
	 * 
	 * @param index the index of the child to remove
	 * @return the removed child
	 */
	public PNode removeChild(int index) {
		PNode child = (PNode) children.remove(index);
		
		if (children.size() == 0) {
			children = null;
		}
		
		child.repaint();
		child.setParent(null);
		invalidateFullBounds();

		firePropertyChange(PROPERTY_CODE_CHILDREN, PROPERTY_CHILDREN, null, children);

		return child;
	}

	/**
	 * Remove all the children in the given collection from this node's
	 * list of children. All removed nodes will have their parent set to
	 * null.
	 * 
	 * @param childrenNodes the collection of children to remove
	 */
	public void removeChildren(Collection childrenNodes) {
		Iterator i = childrenNodes.iterator();
		while (i.hasNext()) {
			PNode each = (PNode) i.next();
			removeChild(each);
		}		
	}
	
	/**
	 * Remove all the children from this node. Node this method is more efficient then
	 * removing each child individually.
	 */
	public void removeAllChildren() {
		if (children != null) {
			int count = children.size();
			for (int i = 0; i < count; i++) {
				PNode each = (PNode) children.get(i);
				each.setParent(null);
			}
			children = null;
			invalidatePaint();
			invalidateFullBounds();
			
			firePropertyChange(PROPERTY_CODE_CHILDREN, PROPERTY_CHILDREN, null, children); 				
		}
	}

	/**
	 * Delete this node by removing it from its parent's list of children.
	 */
	public void removeFromParent() {
		if (parent != null) {
			parent.removeChild(this);
		}
	}
	
	/**
	 * Set the parent of this node, and transform the node in such a way that it
	 * doesn't move in global coordinates.
	 * 
	 * @param newParent The new parent of this node.
	 */
	public void reparent(PNode newParent) {
		AffineTransform originalTransform = getLocalToGlobalTransform(null);
		AffineTransform newTransform = newParent.getGlobalToLocalTransform(null);
		newTransform.concatenate(originalTransform);
		
		removeFromParent();
		setTransform(newTransform);
		newParent.addChild(this);
		computeFullBounds(fullBoundsCache);
	}
	
	/**
	 * Swaps this node out of the scene graph tree, and replaces it with the specified
	 * replacement node.  This node is left dangling, and it is up to the caller to
	 * manage it.  The replacement node will be added to this node's parent in the same
	 * position as this was.  That is, if this was the 3rd child of its parent, then
	 * after calling replaceWith(), the replacement node will also be the 3rd child of its parent.
	 * If this node has no parent when replace is called, then nothing will be done at all.
	 *
	 * @param replacementNode the new node that replaces the current node in the scene graph tree.
	 */
	public void replaceWith(PNode replacementNode) {
		if (parent != null) {
			PNode p = this.parent;
			int index = p.getChildrenReference().indexOf(this);
			p.removeChild(this);
			p.addChild(index, replacementNode);
		}
	}

	/**
	 * Return the number of children that this node has.
	 * 
	 * @return the number of children
	 */
	public int getChildrenCount() {
		if (children == null) {
			return 0;
		}
		return children.size();
	}
	
	/**
	 * Return the child node at the specified index.
	 * 
	 * @param index a child index
	 * @return the child node at the specified index
	 */
	public PNode getChild(int index) {
		return (PNode) children.get(index);
	}

	/**
	 * Return a reference to the list used to manage this node's
	 * children. This list should not be modified.
	 * 
	 * @return reference to the children list
	 */
	public List getChildrenReference() {
		if (children == null) {
			children = new ArrayList();
		}
		return children;
	}
	
	/**
	 * Return an iterator over this node's direct descendent children.
	 *
	 * @return iterator over this nodes children
	 */
	public ListIterator getChildrenIterator() {
		if (children == null) {
			return Collections.EMPTY_LIST.listIterator();
		}
		return Collections.unmodifiableList(children).listIterator();
	}
	
	/**
	 * Return the root node (instance of PRoot). If this node does not
	 * descend from a PRoot then null will be returned.
	 */
	public PRoot getRoot() {
		if (parent != null) {
			return parent.getRoot();
		}
		return null;
	}

	/**
	 * Return a collection containing this node and all of its descendent nodes.
	 * 
	 * @return a new collection containing this node and all descendents
	 */
	public Collection getAllNodes() {
		return getAllNodes(null, null);
	}
	
	/**
	 * Return a collection containing the subset of this node and all of 
	 * its descendent nodes that are accepted by the given node filter. If the 
	 * filter is null then all nodes will be accepted. If the results parameter 
	 * is not null then it will be used to collect this subset instead of 
	 * creating a new collection.
	 * 
	 * @param filter the filter used to determine the subset
	 * @return a collection containing this node and all descendents
	 */
	public Collection getAllNodes(PNodeFilter filter, Collection results) {
		if (results == null) results = new ArrayList();
		if (filter == null || filter.accept(this)) results.add(this);
		
		if (filter == null || filter.acceptChildrenOf(this)) {
			int count = getChildrenCount();
			for (int i = 0; i < count; i++) {
				PNode each = (PNode) children.get(i);
				each.getAllNodes(filter, results);
			}							
		}

		return results;
	}

	//****************************************************************
	// Serialization - Nodes conditionally serialize their parent.
	// This means that only the parents that were unconditionally
	// (using writeObject) serialized by someone else will be restored
	// when the node is unserialized.
	//****************************************************************
	
	/**
	 * Write this node and all of its descendent nodes to the given outputsteam. 
	 * This stream must be an instance of PObjectOutputStream or serialization 
	 * will fail. This nodes parent is written out conditionally, that is it will
	 * only be written out if someone else writes it out unconditionally.
	 * 
	 * @param out the output stream to write to, must be an instance of PObjectOutputStream
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		((PObjectOutputStream)out).writeConditionalObject(parent);
	}

	/**
	 * Read this node and all of its descendents in from the given input stream.
	 * 
	 * @param in the stream to read from
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		parent = (PNode) in.readObject();
	}	
	
	//****************************************************************
	// Debugging - methods for debugging
	//****************************************************************

	/**
	 * Returns a string representation of this object for debugging purposes.
	 */
	public String toString() {
		String result = super.toString().replaceAll(".*\\.", "");
		return result + "[" + paramString() + "]";
	}	
	
	/**
	 * Returns a string representing the state of this node. This method is
	 * intended to be used only for debugging purposes, and the content and
	 * format of the returned string may vary between implementations. The
	 * returned string may be empty but may not be <code>null</code>.
	 *
	 * @return  a string representation of this node's state
	 */
	protected String paramString() {
		StringBuffer result = new StringBuffer();
		
		result.append("bounds=" + (bounds == null ? "null" : bounds.toString()));
		result.append(",fullBounds=" + (fullBoundsCache == null ? "null" : fullBoundsCache.toString()));
		result.append(",transform=" + (transform == null ? "null" : transform.toString()));
		result.append(",paint=" + (paint == null ? "null" : paint.toString()));
		result.append(",transparency=" + transparency);
		result.append(",childrenCount=" + getChildrenCount());

		if (fullBoundsInvalid) {
			result.append(",fullBoundsInvalid");
		}
		
		if (pickable) {
			result.append(",pickable");
		}
		
		if (childrenPickable) {
			result.append(",childrenPickable");
		}
		
		if (visible) {
			result.append(",visible");
		}	
		
		return result.toString();		
	}
}
