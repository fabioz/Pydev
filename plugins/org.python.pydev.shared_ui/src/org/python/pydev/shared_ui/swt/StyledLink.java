/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.swt;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.AbstractHyperlink;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;
import org.python.pydev.shared_ui.utils.ColorParse;

/**
 * A custom link which has the color bound to the HYPERLINK_COLOR in the org.eclipse.ui.workbench preferences.
 */
public class StyledLink extends Hyperlink {

    private static final RGB DEFAULT_COLOR = new RGB(0, 51, 153);
    private static Color fgColor;
    private static List<WeakReference<StyledLink>> weakLinks = new ArrayList<>();

    private static void syncLinksFgColor(StyledLink styledLink) {
        // Remove old ones before adding a new one.
        for (Iterator<WeakReference<StyledLink>> iterator = weakLinks.iterator(); iterator.hasNext();) {
            WeakReference<StyledLink> weak = iterator.next();
            StyledLink link = weak.get();
            if (link == null || link.isDisposed()) {
                iterator.remove();
            }
        }

        // Add new
        weakLinks.add(new WeakReference<>(styledLink));

        if (fgColor == null) {
            // Color hasn't been initialized (first creation) let's do it now.
            IEclipsePreferences node = InstanceScope.INSTANCE.getNode("org.eclipse.ui.workbench");
            String string = node.get("HYPERLINK_COLOR", "");
            if (string != null && string.length() > 0) {
                fgColor = new Color(Display.getCurrent(), ColorParse.parseRGB(string, DEFAULT_COLOR));
            } else {
                // Is this even possible?
                fgColor = new Color(Display.getCurrent(), DEFAULT_COLOR);

            }

            // On first initialization, start hearing changes.
            node.addPreferenceChangeListener(new IPreferenceChangeListener() {

                @Override
                public void preferenceChange(PreferenceChangeEvent event) {
                    if ("HYPERLINK_COLOR".equals(event.getKey())) {
                        Color old = fgColor;

                        final Object newValue = event.getNewValue();
                        if (newValue != null && newValue.toString().length() != 0) {
                            fgColor = new Color(Display.getCurrent(),
                                    ColorParse.parseRGB(newValue.toString(), DEFAULT_COLOR));
                        } else {
                            // Is this even possible?
                            fgColor = new Color(Display.getCurrent(), DEFAULT_COLOR);
                        }

                        // Update active links
                        for (Iterator<WeakReference<StyledLink>> iterator = weakLinks.iterator(); iterator.hasNext();) {
                            WeakReference<StyledLink> weak = iterator.next();
                            StyledLink link = weak.get();
                            if (link == null || link.isDisposed()) {
                                iterator.remove();
                            } else {
                                link.setForeground(fgColor);
                            }
                        }
                        old.dispose();
                    }
                }
            });
        }

        styledLink.setForeground(fgColor);
    }

    public StyledLink(Composite parent, int style) {
        super(parent, style);

        this.setUnderlined(true);
        syncLinksFgColor(this);

    }

    public static class MultiStyledLink extends Composite {

        LinkedList<Control> created = new LinkedListWarningOnSlowOperations<>();

        public MultiStyledLink(Composite parent, int style) {
            super(parent, style);
            final RowLayout layout = new RowLayout();
            layout.wrap = false;
            this.setLayout(layout);
        }

        public void setText(String text) {
            if (this.created.size() > 0) {
                for (Control c : this.created) {
                    c.dispose();
                }
                this.created.clear();
            }
            Composite container = this;
            int start = text.indexOf("<a>");
            int curr = 0;
            while (start != -1) {
                int end = text.indexOf("</a>", start);

                if (start > curr) {
                    Label label = new Label(container, SWT.NONE);
                    label.setText(text.substring(curr, start));
                    created.add(label);
                }

                StyledLink link = new StyledLink(container, SWT.NONE);
                link.setText(text.substring(start + 3, end));
                created.add(link);

                curr = end + 4;
                start = text.indexOf("<a>", curr);
            }

            if (curr < text.length()) {
                Label label = new Label(container, SWT.NONE);
                label.setText(text.substring(curr, text.length()));
                created.add(label);
            }
        }

        /**
         * Get the nTh link created (one is created for each <a> </a>).
         */
        public AbstractHyperlink getLink(int i) {
            int j = 0;
            for (Control c : created) {
                if (c instanceof StyledLink) {
                    if (j == i) {
                        return (AbstractHyperlink) c;
                    }
                    j++;
                }
            }
            return null;
        }
    }

}
