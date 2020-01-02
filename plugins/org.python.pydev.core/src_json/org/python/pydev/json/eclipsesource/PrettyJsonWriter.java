package org.python.pydev.json.eclipsesource;

import java.io.IOException;
import java.io.Writer;

import org.python.pydev.shared_core.string.FastStringBuffer;

public class PrettyJsonWriter extends JsonWriter {

    private FastStringBuffer indent = new FastStringBuffer();

    PrettyJsonWriter(Writer writer) {
        super(writer);
    }

    private void writeIndent() throws IOException {
        writer.write(indent.getInternalCharsArray(), 0, indent.length());
    }

    private void increaseIdent() {
        indent.appendN(' ', 4);
    }

    private void decreaseIndent() {
        indent.deleteLastChars(4);
    }

    private void writeStartScope(char c) throws IOException {
        writer.write(c);
        writer.write('\n');
        increaseIdent();
        writeIndent();
    }

    private void writeEndScope(char c) throws IOException {
        writer.write('\n');
        decreaseIndent();
        writeIndent();
        writer.write(c);
        writer.write('\n');
        writeIndent();
    }

    @Override
    protected void writeBeginObject() throws IOException {
        writeStartScope('{');
    }

    @Override
    protected void writeEndObject() throws IOException {
        writeEndScope('}');
    }

    @Override
    protected void writeNameValueSeparator() throws IOException {
        writer.write(':');
        writer.write(' ');
    }

    @Override
    protected void writeObjectValueSeparator() throws IOException {
        writer.write(',');
        writer.write(' ');
    }

    @Override
    protected void writeBeginArray() throws IOException {
        writeStartScope('[');
    }

    @Override
    protected void writeEndArray() throws IOException {
        writeEndScope(']');
    }

    @Override
    protected void writeArrayValueSeparator() throws IOException {
        writer.write(',');
        writer.write(' ');
    }

}
