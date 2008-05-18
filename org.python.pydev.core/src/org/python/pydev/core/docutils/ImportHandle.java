package org.python.pydev.core.docutils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.IDocument;

/**
 * Class that represents an import found in a document.
 *
 * @author Fabio
 */
public class ImportHandle {
    

    /**
     * Class representing some import information
     *
     * @author Fabio
     */
    public static class ImportHandleInfo{
        
        //spaces* 'from' space+ module space+ import (mod as y)
        private static final Pattern FromImportPattern = Pattern.compile("(from\\s+)(\\.*\\w+)(\\s+import\\s+)(\\w+|\\s|,|#|\\(|\\))*(\\z)");
        private static final Pattern ImportPattern = Pattern.compile("(import\\s+)(\\w+|\\s|,|#|\\(|\\))*(\\z)");
        
        /**
         * Holds the 'KKK' if the import is from KKK import YYY
         * If it's not a From Import, it should be null.
         */
        private String fromStr;

        /**
         * This is the alias that's been imported. E.g.: in from KKK import YYY, ZZZ, this is a list
         * with YYY and ZZZ
         */
        private List<String> importedStr;
        
        /**
         * Comments (one for each imported string) E.g.: in from KKK import (YYY, #comment\n ZZZ), this is a list
         * with #comment and an empty string.
         */
        private List<String> importedStrComments;

        /**
         * Constructor.
         * 
         * Creates the information to be returned later
         * 
         * @param importFound
         * @throws ImportNotRecognizedException 
         */
        public ImportHandleInfo(String importFound) throws ImportNotRecognizedException {
            importFound=importFound.trim();
            char firstChar = importFound.charAt(0);
            
            
            if (firstChar == 'f') {
                //from import
                Matcher matcher = FromImportPattern.matcher(importFound);
                if(matcher.matches()){
                    this.fromStr = matcher.group(2); 
                    
                    //we have to do that because the last group will only have the last match in the string
                    String importedStr = importFound.substring(matcher.end(3), 
                            matcher.end(4)).trim();
                    
                    buildImportedList(importedStr);
                    
                }else{
                    throw new ImportNotRecognizedException("Could not recognize import: "+importFound);
                }
                

            }else if(firstChar == 'i'){
                //regular import
                Matcher matcher = ImportPattern.matcher(importFound);
                if(matcher.matches()){
                    //we have to do that because the last group will only have the last match in the string
                    String importedStr = importFound.substring(matcher.end(1), 
                            matcher.end(2)).trim();
                    
                    buildImportedList(importedStr);
                    
                }else{
                    throw new ImportNotRecognizedException("Could not recognize import: "+importFound);
                }
                
            }else{
                throw new ImportNotRecognizedException("Could not recognize import: "+importFound);
            }
        }

        /**
         * Fills the importedStrComments and importedStr given the importedStr passed 
         * 
         * @param importedStr string with the tokens imported in an import
         */
        private void buildImportedList(String importedStr) {
            ArrayList<String> lst = new ArrayList<String>();
            ArrayList<String> importComments = new ArrayList<String>();
            
            StringBuffer alias = new StringBuffer();
            for(int i=0;i<importedStr.length();i++){
                char c = importedStr.charAt(i);
                if(c == '#'){
                    StringBuffer comments = new StringBuffer();
                    i = ParsingUtils.eatComments(importedStr, comments, i);
                    addImportAlias(lst, importComments, alias, comments.toString());
                    alias = new StringBuffer();
                    
                }else if(c == ',' || c == '\r' || c == '\n'){
                    addImportAlias(lst, importComments, alias, "");
                    alias = new StringBuffer();
                    
                }else if(c == '(' || c == ')'){
                    //do nothing
                    
                    
                }else if(c == ' ' || c == '\t'){
                    String curr = alias.toString();
                    if(curr.endsWith(" as") | curr.endsWith("\tas")){
                        alias = new StringBuffer();
                    }
                    alias.append(c);
                    
                }else{
                    alias.append(c);
                }
            }
            
            if(alias.length() > 0){
                addImportAlias(lst, importComments, alias, "");
                
            }
            
            this.importedStrComments = importComments;
            this.importedStr = lst;
        }

        /**
         * Adds an import and its related comment to the given lists (if there's actually something available to be
         * added)
         * 
         * @param lst list where the alias will be added
         * @param importComments list where the comment will be added
         * @param alias the name of the import to be added
         * @param importComment the comment related to the import
         */
        private void addImportAlias(ArrayList<String> lst, ArrayList<String> importComments, StringBuffer alias, 
                String importComment) {
            
            String aliasStr = alias.toString().trim();
            importComment = importComment.trim();
            
            if(aliasStr.length() > 0){
                lst.add(aliasStr);
                importComments.add(importComment);
            }else if(importComment.length() > 0 && importComments.size() > 0){
                importComments.set(importComments.size()-1, importComment);
            }
        }

        /**
         * @return the from module in the import
         */
        public String getFromImportStr() {
            return this.fromStr;
        }

        /**
         * @return the tokens imported from the module (or the alias if it's specified)
         */
        public List<String> getImportedStr() {
            return this.importedStr;
        }

        /**
         * @return a list with a string for each imported token correspondent to a comment related to that import.
         */
        public List<String> getCommentsForImports() {
            return this.importedStrComments;
        }
        
    }

    
    /**
     * Document where the import was found
     */
    public IDocument doc;
    
    /**
     * The import string found. Note: it may contain comments and multi-lines.
     */
    public String importFound;
    
    /**
     * The initial line where the import was found
     */
    public int startFoundLine;
    
    /**
     * The final line where the import was found
     */
    public int endFoundLine;
    
    /**
     * Import informatiot for the import found and handled in this class (only created on request)
     */
    private List<ImportHandleInfo> importInfo;

    /**
     * Constructor.
     * 
     * Assigns parameters to fields.
     */
    public ImportHandle(IDocument doc, String importFound, int startFoundLine, int endFoundLine) {
        this.doc = doc;
        this.importFound = importFound;
        this.startFoundLine = startFoundLine;
        this.endFoundLine = endFoundLine;
    }

    /**
     * @param realImportRep the import to match. Note that only a single import statement may be passed as a parameter.
     * 
     * @return true if the passed import matches the import in this handle (note: as this class can actually wrap more
     * than 1 import, it'll return true if any of the internal imports match the passed import)
     * @throws ImportNotRecognizedException if the passed import could not be recognized
     */
    public boolean contains(String realImportRep) throws ImportNotRecognizedException {
        ImportHandleInfo otherImportInfo = new ImportHandleInfo(realImportRep);
        List<ImportHandleInfo> importHandleInfo = this.getImportInfo();
        
        for(ImportHandleInfo info : importHandleInfo) {
            if(info.fromStr != otherImportInfo.fromStr){
                if(otherImportInfo.fromStr == null || info.fromStr == null){
                    continue; //keep on to the next possible match
                }
                if(!otherImportInfo.fromStr.equals(info.fromStr)){
                    continue; //keep on to the next possible match
                }
            }
            
            if(otherImportInfo.importedStr.size() != 1){
                continue;
            }
            
            if(info.importedStr.contains(otherImportInfo.importedStr.get(0))){
                return true;
            }
            
        }
        
        
        return false;
    }


    /**
     * @return a list with the import information generated from the import this handle is wrapping.
     */
    public List<ImportHandleInfo> getImportInfo() {
        if(this.importInfo == null){
            this.importInfo = new ArrayList<ImportHandleInfo>();
            
            StringBuffer imp = new StringBuffer();
            for(int i=0;i<importFound.length();i++){
                char c = importFound.charAt(i);
                
                if(c == '#'){
                    i = ParsingUtils.eatComments(importFound, imp, i);
                    
                }else if(c == ';'){
                    try {
                        this.importInfo.add(new ImportHandleInfo(imp.toString()));
                    } catch (ImportNotRecognizedException e) {
                        //that's ok, not a valid import (at least, we couldn't parse it)
                    }
                    imp = new StringBuffer();
                    
                }else{
                    imp.append(c);
                }
                
            }
            try {
                this.importInfo.add(new ImportHandleInfo(imp.toString()));
            } catch (ImportNotRecognizedException e) {
                //that's ok, not a valid import (at least, we couldn't parse it)
            }
        }
        return this.importInfo;
    }

}
