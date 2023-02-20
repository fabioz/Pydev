# coding: utf8
# Note: this is cog just remodelled to be in a single file!
""" Cog content generation tool.
    http://nedbatchelder.com/code/cog

    Copyright 2004-2021, Ned Batchelder.
"""

from __future__ import absolute_import, print_function

import copy
import getopt
import glob
import hashlib
import linecache
import os
import re
import shlex
import sys
import traceback


PY3 = sys.version_info[0] == 3
assert PY3

string_types = (str,bytes)
bytes_types = (bytes,)
def to_bytes(s):
    return s.encode('utf8')
from io import StringIO


__all__ = ['Cog', 'CogUsageError', 'main']

__version__ = '3.3.0'

usage = """\
cog - generate content with inlined Python code.

cog [OPTIONS] [INFILE | @FILELIST] ...

INFILE is the name of an input file, '-' will read from stdin.
FILELIST is the name of a text file containing file names or
    other @FILELISTs.

OPTIONS:
    -c          Checksum the output to protect it against accidental change.
    -d          Delete the generator code from the output file.
    -D name=val Define a global string available to your generator code.
    -e          Warn if a file has no cog code in it.
    -I PATH     Add PATH to the list of directories for data files and modules.
    -n ENCODING Use ENCODING when reading and writing files.
    -o OUTNAME  Write the output to OUTNAME.
    -p PROLOGUE Prepend the generator source with PROLOGUE. Useful to insert an
                import line. Example: -p "import math"
    -P          Use print() instead of cog.outl() for code output.
    -r          Replace the input file with the output.
    -s STRING   Suffix all generated output lines with STRING.
    -U          Write the output with Unix newlines (only LF line-endings).
    -w CMD      Use CMD if the output file needs to be made writable.
                    A %s in the CMD will be filled with the filename.
    -x          Excise all the generated output without running the generators.
    -z          The end-output marker can be omitted, and is assumed at eof.
    -v          Print the version of cog and exit.
    --check     Check that the files would not change if run again.
    --markers='START END END-OUTPUT'
                The patterns surrounding cog inline instructions. Should
                include three values separated by spaces, the start, end,
                and end-output markers. Defaults to '[[[cog ]]] [[[end]]]'.
    --verbosity=VERBOSITY
                Control the amount of output. 2 (the default) lists all files,
                1 lists only changed files, 0 lists no files.
    -h          Print this help.
"""

# Other package modules
def whitePrefix(strings):
    """ Determine the whitespace prefix common to all non-blank lines
        in the argument list.
    """
    # Remove all blank lines from the list
    strings = [s for s in strings if s.strip() != '']

    if not strings: return ''

    # Find initial whitespace chunk in the first line.
    # This is the best prefix we can hope for.
    pat = r'\s*'
    if isinstance(strings[0], bytes_types):
        pat = to_bytes(pat)
    prefix = re.match(pat, strings[0]).group(0)

    # Loop over the other strings, keeping only as much of
    # the prefix as matches each string.
    for s in strings:
        for i in range(len(prefix)):
            if prefix[i] != s[i]:
                prefix = prefix[:i]
                break
    return prefix

def reindentBlock(lines, newIndent=''):
    """ Take a block of text as a string or list of lines.
        Remove any common whitespace indentation.
        Re-indent using newIndent, and return it as a single string.
    """
    sep, nothing = '\n', ''
    if isinstance(lines, bytes_types):
        sep, nothing = b'\n', b''
    if isinstance(lines, string_types):
        lines = lines.split(sep)
    oldIndent = whitePrefix(lines)
    outLines = []
    for l in lines:
        if oldIndent:
            l = l.replace(oldIndent, nothing, 1)
        if l and newIndent:
            l = newIndent + l
        outLines.append(l)
    return sep.join(outLines)

def commonPrefix(strings):
    """ Find the longest string that is a prefix of all the strings.
    """
    if not strings:
        return ''
    prefix = strings[0]
    for s in strings:
        if len(s) < len(prefix):
            prefix = prefix[:len(s)]
        if not prefix:
            return ''
        for i in range(len(prefix)):
            if prefix[i] != s[i]:
                prefix = prefix[:i]
                break
    return prefix


class CogError(Exception):
    """ Any exception raised by Cog.
    """
    def __init__(self, msg, file='', line=0):
        if file:
            Exception.__init__(self, "%s(%d): %s" % (file, line, msg))
        else:
            Exception.__init__(self, msg)

class CogUsageError(CogError):
    """ An error in usage of command-line arguments in cog.
    """
    pass

class CogInternalError(CogError):
    """ An error in the coding of Cog. Should never happen.
    """
    pass

class CogGeneratedError(CogError):
    """ An error raised by a user's cog generator.
    """
    pass

class CogUserException(CogError):
    """ An exception caught when running a user's cog generator.
        The argument is the traceback message to print.
    """
    pass

class CogCheckFailed(CogError):
    """ A --check failed.
    """
    pass

class Redirectable:
    """ An object with its own stdout and stderr files.
    """
    def __init__(self):
        self.stdout = sys.stdout
        self.stderr = sys.stderr

    def setOutput(self, stdout=None, stderr=None):
        """ Assign new files for standard out and/or standard error.
        """
        if stdout:
            self.stdout = stdout
        if stderr:
            self.stderr = stderr

    def prout(self, s, end="\n"):
        print(s, file=self.stdout, end=end)

    def prerr(self, s, end="\n"):
        print(s, file=self.stderr, end=end)


class CogGenerator(Redirectable):
    """ A generator pulled from a source file.
    """
    def __init__(self, options=None):
        Redirectable.__init__(self)
        self.markers = []
        self.lines = []
        self.options = options or CogOptions()

    def parseMarker(self, l):
        self.markers.append(l)

    def parseLine(self, l):
        self.lines.append(l.strip('\n'))

    def getCode(self):
        """ Extract the executable Python code from the generator.
        """
        # If the markers and lines all have the same prefix
        # (end-of-line comment chars, for example),
        # then remove it from all the lines.
        prefIn = commonPrefix(self.markers + self.lines)
        if prefIn:
            self.markers = [ l.replace(prefIn, '', 1) for l in self.markers ]
            self.lines = [ l.replace(prefIn, '', 1) for l in self.lines ]

        return reindentBlock(self.lines, '')

    def evaluate(self, cog, globals, fname):
        # figure out the right whitespace prefix for the output
        prefOut = whitePrefix(self.markers)

        intext = self.getCode()
        if not intext:
            return ''

        prologue = "import " + cog.cogmodulename + " as cog\n"
        if self.options.sPrologue:
            prologue += self.options.sPrologue + '\n'
        code = compile(prologue + intext, str(fname), 'exec')

        # Make sure the "cog" module has our state.
        cog.cogmodule.msg = self.msg
        cog.cogmodule.out = self.out
        cog.cogmodule.outl = self.outl
        cog.cogmodule.error = self.error

        real_stdout = sys.stdout
        if self.options.bPrintOutput:
            sys.stdout = captured_stdout = StringIO()

        self.outstring = ''
        try:
            eval(code, globals)
        except CogError:
            raise
        except:
            typ, err, tb = sys.exc_info()
            frames = (tuple(fr) for fr in traceback.extract_tb(tb.tb_next))
            frames = find_cog_source(frames, prologue)
            msg = "".join(traceback.format_list(frames))
            msg += "{}: {}".format(typ.__name__, err)
            raise CogUserException(msg)
        finally:
            sys.stdout = real_stdout

        if self.options.bPrintOutput:
            self.outstring = captured_stdout.getvalue()

        # We need to make sure that the last line in the output
        # ends with a newline, or it will be joined to the
        # end-output line, ruining cog's idempotency.
        if self.outstring and self.outstring[-1] != '\n':
            self.outstring += '\n'

        return reindentBlock(self.outstring, prefOut)

    def msg(self, s):
        self.prout("Message: "+s)

    def out(self, sOut='', dedent=False, trimblanklines=False):
        """ The cog.out function.
        """
        if trimblanklines and ('\n' in sOut):
            lines = sOut.split('\n')
            if lines[0].strip() == '':
                del lines[0]
            if lines and lines[-1].strip() == '':
                del lines[-1]
            sOut = '\n'.join(lines)+'\n'
        if dedent:
            sOut = reindentBlock(sOut)
        self.outstring += sOut

    def outl(self, sOut='', **kw):
        """ The cog.outl function.
        """
        self.out(sOut, **kw)
        self.out('\n')

    def error(self, msg='Error raised by cog generator.'):
        """ The cog.error function.
            Instead of raising standard python errors, cog generators can use
            this function.  It will display the error without a scary Python
            traceback.
        """
        raise CogGeneratedError(msg)


class NumberedFileReader:
    """ A decorator for files that counts the readline()'s called.
    """
    def __init__(self, f):
        self.f = f
        self.n = 0

    def readline(self):
        l = self.f.readline()
        if l:
            self.n += 1
        return l

    def linenumber(self):
        return self.n


class CogOptions:
    """ Options for a run of cog.
    """
    def __init__(self):
        # Defaults for argument values.
        self.args = []
        self.includePath = []
        self.defines = {}
        self.bShowVersion = False
        self.sMakeWritableCmd = None
        self.bReplace = False
        self.bNoGenerate = False
        self.sOutputName = None
        self.bWarnEmpty = False
        self.bHashOutput = False
        self.bDeleteCode = False
        self.bEofCanBeEnd = False
        self.sSuffix = None
        self.bNewlines = False
        self.sBeginSpec = '[[[cog'
        self.sEndSpec = ']]]'
        self.sEndOutput = '[[[end]]]'
        self.sEncoding = "utf-8"
        self.verbosity = 2
        self.sPrologue = ''
        self.bPrintOutput = False
        self.bCheck = False

    def __eq__(self, other):
        """ Comparison operator for tests to use.
        """
        return self.__dict__ == other.__dict__

    def clone(self):
        """ Make a clone of these options, for further refinement.
        """
        return copy.deepcopy(self)

    def addToIncludePath(self, dirs):
        """ Add directories to the include path.
        """
        dirs = dirs.split(os.pathsep)
        self.includePath.extend(dirs)

    def parseArgs(self, argv):
        # Parse the command line arguments.
        try:
            opts, self.args = getopt.getopt(
                argv,
                'cdD:eI:n:o:rs:p:PUvw:xz',
                [
                    'check',
                    'markers=',
                    'verbosity=',
                ]
            )
        except getopt.error as msg:
            raise CogUsageError(msg)

        # Handle the command line arguments.
        for o, a in opts:
            if o == '-c':
                self.bHashOutput = True
            elif o == '-d':
                self.bDeleteCode = True
            elif o == '-D':
                if a.count('=') < 1:
                    raise CogUsageError("-D takes a name=value argument")
                name, value = a.split('=', 1)
                self.defines[name] = value
            elif o == '-e':
                self.bWarnEmpty = True
            elif o == '-I':
                self.addToIncludePath(os.path.abspath(a))
            elif o == '-n':
                self.sEncoding = a
            elif o == '-o':
                self.sOutputName = a
            elif o == '-r':
                self.bReplace = True
            elif o == '-s':
                self.sSuffix = a
            elif o == '-p':
                self.sPrologue = a
            elif o == '-P':
                self.bPrintOutput = True
            elif o == '-U':
                self.bNewlines = True
            elif o == '-v':
                self.bShowVersion = True
            elif o == '-w':
                self.sMakeWritableCmd = a
            elif o == '-x':
                self.bNoGenerate = True
            elif o == '-z':
                self.bEofCanBeEnd = True
            elif o == '--check':
                self.bCheck = True
            elif o == '--markers':
                self._parse_markers(a)
            elif o == '--verbosity':
                self.verbosity = int(a)
            else:
                # Since getopt.getopt is given a list of possible flags,
                # this is an internal error.
                raise CogInternalError("Don't understand argument %s" % o)

    def _parse_markers(self, val):
        try:
            self.sBeginSpec, self.sEndSpec, self.sEndOutput = val.split(' ')
        except ValueError:
            raise CogUsageError(
                '--markers requires 3 values separated by spaces, could not parse %r' % val
            )

    def validate(self):
        """ Does nothing if everything is OK, raises CogError's if it's not.
        """
        if self.bReplace and self.bDeleteCode:
            raise CogUsageError("Can't use -d with -r (or you would delete all your source!)")

        if self.bReplace and self.sOutputName:
            raise CogUsageError("Can't use -o with -r (they are opposites)")


class Cog(Redirectable):
    """ The Cog engine.
    """
    def __init__(self):
        Redirectable.__init__(self)
        self.options = CogOptions()
        self._fixEndOutputPatterns()
        self.cogmodulename = "cog"
        self.createCogModule()
        self.bCheckFailed = False

    def _fixEndOutputPatterns(self):
        end_output = re.escape(self.options.sEndOutput)
        self.reEndOutput = re.compile(end_output + r'(?P<hashsect> *\(checksum: (?P<hash>[a-f0-9]+)\))')
        self.sEndFormat = self.options.sEndOutput + ' (checksum: %s)'

    def showWarning(self, msg):
        self.prout("Warning: "+msg)

    def isBeginSpecLine(self, s):
        return self.options.sBeginSpec in s

    def isEndSpecLine(self, s):
        return self.options.sEndSpec in s and not self.isEndOutputLine(s)

    def isEndOutputLine(self, s):
        return self.options.sEndOutput in s

    def createCogModule(self):
        """ Make a cog "module" object so that imported Python modules
            can say "import cog" and get our state.
        """
        class DummyModule(object):
            """Modules don't have to be anything special, just an object will do."""
            pass
        self.cogmodule = DummyModule()
        self.cogmodule.path = []

    def openOutputFile(self, fname):
        """ Open an output file, taking all the details into account.
        """
        opts = {}
        mode = "w"
        if PY3:
            opts['encoding'] = self.options.sEncoding
        if self.options.bNewlines:
            if PY3:
                opts['newline'] = "\n"
            else:
                mode = "wb"
        fdir = os.path.dirname(fname)
        if os.path.dirname(fdir) and not os.path.exists(fdir):
            os.makedirs(fdir)
        return open(fname, mode, **opts)

    def openInputFile(self, fname):
        """ Open an input file. """
        if fname == "-":
            return sys.stdin
        else:
            opts = {}
            if PY3:
                opts['encoding'] = self.options.sEncoding
            return open(fname, "r", **opts)

    def processFile(self, fIn, fOut, fname=None, globals=None):
        """ Process an input file object to an output file object.
            fIn and fOut can be file objects, or file names.
        """

        sFileIn = fname or ''
        sFileOut = fname or ''
        fInToClose = fOutToClose = None
        # Convert filenames to files.
        if isinstance(fIn, string_types):
            # Open the input file.
            sFileIn = fIn
            fIn = fInToClose = self.openInputFile(fIn)
        if isinstance(fOut, string_types):
            # Open the output file.
            sFileOut = fOut
            fOut = fOutToClose = self.openOutputFile(fOut)

        try:
            fIn = NumberedFileReader(fIn)

            bSawCog = False

            self.cogmodule.inFile = sFileIn
            self.cogmodule.outFile = sFileOut
            self.cogmodulename = 'cog_' + hashlib.md5(sFileOut.encode()).hexdigest()
            sys.modules[self.cogmodulename] = self.cogmodule
            # if "import cog" explicitly done in code by user, note threading will cause clashes.
            sys.modules['cog'] = self.cogmodule

            # The globals dict we'll use for this file.
            if globals is None:
                globals = {}

            # If there are any global defines, put them in the globals.
            globals.update(self.options.defines)

            # loop over generator chunks
            l = fIn.readline()
            while l:
                # Find the next spec begin
                while l and not self.isBeginSpecLine(l):
                    if self.isEndSpecLine(l):
                        raise CogError("Unexpected '%s'" % self.options.sEndSpec,
                            file=sFileIn, line=fIn.linenumber())
                    if self.isEndOutputLine(l):
                        raise CogError("Unexpected '%s'" % self.options.sEndOutput,
                            file=sFileIn, line=fIn.linenumber())
                    fOut.write(l)
                    l = fIn.readline()
                if not l:
                    break
                if not self.options.bDeleteCode:
                    fOut.write(l)

                # l is the begin spec
                gen = CogGenerator(options=self.options)
                gen.setOutput(stdout=self.stdout)
                gen.parseMarker(l)
                firstLineNum = fIn.linenumber()
                self.cogmodule.firstLineNum = firstLineNum

                # If the spec begin is also a spec end, then process the single
                # line of code inside.
                if self.isEndSpecLine(l):
                    beg = l.find(self.options.sBeginSpec)
                    end = l.find(self.options.sEndSpec)
                    if beg > end:
                        raise CogError("Cog code markers inverted",
                            file=sFileIn, line=firstLineNum)
                    else:
                        sCode = l[beg+len(self.options.sBeginSpec):end].strip()
                        gen.parseLine(sCode)
                else:
                    # Deal with an ordinary code block.
                    l = fIn.readline()

                    # Get all the lines in the spec
                    while l and not self.isEndSpecLine(l):
                        if self.isBeginSpecLine(l):
                            raise CogError("Unexpected '%s'" % self.options.sBeginSpec,
                                file=sFileIn, line=fIn.linenumber())
                        if self.isEndOutputLine(l):
                            raise CogError("Unexpected '%s'" % self.options.sEndOutput,
                                file=sFileIn, line=fIn.linenumber())
                        if not self.options.bDeleteCode:
                            fOut.write(l)
                        gen.parseLine(l)
                        l = fIn.readline()
                    if not l:
                        raise CogError(
                            "Cog block begun but never ended.",
                            file=sFileIn, line=firstLineNum)

                    if not self.options.bDeleteCode:
                        fOut.write(l)
                    gen.parseMarker(l)

                l = fIn.readline()

                # Eat all the lines in the output section.  While reading past
                # them, compute the md5 hash of the old output.
                previous = ""
                hasher = hashlib.md5()
                while l and not self.isEndOutputLine(l):
                    if self.isBeginSpecLine(l):
                        raise CogError("Unexpected '%s'" % self.options.sBeginSpec,
                            file=sFileIn, line=fIn.linenumber())
                    if self.isEndSpecLine(l):
                        raise CogError("Unexpected '%s'" % self.options.sEndSpec,
                            file=sFileIn, line=fIn.linenumber())
                    previous += l
                    hasher.update(to_bytes(l))
                    l = fIn.readline()
                curHash = hasher.hexdigest()

                if not l and not self.options.bEofCanBeEnd:
                    # We reached end of file before we found the end output line.
                    raise CogError("Missing '%s' before end of file." % self.options.sEndOutput,
                        file=sFileIn, line=fIn.linenumber())

                # Make the previous output available to the current code
                self.cogmodule.previous = previous

                # Write the output of the spec to be the new output if we're
                # supposed to generate code.
                hasher = hashlib.md5()
                if not self.options.bNoGenerate:
                    sFile = "<cog %s:%d>" % (sFileIn, firstLineNum)
                    sGen = gen.evaluate(cog=self, globals=globals, fname=sFile)
                    sGen = self.suffixLines(sGen)
                    hasher.update(to_bytes(sGen))
                    fOut.write(sGen)
                newHash = hasher.hexdigest()

                bSawCog = True

                # Write the ending output line
                hashMatch = self.reEndOutput.search(l)
                if self.options.bHashOutput:
                    if hashMatch:
                        oldHash = hashMatch.groupdict()['hash']
                        if oldHash != curHash:
                            raise CogError("Output has been edited! Delete old checksum to unprotect.",
                                file=sFileIn, line=fIn.linenumber())
                        # Create a new end line with the correct hash.
                        endpieces = l.split(hashMatch.group(0), 1)
                    else:
                        # There was no old hash, but we want a new hash.
                        endpieces = l.split(self.options.sEndOutput, 1)
                    l = (self.sEndFormat % newHash).join(endpieces)
                else:
                    # We don't want hashes output, so if there was one, get rid of
                    # it.
                    if hashMatch:
                        l = l.replace(hashMatch.groupdict()['hashsect'], '', 1)

                if not self.options.bDeleteCode:
                    fOut.write(l)
                l = fIn.readline()

            if not bSawCog and self.options.bWarnEmpty:
                self.showWarning("no cog code found in %s" % sFileIn)
        finally:
            if fInToClose:
                fInToClose.close()
            if fOutToClose:
                fOutToClose.close()


    # A regex for non-empty lines, used by suffixLines.
    reNonEmptyLines = re.compile(r"^\s*\S+.*$", re.MULTILINE)

    def suffixLines(self, text):
        """ Add suffixes to the lines in text, if our options desire it.
            text is many lines, as a single string.
        """
        if self.options.sSuffix:
            # Find all non-blank lines, and add the suffix to the end.
            repl = r"\g<0>" + self.options.sSuffix.replace('\\', '\\\\')
            text = self.reNonEmptyLines.sub(repl, text)
        return text

    def processString(self, sInput, fname=None):
        """ Process sInput as the text to cog.
            Return the cogged output as a string.
        """
        fOld = StringIO(sInput)
        fNew = StringIO()
        self.processFile(fOld, fNew, fname=fname)
        return fNew.getvalue()

    def replaceFile(self, sOldPath, sNewText):
        """ Replace file sOldPath with the contents sNewText
        """
        if not os.access(sOldPath, os.W_OK):
            # Need to ensure we can write.
            if self.options.sMakeWritableCmd:
                # Use an external command to make the file writable.
                cmd = self.options.sMakeWritableCmd.replace('%s', sOldPath)
                self.stdout.write(os.popen(cmd).read())
                if not os.access(sOldPath, os.W_OK):
                    raise CogError("Couldn't make %s writable" % sOldPath)
            else:
                # Can't write!
                raise CogError("Can't overwrite %s" % sOldPath)
        f = self.openOutputFile(sOldPath)
        f.write(sNewText)
        f.close()

    def saveIncludePath(self):
        self.savedInclude = self.options.includePath[:]
        self.savedSysPath = sys.path[:]

    def restoreIncludePath(self):
        self.options.includePath = self.savedInclude
        self.cogmodule.path = self.options.includePath
        sys.path = self.savedSysPath

    def addToIncludePath(self, includePath):
        self.cogmodule.path.extend(includePath)
        sys.path.extend(includePath)

    def processOneFile(self, sFile):
        """ Process one filename through cog.
        """

        self.saveIncludePath()
        bNeedNewline = False

        try:
            self.addToIncludePath(self.options.includePath)
            # Since we know where the input file came from,
            # push its directory onto the include path.
            self.addToIncludePath([os.path.dirname(sFile)])

            # How we process the file depends on where the output is going.
            if self.options.sOutputName:
                self.processFile(sFile, self.options.sOutputName, sFile)
            elif self.options.bReplace or self.options.bCheck:
                # We want to replace the cog file with the output,
                # but only if they differ.
                verb = "Cogging" if self.options.bReplace else "Checking"
                if self.options.verbosity >= 2:
                    self.prout("%s %s" % (verb, sFile), end="")
                    bNeedNewline = True

                try:
                    fOldFile = self.openInputFile(sFile)
                    sOldText = fOldFile.read()
                    fOldFile.close()
                    sNewText = self.processString(sOldText, fname=sFile)
                    if sOldText != sNewText:
                        if self.options.verbosity >= 1:
                            if self.options.verbosity < 2:
                                self.prout("%s %s" % (verb, sFile), end="")
                            self.prout("  (changed)")
                            bNeedNewline = False
                        if self.options.bReplace:
                            self.replaceFile(sFile, sNewText)
                        else:
                            assert self.options.bCheck
                            self.bCheckFailed = True
                finally:
                    # The try-finally block is so we can print a partial line
                    # with the name of the file, and print (changed) on the
                    # same line, but also make sure to break the line before
                    # any traceback.
                    if bNeedNewline:
                        self.prout("")
            else:
                self.processFile(sFile, self.stdout, sFile)
        finally:
            self.restoreIncludePath()

    def processWildcards(self, sFile):
        files = glob.glob(sFile)
        if files:
            for sMatchingFile in files:
                self.processOneFile(sMatchingFile)
        else:
            self.processOneFile(sFile)

    def processFileList(self, sFileList):
        """ Process the files in a file list.
        """
        flist = self.openInputFile(sFileList)
        lines = flist.readlines()
        flist.close()
        for l in lines:
            # Use shlex to parse the line like a shell.
            lex = shlex.shlex(l, posix=True)
            lex.whitespace_split = True
            lex.commenters = '#'
            # No escapes, so that backslash can be part of the path
            lex.escape = ''
            args = list(lex)
            if args:
                self.processArguments(args)

    def processArguments(self, args):
        """ Process one command-line.
        """
        saved_options = self.options
        self.options = self.options.clone()

        self.options.parseArgs(args[1:])
        self.options.validate()

        if args[0][0] == '@':
            if self.options.sOutputName:
                raise CogUsageError("Can't use -o with @file")
            self.processFileList(args[0][1:])
        else:
            self.processWildcards(args[0])

        self.options = saved_options

    def callableMain(self, argv):
        """ All of command-line cog, but in a callable form.
            This is used by main.
            argv is the equivalent of sys.argv.
        """
        argv = argv[1:]

        # Provide help if asked for anywhere in the command line.
        if '-?' in argv or '-h' in argv:
            self.prerr(usage, end="")
            return

        self.options.parseArgs(argv)
        self.options.validate()
        self._fixEndOutputPatterns()

        if self.options.bShowVersion:
            self.prout("Cog version %s" % __version__)
            return

        if self.options.args:
            for a in self.options.args:
                self.processArguments([a])
        else:
            raise CogUsageError("No files to process")

        if self.bCheckFailed:
            raise CogCheckFailed("Check failed")

    def main(self, argv):
        """ Handle the command-line execution for cog.
        """

        try:
            self.callableMain(argv)
            return 0
        except CogUsageError as err:
            self.prerr(err)
            self.prerr("(for help use -h)")
            return 2
        except CogGeneratedError as err:
            self.prerr("Error: %s" % err)
            return 3
        except CogUserException as err:
            self.prerr("Traceback (most recent call last):")
            self.prerr(err.args[0])
            return 4
        except CogCheckFailed as err:
            self.prerr(err)
            return 5
        except CogError as err:
            self.prerr(err)
            return 1


def find_cog_source(frame_summary, prologue):
    """Find cog source lines in a frame summary list, for printing tracebacks.

    Arguments:
        frame_summary: a list of 4-item tuples, as returned by traceback.extract_tb.
        prologue: the text of the code prologue.

    Returns
        A list of 4-item tuples, updated to correct the cog entries.

    """
    prolines = prologue.splitlines()
    for filename, lineno, funcname, source in frame_summary:
        if not source:
            m = re.search(r"^<cog ([^:]+):(\d+)>$", filename)
            if m:
                if lineno <= len(prolines):
                    filename = '<prologue>'
                    source = prolines[lineno-1]
                    lineno -= 1     # Because "import cog" is the first line in the prologue
                else:
                    filename, coglineno = m.groups()
                    coglineno = int(coglineno)
                    lineno += coglineno - len(prolines)
                    source = linecache.getline(filename, lineno).strip()
        yield filename, lineno, funcname, source


def main():
    """Main function for entry_points to use."""
    return Cog().main(sys.argv)


#=======================================================================================================================
# RunCogInFiles
#=======================================================================================================================
def RunCogInFiles(files):
    import time
    start = time.clock()

    #-r to write back to the input file!
    #-U to write \n and not native lines
    ret = Cog().main(['unused', '-r', '-U']+files)

    print("Time to run cog: %.2f sec" % (time.clock() - start))
    return ret

#=======================================================================================================================
# main
#=======================================================================================================================
if __name__ == '__main__':
    ret = RunCogInFiles(sys.argv)
    sys.exit(ret)
