/**
 * Copyright: Fabio Zadrozny
 * License: EPL
 */
package org.python.pydev.shared_core.partitioner;


public interface IScannerWithOffPartition {

    /**
     * @return the code reader or null if something goes bad.
     */
    PartitionCodeReader getOffPartitionCodeReader(int currOffset);

}
