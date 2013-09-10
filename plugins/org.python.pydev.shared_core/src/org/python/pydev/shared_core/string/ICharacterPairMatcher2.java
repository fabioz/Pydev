/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.string;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ICharacterPairMatcher;

public interface ICharacterPairMatcher2 extends ICharacterPairMatcher {

    public int searchForAnyOpeningPeer(int offset, IDocument document);

    public int searchForOpeningPeer(int offset, char openingPeer, char closingPeer, IDocument document);

    public int searchForClosingPeer(int offset, char openingPeer, char closingPeer, IDocument document);
}
