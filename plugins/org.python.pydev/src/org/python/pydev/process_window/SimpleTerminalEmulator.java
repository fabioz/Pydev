package org.python.pydev.process_window;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

public class SimpleTerminalEmulator {

    private Text output;
    private int cursor = 0;

    public SimpleTerminalEmulator(Composite composite, int flags) {
        output = new Text(composite, flags);
    }

    public void clearOutput() {
        output.setText("");
        cursor = 0;
    }

    public Control getControl() {
        return output;
    }

    public void processText(String contents) {
        List<String> splitInLines = StringUtils.splitInLines(contents, true);
        for (String line : splitInLines) {
            System.out.println("start line ---");
            char[] charArray = line.toCharArray();
            for (char c : charArray) {
                if (Character.isJavaIdentifierPart(c)) {
                    System.out.print(c);
                } else {
                    if (c == '\r') {
                        System.out.print("\\r");

                    } else if (c == '\n') {
                        System.out.println("\\n");
                    } else {
                        System.out.print(c);
                    }
                }
            }
            System.out.println("end line ---");
            if (line.endsWith("\r")) {
                // Work as a terminal emulator and go to the start of the line
                char[] textChars = output.getTextChars();
                FastStringBuffer buf = new FastStringBuffer(textChars);
                while (buf.length() > 0 && !buf.endsWith('\n')) {
                    buf.deleteLast();
                }
                cursor = buf.length();
                output.append(line.substring(0, line.length() - 1));
            } else {
                String text = output.getText();
                if (text.length() == cursor) {
                    output.append(line);
                    cursor += line.length();
                } else if (line.equals("\r\n") || line.equals("\n")) {
                    cursor = text.length() + line.length();
                    output.append(line);
                } else {
                    text = text.substring(0, cursor);
                    text += line;
                    output.setText(text);
                    cursor = text.length();
                }
            }
        }
    }
}
