/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under1 the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.bindings;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.commands.contexts.Context;
import org.eclipse.core.commands.contexts.ContextManager;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.BindingManager;
import org.eclipse.jface.bindings.Scheme;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.keys.IBindingService;
import org.python.pydev.shared_core.log.Log;

/**
 * Eclipse has no real API to create a keybinding, so, what we're doing here is providing one
 * using the same approach that the KeysPreferencePage uses.
 */
public class BindKeysHelper {

    private final ContextManager contextManager = new ContextManager();

    /**
     * A binding manager local to this helper. When it is
     * initialized, the current bindings are read out from the binding service
     * and placed in this manager. This manager is then updated as the user
     * makes changes. When the user has finished, the contents of this manager
     * are compared with the contents of the binding service. The changes are
     * then persisted.
     */
    private final BindingManager localChangeManager = new BindingManager(
            contextManager, new CommandManager());

    private final IBindingService bindingService;

    private final String contextId;

    private final Set<Object> initialState = new HashSet<>();

    /**
     * @param contextId defines the keys context we'll work with...
     *
     * We'll only remove/add bindings to this context.
     */
    public BindKeysHelper(String contextId) {
        Assert.isNotNull(contextId);
        this.contextId = contextId;

        // Set the context we're working with.
        Set<String> activeContextIds = new HashSet<>();
        activeContextIds.add(contextId);
        contextManager.setActiveContextIds(activeContextIds);

        // Check that the context we're working with actually exists
        IWorkbench workbench = PlatformUI.getWorkbench();
        bindingService = (IBindingService) workbench.getService(IBindingService.class);
        IContextService contextService = (IContextService) workbench.getService(IContextService.class);
        Context context = contextService.getContext(contextId);
        if (context == null || context.isDefined() == false) {
            throw new RuntimeException("The context: " + contextId + " does not exist.");
        }

        Scheme activeScheme = bindingService.getActiveScheme();
        final Scheme[] definedSchemes = bindingService.getDefinedSchemes();

        // Make a copy we can work with locally (we'll apply changes later based on this copy).
        try {
            for (int i = 0; i < definedSchemes.length; i++) {
                final Scheme scheme = definedSchemes[i];
                final Scheme copy = localChangeManager.getScheme(scheme.getId());
                copy.define(scheme.getName(), scheme.getDescription(), scheme.getParentId());
            }
            localChangeManager.setActiveScheme(activeScheme);
        } catch (final NotDefinedException e) {
            throw new Error("There is a programmer error in the bind keys helper"); //$NON-NLS-1$
        }
        localChangeManager.setLocale(bindingService.getLocale());
        localChangeManager.setPlatform(bindingService.getPlatform());
        Binding[] bindings = bindingService.getBindings();
        for (Binding binding : bindings) {
            initialState.add(binding);
        }
        localChangeManager.setBindings(bindings);
    }

    /**
     * @param force if true, we'll create the user binding regardless of having some existing binding. Otherwise,
     * we'll not allow the creation if a binding already exists for it.
     *
     * Note: conflicting bindings should be removed before (through removeUserBindingsWithFilter). If they're
     * not removed, a conflict will be created in the bindings.
     */
    public void addUserBindings(KeySequence keySequence, ParameterizedCommand command) throws Exception {
        Scheme activeScheme = bindingService.getActiveScheme();
        String schemeId = activeScheme.getId();

        localChangeManager.addBinding(new KeyBinding(keySequence, command,
                schemeId, contextId, null, null, null, Binding.USER));

    }

    /**
     * Helper class to remove bindings.
     */
    public static interface IFilter {

        boolean removeBinding(Binding binding);

    }

    /**
     * Removes any bindings which match the given filter.
     */
    public void removeUserBindingsWithFilter(IFilter iFilter) {
        Binding[] bindings = localChangeManager.getBindings();
        for (int i = 0; i < bindings.length; i++) {
            Binding binding = bindings[i];
            // Note: we'll only work with the defined context!
            if (binding.getContextId().equals(this.contextId)) {
                if (iFilter.removeBinding(binding)) {
                    localChangeManager.removeBinding(binding);
                }
            }
        }
    }

    /**
     * Saves the changes (if any change was done to the bindings).
     */
    public void saveIfChanged() {
        try {
            Binding[] newBindings = localChangeManager.getBindings();
            Set<Object> newState = new HashSet<>();
            for (Binding binding : newBindings) {
                newState.add(binding);
            }
            if (newState.equals(initialState)) {
                return;
            }

            bindingService.savePreferences(localChangeManager.getActiveScheme(), newBindings);
        } catch (Exception e) {
            Log.log(e);
        }

    }
}
