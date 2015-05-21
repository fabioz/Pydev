/******************************************************************************
* Copyright (C) 2009-2012  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.parser.grammarcommon;

import java.lang.reflect.Field;

/**
 * With this class we're able to get constants from a class (note, to
 * recreate this code, the make_replace.py must be run, as it's COG-generated)
 */
public abstract class AbstractTokenManagerWithConstants {

    @SuppressWarnings("rawtypes")
    protected abstract Class getConstantsClass();

    @SuppressWarnings("rawtypes")
    protected final int getFromConstants(String constant) {
        try {
            Class c = getConstantsClass();
            Field declaredField = c.getDeclaredField(constant);
            return declaredField.getInt(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    protected AbstractTokenManagerWithConstants() {

        /*[[[cog
        import cog
        
        names = [       
        "EOF",
        "SPACE" ,
        "CONTINUATION"     ,
        "NEWLINE1"   ,
        "NEWLINE"     ,
        "NEWLINE2"     ,
        "CRLF1"     ,
        "DEDENT" ,
        "INDENT" ,
        "TRAILING_COMMENT" ,
        "SINGLE_LINE_COMMENT" ,
        "LPAREN" ,
        "RPAREN" ,
        "LBRACE" ,
        "RBRACE" ,
        "LBRACKET", 
        "RBRACKET" ,
        "SEMICOLON" ,
        "COMMA" ,
        "DOT" ,
        "COLON", 
        "PLUS" ,
        "MINUS" ,
        "MULTIPLY" ,
        "DIVIDE" ,
        "FLOORDIVIDE" ,
        "POWER" ,
        "LSHIFT" ,
        "RSHIFT" ,
        "MODULO" ,
        "NOT"     ,
        "XOR" ,
        "OR" ,
        "AND" ,
        "EQUAL", 
        "GREATER" ,
        "LESS"     ,
        "EQEQUAL" ,
        "EQLESS" ,
        "EQGREATER" ,
        "NOTEQUAL" ,
        "PLUSEQ" ,
        "MINUSEQ" ,
        "MULTIPLYEQ" ,
        "DIVIDEEQ" ,
        "FLOORDIVIDEEQ" ,
        "MODULOEQ" ,
        "ANDEQ"     ,
        "OREQ" ,
        "XOREQ" ,
        "LSHIFTEQ" ,
        "RSHIFTEQ" ,
        "POWEREQ" ,
        "OR_BOOL" ,
        "AND_BOOL" ,
        "NOT_BOOL" ,
        "IS" ,
        "IN" ,
        "LAMBDA" ,
        "IF" ,
        "ELSE", 
        "ELIF" ,
        "WHILE" ,
        "FOR" ,
        "TRY" ,
        "EXCEPT" ,
        "DEF"     ,
        "CLASS" ,
        "FINALLY", 
        "PASS"     ,
        "BREAK" ,
        "CONTINUE" ,
        "RETURN" ,
        "YIELD" ,
        "IMPORT" ,
        "FROM" ,
        "DEL" ,
        "RAISE", 
        "GLOBAL", 
        #"NONLOCAL" , --not available for all
        "ASSERT" ,
        #"AS" , --not available for all
        #"WITH", --not available for all
        #"FALSE", --not available for all 
        #"TRUE" , --not available for all
        #"NONE" , --not available for all
        "AT" ,
        "NAME", 
        "LETTER" ,
        "DECNUMBER" ,
        "HEXNUMBER" ,
        "OCTNUMBER" ,
        "FLOAT" ,
        "COMPLEX", 
        "EXPONENT", 
        "DIGIT" ,
        "SINGLE_STRING" ,
        "SINGLE_STRING2" ,
        "TRIPLE_STRING" ,
        "TRIPLE_STRING2" ,
        
        #--not available for all
        #"SINGLE_BSTRING" ,
        #"SINGLE_BSTRING2" ,
        #"TRIPLE_BSTRING" ,
        #"TRIPLE_BSTRING2" ,
        ]
        
        
        lexer_names=[
        "DEFAULT" ,
        "FORCE_NEWLINE1" ,
        "FORCE_NEWLINE2" ,
        "INDENTING" ,
        "INDENTATION_UNCHANGED" ,
        "UNREACHABLE" ,
        "IN_STRING11" ,
        "IN_STRING21" ,
        "IN_STRING13" ,
        "IN_STRING23" ,
        
        #--not available for all
        #"IN_BSTRING11" ,
        #"IN_BSTRING21" ,
        #"IN_BSTRING13" ,
        #"IN_BSTRING23" ,
        #"IN_STRING1NLC" ,
        #"IN_STRING2NLC" ,
        #"IN_BSTRING1NLC" ,
        #"IN_BSTRING2NLC" ,
        ]

        def GetTitles(name):
            as_title = name.title().replace('_', '')
            as_title_1st_lower = as_title[0].lower()+as_title[1:]
            return as_title, as_title_1st_lower
            
        def WriteAssign(name, is_lexer=False):
            as_title, as_title_1st_lower = GetTitles(name)
            if not is_lexer:
                cog.outl("%sId =  getFromConstants(\"%s\");" % (as_title_1st_lower, name))
            else:
                cog.outl("lexer%sId =  getFromConstants(\"%s\");" % (as_title, name))
                
        def WriteField(name, is_lexer=False):
            as_title, as_title_1st_lower = GetTitles(name)
            if not is_lexer:
                cog.outl("private final int %sId;" % (as_title_1st_lower))
            else:
                cog.outl("private final int lexer%sId;" % (as_title))
            
        def WriteGetter(name, is_lexer=False):
            as_title, as_title_1st_lower = GetTitles(name)
            if not is_lexer:
                cog.outl("public final int get%sId(){return %sId;}" % (as_title, as_title_1st_lower))
            else:
                cog.outl("public final int getLexer%sId(){return lexer%sId;}" % (as_title, as_title))
            
        
        #finish the constructor
        for name in names:
            WriteAssign(name)
        for name in lexer_names:
            WriteAssign(name, True)
            
        cog.outl("}")
            
        cog.outl("")
        cog.outl("//Fields ----------")
        for name in names:
            WriteField(name)
        for name in lexer_names:
            WriteField(name, True)
            
            
        cog.outl("")
        cog.outl("//Getters ----------")
        for name in names:
            WriteGetter(name)
        for name in lexer_names:
            WriteGetter(name, True)
        ]]]*/
        eofId =  getFromConstants("EOF");
        spaceId =  getFromConstants("SPACE");
        continuationId =  getFromConstants("CONTINUATION");
        newline1Id =  getFromConstants("NEWLINE1");
        newlineId =  getFromConstants("NEWLINE");
        newline2Id =  getFromConstants("NEWLINE2");
        crlf1Id =  getFromConstants("CRLF1");
        dedentId =  getFromConstants("DEDENT");
        indentId =  getFromConstants("INDENT");
        trailingCommentId =  getFromConstants("TRAILING_COMMENT");
        singleLineCommentId =  getFromConstants("SINGLE_LINE_COMMENT");
        lparenId =  getFromConstants("LPAREN");
        rparenId =  getFromConstants("RPAREN");
        lbraceId =  getFromConstants("LBRACE");
        rbraceId =  getFromConstants("RBRACE");
        lbracketId =  getFromConstants("LBRACKET");
        rbracketId =  getFromConstants("RBRACKET");
        semicolonId =  getFromConstants("SEMICOLON");
        commaId =  getFromConstants("COMMA");
        dotId =  getFromConstants("DOT");
        colonId =  getFromConstants("COLON");
        plusId =  getFromConstants("PLUS");
        minusId =  getFromConstants("MINUS");
        multiplyId =  getFromConstants("MULTIPLY");
        divideId =  getFromConstants("DIVIDE");
        floordivideId =  getFromConstants("FLOORDIVIDE");
        powerId =  getFromConstants("POWER");
        lshiftId =  getFromConstants("LSHIFT");
        rshiftId =  getFromConstants("RSHIFT");
        moduloId =  getFromConstants("MODULO");
        notId =  getFromConstants("NOT");
        xorId =  getFromConstants("XOR");
        orId =  getFromConstants("OR");
        andId =  getFromConstants("AND");
        equalId =  getFromConstants("EQUAL");
        greaterId =  getFromConstants("GREATER");
        lessId =  getFromConstants("LESS");
        eqequalId =  getFromConstants("EQEQUAL");
        eqlessId =  getFromConstants("EQLESS");
        eqgreaterId =  getFromConstants("EQGREATER");
        notequalId =  getFromConstants("NOTEQUAL");
        pluseqId =  getFromConstants("PLUSEQ");
        minuseqId =  getFromConstants("MINUSEQ");
        multiplyeqId =  getFromConstants("MULTIPLYEQ");
        divideeqId =  getFromConstants("DIVIDEEQ");
        floordivideeqId =  getFromConstants("FLOORDIVIDEEQ");
        moduloeqId =  getFromConstants("MODULOEQ");
        andeqId =  getFromConstants("ANDEQ");
        oreqId =  getFromConstants("OREQ");
        xoreqId =  getFromConstants("XOREQ");
        lshifteqId =  getFromConstants("LSHIFTEQ");
        rshifteqId =  getFromConstants("RSHIFTEQ");
        powereqId =  getFromConstants("POWEREQ");
        orBoolId =  getFromConstants("OR_BOOL");
        andBoolId =  getFromConstants("AND_BOOL");
        notBoolId =  getFromConstants("NOT_BOOL");
        isId =  getFromConstants("IS");
        inId =  getFromConstants("IN");
        lambdaId =  getFromConstants("LAMBDA");
        ifId =  getFromConstants("IF");
        elseId =  getFromConstants("ELSE");
        elifId =  getFromConstants("ELIF");
        whileId =  getFromConstants("WHILE");
        forId =  getFromConstants("FOR");
        tryId =  getFromConstants("TRY");
        exceptId =  getFromConstants("EXCEPT");
        defId =  getFromConstants("DEF");
        classId =  getFromConstants("CLASS");
        finallyId =  getFromConstants("FINALLY");
        passId =  getFromConstants("PASS");
        breakId =  getFromConstants("BREAK");
        continueId =  getFromConstants("CONTINUE");
        returnId =  getFromConstants("RETURN");
        yieldId =  getFromConstants("YIELD");
        importId =  getFromConstants("IMPORT");
        fromId =  getFromConstants("FROM");
        delId =  getFromConstants("DEL");
        raiseId =  getFromConstants("RAISE");
        globalId =  getFromConstants("GLOBAL");
        assertId =  getFromConstants("ASSERT");
        atId =  getFromConstants("AT");
        nameId =  getFromConstants("NAME");
        letterId =  getFromConstants("LETTER");
        decnumberId =  getFromConstants("DECNUMBER");
        hexnumberId =  getFromConstants("HEXNUMBER");
        octnumberId =  getFromConstants("OCTNUMBER");
        floatId =  getFromConstants("FLOAT");
        complexId =  getFromConstants("COMPLEX");
        exponentId =  getFromConstants("EXPONENT");
        digitId =  getFromConstants("DIGIT");
        singleStringId =  getFromConstants("SINGLE_STRING");
        singleString2Id =  getFromConstants("SINGLE_STRING2");
        tripleStringId =  getFromConstants("TRIPLE_STRING");
        tripleString2Id =  getFromConstants("TRIPLE_STRING2");
        lexerDefaultId =  getFromConstants("DEFAULT");
        lexerForceNewline1Id =  getFromConstants("FORCE_NEWLINE1");
        lexerForceNewline2Id =  getFromConstants("FORCE_NEWLINE2");
        lexerIndentingId =  getFromConstants("INDENTING");
        lexerIndentationUnchangedId =  getFromConstants("INDENTATION_UNCHANGED");
        lexerUnreachableId =  getFromConstants("UNREACHABLE");
        lexerInString11Id =  getFromConstants("IN_STRING11");
        lexerInString21Id =  getFromConstants("IN_STRING21");
        lexerInString13Id =  getFromConstants("IN_STRING13");
        lexerInString23Id =  getFromConstants("IN_STRING23");
        }

        //Fields ----------
        private final int eofId;
        private final int spaceId;
        private final int continuationId;
        private final int newline1Id;
        private final int newlineId;
        private final int newline2Id;
        private final int crlf1Id;
        private final int dedentId;
        private final int indentId;
        private final int trailingCommentId;
        private final int singleLineCommentId;
        private final int lparenId;
        private final int rparenId;
        private final int lbraceId;
        private final int rbraceId;
        private final int lbracketId;
        private final int rbracketId;
        private final int semicolonId;
        private final int commaId;
        private final int dotId;
        private final int colonId;
        private final int plusId;
        private final int minusId;
        private final int multiplyId;
        private final int divideId;
        private final int floordivideId;
        private final int powerId;
        private final int lshiftId;
        private final int rshiftId;
        private final int moduloId;
        private final int notId;
        private final int xorId;
        private final int orId;
        private final int andId;
        private final int equalId;
        private final int greaterId;
        private final int lessId;
        private final int eqequalId;
        private final int eqlessId;
        private final int eqgreaterId;
        private final int notequalId;
        private final int pluseqId;
        private final int minuseqId;
        private final int multiplyeqId;
        private final int divideeqId;
        private final int floordivideeqId;
        private final int moduloeqId;
        private final int andeqId;
        private final int oreqId;
        private final int xoreqId;
        private final int lshifteqId;
        private final int rshifteqId;
        private final int powereqId;
        private final int orBoolId;
        private final int andBoolId;
        private final int notBoolId;
        private final int isId;
        private final int inId;
        private final int lambdaId;
        private final int ifId;
        private final int elseId;
        private final int elifId;
        private final int whileId;
        private final int forId;
        private final int tryId;
        private final int exceptId;
        private final int defId;
        private final int classId;
        private final int finallyId;
        private final int passId;
        private final int breakId;
        private final int continueId;
        private final int returnId;
        private final int yieldId;
        private final int importId;
        private final int fromId;
        private final int delId;
        private final int raiseId;
        private final int globalId;
        private final int assertId;
        private final int atId;
        private final int nameId;
        private final int letterId;
        private final int decnumberId;
        private final int hexnumberId;
        private final int octnumberId;
        private final int floatId;
        private final int complexId;
        private final int exponentId;
        private final int digitId;
        private final int singleStringId;
        private final int singleString2Id;
        private final int tripleStringId;
        private final int tripleString2Id;
        private final int lexerDefaultId;
        private final int lexerForceNewline1Id;
        private final int lexerForceNewline2Id;
        private final int lexerIndentingId;
        private final int lexerIndentationUnchangedId;
        private final int lexerUnreachableId;
        private final int lexerInString11Id;
        private final int lexerInString21Id;
        private final int lexerInString13Id;
        private final int lexerInString23Id;

        //Getters ----------
        public final int getEofId(){return eofId;}
        public final int getSpaceId(){return spaceId;}
        public final int getContinuationId(){return continuationId;}
        public final int getNewline1Id(){return newline1Id;}
        public final int getNewlineId(){return newlineId;}
        public final int getNewline2Id(){return newline2Id;}
        public final int getCrlf1Id(){return crlf1Id;}
        public final int getDedentId(){return dedentId;}
        public final int getIndentId(){return indentId;}
        public final int getTrailingCommentId(){return trailingCommentId;}
        public final int getSingleLineCommentId(){return singleLineCommentId;}
        public final int getLparenId(){return lparenId;}
        public final int getRparenId(){return rparenId;}
        public final int getLbraceId(){return lbraceId;}
        public final int getRbraceId(){return rbraceId;}
        public final int getLbracketId(){return lbracketId;}
        public final int getRbracketId(){return rbracketId;}
        public final int getSemicolonId(){return semicolonId;}
        public final int getCommaId(){return commaId;}
        public final int getDotId(){return dotId;}
        public final int getColonId(){return colonId;}
        public final int getPlusId(){return plusId;}
        public final int getMinusId(){return minusId;}
        public final int getMultiplyId(){return multiplyId;}
        public final int getDivideId(){return divideId;}
        public final int getFloordivideId(){return floordivideId;}
        public final int getPowerId(){return powerId;}
        public final int getLshiftId(){return lshiftId;}
        public final int getRshiftId(){return rshiftId;}
        public final int getModuloId(){return moduloId;}
        public final int getNotId(){return notId;}
        public final int getXorId(){return xorId;}
        public final int getOrId(){return orId;}
        public final int getAndId(){return andId;}
        public final int getEqualId(){return equalId;}
        public final int getGreaterId(){return greaterId;}
        public final int getLessId(){return lessId;}
        public final int getEqequalId(){return eqequalId;}
        public final int getEqlessId(){return eqlessId;}
        public final int getEqgreaterId(){return eqgreaterId;}
        public final int getNotequalId(){return notequalId;}
        public final int getPluseqId(){return pluseqId;}
        public final int getMinuseqId(){return minuseqId;}
        public final int getMultiplyeqId(){return multiplyeqId;}
        public final int getDivideeqId(){return divideeqId;}
        public final int getFloordivideeqId(){return floordivideeqId;}
        public final int getModuloeqId(){return moduloeqId;}
        public final int getAndeqId(){return andeqId;}
        public final int getOreqId(){return oreqId;}
        public final int getXoreqId(){return xoreqId;}
        public final int getLshifteqId(){return lshifteqId;}
        public final int getRshifteqId(){return rshifteqId;}
        public final int getPowereqId(){return powereqId;}
        public final int getOrBoolId(){return orBoolId;}
        public final int getAndBoolId(){return andBoolId;}
        public final int getNotBoolId(){return notBoolId;}
        public final int getIsId(){return isId;}
        public final int getInId(){return inId;}
        public final int getLambdaId(){return lambdaId;}
        public final int getIfId(){return ifId;}
        public final int getElseId(){return elseId;}
        public final int getElifId(){return elifId;}
        public final int getWhileId(){return whileId;}
        public final int getForId(){return forId;}
        public final int getTryId(){return tryId;}
        public final int getExceptId(){return exceptId;}
        public final int getDefId(){return defId;}
        public final int getClassId(){return classId;}
        public final int getFinallyId(){return finallyId;}
        public final int getPassId(){return passId;}
        public final int getBreakId(){return breakId;}
        public final int getContinueId(){return continueId;}
        public final int getReturnId(){return returnId;}
        public final int getYieldId(){return yieldId;}
        public final int getImportId(){return importId;}
        public final int getFromId(){return fromId;}
        public final int getDelId(){return delId;}
        public final int getRaiseId(){return raiseId;}
        public final int getGlobalId(){return globalId;}
        public final int getAssertId(){return assertId;}
        public final int getAtId(){return atId;}
        public final int getNameId(){return nameId;}
        public final int getLetterId(){return letterId;}
        public final int getDecnumberId(){return decnumberId;}
        public final int getHexnumberId(){return hexnumberId;}
        public final int getOctnumberId(){return octnumberId;}
        public final int getFloatId(){return floatId;}
        public final int getComplexId(){return complexId;}
        public final int getExponentId(){return exponentId;}
        public final int getDigitId(){return digitId;}
        public final int getSingleStringId(){return singleStringId;}
        public final int getSingleString2Id(){return singleString2Id;}
        public final int getTripleStringId(){return tripleStringId;}
        public final int getTripleString2Id(){return tripleString2Id;}
        public final int getLexerDefaultId(){return lexerDefaultId;}
        public final int getLexerForceNewline1Id(){return lexerForceNewline1Id;}
        public final int getLexerForceNewline2Id(){return lexerForceNewline2Id;}
        public final int getLexerIndentingId(){return lexerIndentingId;}
        public final int getLexerIndentationUnchangedId(){return lexerIndentationUnchangedId;}
        public final int getLexerUnreachableId(){return lexerUnreachableId;}
        public final int getLexerInString11Id(){return lexerInString11Id;}
        public final int getLexerInString21Id(){return lexerInString21Id;}
        public final int getLexerInString13Id(){return lexerInString13Id;}
        public final int getLexerInString23Id(){return lexerInString23Id;}
    //[[[end]]]

}