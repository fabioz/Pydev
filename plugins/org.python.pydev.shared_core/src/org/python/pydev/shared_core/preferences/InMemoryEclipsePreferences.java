package org.python.pydev.shared_core.preferences;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class InMemoryEclipsePreferences implements IEclipsePreferences {

    private static final String FALSE = "false"; //$NON-NLS-1$
    private static final String TRUE = "true"; //$NON-NLS-1$

    private ListenerList<IPreferenceChangeListener> preferenceChangeListeners;

    private Map<String, String> properties = new HashMap<>();

    public InMemoryEclipsePreferences() {
    }

    @Override
    public String get(String key, String defaultValue) {
        String value = internalGet(key);
        return value == null ? defaultValue : value;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = internalGet(key);
        return value == null ? defaultValue : TRUE.equalsIgnoreCase(value);
    }

    @Override
    public byte[] getByteArray(String key, byte[] defaultValue) {
        String value = internalGet(key);
        return value == null ? defaultValue : value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        String value = internalGet(key);
        double result = defaultValue;
        if (value != null) {
            try {
                result = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                // use default
            }
        }
        return result;
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        String value = internalGet(key);
        float result = defaultValue;
        if (value != null) {
            try {
                result = Float.parseFloat(value);
            } catch (NumberFormatException e) {
                // use default
            }
        }
        return result;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = internalGet(key);
        int result = defaultValue;
        if (value != null) {
            try {
                result = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // use default
            }
        }
        return result;
    }

    @Override
    public long getLong(String key, long defaultValue) {
        String value = internalGet(key);
        long result = defaultValue;
        if (value != null) {
            try {
                result = Long.parseLong(value);
            } catch (NumberFormatException e) {
                // use default
            }
        }
        return result;
    }

    /**
     * Returns the existing value at the given key, or null if
     * no such value exists.
     */
    protected String internalGet(String key) {
        // throw NPE if key is null
        if (key == null) {
            throw new NullPointerException();
        }
        String result = properties.get(key);
        return result;
    }

    /**
     * Stores the given (key,value) pair, performing lazy initialization of the
     * properties field if necessary. Returns the old value for the given key,
     * or null if no value existed.
     */
    protected String internalPut(String key, String newValue) {
        String oldValue = properties.get(key);
        if (oldValue != null && oldValue.equals(newValue)) {
            return oldValue;
        }
        return properties.put(key, newValue);
    }

    @Override
    public String[] keys() {
        return properties.keySet().toArray(new String[0]);
    }

    @Override
    public String name() {
        throw new RuntimeException("Not implemented");
    }

    /*
     * Convenience method for notifying preference change listeners.
     */
    protected void firePreferenceEvent(String key, Object oldValue, Object newValue) {
        if (preferenceChangeListeners == null) {
            return;
        }
        final PreferenceChangeEvent event = new PreferenceChangeEvent(this, key, oldValue, newValue);
        for (final IPreferenceChangeListener listener : preferenceChangeListeners) {
            ISafeRunnable job = new ISafeRunnable() {
                @Override
                public void handleException(Throwable exception) {
                    // already logged in Platform#run()
                }

                @Override
                public void run() throws Exception {
                    listener.preferenceChange(event);
                }
            };
            SafeRunner.run(job);
        }
    }

    @Override
    public void put(String key, String newValue) {
        if (key == null || newValue == null) {
            throw new NullPointerException();
        }
        String oldValue = internalPut(key, newValue);
        if (!newValue.equals(oldValue)) {

            firePreferenceEvent(key, oldValue, newValue);
        }
    }

    @Override
    public void putBoolean(String key, boolean value) {
        if (key == null) {
            throw new NullPointerException();
        }
        String newValue = value ? TRUE : FALSE;
        String oldValue = internalPut(key, newValue);
        if (!newValue.equals(oldValue)) {

            firePreferenceEvent(key, oldValue, newValue);
        }
    }

    @Override
    public void putByteArray(String key, byte[] value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        String newValue = new String(value, StandardCharsets.UTF_8);
        String oldValue = internalPut(key, newValue);
        if (!newValue.equals(oldValue)) {

            firePreferenceEvent(key, oldValue, newValue);
        }
    }

    @Override
    public void putDouble(String key, double value) {
        if (key == null) {
            throw new NullPointerException();
        }
        String newValue = Double.toString(value);
        String oldValue = internalPut(key, newValue);
        if (!newValue.equals(oldValue)) {

            firePreferenceEvent(key, oldValue, newValue);
        }
    }

    @Override
    public void putFloat(String key, float value) {
        if (key == null) {
            throw new NullPointerException();
        }
        String newValue = Float.toString(value);
        String oldValue = internalPut(key, newValue);
        if (!newValue.equals(oldValue)) {

            firePreferenceEvent(key, oldValue, newValue);
        }
    }

    @Override
    public void putInt(String key, int value) {
        if (key == null) {
            throw new NullPointerException();
        }
        String newValue = Integer.toString(value);
        String oldValue = internalPut(key, newValue);
        if (!newValue.equals(oldValue)) {

            firePreferenceEvent(key, oldValue, newValue);
        }
    }

    @Override
    public void putLong(String key, long value) {
        if (key == null) {
            throw new NullPointerException();
        }
        String newValue = Long.toString(value);
        String oldValue = internalPut(key, newValue);
        if (!newValue.equals(oldValue)) {
            firePreferenceEvent(key, oldValue, newValue);
        }
    }

    @Override
    public void remove(String key) {
        String oldValue;
        // illegal state if this node has been removed
        oldValue = properties.get(key);
        if (oldValue == null) {
            return;
        }
        properties.remove(key);

        firePreferenceEvent(key, oldValue, null);
    }

    @Override
    public void clear() throws BackingStoreException {
        throw new RuntimeException("not implemented");

    }

    @Override
    public String[] childrenNames() throws BackingStoreException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Preferences parent() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean nodeExists(String pathName) throws BackingStoreException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public String absolutePath() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void flush() throws BackingStoreException {
    }

    @Override
    public void sync() throws BackingStoreException {
    }

    @Override
    public void addNodeChangeListener(INodeChangeListener listener) {
        throw new RuntimeException("not implemented");

    }

    @Override
    public void removeNodeChangeListener(INodeChangeListener listener) {
        throw new RuntimeException("not implemented");

    }

    @Override
    public void addPreferenceChangeListener(IPreferenceChangeListener listener) {
        if (preferenceChangeListeners == null) {
            preferenceChangeListeners = new ListenerList<>();
        }
        preferenceChangeListeners.add(listener);

    }

    @Override
    public void removePreferenceChangeListener(IPreferenceChangeListener listener) {
        if (preferenceChangeListeners == null) {
            return;
        }
        preferenceChangeListeners.remove(listener);
        if (preferenceChangeListeners.size() == 0) {
            preferenceChangeListeners = null;
        }

    }

    @Override
    public void removeNode() throws BackingStoreException {
        throw new RuntimeException("not implemented");

    }

    @Override
    public Preferences node(String path) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void accept(IPreferenceNodeVisitor visitor) throws BackingStoreException {
        throw new RuntimeException("not implemented");

    }

}
