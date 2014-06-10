""" Wrap Paragraph by Don Taylor.

A Pydev script for rewrapping the current paragraph to fit inside the print
margin preference in Eclipse (defaults to 80 columns).  A paragraph is a block
of lines with a common leading string such as '# ' or a number of spaces. The
lines in the newly wrapped paragraph will all have the same leading string as
the original paragraph.

Usage: Position cursor inside paragraph to be rewrapped and hit <ctrl+2>, w

Caveats:  Embedded tabs are always replaced by single spaces.
          Does not wrap if the cursor is within the first line of a docstring.
          Wrap Paragraph makes simple assumptions about paragraphs.  Check your
          results, <ctrl-Z> will undo the last rewrap.

Note: Activates with 'w' by default. Edit the constants ACTIVATION_STRING
      and WAIT_FOR_ENTER near the end of this file if this does not suit your
      needs.
      
Version: 0.1.1 - alpha

Date:    May 2006

License: Available under the same conditions as PyDev.  See PyDev license for
         details: http://pydev.sourceforge.net
         
Support: Contact the author for bug reports/feature requests via the Pydev
         users list (or use the source).

History: 20 May 2006 - Initial release.
         21 May 2006 - Changed no of columns wrapped from 80 to the Eclipse
         setting for the print margin preference.

"""
#===============================================================================
# The following is a copy of textwrap.py from the CPython 2.4 standard library
# - slightly modified for Jython 2.1 compatibility.  Included here directly
# instead of as an imported module so that the Wrap Paragraph Jython Pydev
# extension can consist of a single file.  The extension code starts at around
# line 400.
#===============================================================================
"""Text wrapping and filling.
"""

# Copyright (C) 1999-2001 Gregory P. Ward.
# Copyright (C) 2002, 2003 Python Software Foundation.
# Written by Greg Ward <gward@python.net>

__revision__ = "$Id$"


#===============================================================================
# Pydev Extensions in Jython code protocol
#===============================================================================
if False:
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit
    systemGlobals = {}

#---------------------------- REQUIRED LOCALS-----------------------------------
# interface: String indicating which command will be executed As this script
# will be watching the PyEdit (that is the actual editor in Pydev), and this
# script will be listening to it, this string can indicate any of the methods of
# org.python.pydev.editor.IPyEditListener
assert cmd is not None

# interface: PyEdit object: this is the actual editor that we will act upon
assert editor is not None

if cmd == 'onCreateActions':
#----------------------------------Paragrapher----------------------------------
    Paragrapher = systemGlobals.get('Paragrapher')
    if Paragrapher is None:
        class Paragrapher:
            ''' Provides tools to process a paragraph of text in the Pydev editor. 
            
            '''
            def __init__(self, editor):
                self.selection = editor.createPySelection()
                self.document = editor.getDocument()
    
                self.offset = self.selection.getAbsoluteCursorOffset()
                self.currentLineNo = self.selection.getLineOfOffset(self.offset)
    
                self.docDelimiter = self.selection.getDelimiter(self.document)
                self.currentLine = self.selection.getLine(self.currentLineNo)
    
                self.pattern = r'''(\s*#\s*|\s*"""\s*|''' \
                        + r"""\s*'''\s*|""" \
                        + r'''\s*"\s*|''' \
                        + r"""\s*'\s*|\s*)"""
    
                import re
                self.compiledRe = re.compile(self.pattern)
    
                self.leadingString, self.mainText = \
                    self._splitLine(self.currentLine)
    
                self.offsetOfOriginalParagraph = 0
                self.lengthOfOriginalParagraph = 0
    
                self.numberOfLinesInDocument = self.document.getNumberOfLines()
    
            def _splitLine(self, line):
                ''' _splitLine(string: line) -> (string: leadingString,\
                                                string: mainText)
                
                Split the line into two parts - a leading string and the remaining
                text.
                '''
    
                matched = self.compiledRe.match(line)
                leadingString = line[0:matched.end()]
                mainText = line[matched.end():]
    
                return (leadingString, mainText)
    
            def getCurrentLine(self):
                ''' getCurrentLine() -> string
                
                Return the main part of the text of the current line as a string.            
                '''
    
                self.currentLine = self.selection.getLine(self.currentLineNo)
                self.mainText = self._splitLine(self.currentLine)[1]
                return self.mainText
    
            def previousLineIsInParagraph(self):
                ''' previousLineIsInParagraph() -> bool '''
    
                previousLine = self.selection.getLine(self.currentLineNo - 1)
                leadingStringOfPreviousLine, mainTextOfPreviousLine = \
                     self._splitLine(previousLine)
    
                if (self.currentLineNo == 0) | \
                (mainTextOfPreviousLine.strip() == "") | \
                (leadingStringOfPreviousLine != self.leadingString): # diff para [1]
                    line = self.selection.getLine(self.currentLineNo)
                    lineEndsAt = self.selection.getEndLineOffset(self.currentLineNo)
                    self.offsetOfOriginalParagraph = lineEndsAt - len(line)
                    return False
                else:
                    return True # same para
    
                # [1]  The current line is the first line of a paragraph. Calculate
                # starting offset of the first character of the original paragraph.
    
            def nextLineIsInParagraph(self):
                ''' nextLineIsInParagraph() -> bool '''
    
                nextLine = self.selection.getLine(self.currentLineNo + 1)
                leadingStringOfNextLine, mainTextOfNextLine = \
                    self._splitLine(nextLine)
    
                if (self.currentLineNo + 1 == self.numberOfLinesInDocument) | \
                (mainTextOfNextLine.strip() == "") | \
                (leadingStringOfNextLine != self.leadingString): # diff para [1]
                    self.lengthOfOriginalParagraph = \
                        self.selection.getEndLineOffset(self.currentLineNo) - \
                        self.offsetOfOriginalParagraph
                    return False
                else:
                    return True # same para
    
                # [1]  The current line is the last line of a paragraph. Calculate
                # the length of the original paragraph.
        systemGlobals['Paragrapher'] = Paragrapher
        
#------------------------------end of Paragrapher-------------------------------

    WrapParagraph = systemGlobals.get('WrapParagraph')
    if WrapParagraph is None:
        Action = editor.getActionClass() #from org.eclipse.jface.action import Action #@UnresolvedImport
        from java.lang import Runnable #@UnresolvedImport
        
        class WrapParagraph(Action):
            ''' Rewrap the text of the current paragraph.
            
            WrapParagraph searches for the beginning and end of the paragraph that
            contains the selection, rewraps it to fit into 79 character lines and
            replaces the original paragraph with the newly wrapped paragraph.
                
            The current paragraph is the text surrounding the current selection
            point.  
            
            Only one paragraph at a time is wrapped.
            
            A paragraph is a consecutive block of lines whose alphanumeric text all
            begins at the same column position.  Any constant leading string will be
            retained in the newly wrapped paragraph.  This handles indented
            paragraphs and # comment blocks, and avoids wrapping indented code
            examples - but not code samples that are not indented.
            
            The first, or only, line of a docstring is handled as a special case and
            is not wrapped at all.
            
            '''
            
            def __init__(self, editor):
                self.editor = editor
                
                
            def displayStatusMessage(self):
                self.editor.setMessage(False, "Cannot rewrap docstrings")
            
                
            class RunInUi(Runnable):
                '''Helper class that implements a Runnable (just so that we
                can pass it to the Java side). It simply calls some callable.
                '''
        
                def __init__(self, c):
                    self.callable = c
                def run(self):
                    self.callable()

                
            def run(self):
                editor = self.editor
                p = Paragrapher(editor)
    
                # Start building a list of lines of text in paragraph
                paragraph = [p.getCurrentLine()]
    
                isDocstring = (p.leadingString.find('"""') != -1) | \
                        (p.leadingString.find("'") != -1) | \
                        (p.leadingString.find('"') != -1)
                if isDocstring:
                    editor.asyncExec(self.RunInUi(self.displayStatusMessage))
    
                # Don't wrap empty lines or docstrings.
                if ((paragraph[0].strip() != "") & (not isDocstring)):
                    startingLineNo = p.currentLineNo
                    # Add the lines before the line containing the selection.
                    while p.previousLineIsInParagraph():
                        p.currentLineNo -= 1
                        paragraph.insert(0, p.getCurrentLine())
    
                    # Add the lines after the line containing the selection.
                    p.currentLineNo = startingLineNo
                    while p.nextLineIsInParagraph():
                        p.currentLineNo += 1
                        paragraph.append(p.getCurrentLine())
    
                    # paragraph now contains all of the lines so rewrap it [1].
                    noCols = editor.getPrintMarginColums()
                    paragraph = [line.rstrip() + " "  for line in paragraph]
    
                    import textwrap
                    paragraph = textwrap.wrap("".join(paragraph), \
                                     width=noCols - len(p.leadingString), \
                                     expand_tabs=False, \
                                     )
                    # Add line terminators.
                    paragraph = map((lambda aLine: p.leadingString + aLine + \
                                     p.docDelimiter), paragraph)
                    paragraph[-1] = paragraph[-1].replace(p.docDelimiter, "") # [2]
    
                    # Replace original paragraph.
                    p.document.replace(p.offsetOfOriginalParagraph, \
                                       p.lengthOfOriginalParagraph, \
                                       "".join(paragraph))
                    # and we are done.
    
    
            # [1]  paragraph now contains all of the lines of the paragraph to be
            # rewrapped and the lines have all been stripped of their leading
            # strings.
            #
            # Rewrap the paragraph allowing space to insert the leading strings back
            # in again after the wrapping is done. But first we need to make sure
            # that there is at least one space at the end of each line otherwise the
            # wrap routine will combine the last word of one line with the first
            # word of the next line.  We cannot just add a space as this will be
            # kept if there is one there already so strip off any trailing white
            # space first and add back just a single space character.
            #
            # [2]  Add line terminators to the end of every line in paragraph except
            # the last line otherwise the new paragraph will have an extra line
            # terminator at the end.
            
        systemGlobals['WrapParagraph'] = WrapParagraph
        
    # Change these constants if the default does not suit your needs
    ACTIVATION_STRING = 'w'
    WAIT_FOR_ENTER = False

    # Register the extension as an ActionListener.
    editor.addOfflineActionListener(ACTIVATION_STRING, WrapParagraph(editor), \
                                    'Wrap paragraph', \
                                    WAIT_FOR_ENTER)
