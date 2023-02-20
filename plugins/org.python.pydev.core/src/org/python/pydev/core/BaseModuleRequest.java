package org.python.pydev.core;

import org.python.pydev.core.preferences.InterpreterGeneralPreferences;

public class BaseModuleRequest implements IModuleRequestState {

    private boolean acceptTypeshed;

    public BaseModuleRequest(boolean acceptTypeshed) {
        if (!InterpreterGeneralPreferences.getUseTypeshed()) {
            // i.e.: disabled in the preferences: never use typeshed.
            acceptTypeshed = false;
        }
        this.acceptTypeshed = acceptTypeshed;
    }

    @Override
    public boolean getAcceptTypeshed() {
        return acceptTypeshed;
    }

}
