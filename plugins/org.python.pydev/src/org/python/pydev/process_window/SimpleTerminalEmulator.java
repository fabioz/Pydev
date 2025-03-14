package org.python.pydev.process_window;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.FontUtils;
import org.python.pydev.shared_ui.IFontUsage;

/**
 * This class is a helper to emulate a terminal in a Text component (right now only deals with \r and \n).
 */
public class SimpleTerminalEmulator {

    private ITextWrapper output;
    private int cursor = 0;

    public SimpleTerminalEmulator(ITextWrapper textWrapper) {
        this.output = textWrapper;
    }

    public SimpleTerminalEmulator(Composite composite, int flags) {
        this(createTextWrapper(createText(composite, flags)));
    }

    private static Text createText(Composite composite, int flags) {
        Text text = new Text(composite, flags);
        text.setFont(new Font(composite.getDisplay(), FontUtils.getFontData(IFontUsage.STYLED, true)));
        return text;
    }

    private static ITextWrapper createTextWrapper(final Text text) {
        return new ITextWrapper() {

            @Override
            public void setText(String string) {
                text.setText(string);
            }

            @Override
            public char[] getTextChars() {
                return text.getTextChars();
            }

            @Override
            public String getText() {
                return text.getText();
            }

            @Override
            public Control getControl() {
                return text;
            }

            @Override
            public void append(String substring) {
                text.append(substring);
            }
        };
    }

    public void clearOutput() {
        output.setText("");
        cursor = 0;
    }

    public Control getControl() {
        return output.getControl();
    }

    public void processText(String contents) {
        List<String> splitInLines = StringUtils.splitInLines(contents, true);
        FastStringBuffer buf = new FastStringBuffer(output.getTextChars()).removeChars(Collections.singleton('\r'));
        for (String line : splitInLines) {
            // System.out.println("start line ---");
            // char[] charArray = line.toCharArray();
            // for (char c : charArray) {
            //     if (Character.isJavaIdentifierPart(c)) {
            //         System.out.print(c);
            //     } else {
            //         if (c == '\r') {
            //             System.out.print("\\r");
            //
            //         } else if (c == '\n') {
            //             System.out.println("\\n");
            //         } else {
            //             System.out.print(c);
            //         }
            //     }
            // }
            // System.out.println("end line ---");

            // Convert Windows newlines to Unix.
            if (line.endsWith("\r\n")) {
                line = line.substring(0, line.length() - 2) + "\n";
            }
            // Rewrite each line in between carriage returns.
            while (!line.isEmpty()) {
                String cutString = line;
                int crIndex = line.indexOf('\r');
                if (crIndex > -1) {
                    cutString = line.substring(0, crIndex);
                    line = line.substring(crIndex + 1);
                }
                if (cursor < buf.length()) {
                    buf.setLength(cursor);
                    buf.append(cutString);
                    output.setText(buf.toString());
                    cursor = buf.length();
                } else {
                    output.append(cutString);
                    buf.append(cutString);
                    cursor += cutString.length();
                }
                if (line.isEmpty()) {
                    // Line ended with \r, so line.substring made it blank,
                    // so we reset the cursor to the last newline character.
                    cursor = buf.lastIndexOf('\n') + 1;
                } else if (crIndex < 0) {
                    // \r didn't exist so we already processed a whole line.
                    line = "";
                }
            }
        }
    }
}
