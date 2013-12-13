package org.python.pydev.shared_interactive_console.console.ui.internal;


/**
 * Interface for objects which are interested in getting informed about
 * console stream changes. A listener is informed about console stream only 
 * when they are ready.
 * 
 * This does not use the same interface as iog.eclipse.debug.core as this 
 * merges multiple streams into a single callback interface. 
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see org.eclipse.jface.text.IDocument
 */
public interface IStreamListener {
    void onStream(StreamMessage message);
}
