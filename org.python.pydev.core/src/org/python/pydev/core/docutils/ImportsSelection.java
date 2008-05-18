package org.python.pydev.core.docutils;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;

public class ImportsSelection {

    /**
     * @param doc
     * @param documentOffset
     * @return the import info or null if none is available
     */
    public static ImportInfo getImportsTipperStr(String trimmedLine, boolean returnEvenEmpty) {
        String importMsg = "";
    
        if (!trimmedLine.startsWith("from") && !trimmedLine.startsWith("import")) {
            
            return new ImportInfo("", false); // it is not an import
        }
    
        int fromIndex = trimmedLine.indexOf("from");
        int importIndex = trimmedLine.indexOf("import");
        boolean foundImportOnArray = false;
    
        // check if we have a from or an import.
        if (fromIndex != -1 || importIndex != -1) {
            trimmedLine = trimmedLine.replaceAll("#.*", ""); // remove comments
            String[] strings = trimmedLine.split(" ");
    
            if (fromIndex != -1 && importIndex == -1) {
                if (strings.length > 2) {
                    // user has spaces as in 'from xxx uuu'
                    return new ImportInfo("", foundImportOnArray);
                }
            }
    
            for (int i = 0; i < strings.length; i++) {
                if (strings[i].equals("import")) {
                    foundImportOnArray = true;
                }
    
                if (strings[i].equals("from") == false && strings[i].equals("import") == false) {
                    if (importMsg.length() != 0) {
                        importMsg += '.';
                    }
                    importMsg += strings[i];
                }
                // now, if we have a from xxx import something, we'll always
                // want to return only the xxx
                if (fromIndex != -1 && importIndex == -1 && (foundImportOnArray || i == strings.length - 1)) {
                    if (importMsg.length() == 0) {
                        return ImportsSelection.doExistingOrEmptyReturn(returnEvenEmpty, importMsg, foundImportOnArray);
                    }
                    if (importMsg.startsWith(".")) {
                        return new ImportInfo(importMsg, foundImportOnArray);
                    }
                    if (importMsg.indexOf(".") == -1) {
                        return ImportsSelection.doExistingOrEmptyReturn(returnEvenEmpty, importMsg, foundImportOnArray);
                    }
                    return new ImportInfo(importMsg.substring(0, importMsg.lastIndexOf(".") + 1), foundImportOnArray);
    
                }
            }
    
            if (fromIndex != -1 && importIndex != -1) {
                if (strings.length == 3) {
                    importMsg += '.';
                }
            }
        } else {
            return new ImportInfo("", foundImportOnArray);
        }
        if (importMsg.indexOf(".") == -1) {
            // we have only import fff or from iii (so, we're going for all
            // imports).
            return ImportsSelection.doExistingOrEmptyReturn(returnEvenEmpty, importMsg, foundImportOnArray);
        }
    
        if (fromIndex == -1 && importMsg.indexOf(',') != -1) {
            // we have something like import xxx, yyy, ...
            importMsg = importMsg.substring(importMsg.lastIndexOf(',') + 1, importMsg.length());
            if (importMsg.startsWith(".")) {
                importMsg = importMsg.substring(1);
            }
    
            int j = importMsg.lastIndexOf('.');
            if (j != -1) {
                importMsg = importMsg.substring(0, j);
                return new ImportInfo(importMsg, foundImportOnArray);
            } else {
                return ImportsSelection.doExistingOrEmptyReturn(returnEvenEmpty, importMsg, foundImportOnArray);
            }
    
        } else {
            // now, we may still have something like 'unittest.test,' or
            // 'unittest.test.,'
            // so, we have to remove this comma (s).
            int i;
            boolean removed = false;
            while ((i = importMsg.indexOf(',')) != -1) {
                if (importMsg.charAt(i - 1) == '.') {
                    int j = importMsg.lastIndexOf('.');
                    importMsg = importMsg.substring(0, j);
                }
    
                int j = importMsg.lastIndexOf('.');
                importMsg = importMsg.substring(0, j);
                removed = true;
            }
    
            // if it is something like aaa.sss.bb : removes the bb because it is
            // the qualifier
            // if it is something like aaa.sss. : removes only the last point
            if (!removed && importMsg.length() > 0 && importMsg.indexOf('.') != -1) {
                importMsg = importMsg.substring(0, importMsg.lastIndexOf('.'));
            }
    
            return new ImportInfo(importMsg, foundImportOnArray);
        }
    }

    public static ImportInfo getImportsTipperStr(IDocument doc, int documentOffset) {
        PySelection.DocIterator iterator = new PySelection.DocIterator(false, new PySelection(doc, documentOffset));
        StringBuffer buffer = new StringBuffer();
        
        //it may still be a multiline-import... to check that, we have to go backward in the document and see if the
        //lines keep ending with a \ from the import line or if the import line has a (
        
        boolean expectContinue = false;
        boolean allEndingWithSlash = true;
        boolean found = false;
        
        while(iterator.hasNext()){
            String line = ParsingUtils.removeComments(iterator.next());
            String trimmedLine = line.trim();
            
            if (trimmedLine.startsWith("from") || trimmedLine.startsWith("import")) {
                if(expectContinue){
                    boolean correct = false;
                    
                    if(trimmedLine.indexOf('(') != -1){
                        correct = true;
                    }
                    if(trimmedLine.endsWith("\\")){
                        if(allEndingWithSlash){
                            correct = true;
                        }
                    }
                    
                    if(!correct){
                        break;
                    }
                }
                //that's it, we found it!
                found = true;
                buffer.insert(0, trimmedLine);
                break;
                
            }else{
                if(expectContinue){
                    if(trimmedLine.indexOf(')') != -1){
                        break;
                    }
                    if(allEndingWithSlash && trimmedLine.length() > 0){
                        char c = trimmedLine.charAt(trimmedLine.length()-1);
                        if(c != '\\' && c != ',' && c != '('){
                            break; //let's keep the analysis to lines that end with , ( \ (because otherwise it can be time-consuming)
                        }
                        if(c != '\\'){
                            allEndingWithSlash = false;
                        }
                    }
                }
                expectContinue = true;
                buffer.insert(0, line);
            }
        }
        
        if(!found){
            return new ImportInfo("", false); // it is not an import
        }
        
        return getImportsTipperStr(buffer.toString(), true);
    }

    private static ImportInfo doExistingOrEmptyReturn(boolean returnEvenEmpty, String importMsg, boolean foundImportOnArray) {
        if (returnEvenEmpty || importMsg.trim().length() > 0) {
            return new ImportInfo(" ", foundImportOnArray);
        } else {
            return new ImportInfo("", foundImportOnArray);
        }
    }

}
