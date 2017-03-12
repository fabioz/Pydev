package org.python.pydev.debug.codecoverage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CoverageXmlInfo {

    public static final SAXParserFactory parserFactory = SAXParserFactory.newInstance();

    public static SAXParser getSAXParser() {
        SAXParser parser = null;
        try {
            synchronized (parserFactory) {
                parser = parserFactory.newSAXParser();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return parser;
    }

    public static class FillCacheFromXmlHandler extends DefaultHandler {

        private CoverageCache cache;
        private File file;
        private int stmts = 0;
        private int miss = 0;
        private TreeSet<Integer> missedLines = new TreeSet<>();

        public FillCacheFromXmlHandler(CoverageCache cache) {
            this.cache = cache;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if (qName.equals("class")) {
                String filename = attributes.getValue("filename");
                file = new File(filename);
                stmts = 0;
                miss = 0;
                missedLines.clear();
            } else if (qName.equals("line")) {
                try {
                    int lineNum = Integer.parseInt(attributes.getValue("number"));
                    stmts += 1;
                    String hits = attributes.getValue("hits");
                    if ("0".equals(hits)) {
                        miss += 1;
                        missedLines.add(lineNum);
                    }
                } catch (Exception e) {
                    Log.log(e);
                    return;
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("class")) {
                cache.addFile(file, file.getParentFile(), stmts, miss, calculateMissingBuf(missedLines));
                file = null;
                stmts = 0;
                miss = 0;
                missedLines.clear();
            }
        }

        public static String calculateMissingBuf(TreeSet<Integer> missedLines) {
            int last = -1;
            int gapStart = -1;
            FastStringBuffer missingBuf = new FastStringBuffer();
            for (Integer missedLine : missedLines) {
                if (last == -1) {
                    last = gapStart = missedLine;
                } else {
                    if (missedLine == last + 1) {
                        last = missedLine;
                    } else {
                        if (missingBuf.length() > 0) {
                            missingBuf.append(", ");
                        }
                        if (gapStart == last) {
                            missingBuf.append(last);
                        } else {
                            missingBuf.append(gapStart);
                            missingBuf.append('-');
                            missingBuf.append(last);
                        }
                        last = gapStart = missedLine;
                    }
                }
            }

            if (last != -1) {
                if (missingBuf.length() > 0) {
                    missingBuf.append(", ");
                }
                if (gapStart == last) {
                    missingBuf.append(last);
                } else {
                    missingBuf.append(gapStart);
                    missingBuf.append('-');
                    missingBuf.append(last);
                }
            }
            return missingBuf.toString();
        }
    }

    public static void analyze(CoverageCache cache, File coverageXmlLocation) throws SAXException, IOException {
        String fileContents = FileUtils.getFileContents(coverageXmlLocation);
        SAXParser parser = getSAXParser();
        parser.parse(new ByteArrayInputStream(fileContents.getBytes()), new FillCacheFromXmlHandler(cache));
    }

}
