package org.python.pydev.shared_core.string;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ICharacterPairMatcher;

public interface ICharacterPairMatcher2 extends ICharacterPairMatcher {

    public int searchForAnyOpeningPeer(int offset, IDocument document);

    public int searchForOpeningPeer(int offset, char openingPeer, char closingPeer, IDocument document);

    public int searchForClosingPeer(int offset, char openingPeer, char closingPeer, IDocument document);
}
