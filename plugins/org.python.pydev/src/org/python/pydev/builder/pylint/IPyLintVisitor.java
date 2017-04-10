/**
 * Copyright (c) 2017 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.builder.pylint;

import java.util.List;

import org.python.pydev.shared_ui.utils.PyMarkerUtils.MarkerInfo;

/**
 * See: org.python.pydev.builder.pylint.PyLintVisitorFactory to create PyLint visitors.
 */
public interface IPyLintVisitor {

    /**
     * Deletes any related markers.
     */
    void deleteMarkers();

    /**
     * Starts visiting with PyLint (i.e.: creates process)
     */
    void startVisit();

    /**
     * Waits until the PyLint visitor finishes its execution (note
     * that there's no API to cancel it, the canceling must be
     * done by canceling the monitor which was passed to it).
     */
    void join();

    /**
     * The list of markers that the visitor generated (must be gotten only after {@link #join()}).
     * @return the list of markers or null if no markers were generated.
     */
    List<MarkerInfo> getMarkers();

}
