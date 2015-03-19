package org.python.pydev.editor.codecompletion.shell;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.python.pydev.core.IToken;
import org.python.pydev.core.ObjectsInternPool;
import org.python.pydev.core.ObjectsInternPool.ObjectsPoolMap;
import org.python.pydev.core.log.Log;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;

/*default*/class ShellConvert {

    private static final String TYPE_UNKNOWN_STR = String.valueOf(IToken.TYPE_UNKNOWN);
    private static final String ENCODING_UTF_8 = AbstractShell.ENCODING_UTF_8;

    /**
     * @return
     */
    private static Tuple<String, List<String[]>> getInvalidCompletion() {
        List<String[]> l = new ArrayList<String[]>();
        return new Tuple<String, List<String[]>>(null, l);
    }

    /**
     * @throws IOException
     */
    static/*default*/Tuple<String, List<String[]>> convertStringToCompletions(FastStringBuffer read)
            throws IOException {
        if (read == null) {
            return getInvalidCompletion();
        }
        ArrayList<String[]> list = new ArrayList<String[]>();
        FastStringBuffer string = read.replaceAll("(", "").replaceAll(")", "");
        StringTokenizer tokenizer = new StringTokenizer(string.toString(), ",");
        string = null;

        ObjectsPoolMap map = new ObjectsPoolMap();
        //the first token is always the file for the module (no matter what)
        String file = "";
        if (tokenizer.hasMoreTokens()) {
            file = URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8);

            while (tokenizer.hasMoreTokens()) {
                String token = ObjectsInternPool.internLocal(map, URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8));
                if (!tokenizer.hasMoreTokens()) {
                    return new Tuple<String, List<String[]>>(file, list);
                }
                String description = ObjectsInternPool.internLocal(map,
                        URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8));

                String args = "";
                if (tokenizer.hasMoreTokens()) {
                    args = ObjectsInternPool.internLocal(map, URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8));
                }

                String type = TYPE_UNKNOWN_STR;
                if (tokenizer.hasMoreTokens()) {
                    type = ObjectsInternPool.internLocal(map, URLDecoder.decode(tokenizer.nextToken(), ENCODING_UTF_8));
                }

                //dbg(token);
                //dbg(description);

                if (!token.equals("ERROR:")) {
                    list.add(new String[] { token, description, args, type });
                } else {
                    if (DebugSettings.DEBUG_CODE_COMPLETION) {
                        Log.addLogLevel();
                        try {
                            Log.toLogFile("Code completion shell error:", AbstractShell.class);
                            Log.toLogFile(token, AbstractShell.class);
                            Log.toLogFile(description, AbstractShell.class);
                            Log.toLogFile(args, AbstractShell.class);
                            Log.toLogFile(type, AbstractShell.class);
                        } finally {
                            Log.remLogLevel();
                        }
                    }
                }

            }
        }
        return new Tuple<String, List<String[]>>(file, list);
    }
}
