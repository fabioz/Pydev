"""Quick Assistant: (un)Wrap expression.

Effect
======
Wrap (or unwrap) a paranthesized expression over multiple lines, using the
standard python coding style (PEP8): wrap on comma, one item per line.

Use case
========
Wrap long def lines, lists, tuples and dict literals for maximum
readability. Or unwrap them again if you decide that looks better.

This assist is also useful when reading code, as it is pretty simple to
wrap a complex expression and immediately see what the items are, and what
part belong to nested expressions. At the same time it's equally simple to
just Undo the wrap when you are done reading and return the code to
pristine condition.

Valid when
==========
The cursor is in a parenthesized (or bracketed, or braced) expression that
contains non-nested commas, but is free from comments or block quote
boundaries. It also works when the cursor is placed "to the left" of the
start of such an expression (eg. on the word "def" on a function def line).

This assist is written to be reasonably independent from surrounding code,
and should work even in code examples within block quotes, or within larger
blocks with syntactically incorrect code, as long as the expression itself
is not affected. Try it! If it doesn't work or doesn't do what you want,
then well, that's what Undo is for.

Installation
============
Place this file in your pydev jython script dir, open a new editor, and you
are ready to go.

See the pydev docs if you don't know where your jython script dir is.

Example
=======
Before:
----------------------------------------------
def func(frobnitz_foobalizer=charge(), gobbledygook=False, sillyizer=True):
goop = [leptonium_masquerader, fontainebleu_sample, murchinson_meteoritics]
DEFS = {'LOG_FEEDER': LOG_FEEDER, 'MAXIMUM_INTEGER_NUMBER': sys.maxint}
----------------------------------------------

After:
----------------------------------------------
def func(frobnitz_foobalizer=charge(),
         gobbledygook=False,
         sillyizer=True):
goop = [leptonium_masquerader,
        fontainebleu_sample,
        murchinson_meteoritics]
DEFS = {'LOG_FEEDER': LOG_FEEDER,
        'MAXIMUM_INTEGER_NUMBER': sys.maxint}
----------------------------------------------

"""
__author__ = """Joel Hedlund <joel at nsc.liu.se>"""

__version__ = "1.0.0"

__copyright__ = '''Available under the same conditions as PyDev.

See PyDev license for details.
http://pydev.sourceforge.net
'''

#
# Boring boilerplate preamble code. This can be safely copied to every pydev
# jython script that you write. The interesting stuff is further down below.
#

# Set to True to do inefficient stuff that is only useful for debugging
# and development purposes. Should always be False if not debugging.
DEBUG_WRAP_EXPRESSION = False

# This is a magic trick that tells the PyDev Extensions editor about the
# namespace provided for pydev scripts:
if False:
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit
assert cmd is not None
assert editor is not None

# We don't need to add the same assist proposal more than once.
if not (cmd == 'onCreateActions' or (DEBUG_WRAP_EXPRESSION and cmd == 'onSave')):
    from org.python.pydev.jython import ExitScriptException #@UnresolvedImport
    raise ExitScriptException()

# We want a fresh interpreter if we're DEBUG_WRAP_EXPRESSIONging this script!
if DEBUG_WRAP_EXPRESSION and cmd == 'onSave':
    from org.python.pydev.jython import JythonPlugin #@UnresolvedImport
    editor.pyEditScripting.interpreter = JythonPlugin.newPythonInterpreter()

#
# Interesting stuff starts here!
#

import re
from assist_proposal import AssistProposal, register_proposal

openers = '([{'
closers = ')]}'
quotes = "\"'"

def skip_over_string_literal(text, offset):
    current_open_quote = text[offset]
    escaped = False
    for pos in range(offset + 1, len(text)):
        char = text[pos]
        if char == '\n':
            break
        elif char == '\\':
            escaped = not escaped
        elif char == current_open_quote and not escaped:
            return pos
        else:
            escaped = False
    if DEBUG_WRAP_EXPRESSION:
        print "no unclosed string literals allowed"
    return -1

def escape_from_string_literal(line, column):
    pos = 0
    while pos < column:
        char = line[pos]
        if char in quotes:
            if line[pos:pos+3] == char * 3:
                if DEBUG_WRAP_EXPRESSION:
                    print "no block quotes allowed"
                return -1
            end = skip_over_string_literal(line, pos)
            if end < 0:
                return -1
            if pos < column <= end:
                return pos
            pos = end
        elif char == '#':
            if DEBUG_WRAP_EXPRESSION:
                print "no comments allowed"
            return -1
        pos += 1
    # We good.
    return column

def check_if_in_expression(selection, offset):
    text = selection.getDoc().get()
    depths = [0] * len(openers)
    pos = offset
    while pos >= 0:
        pos -= 1
        char = text[pos]
        i = openers.find(char)
        if i >= 0:
            depths[i] -= 1
            if depths[i] < 0:
                return pos, openers[i], closers[i]
            continue
        i = closers.find(char)
        if i >= 0:
            depths[i] += 1
            continue
        if pos == offset:
            continue
        i = quotes.find(char)
        if i >= 0:
            line_number = selection.getLineOfOffset(pos)
            line = selection.getLine(line_number)
            line_offset = selection.getLineOffset(line_number)
            col = escape_from_string_literal(line, pos - line_offset)
            if col < 0:
                break
            pos = line_offset + col

def check_for_expression_further_down_same_line(selection, offset):
    line = selection.getCursorLineContents()
    line_number = selection.getCursorLine()
    line_offset = selection.getLineOffset(line_number)
    col = selection.getCursorColumn()
    while col < len(line):
        char = line[col]
        i = openers.find(char)
        if i >= 0:
            return line_offset + col, openers[i], closers[i]
        i = quotes.find(char)
        if i >= 0:
            col = skip_over_string_literal(line, col)
            if col < 0:
                break
        col += 1

def find_expression_opener(selection, offset):
    result = check_if_in_expression(selection, offset)
    if not result:
        result = check_for_expression_further_down_same_line(selection, offset)
    if not result:
        if DEBUG_WRAP_EXPRESSION:
            print "no expression available"
        return None, None, None
    closer_offset, has_commas, is_wrapped = result
    return closer_offset, has_commas, is_wrapped

def get_closer_offset(text, offset, closer):
    opener = text[offset]
    has_commas = False
    is_wrapped_already = True
    pos = offset + 1
    while pos < len(text):
        old_pos = pos
        char = text[pos]
        if char == ',':
            has_commas = True
            if not (text[pos + 1] == '\n' or text[pos + 1:pos + 3] == '\r\n'):
                is_wrapped_already = False
        elif char == closer:
            return pos, has_commas, is_wrapped_already
        elif char in openers:
            pos, _ignored, _ignored = get_closer_offset(text, pos, closers[openers.index(char)])
        elif char in quotes:
            pos = skip_over_string_literal(text, pos)

        if pos < 0:
            break
        if pos < old_pos:
            pos = old_pos # Make sure we always go forward

        pos += 1
    return -1, False, True

class WrapExpression(AssistProposal):
    description = "Wrap expression"
    tag = "WRAP_EXPRESSION"

    def store_data(self, opener_offset, closer_offset, indent, indent_to_par_level):
        self.indent_to_par_level = indent_to_par_level
        self.opener_offset = opener_offset
        self.closer_offset = closer_offset
        self.indent = indent

    def isValid(self, selection, current_line, editor, offset):
        col = selection.getCursorColumn()
        col = escape_from_string_literal(current_line, col)
        if col < 0:
            return False
        opener_offset, opener, closer = find_expression_opener(selection, offset)
        if opener_offset is None:
            return False
        document = selection.getDoc().get()
        closer_offset, has_commas, is_wrapped = get_closer_offset(document, opener_offset, closer)
        if closer_offset < 0:
            if DEBUG_WRAP_EXPRESSION:
                print "expression is not closed"
            return False
        if not has_commas:
            if DEBUG_WRAP_EXPRESSION:
                print "expression does not have commas"
            return False
        if is_wrapped:
            if DEBUG_WRAP_EXPRESSION:
                print "expression is already wrapped"
            return False
        line_offset = selection.getLineOffset(selection.getLineOfOffset(opener_offset))
        prefs = editor.getIndentPrefs()
        if prefs.getIndentToParLevel():
            indent = ' ' * (opener_offset - line_offset + 1)
        else:
            first_line = document[line_offset:opener_offset + 1]
            first_line_indent = selection.getIndentationFromLine(first_line)
            n_extra_indents = 1
            if first_line.strip().startswith('def '):
                n_extra_indents += 1
            indent = first_line_indent + prefs.getIndentationString() * n_extra_indents
        self.store_data(opener_offset, closer_offset, indent, prefs.getIndentToParLevel())
        return True

    def apply(self, document):
        lines = []
        text = document.get()
        opener = text[self.opener_offset]
        closer = text[self.closer_offset]
        previous = self.opener_offset
        depth = 0
        offset = previous + 1
        while offset < self.closer_offset:
            char = text[offset]
            if char == ',':
                lines.append(text[previous + 1:offset + 1].strip())
                previous = offset
            elif char in openers:
                offset, _ignored, _ignored = get_closer_offset(document.get(), offset, closers[openers.index(char)])
            elif char in quotes:
                offset = skip_over_string_literal(text, offset)
            offset += 1
        lines.append(text[previous + 1:self.closer_offset].strip())
        indent = '\n' + self.indent
        if self.indent_to_par_level:
            replacement_text = indent.join(lines)
        else:
            replacement_text = indent + indent.join(lines)
        length = self.closer_offset - self.opener_offset - 1
        document.replace(self.opener_offset + 1, length, replacement_text)

class UnwrapExpression(AssistProposal):
    description = "Unwrap expression"
    tag = "UNWRAP_EXPRESSION"

    def store_data(self,
                   opener_offset,
                   closer_offset):
        self.opener_offset = opener_offset
        self.closer_offset = closer_offset

    def isValid(self, selection, current_line, editor, offset):
        opener_offset, opener, closer = find_expression_opener(selection, offset)
        if opener_offset is None:
            return False
        closer_offset, has_commas, _ignored = get_closer_offset(selection.getDoc().get(), opener_offset, closer)
        if closer_offset < 0:
            if DEBUG_WRAP_EXPRESSION:
                print "expression is not closed"
            return False
        if not has_commas:
            if DEBUG_WRAP_EXPRESSION:
                print "expression does not have commas"
            return False
        if selection.getLineOfOffset(opener_offset) == selection.getLineOfOffset(closer_offset):
            if DEBUG_WRAP_EXPRESSION:
                print "expression is already single line"
            return False
        self.store_data(opener_offset, closer_offset)
        return True

    def apply(self, document):
        lines = []
        text = document.get()[self.opener_offset+1:self.closer_offset]
        replacement_text = re.sub(r'\s*\n *', ' ', text.replace('\r', '')).strip()
        length = self.closer_offset - self.opener_offset - 1
        document.replace(self.opener_offset + 1, length, replacement_text)

register_proposal(WrapExpression(), DEBUG_WRAP_EXPRESSION)
register_proposal(UnwrapExpression(), DEBUG_WRAP_EXPRESSION)
