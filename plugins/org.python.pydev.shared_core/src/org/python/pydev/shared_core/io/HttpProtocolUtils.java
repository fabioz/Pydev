package org.python.pydev.shared_core.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class HttpProtocolUtils {
    /**
     * can be specified to debug this class
     */
    private static final boolean DEBUG = false;

    private byte[] buffer = new byte[32 * 1024];
    private FastStringBuffer contents = new FastStringBuffer();
    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    public String readContents(InputStream in, ICallback<String, Object> onUnexpectedMessage) throws IOException {
        int bytesToRead = -1;

        while (true) {
            FileUtils.readLine(in, contents.clear());
            contents.trim(); // Remove the \r\n in the end.

            if (contents.length() == 0) {
                // Ok, real payload ahead.
                // Read once from stdin and print result to stdout
                if (bytesToRead == -1) {
                    Log.log("Error. pydevd did not respect protocol (Content-Length not passed in header).");
                    return null;
                }

                int bytesRead;
                while ((bytesRead = in.read(buffer, 0, Math.min(bytesToRead, buffer.length))) > 0) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                    bytesToRead -= bytesRead;
                }
                byte[] bytes = byteArrayOutputStream.toByteArray();
                byteArrayOutputStream.reset();
                return new String(bytes, StandardCharsets.UTF_8);
            } else {
                // Header found
                String contentLen = "Content-Length: ";
                if (contents.startsWith(contentLen)) {
                    if (DEBUG) {
                        System.err.println("receive cmd: " + contents);
                    }
                    contents.deleteFirstChars(contentLen.length());
                    try {
                        bytesToRead = Integer.parseInt(contents.trim().toString());
                    } catch (NumberFormatException e) {
                        throw new IOException("Error getting number of bytes to load. Found: " + contents);
                    }
                } else {
                    // Unexpected header.
                    if (onUnexpectedMessage != null) {
                        return onUnexpectedMessage.call(contents);
                    }
                    Log.log("Unexpected header: " + contents);
                    return null;
                }
            }
        }
    }
}
