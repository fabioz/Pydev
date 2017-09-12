package org.python.pydev.process_window;

import junit.framework.TestCase;

public class SimpleTerminalEmulatorTest extends TestCase {

    public void testTerminalEmulator() throws Exception {
        // Now working as it should for now (just adding everything sequentially).
        //        final FastStringBuffer textBuffer = new FastStringBuffer();
        //
        //        ITextWrapper textWrapper = new ITextWrapper() {
        //
        //            @Override
        //            public void setText(String string) {
        //                textBuffer.clear();
        //                textBuffer.append(string);
        //            }
        //
        //            @Override
        //            public char[] getTextChars() {
        //                return textBuffer.toCharArray();
        //            }
        //
        //            @Override
        //            public String getText() {
        //                return textBuffer.toString();
        //            }
        //
        //            @Override
        //            public Control getControl() {
        //                return null;
        //            }
        //
        //            @Override
        //            public void append(String substring) {
        //                textBuffer.append(substring);
        //            }
        //        };
        //        SimpleTerminalEmulator emulator = new SimpleTerminalEmulator(textWrapper);
        //        emulator.processText("test\rfooo");
        //        assertEquals(textBuffer.toString(), "fooo");
        //        emulator.processText("\rbbbb");
        //        assertEquals(textBuffer.toString(), "bbbb");
        //        emulator.processText("\r\r\nbar");
        //        assertEquals(textBuffer.toString(), "bbbb\r\nbar");
        //        emulator.processText("\rxxx\r\nbar");
        //        assertEquals(textBuffer.toString(), "bbbb\r\nxxx\r\nbar");
        //        emulator.processText("\nla");
        //        assertEquals(textBuffer.toString(), "bbbb\r\nxxx\r\nbar\nla");
    }
}
