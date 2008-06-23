package org.python.pydev.parser.fastparser;

import org.python.pydev.core.docutils.ParsingUtils;

/**
 * @note: Unfinished
 * 
 * This class should be able to gather the definitions found in a module in a very fast way.
 * 
 * The target is having a performance around 10x faster than doing a regular parse, focusing on getting
 * the name tokens for:
 * 
 * classes, functions, class attributes, instance attributes -- basically the tokens that provide a 
 * definition that can be 'globally' accessed.
 *
 * @author Fabio
 */
public class FastDefinitionsParser {

    public static void parse(String cs) {
        
        char lastChar = '\0';
        int length = cs.length();
        for (int i = 0; i < length; i++) {
            char c = cs.charAt(i);

            if (c == '\'' || c == '"') { 
                //go to the end of the literal
                i = ParsingUtils.getLiteralEnd(cs, i, c);

            } else if (c == '#') { 
                //go to the end of the comment
                while(i < length && (c = cs.charAt(i)) != '\n' && c != '\r'){
                    i++;
                }

            } else {
                //skip the line
                if (c == '\r' || c == '\n') {
                    while(i < length && (c = cs.charAt(i)) == '\n' || c == '\r'){
                        i++;
                    }
                }
            }
            lastChar = c;
        }
    }

}
