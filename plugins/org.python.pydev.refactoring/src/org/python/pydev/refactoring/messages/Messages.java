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

package org.python.pydev.refactoring.messages;

import java.text.MessageFormat;

import org.eclipse.osgi.util.NLS;

public final class Messages {
    private Messages() {
    }

    private static final String BUNDLE_NAME = "org.python.pydev.refactoring.messages.messages";

    /* Labels */
    public static String generatePropertiesLabel;
    public static String extractMethodLabel;
    public static String extractLocalLabel;
    public static String inlineLocalLabel;
    public static String overrideMethodsLabel;
    public static String constructorFieldLabel;
    public static String renameLabel;

    public static String errorTitle;
    public static String errorUnexpected;
    public static String generatePropertiesDelete;
    public static String generatePropertiesDocString;
    public static String generatePropertiesGetter;
    public static String generatePropertiesProperty;
    public static String generatePropertiesSelect;
    public static String generatePropertiesSetter;
    public static String generatePropertiesUnavailable;

    public static String infoEditorUnsupported;
    public static String infoFixCode;
    public static String infoTitle;
    public static String infoUnavailable;

    public static String offsetStrategyAfterInit;
    public static String offsetStrategyBegin;
    public static String offsetStrategyEnd;
    public static String offsetStrategyBeginModule;
    public static String offsetStrategyEndModule;
    public static String offsetStrategyInsertionPointMethod;
    public static String offsetStrategyInsertionPointProperty;

    public static String wizardAccessModifier;
    public static String wizardAccessPrivate;
    public static String wizardAccessPseudo;
    public static String wizardAccessPublic;
    public static String wizardDeselectAll;
    public static String wizardSelectAll;

    public static String constructorFieldConstructor;
    public static String constructorFieldSelect;
    public static String constructorFieldUnavailable;

    public static String overrideMethodsSelect;
    public static String overrideMethodsUnavailable;
    public static String overrideMethodsMethods;

    public static String extractMethodReplaceWithCall;
    public static String extractMethodArgumentName;
    public static String extractMethodArgumentsTitle;
    public static String extractMethodFunctionTitle;
    public static String extractMethodChangeName;
    public static String extractMethodIncompleteSelection;
    public static String extractMethodScopeInvalid;
    public static String extractMethodFunctionPreview;
    public static String extractMethodEditButton;
    public static String extractMethodUpButton;
    public static String extractMethodDownButton;

    public static String extractLocalVariableName;
    public static String extractLocalNoExpressionSelected;
    public static String extractLocalCreateLocalVariable;
    public static String extractLocalReplaceWithVariable;

    public static String inlineLocalRemoveAssignment;
    public static String inlineLocalReplaceWithExpression;
    public static String inlineLocalMessage;
    public static String inlineLocalMessageMany;
    public static String inlinelocalNoAssignment;
    public static String inlineLocalParameter;
    public static String inlineLocalMultipleAssignments;

    public static String renameName;
    public static String renameMultipleTypes;

    public static String imagePath;
    public static String imgClass;
    public static String imgMethod;
    public static String imgAttribute;
    public static String imgLogo;

    public static String validationContainsInvalidChars;
    public static String validationReservedKeyword;
    public static String validationNameAlreadyUsed;
    public static String validationUsedAsParameter;
    public static String validationNameIsEmpty;
    public static String validationNoNameSelected;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    public static String format(String message, Object... objects) {
        return MessageFormat.format(message, objects);
    }
}
