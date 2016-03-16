/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.change;

import java.util.List;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.python.pydev.core.MisconfigurationException;

public class CompositeChangeProcessor implements IChangeProcessor {

    private String name;

    private List<IChangeProcessor> processors;

    public CompositeChangeProcessor(String name, List<IChangeProcessor> processors) {
        this.name = name;
        this.processors = processors;
    }

    @Override
    public Change createChange() throws MisconfigurationException {
        CompositeChange change = new CompositeChange(name);
        for (IChangeProcessor processor : processors) {
            change.add(processor.createChange());
        }

        return change;
    }
}
