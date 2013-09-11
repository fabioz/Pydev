/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 12/10/2005
 */
package org.python.pydev.core;

public interface IDeltaProcessor<X> {

    /**
     * Process some update that was added with the passed data.
     */
    void processUpdate(X data);

    /**
     * Process some delete that was added with the passed data.
     */
    void processDelete(X data);

    /**
     * Process some insert that was added with the passed data.
     */
    void processInsert(X data);

    /**
     * Ends the processing (so that the processor might save all the delta info in a large chunck,
     * as the deltas will be deleted from the disk after this call).
     */
    void endProcessing();
}
