/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.core.IToken;
import org.python.pydev.shared_core.string.FastStringBuffer;

import com.python.pydev.analysis.IAnalysisPreferences;

/**
 * This is a message that's composed of several other messages. It can be used to create wild imports.
 *
 * @author Fabio
 */
public class CompositeMessage extends AbstractMessage {

    public CompositeMessage(int type, IToken generator, IAnalysisPreferences prefs) {
        super(type, generator, prefs);
    }

    public CompositeMessage(int type, int startLine, int endLine, int startCol, int endCol, IAnalysisPreferences prefs) {
        super(type, startLine, endLine, startCol, endCol, prefs);
    }

    List<IMessage> msgs = new ArrayList<IMessage>();

    /**
     * This determines the maximum amount of messages that a composite message can have.
     * 
     * This is used because if messages become too big, there may be problems to save them later,
     * and when it becomes too big, it may not be worth to the user anyways.
     */
    public static int MAXIMUM_NUMBER_OF_INTERNAL_MESSAGES = 15;

    /**
     * If true, a part of the message in this message was suppressed.
     */
    private boolean addSupressMessage = false;

    /**
     * The cached inner message of this composite message
     */
    private String shortMessage;

    /**
     * Add a component message to this composite.
     */
    public void addMessage(IMessage msg) {
        if (shortMessage != null) {
            throw new RuntimeException("Cannot add more messages after it's own short message was requested.");
        }
        if (msgs.size() > MAXIMUM_NUMBER_OF_INTERNAL_MESSAGES) {
            addSupressMessage = true;
            return;
        }

        if (!msgs.contains(msg)) {
            msgs.add(msg);
        }
    }

    /**
     * @return the message considering all the internal messages available.
     */
    @Override
    public String getShortMessage() {
        if (shortMessage == null) {
            FastStringBuffer buffer = new FastStringBuffer(msgs.size() * 40);

            List<String> messages = new ArrayList<String>();
            for (Iterator<IMessage> iter = msgs.iterator(); iter.hasNext();) {
                IMessage msg = iter.next();
                messages.add(msg.getShortMessage().toString());
            }

            //sort them accordingly
            Collections.sort(messages);

            for (Iterator<String> iter = messages.iterator(); iter.hasNext();) {
                buffer.append(iter.next());
                if (iter.hasNext()) {
                    buffer.append(", ");
                }
            }

            //let the user know if we suppressed something
            if (addSupressMessage) {
                buffer.append("... others suppressed");
            }
            shortMessage = buffer.toString();
        }
        return shortMessage;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CompositeMessage)) {
            return false;
        }
        CompositeMessage m = (CompositeMessage) obj;
        return m.getMessage().equals(getMessage());
    }

    @Override
    public int hashCode() {
        return getMessage().hashCode();
    }
}
