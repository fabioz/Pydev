from __future__ import nested_scopes # for Jython 2.1 compatibility
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

import string, re

# Do the right thing with boolean values for all known Python versions (so this
# module can be copied to projects that don't depend on Python 2.3, e.g. Optik
# and Docutils).
try:
    True, False #@UndefinedVariable
except NameError:
    (True, False) = (1, 0)

__all__ = ['TextWrapper', 'wrap', 'fill']

# Hardcode the recognized whitespace characters to the US-ASCII whitespace
# characters.  The main reason for doing this is that in ISO-8859-1, 0xa0 is
# non-breaking whitespace, so in certain locales that character winds up in
# string.whitespace.  Respecting string.whitespace in those cases would 1) make
# textwrap treat 0xa0 the same as any other whitespace char, which is clearly
# wrong (it's a *non-breaking* space), 2) possibly cause problems with Unicode,
# since 0xa0 is not in range(128).
_whitespace = '\t\n\x0b\x0c\r '

class TextWrapper:
    """
    Object for wrapping/filling text.  The public interface consists of the
    wrap() and fill() methods; the other methods are just there for subclasses
    to override in order to tweak the default behaviour. If you want to
    completely replace the main wrapping algorithm, you'll probably have to
    override _wrap_chunks().

    Several instance attributes control various aspects of wrapping:
      width (default: 70)
        the maximum width of wrapped lines (unless break_long_words
        is false)
      initial_indent (default: "")
        string that will be prepended to the first line of wrapped
        output.  Counts towards the line's width.
      subsequent_indent (default: "")
        string that will be prepended to all lines save the first
        of wrapped output; also counts towards each line's width.
      expand_tabs (default: true)
        Expand tabs in input text to spaces before further processing.
        Each tab will become 1 .. 8 spaces, depending on its position in
        its line.  If false, each tab is treated as a single character.
      replace_whitespace (default: true)
        Replace all whitespace characters in the input text by spaces
        after tab expansion.  Note that if expand_tabs is false and
        replace_whitespace is true, every tab will be converted to a
        single space!
      fix_sentence_endings (default: false)
        Ensure that sentence-ending punctuation is always followed
        by two spaces.  Off by default because the algorithm is
        (unavoidably) imperfect.
      break_long_words (default: true)
        Break words longer than 'width'.  If false, those words will not
        be broken, and some lines might be longer than 'width'.
    """

    whitespace_trans = string.maketrans(_whitespace, ' ' * len(_whitespace))

    unicode_whitespace_trans = {}
    uspace = ord(u' ')
    for x in map(ord, _whitespace):
        unicode_whitespace_trans[x] = uspace

    # This funky little regex is just the trick for splitting
    # text up into word-wrappable chunks.  E.g.
    #   "Hello there -- you goof-ball, use the -b option!"
    # splits into
    #   Hello/ /there/ /--/ /you/ /goof-/ball,/ /use/ /the/ /-b/ /option!
    # (after stripping out empty strings).
    wordsep_re = re.compile(
        r'(\s+|'                                  # any whitespace
        r'[^\s\w]*\w+[a-zA-Z]-(?=\w+[a-zA-Z])|'   # hyphenated words
        r'(?<=[\w\!\"\'\&\.\,\?])-{2,}(?=\w))')   # em-dash

    # XXX this is not locale- or charset-aware -- string.lowercase
    # is US-ASCII only (and therefore English-only)
    sentence_end_re = re.compile(r'[%s]'              # lowercase letter
                                 r'[\.\!\?]'          # sentence-ending punct.
                                 r'[\"\']?'           # optional end-of-quote
                                 % string.lowercase)


    def __init__(self,
                 width=70,
                 initial_indent="",
                 subsequent_indent="",
                 expand_tabs=True,
                 replace_whitespace=True,
                 fix_sentence_endings=False,
                 break_long_words=True):
        self.width = width
        self.initial_indent = initial_indent
        self.subsequent_indent = subsequent_indent
        self.expand_tabs = expand_tabs
        self.replace_whitespace = replace_whitespace
        self.fix_sentence_endings = fix_sentence_endings
        self.break_long_words = break_long_words


    # -- Private methods -----------------------------------------------
    # (possibly useful for subclasses to override)

    def _munge_whitespace(self, text):
        """_munge_whitespace(text : string) -> string

        Munge whitespace in text: expand tabs and convert all other
        whitespace characters to spaces.  Eg. " foo\tbar\n\nbaz"
        becomes " foo    bar  baz".
        """
        if self.expand_tabs:
            text = text.expandtabs()
        if self.replace_whitespace:
            text = text.translate(self.unicode_whitespace_trans) # (1 - below)
#            if isinstance(text, str):
#                text = text.translate(self.whitespace_trans)
#            elif isinstance(text, unicode):
#                text = text.translate(self.unicode_whitespace_trans)
#            (1) This line replaces the following 4 lines that are commented out
#            because Jython only supports Unicode strings.
        return text


    def _split(self, text):
        """_split(text : string) -> [string]

        Split the text to wrap into indivisible chunks.  Chunks are
        not quite the same as words; see wrap_chunks() for full
        details.  As an example, the text
          Look, goof-ball -- use the -b option!
        breaks into the following chunks:
          'Look,', ' ', 'goof-', 'ball', ' ', '--', ' ',
          'use', ' ', 'the', ' ', '-b', ' ', 'option!'
        """
        chunks = self.wordsep_re.split(text)
        chunks = filter(None, chunks)
        return chunks

    def _fix_sentence_endings(self, chunks):
        """_fix_sentence_endings(chunks : [string])

        Correct for sentence endings buried in 'chunks'.  Eg. when the
        original text contains "... foo.\nBar ...", munge_whitespace()
        and split() will convert that to [..., "foo.", " ", "Bar", ...]
        which has one too few spaces; this method simply changes the one
        space to two.
        """
        i = 0
        pat = self.sentence_end_re
        while i < len(chunks)-1:
            if chunks[i+1] == " " and pat.search(chunks[i]):
                chunks[i+1] = "  "
                i += 2
            else:
                i += 1

    def _handle_long_word(self, reversed_chunks, cur_line, cur_len, width):
        """_handle_long_word(chunks : [string],
                             cur_line : [string],
                             cur_len : int, width : int)

        Handle a chunk of text (most likely a word, not whitespace) that
        is too long to fit in any line.
        """
        space_left = max(width - cur_len, 1)

        # If we're allowed to break long words, then do so: put as much
        # of the next chunk onto the current line as will fit.
        if self.break_long_words:
            cur_line.append(reversed_chunks[-1][:space_left])
            reversed_chunks[-1] = reversed_chunks[-1][space_left:]

        # Otherwise, we have to preserve the long word intact.  Only add
        # it to the current line if there's nothing already there --
        # that minimizes how much we violate the width constraint.
        elif not cur_line:
            cur_line.append(reversed_chunks.pop())

        # If we're not allowed to break long words, and there's already
        # text on the current line, do nothing.  Next time through the
        # main loop of _wrap_chunks(), we'll wind up here again, but
        # cur_len will be zero, so the next line will be entirely
        # devoted to the long word that we can't handle right now.

    def _wrap_chunks(self, chunks):
        """_wrap_chunks(chunks : [string]) -> [string]

        Wrap a sequence of text chunks and return a list of lines of
        length 'self.width' or less.  (If 'break_long_words' is false,
        some lines may be longer than this.)  Chunks correspond roughly
        to words and the whitespace between them: each chunk is
        indivisible (modulo 'break_long_words'), but a line break can
        come between any two chunks.  Chunks should not have internal
        whitespace; ie. a chunk is either all whitespace or a "word".
        Whitespace chunks will be removed from the beginning and end of
        lines, but apart from that whitespace is preserved.
        """
        lines = []
        if self.width <= 0:
            raise ValueError("invalid width %r (must be > 0)" % self.width)

        # Arrange in reverse order so items can be efficiently popped
        # from a stack of chucks.
        chunks.reverse()

        while chunks:

            # Start the list of chunks that will make up the current line.
            # cur_len is just the length of all the chunks in cur_line.
            cur_line = []
            cur_len = 0

            # Figure out which static string will prefix this line.
            if lines:
                indent = self.subsequent_indent
            else:
                indent = self.initial_indent

            # Maximum width for this line.
            width = self.width - len(indent)

            # First chunk on line is whitespace -- drop it, unless this
            # is the very beginning of the text (ie. no lines started yet).
            if chunks[-1].strip() == '' and lines:
                del chunks[-1]

            while chunks:
                l = len(chunks[-1])

                # Can at least squeeze this chunk onto the current line.
                if cur_len + l <= width:
                    cur_line.append(chunks.pop())
                    cur_len += l

                # Nope, this line is full.
                else:
                    break

            # The current line is full, and the next chunk is too big to
            # fit on *any* line (not just this one).
            if chunks and len(chunks[-1]) > width:
                self._handle_long_word(chunks, cur_line, cur_len, width)

            # If the last chunk on this line is all whitespace, drop it.
            if cur_line and cur_line[-1].strip() == '':
                del cur_line[-1]

            # Convert current line back to a string and store it in list
            # of all lines (return value).
            if cur_line:
                lines.append(indent + ''.join(cur_line))

        return lines


    # -- Public interface ----------------------------------------------

    def wrap(self, text):
        """wrap(text : string) -> [string]

        Reformat the single paragraph in 'text' so it fits in lines of
        no more than 'self.width' columns, and return a list of wrapped
        lines.  Tabs in 'text' are expanded with string.expandtabs(),
        and all other whitespace characters (including newline) are
        converted to space.
        """
        text = self._munge_whitespace(text)
        chunks = self._split(text)
        if self.fix_sentence_endings:
            self._fix_sentence_endings(chunks)
        return self._wrap_chunks(chunks)

    def fill(self, text):
        """fill(text : string) -> string

        Reformat the single paragraph in 'text' to fit in lines of no
        more than 'self.width' columns, and return a new string
        containing the entire wrapped paragraph.
        """
        return "\n".join(self.wrap(text))


# -- Convenience interface ---------------------------------------------

def wrap(text, width=70, **kwargs):
    """Wrap a single paragraph of text, returning a list of wrapped lines.

    Reformat the single paragraph in 'text' so it fits in lines of no
    more than 'width' columns, and return a list of wrapped lines.  By
    default, tabs in 'text' are expanded with string.expandtabs(), and
    all other whitespace characters (including newline) are converted to
    space.  See TextWrapper class for available keyword args to customize
    wrapping behaviour.
    """
    w = TextWrapper(width=width, **kwargs)
    return w.wrap(text)

def fill(text, width=70, **kwargs):
    """Fill a single paragraph of text, returning a new string.

    Reformat the single paragraph in 'text' to fit in lines of no more
    than 'width' columns, and return a new string containing the entire
    wrapped paragraph.  As with wrap(), tabs are expanded and other
    whitespace characters converted to space.  See TextWrapper class for
    available keyword args to customize wrapping behaviour.
    """
    w = TextWrapper(width=width, **kwargs)
    return w.fill(text)


# -- Loosely related functionality -------------------------------------

def dedent(text):
    """dedent(text : string) -> string

    Remove any whitespace than can be uniformly removed from the left
    of every line in `text`.

    This can be used e.g. to make triple-quoted strings line up with
    the left edge of screen/whatever, while still presenting it in the
    source code in indented form.

    For example:

        def test():
            # end first line with \ to avoid the empty line!
            s = '''\
            hello
              world
            '''
            print repr(s)          # prints '    hello\n      world\n    '
            print repr(dedent(s))  # prints 'hello\n  world\n'
    """
    lines = text.expandtabs().split('\n')
    margin = None
    for line in lines:
        content = line.lstrip()
        if not content:
            continue
        indent = len(line) - len(content)
        if margin is None:
            margin = indent
        else:
            margin = min(margin, indent)

    if margin is not None and margin > 0:
        for i in range(len(lines)):
            lines[i] = lines[i][margin:]

    return '\n'.join(lines)

#===============================================================================
# End of copy of textwrap.py
#===============================================================================
#===============================================================================
# Pydev Extensions in Jython code protocol
#=============================================================================== 
True, False = 1,0
if False:
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit

#---------------------------- REQUIRED LOCALS-----------------------------------
# interface: String indicating which command will be executed As this script
# will be watching the PyEdit (that is the actual editor in Pydev), and this
# script will be listening to it, this string can indicate any of the methods of
# org.python.pydev.editor.IPyEditListener
assert cmd is not None 

# interface: PyEdit object: this is the actual editor that we will act upon
assert editor is not None
     
if cmd == 'onCreateActions':
    from org.eclipse.jface.action import Action #@UnresolvedImport
    from org.python.pydev.core.docutils import PySelection #@UnresolvedImport
    from org.eclipse.ui.texteditor import IEditorStatusLine #@UnresolvedImport
    from org.eclipse.swt.widgets import Display #@UnresolvedImport
    from java.lang import Runnable #@UnresolvedImport
    from org.python.pydev.plugin.preferences import PydevPrefs #@UnresolvedImport
    from org.eclipse.ui.texteditor import AbstractDecoratedTextEditorPreferenceConstants #@UnresolvedImport
#------------------------ HELPER TO RUN THINGS IN THE UI -----------------------

    class RunInUi(Runnable):
        '''Helper class that implements a Runnable (just so that we
        can pass it to the Java side). It simply calls some callable.
        '''
       
        def __init__(self, c):
            self.callable = c
        def run(self):
            self.callable ()
           
    def runInUi(callable):
        '''
        @param callable: the callable that will be run in the UI
        '''
        Display.getDefault().asyncExec(RunInUi(callable))
       
#---------------------- END HELPER TO RUN THINGS IN THE UI ---------------------    


#----------------------------------Paragrapher----------------------------------
    class Paragrapher:
        ''' Provides tools to process a paragraph of text in the Pydev editor. 
        
        '''
        def __init__(self):
          
            self.selection = PySelection(editor)
            self.document = editor.getDocument()
            
            self.offset = self.selection.getAbsoluteCursorOffset()
            self.currentLineNo = self.selection.getLineOfOffset(self.offset)

            self.docDelimiter = self.selection.getDelimiter(self.document) 
            self.currentLine = self.selection.getLine(self.currentLineNo)
            
            self.pattern = r'''(\s*#\s*|\s*"""\s*|''' \
                    + r"""\s*'''\s*|""" \
                    + r'''\s*"\s*|''' \
                    + r"""\s*'\s*|\s*)"""
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
                           
            if (self.currentLineNo == 0) |\
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
                    
#------------------------------end of Paragrapher------------------------------- 
    
    class wrapParagraph(Action):
        ''' Rewrap the text of the current paragraph.
        
        wrapParagraph searches for the beginning and end of the paragraph that
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
        def run(self):
                                
            def displayStatusMessage():
                ''' Displays the message in 'statusMessage' in the Eclipse
                status area.
                
                Uses runInUi so that it runs in the Eclispe UI thread otherwise
                it won't show up.  Called thus:
                    statusMessage = "Cannot rewrap docstrings"
                    runInUi(displayStatusMessage)
                                
                '''
                
                statusLine = editor.getAdapter(IEditorStatusLine)
                if statusLine is not None:
                    statusLine.setMessage(False, statusMessage, None)
                                                               
            p = Paragrapher()
 
            # Start building a list of lines of text in paragraph
            paragraph = [p.getCurrentLine()] 
                                
            isDocstring = (p.leadingString.find('"""') != -1) |\
                    (p.leadingString.find("'") != -1) |\
                    (p.leadingString.find('"') != -1)
            if isDocstring:
                statusMessage = "Cannot rewrap docstrings"
                runInUi(displayStatusMessage)
                                
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
                noCols = PydevPrefs.getChainedPrefStore().\
                         getInt(AbstractDecoratedTextEditorPreferenceConstants.\
                         EDITOR_PRINT_MARGIN_COLUMN )
                paragraph = [line.rstrip() + " "  for line in paragraph]
                paragraph = wrap("".join(paragraph), \
                                 width = noCols - len(p.leadingString), \
                                 expand_tabs = False, \
                                 )
                # Add line terminators.
                paragraph = map((lambda aLine: p.leadingString + aLine + \
                                 p.docDelimiter), paragraph)
                paragraph[-1] = paragraph[-1].replace(p.docDelimiter,"") # [2]

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
        
    # Change these constants if the default does not suit your needs
    ACTIVATION_STRING = 'w'
    WAIT_FOR_ENTER = False
    
    # Register the extension as an ActionListener.    
    editor.addOfflineActionListener(ACTIVATION_STRING, wrapParagraph(),\
                                    'Wrap paragraph',\
                                    WAIT_FOR_ENTER)