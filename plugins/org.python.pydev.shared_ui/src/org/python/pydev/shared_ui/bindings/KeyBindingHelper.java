/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.bindings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * Helper for knowing about keybindings and related actions
 *
 * @author Fabio
 */
public class KeyBindingHelper {

    //pre-defined helpers
    /**
     * @return true if the given event matches a content assistant keystroke (and false otherwise).
     */
    public static boolean matchesContentAssistKeybinding(KeyEvent event) {
        return matchesCommandKeybinding(event, ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
    }

    /**
     * @return the key sequence that is the best match for a content assist request.
     */
    public static KeySequence getContentAssistProposalBinding() {
        return getCommandKeyBinding(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
    }

    /**
     * @return true if the given event matches a quick assistant keystroke (and false otherwise).
     */
    public static boolean matchesQuickAssistKeybinding(KeyEvent event) {
        return matchesCommandKeybinding(event, ITextEditorActionDefinitionIds.QUICK_ASSIST);
    }

    /**
     * @return the key sequence that is the best match for a quick assist request.
     */
    public static KeySequence getQuickAssistProposalBinding() {
        return getCommandKeyBinding(ITextEditorActionDefinitionIds.QUICK_ASSIST);
    }

    /**
     * @return true if the given event matches the given KeySequence.
     */
    public static boolean matchesKeysequence(Event event, KeySequence keySequence) {
        List<KeyStroke> possibleKeyStrokes = generatePossibleKeyStrokes(event);
        return matchesKeyStokesAndKeySequence(possibleKeyStrokes, keySequence);
    }

    public static boolean matchesCommandKeybinding(Event event, String commandId) {
        List<KeyStroke> possibleKeyStrokes = generatePossibleKeyStrokes(event);
        return matchesCommandKeybinding(possibleKeyStrokes, commandId);
    }

    private static boolean matchesCommandKeybinding(List<KeyStroke> possibleKeyStrokes, String commandId) {

        final IBindingService bindingSvc = PlatformUI.getWorkbench()
                .getAdapter(IBindingService.class);
        TriggerSequence[] activeBindingsFor = bindingSvc.getActiveBindingsFor(commandId);

        for (TriggerSequence seq : activeBindingsFor) {
            if (seq instanceof KeySequence) {
                KeySequence keySequence = (KeySequence) seq;
                if (matchesKeyStokesAndKeySequence(possibleKeyStrokes, keySequence)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param event the key event to be checked
     * @param commandId the command to be checked
     * @return true if the given key event can trigger the passed command (and false otherwise).
     */
    public static boolean matchesCommandKeybinding(KeyEvent keyEvent, String commandId) {
        Event event = new Event();
        event.stateMask = keyEvent.stateMask;
        event.keyCode = keyEvent.keyCode;
        event.character = keyEvent.character;
        return matchesCommandKeybinding(event, commandId);
    }

    public static KeySequence getKeySequence(String text) throws ParseException, IllegalArgumentException {
        KeySequence keySequence = KeySequence.getInstance(KeyStroke.getInstance(text));
        return keySequence;
    }

    /**
     * Gotten from: org.eclipse.e4.ui.bindings.keys.KeyBindingDispatcher
     */
    private static List<KeyStroke> generatePossibleKeyStrokes(Event event) {
        final List<KeyStroke> keyStrokes = new ArrayList<>(3);

        /*
         * If this is not a keyboard event, then there are no key strokes. This can happen if we are
         * listening to focus traversal events.
         */
        if ((event.stateMask == 0) && (event.keyCode == 0) && (event.character == 0)) {
            return keyStrokes;
        }

        // Add each unique key stroke to the list for consideration.
        final int firstAccelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(event);
        keyStrokes.add(SWTKeySupport.convertAcceleratorToKeyStroke(firstAccelerator));

        // We shouldn't allow delete to undergo shift resolution.
        if (event.character == SWT.DEL) {
            return keyStrokes;
        }

        final int secondAccelerator = SWTKeySupport
                .convertEventToUnshiftedModifiedAccelerator(event);
        if (secondAccelerator != firstAccelerator) {
            keyStrokes.add(SWTKeySupport.convertAcceleratorToKeyStroke(secondAccelerator));
        }

        final int thirdAccelerator = SWTKeySupport.convertEventToModifiedAccelerator(event);
        if ((thirdAccelerator != secondAccelerator) && (thirdAccelerator != firstAccelerator)) {
            keyStrokes.add(SWTKeySupport.convertAcceleratorToKeyStroke(thirdAccelerator));
        }

        return keyStrokes;
    }

    private static boolean matchesKeyStokesAndKeySequence(List<KeyStroke> possibleKeyStrokes, KeySequence keySequence) {
        KeyStroke[] keyStrokes = keySequence.getKeyStrokes();

        for (KeyStroke keyStroke : keyStrokes) {
            for (KeyStroke possibleKeyStroke : possibleKeyStrokes) {
                if (keyStroke.equals(possibleKeyStroke)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param commandId the command we want to know about
     * @return the 'best' key sequence that will activate the given command
     */
    public static KeySequence getCommandKeyBinding(String commandId) {
        Assert.isNotNull(commandId);
        IBindingService bindingSvc;
        try {
            bindingSvc = PlatformUI.getWorkbench()
                    .getAdapter(IBindingService.class);
        } catch (IllegalStateException e) {
            return null;
        }

        TriggerSequence keyBinding = bindingSvc.getBestActiveBindingFor(commandId);
        if (keyBinding instanceof KeySequence) {
            return (KeySequence) keyBinding;
        }

        List<Tuple<Binding, ParameterizedCommand>> matches = new ArrayList<Tuple<Binding, ParameterizedCommand>>();
        //Ok, it may be that the binding we're looking for is not active, so, let's give a spin on all
        //the bindings
        Binding[] bindings = bindingSvc.getBindings();
        for (Binding binding : bindings) {
            ParameterizedCommand command = binding.getParameterizedCommand();
            if (command != null) {
                if (commandId.equals(command.getId())) {
                    matches.add(new Tuple<Binding, ParameterizedCommand>(binding, command));
                }
            }
        }
        for (Tuple<Binding, ParameterizedCommand> tuple : matches) {
            if (tuple.o1.getTriggerSequence() instanceof KeySequence) {
                KeySequence keySequence = (KeySequence) tuple.o1.getTriggerSequence();
                return keySequence;
            }
        }

        return null;
    }

    /**
     * @param fActivateEditorBinding
     */
    public static void executeCommand(String commandId) {
        IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
        try {
            handlerService.executeCommand(commandId, null);
        } catch (Exception e) {
            Log.log(e);
        }

    }

}
