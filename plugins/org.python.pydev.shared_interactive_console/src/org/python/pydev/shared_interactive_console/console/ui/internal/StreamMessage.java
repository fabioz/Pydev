package org.python.pydev.shared_interactive_console.console.ui.internal;

public class StreamMessage {
    public StreamType type;
    public String message;

    public StreamMessage(StreamType type, String message) {
        this.type = type;
        this.message = message;
    }
}
