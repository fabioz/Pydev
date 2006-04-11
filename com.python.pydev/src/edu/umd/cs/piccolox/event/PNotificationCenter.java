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
 * This class PNotificationCenter center is derived from the class
 * NSNotificationCenter from:
 * 
 * Wotonomy: OpenStep design patterns for pure Java
 * applications. Copyright (C) 2000 Blacksmith, Inc.
 */
package edu.umd.cs.piccolox.event;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <b>PNotificationCenter</b> provides a way for objects that don't know about
 * each other to communicate. It receives PNotification objects and broadcasts
 * them to all interested listeners. Unlike standard Java events, the event
 * listeners don't need to know about the event source, and the event source
 * doesn't need to maintain the list of listeners.
 * <P>
 * Listeners of the notfications center are held by weak references. So the
 * notfication center will not create garbage collection problems as standard
 * java event listeners do.
 * <P>
 * @author Jesse Grosjean
 */
public class PNotificationCenter {

	public static final Object NULL_MARKER = new Object();

	protected static PNotificationCenter DEFAULT_CENTER;

	protected HashMap listenersMap;
	protected ReferenceQueue keyQueue;

	public static PNotificationCenter defaultCenter() {
		if (DEFAULT_CENTER == null) {
			DEFAULT_CENTER = new PNotificationCenter();
		}
		return DEFAULT_CENTER;
	}

	private PNotificationCenter() {
		listenersMap = new HashMap();
		keyQueue = new ReferenceQueue();
	}

	//****************************************************************
	// Add Listener Methods
	//****************************************************************

	/**
	 * Registers the 'listener' to receive notifications with the name
	 * 'notificationName' and/or containing 'object'. When a matching
	 * notification is posted the callBackMethodName message will be sent to the
	 * listener with a single PNotification argument. If notificationName is null
	 * then the listener will receive all notifications with an object matching
	 * 'object'. If 'object' is null the listener will receive all notifications
	 * with the name 'notificationName'. 
	 */
	public void addListener(Object listener, String callbackMethodName, String notificationName, Object object) {
		processKeyQueue();

		Object name = notificationName;
		Method method = null;

		try {
			method = listener.getClass().getMethod(callbackMethodName, new Class[] { PNotification.class });
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			return;
		}
		
		if (name == null) name = NULL_MARKER;
		if (object == null) object = NULL_MARKER;

		Object key = new CompoundKey(name, object);
		Object value = new CompoundValue(listener, method);

		List list = (List) listenersMap.get(key);
		if (list == null) {
			list = new ArrayList();
			listenersMap.put(new CompoundKey(name, object, keyQueue), list);
		}

		if (!list.contains(value)) {
			list.add(value);
		}
	}	

	//****************************************************************
	// Remove Listener Methods
	//****************************************************************

	/**
	 * Removes the listener so that it no longer recives notfications from this
	 * notfication center.
	 */
	public void removeListener(Object listener) {
		processKeyQueue();

		Iterator i = new LinkedList(listenersMap.keySet()).iterator();
		while (i.hasNext()) {
			removeListener(listener, i.next());
		}
	}

	/**
	 * Removes the listeners as the listener of notifications matching
	 * notificationName and object. If listener is null all listeners matching
	 * notificationName and object are removed. If notificationName is null the
	 * listener will be removed from all notifications containing the object. If
	 * the object is null then the listener will be removed from all
	 * notifications matching notficationName.
	 */
	public void removeListener(Object listener, String notificationName, Object object) {
		processKeyQueue();

		List keys = matchingKeys(notificationName, object);
		Iterator it = keys.iterator();
		while (it.hasNext()) {
			removeListener(listener, it.next());
		}
	}

	//****************************************************************
	// Post PNotification Methods
	//****************************************************************

	/**
	 * Post a new notfication with notificationName and object. The object is
	 * typically the object posting the notification. The object may be null.
	 */
	public void postNotification(String notificationName, Object object) {
		postNotification(notificationName, object, null);
	}

	/**
	 * Creates a notification with the name notificationName, associates it with
	 * the object, and posts it to this notification center. The object is
	 * typically the object posting the notification. It may be nil.
	 */
	public void postNotification(String notificationName, Object object, Map userInfo) {
		postNotification(new PNotification(notificationName, object, userInfo));
	}

	/**
	 * Post the notification to this notification center. Most often clients will
	 * instead use one of this classes convenience postNotifcations methods.
	 */
	public void postNotification(PNotification aNotification) {
		List mergedListeners = new LinkedList();
		List listenersList;
		
		Object name = aNotification.getName();
		Object object = aNotification.getObject();

		if (name != null) {
			if (object != null) { // both are specified
				listenersList = (List) listenersMap.get(new CompoundKey(name, object));
				if (listenersList != null) {
					mergedListeners.addAll(listenersList);
				}
				listenersList = (List) listenersMap.get(new CompoundKey(name, NULL_MARKER));
				if (listenersList != null) {
					mergedListeners.addAll(listenersList);
				}
				listenersList = (List) listenersMap.get(new CompoundKey(NULL_MARKER, object));
				if (listenersList != null) {
					mergedListeners.addAll(listenersList);
				}
			} else { // object is null
				listenersList = (List) listenersMap.get(new CompoundKey(name, NULL_MARKER));
				if (listenersList != null) {
					mergedListeners.addAll(listenersList);
				}
			}
		} else if (object != null) { // name is null
			listenersList = (List) listenersMap.get(new CompoundKey(NULL_MARKER, object));
			if (listenersList != null) {
				mergedListeners.addAll(listenersList);
			}
		}

		Object key = new CompoundKey(NULL_MARKER, NULL_MARKER);
		listenersList = (List) listenersMap.get(key);
		if (listenersList != null) {
			mergedListeners.addAll(listenersList);
		}

		CompoundValue value;
		Iterator it = mergedListeners.iterator();

		while (it.hasNext()) {
			value = (CompoundValue) it.next();
			if (value.get() == null) {
				it.remove();
			} else {
				try {
					value.getMethod().invoke(value.get(), new Object[] { aNotification });
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	//****************************************************************
	// Implementation classes and methods
	//****************************************************************

	protected List matchingKeys(String name, Object object) {
		List result = new LinkedList();

		Iterator it = listenersMap.keySet().iterator();
		while (it.hasNext()) {
			CompoundKey key = (CompoundKey) it.next();
			if ((name == null) || (name == key.name())) {
				if ((object == null) || (object == key.get())) {
					result.add(key);
				}
			}
		}

		return result;
	}
	
	protected void removeListener(Object listener, Object key) {
		if (listener == null) {
			listenersMap.remove(key);
			return;
		}

		List list = (List) listenersMap.get(key);
		if (list == null)
			return;

		Iterator it = list.iterator();
		while (it.hasNext()) {
			Object observer = ((CompoundValue) it.next()).get();
			if ((observer == null) || (listener == observer)) {
				it.remove();
			}
		}
		
		if (list.size() == 0) {
			listenersMap.remove(key);
		}
	}
	
	protected void processKeyQueue() {
		CompoundKey key;
		while ((key = (CompoundKey) keyQueue.poll()) != null) {
			listenersMap.remove(key);
		}
	}
		
	protected static class CompoundKey extends WeakReference {

		private Object name;
		private int hashCode;

		public CompoundKey(Object aName, Object anObject) {
			super(anObject);			
			name = aName;
			hashCode = aName.hashCode() + anObject.hashCode();
		}

		public CompoundKey(Object aName, Object anObject, ReferenceQueue aQueue) {
			super(anObject, aQueue);
			name = aName;
			hashCode = aName.hashCode() + anObject.hashCode();
		}

		public Object name() {
			return name;
		}

		public int hashCode() {
			return hashCode;
		}

		public boolean equals(Object anObject) {
			if (this == anObject) return true;
			CompoundKey key = (CompoundKey) anObject;
			if (name == key.name || (name != null && name.equals(key.name))) {
				Object object = get();
				if (object != null) {
					if ( object == (key.get())) {
						return true;
					}
				}
			}
			return false;
		}

		public String toString() {
			return "[CompoundKey:" + name() + ":" + get() + "]";
		}
	}

	protected static class CompoundValue extends WeakReference {

		protected int hashCode;
		protected Method method;

		public CompoundValue(Object object, Method method) {
			super(object);
			hashCode = object.hashCode();
			this.method = method;
		}

		public Method getMethod() {
			return method;
		}

		public int hashCode() {
			return hashCode;
		}

		public boolean equals(Object object) {
			if (this == object) return true;
			CompoundValue value = (CompoundValue) object;
			if (method == value.method || (method != null && method.equals(value.method))) {
				Object o = get();
				if (o != null) {
					if (o == value.get()) {
						return true;
					}
				}
			}
			return false;
		}

		public String toString() {
			return "[CompoundValue:" + get() + ":" + getMethod().getName() + "]";
		}
	}	
}
