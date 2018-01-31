package org.python.pydev.shared_core.partitioner;

import org.eclipse.jface.text.IDocument;

/**
 * A partition token scanner returns tokens that represent partitions. For that reason,
 * a partition token scanner is vulnerable in respect to the document offset it starts
 * scanning. In a simple case, a partition token scanner must always start at a partition
 * boundary. A partition token scanner can also start in the middle of a partition,
 * if it knows the type of the partition.
 *
 * @since 2.0
 */
public interface IPartitionTokenScanner extends ITokenScanner {

    /**
     * Configures the scanner by providing access to the document range that should be scanned. The
     * range may not only contain complete partitions but starts at the beginning of a line in the
     * middle of a partition of the given content type. This requires that a partition delimiter can
     * not contain a line delimiter.
     *
     * @param document the document to scan
     * @param offset the offset of the document range to scan
     * @param length the length of the document range to scan
     * @param contentType the content type at the given offset
     * @param partitionOffset the offset at which the partition of the given offset starts
     */
    void setPartialRange(IDocument document, int offset, int length, String contentType, int partitionOffset);
}
