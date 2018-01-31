package org.python.pydev.editor;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.python.pydev.shared_core.partitioner.ITokenScanner;

public class FullPartitionDamagerRepairer extends PyDefaultDamagerRepairer {

    public FullPartitionDamagerRepairer(ITokenScanner scanner) {
        super(scanner);
    }

    @Override
    public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent e, boolean documentPartitioningChanged) {
        return partition;
    }

}
