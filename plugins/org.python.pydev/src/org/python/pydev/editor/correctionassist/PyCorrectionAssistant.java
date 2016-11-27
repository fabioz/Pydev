/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 23, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist;

import java.lang.reflect.Field;

import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.python.pydev.shared_core.utils.PlatformUtils;
import org.python.pydev.shared_ui.content_assist.ContentAssistHackingAroundBugs;

/**
 * 
 * The PyCorrectionAssistant was based on org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor
 * assistant. (after many hour of exploration)...
 * 
 * @author Fabio Zadrozny
 */
public class PyCorrectionAssistant extends QuickAssistAssistant {

    public PyCorrectionAssistant() {
        if (PlatformUtils.isLinuxPlatform()) {
            // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=508245 (hack can be removed when that's fixed).
            try {
                Field field = QuickAssistAssistant.class.getDeclaredField("fQuickAssistAssistantImpl");
                field.setAccessible(true);
                ContentAssistant assistant = (ContentAssistant) field.get(this);
                ContentAssistHackingAroundBugs.fixAssistBugs(assistant);
            } catch (Throwable e) {
                // Just ignore if this hack fails.
            }
        }
    }

}
