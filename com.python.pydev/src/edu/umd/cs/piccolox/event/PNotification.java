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
 * 
 * This class PNotification center is derived from the class
 * NSNotification from:
 * 
 * Wotonomy: OpenStep design patterns for pure Java
 * applications. Copyright (C) 2000 Blacksmith, Inc.
 */
package edu.umd.cs.piccolox.event;

import java.util.Map;

/**
 * <b>PNotification</b> objects encapsulate information so that it can be
 * broadcast to other objects by a PNotificationCenter. A PNotification contains a
 * name, an object, and an optional properties map. The name is a tag
 * identifying the notification. The object is any object that the poster of the
 * notification wants to send to observers of that notification (typically, it
 * is the object that posted the notification). The properties map stores other
 * related objects, if any.
 * <P>
 * You don't usually create your own notifications directly. The
 * PNotificationCenter method postNotification() allow you to conveniently post a
 * notification without creating it first.
 * <P>
 * @author Jesse Grosjean
 */
public class PNotification {
	
	protected String name;
	protected Object source;
	protected Map properties;

	public PNotification(String name, Object source, Map properties) {
		this.name = name;
		this.source = source;
		this.properties = properties;
	}

	/**
	 * Return the name of the notification. This is the same as the name used to
	 * register with the notfication center.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the object associated with this notification. This is most often
	 * the same object that posted the notfication. It may be null.
	 */
	public Object getObject() {
		return source;
	}

	/**
	 * Return a property associated with the notfication.
	 */
	public Object getProperty(Object key) {
		if (properties != null) {
			return properties.get(key);
		}
		return null;
	}
}
