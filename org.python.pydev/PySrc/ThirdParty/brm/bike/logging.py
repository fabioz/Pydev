#! /usr/bin/env python
#
# Copyright 2001-2002 by Vinay Sajip. All Rights Reserved.
#
# Permission to use, copy, modify, and distribute this software and its
# documentation for any purpose and without fee is hereby granted,
# provided that the above copyright notice appear in all copies and that
# both that copyright notice and this permission notice appear in
# supporting documentation, and that the name of Vinay Sajip
# not be used in advertising or publicity pertaining to distribution
# of the software without specific, written prior permission.
# VINAY SAJIP DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING
# ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL
# VINAY SAJIP BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR
# ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER
# IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
# OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
#
# For the change history, see README.txt in the distribution.
#
# This file is part of the Python logging distribution. See
# http://www.red-dove.com/python_logging.html
#

"""
Logging module for Python. Based on PEP 282 and comments thereto in
comp.lang.python, and influenced by Apache's log4j system.

Should work under Python versions >= 1.5.2, except that source line
information is not available unless 'inspect' is.

Copyright (C) 2001-2002 Vinay Sajip. All Rights Reserved.

To use, simply 'import logging' and log away!
"""

import sys, os, types, time, string, socket, cPickle, cStringIO

from SocketServer import ThreadingTCPServer, StreamRequestHandler


try:
    import thread
except ImportError:
    thread = None
try:
    import inspect
except ImportError:
    inspect = None

__author__  = "Vinay Sajip <vinay_sajip@red-dove.com>"
__status__  = "alpha"
__version__ = "0.4.5"
__date__    = "4 June 2002"

#---------------------------------------------------------------------------
#   Miscellaneous module data
#---------------------------------------------------------------------------

#
#_srcfile is used when walking the stack to check when we've got the first
# caller stack frame.
#If run as a script, __file__ is not bound.
#
if __name__ == "__main__":
    _srcfile = None
else:
    if string.lower(__file__[-4:]) in ['.pyc', '.pyo']:
        _srcfile = __file__[:-4] + '.py'
    else:
        _srcfile = __file__
    _srcfile = os.path.normcase(_srcfile)

#
#_startTime is used as the base when calculating the relative time of events
#
_startTime = time.time()

#
# Some constants...
#

DEFAULT_TCP_LOGGING_PORT    = 9020
DEFAULT_UDP_LOGGING_PORT    = 9021
DEFAULT_HTTP_LOGGING_PORT   = 9022
DEFAULT_SOAP_LOGGING_PORT   = 9023
DEFAULT_LOGGING_CONFIG_PORT = 9030
SYSLOG_UDP_PORT             = 514

#---------------------------------------------------------------------------
#   Level related stuff
#---------------------------------------------------------------------------
#
# Default levels and level names, these can be replaced with any positive set
# of values having corresponding names. There is a pseudo-level, ALL, which
# is only really there as a lower limit for user-defined levels. Handlers and
# loggers are initialized with ALL so that they will log all messages, even
# at user-defined levels.
#
CRITICAL = 50
FATAL = CRITICAL
ERROR = 40
WARN = 30
INFO = 20
DEBUG = 10
ALL = 0

_levelNames = {
    CRITICAL : 'CRITICAL',
    ERROR    : 'ERROR',
    WARN     : 'WARN',
    INFO     : 'INFO',
    DEBUG    : 'DEBUG',
    ALL      : 'ALL',
    'CRITICAL'  : CRITICAL,
    'ERROR'     : ERROR,
    'WARN'      : WARN,
    'INFO'      : INFO,
    'DEBUG'     : DEBUG,
    'ALL'       : ALL,
}

def getLevelName(lvl):
    """
    Return the textual representation of logging level 'lvl'. If the level is
    one of the predefined levels (CRITICAL, ERROR, WARN, INFO, DEBUG) then you
    get the corresponding string. If you have associated levels with names
    using addLevelName then the name you have associated with 'lvl' is
    returned. Otherwise, the string "Level %s" % lvl is returned.
    """
    return _levelNames.get(lvl, ("Level %s" % lvl))

def addLevelName(lvl, levelName):
    """
    Associate 'levelName' with 'lvl'. This is used when converting levels
    to text during message formatting.
    """
    _acquireLock()
    try:    #unlikely to cause an exception, but you never know...
        _levelNames[lvl] = levelName
        _levelNames[levelName] = lvl
    finally:
        _releaseLock()

#---------------------------------------------------------------------------
#   Thread-related stuff
#---------------------------------------------------------------------------

#
#_lock is used to serialize access to shared data structures in this module.
#This needs to be an RLock because fileConfig() creates Handlers and so
#might arbitrary user threads. Since Handler.__init__() updates the shared
#dictionary _handlers, it needs to acquire the lock. But if configuring,
#the lock would already have been acquired - so we need an RLock.
#The same argument applies to Loggers and Manager.loggerDict.
#
_lock = None

def _acquireLock():
    """
    Acquire the module-level lock for serializing access to shared data.
    This should be released with _releaseLock().
    """
    global _lock
    if (not _lock) and thread:
        import threading    #this had better work
        _lock = threading.RLock()
    if _lock:
        _lock.acquire()

def _releaseLock():
    """
    Release the module-level lock acquired by calling _acquireLock().
    """
    if _lock:
        _lock.release()

#---------------------------------------------------------------------------
#   The logging record
#---------------------------------------------------------------------------

class LogRecord:
    """
    LogRecord instances are created every time something is logged. They
    contain all the information pertinent to the event being logged. The
    main information passed in is in msg and args, which are combined
    using msg % args to create the message field of the record. The record
    also includes information such as when the record was created, the
    source line where the logging call was made, and any exception
    information to be logged.
    """
    def __init__(self, name, lvl, pathname, lineno, msg, args, exc_info):
        """
        Initialize a logging record with interesting information.
        """
        ct = time.time()
        self.name = name
        self.msg = msg
        self.args = args
        self.levelname = getLevelName(lvl)
        self.levelno = lvl
        self.pathname = pathname
        try:
            self.filename = os.path.basename(pathname)
            self.module = os.path.splitext(self.filename)[0]
        except:
            self.filename = pathname
            self.module = "Unknown module"
        self.exc_info = exc_info
        self.lineno = lineno
        self.created = ct
        self.msecs = (ct - long(ct)) * 1000
        self.relativeCreated = (self.created - _startTime) * 1000
        if thread:
            self.thread = thread.get_ident()
        else:
            self.thread = None

    def __str__(self):
        return '<LogRecord: %s, %s, %s, %s, "%s">'%(self.name, self.levelno,
            self.pathname, self.lineno, self.msg)

    def getMessage(self):
        """
        Return the message for this LogRecord, merging any user-supplied
        arguments with the message.
        """
        msg = str(self.msg)
        if self.args:
            msg = msg % self.args
        return msg

#---------------------------------------------------------------------------
#   Formatter classes and functions
#---------------------------------------------------------------------------

class Formatter:
    """
    Formatters need to know how a LogRecord is constructed. They are
    responsible for converting a LogRecord to (usually) a string which can
    be interpreted by either a human or an external system. The base Formatter
    allows a formatting string to be specified. If none is supplied, the
    default value of "%s(message)\\n" is used.

    The Formatter can be initialized with a format string which makes use of
    knowledge of the LogRecord attributes - e.g. the default value mentioned
    above makes use of the fact that the user's message and arguments are pre-
    formatted into a LogRecord's message attribute. Currently, the useful
    attributes in a LogRecord are described by:

    %(name)s            Name of the logger (logging channel)
    %(levelno)s         Numeric logging level for the message (DEBUG, INFO,
                        WARN, ERROR, CRITICAL)
    %(levelname)s       Text logging level for the message ("DEBUG", "INFO",
                        "WARN", "ERROR", "CRITICAL")
    %(pathname)s        Full pathname of the source file where the logging
                        call was issued (if available)
    %(filename)s        Filename portion of pathname
    %(module)s          Module (name portion of filename)
    %(lineno)d          Source line number where the logging call was issued
                        (if available)
    %(created)f         Time when the LogRecord was created (time.time()
                        return value)
    %(asctime)s         Textual time when the LogRecord was created
    %(msecs)d           Millisecond portion of the creation time
    %(relativeCreated)d Time in milliseconds when the LogRecord was created,
                        relative to the time the logging module was loaded
                        (typically at application startup time)
    %(thread)d          Thread ID (if available)
    %(message)s         The result of msg % args, computed just as the
                        record is emitted
    """
    def __init__(self, fmt=None, datefmt=None):
        """
        Initialize the formatter either with the specified format string, or a
        default as described above. Allow for specialized date formatting with
        the optional datefmt argument (if omitted, you get the ISO8601 format).
        """
        if fmt:
            self._fmt = fmt
        else:
            self._fmt = "%(message)s"
        self.datefmt = datefmt

    def formatTime(self, record, datefmt=None):
        """
        This method should be called from format() by a formatter which
        wants to make use of a formatted time. This method can be overridden
        in formatters to provide for any specific requirement, but the
        basic behaviour is as follows: if datefmt (a string) is specified,
        it is used with time.strftime() to format the creation time of the
        record. Otherwise, the ISO8601 format is used. The resulting
        string is returned.
        """
        ct = record.created
        if datefmt:
            s = time.strftime(datefmt, time.localtime(ct))
        else:
            t = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(ct))
            s = "%s,%03d" % (t, record.msecs)
        return s

    def formatException(self, ei):
        """
        Format the specified exception information as a string. This
        default implementation just uses traceback.print_exception()
        """
        import traceback
        sio = cStringIO.StringIO()
        traceback.print_exception(ei[0], ei[1], ei[2], None, sio)
        s = sio.getvalue()
        sio.close()
        if s[-1] == "\n":
            s = s[:-1]
        return s

    def format(self, record):
        """
        The record's attribute dictionary is used as the operand to a
        string formatting operation which yields the returned string.
        Before formatting the dictionary, a couple of preparatory steps
        are carried out. The message attribute of the record is computed
        using msg % args. If the formatting string contains "%(asctime)",
        formatTime() is called to format the event time. If there is
        exception information, it is formatted using formatException()
        and appended to the message.
        """
        record.message = record.getMessage()
        if string.find(self._fmt,"%(asctime)") >= 0:
            record.asctime = self.formatTime(record, self.datefmt)
        s = self._fmt % record.__dict__
        if record.exc_info:
            if s[-1] != "\n":
                s = s + "\n"
            s = s + self.formatException(record.exc_info)
        return s

#
#   The default formatter to use when no other is specified
#
_defaultFormatter = Formatter()

class BufferingFormatter:
    """
    A formatter suitable for formatting a number of records.
    """
    def __init__(self, linefmt=None):
        """
        Optionally specify a formatter which will be used to format each
        individual record.
        """
        if linefmt:
            self.linefmt = linefmt
        else:
            self.linefmt = _defaultFormatter

    def formatHeader(self, records):
        """
        Return the header string for the specified records.
        """
        return ""

    def formatFooter(self, records):
        """
        Return the footer string for the specified records.
        """
        return ""

    def format(self, records):
        """
        Format the specified records and return the result as a string.
        """
        rv = ""
        if len(records) > 0:
            rv = rv + self.formatHeader(records)
            for record in records:
                rv = rv + self.linefmt.format(record)
            rv = rv + self.formatFooter(records)
        return rv

#---------------------------------------------------------------------------
#   Filter classes and functions
#---------------------------------------------------------------------------

class Filter:
    """
    The base filter class. Loggers and Handlers can optionally use Filter
    instances to filter records as desired. The base filter class only allows
    events which are below a certain point in the logger hierarchy. For
    example, a filter initialized with "A.B" will allow events logged by
    loggers "A.B", "A.B.C", "A.B.C.D", "A.B.D" etc. but not "A.BB", "B.A.B"
    etc. If initialized with the empty string, all events are passed.
    """
    def __init__(self, name=''):
        """
        Initialize with the name of the logger which, together with its
        children, will have its events allowed through the filter. If no
        name is specified, allow every event.
        """
        self.name = name
        self.nlen = len(name)

    def filter(self, record):
        """
        Is the specified record to be logged? Returns 0 for no, nonzero for
        yes. If deemed appropriate, the record may be modified in-place.
        """
        if self.nlen == 0:
            return 1
        elif self.name == record.name:
            return 1
        elif string.find(record.name, self.name, 0, self.nlen) != 0:
            return 0
        return (record.name[self.nlen] == ".")

class Filterer:
    """
    A base class for loggers and handlers which allows them to share
    common code.
    """
    def __init__(self):
        """
        Initialize the list of filters to be an empty list.
        """
        self.filters = []

    def addFilter(self, filter):
        """
        Add the specified filter to this handler.
        """
        if not (filter in self.filters):
            self.filters.append(filter)

    def removeFilter(self, filter):
        """
        Remove the specified filter from this handler.
        """
        if filter in self.filters:
            self.filters.remove(filter)

    def filter(self, record):
        """
        Determine if a record is loggable by consulting all the filters. The
        default is to allow the record to be logged; any filter can veto this
        and the record is then dropped. Returns a boolean value.
        """
        rv = 1
        for f in self.filters:
            if not f.filter(record):
                rv = 0
                break
        return rv

#---------------------------------------------------------------------------
#   Handler classes and functions
#---------------------------------------------------------------------------

_handlers = {}  #repository of handlers (for flushing when shutdown called)

class Handler(Filterer):
    """
    The base handler class. Acts as a placeholder which defines the Handler
    interface. Handlers can optionally use Formatter instances to format
    records as desired. By default, no formatter is specified; in this case,
    the 'raw' message as determined by record.message is logged.
    """
    def __init__(self, level=ALL):
        """
        Initializes the instance - basically setting the formatter to None
        and the filter list to empty.
        """
        Filterer.__init__(self)
        self.level = level
        self.formatter = None
        #get the module data lock, as we're updating a shared structure.
        _acquireLock()
        try:    #unlikely to raise an exception, but you never know...
            _handlers[self] = 1
        finally:
            _releaseLock()
        self.createLock()

    def createLock(self):
        """
        Acquire a thread lock for serializing access to the underlying I/O.
        """
        if thread:
            self.lock = thread.allocate_lock()
        else:
            self.lock = None

    def acquire(self):
        """
        Acquire the I/O thread lock.
        """
        if self.lock:
            self.lock.acquire()

    def release(self):
        """
        Release the I/O thread lock.
        """
        if self.lock:
            self.lock.release()

    def setLevel(self, lvl):
        """
        Set the logging level of this handler.
        """
        self.level = lvl

    def format(self, record):
        """
        Do formatting for a record - if a formatter is set, use it.
        Otherwise, use the default formatter for the module.
        """
        if self.formatter:
            fmt = self.formatter
        else:
            fmt = _defaultFormatter
        return fmt.format(record)

    def emit(self, record):
        """
        Do whatever it takes to actually log the specified logging record.
        This version is intended to be implemented by subclasses and so
        raises a NotImplementedError.
        """
        raise NotImplementedError, 'emit must be implemented '\
                                    'by Handler subclasses'

    def handle(self, record):
        """
        Conditionally emit the specified logging record, depending on
        filters which may have been added to the handler. Wrap the actual
        emission of the record with acquisition/release of the I/O thread
        lock.
        """
        if self.filter(record):
            self.acquire()
            try:
                self.emit(record)
            finally:
                self.release()

    def setFormatter(self, fmt):
        """
        Set the formatter for this handler.
        """
        self.formatter = fmt

    def flush(self):
        """
        Ensure all logging output has been flushed. This version does
        nothing and is intended to be implemented by subclasses.
        """
        pass

    def close(self):
        """
        Tidy up any resources used by the handler. This version does
        nothing and is intended to be implemented by subclasses.
        """
        pass

    def handleError(self):
        """
        This method should be called from handlers when an exception is
        encountered during an emit() call. By default it does nothing,
        which means that exceptions get silently ignored. This is what is
        mostly wanted for a logging system - most users will not care
        about errors in the logging system, they are more interested in
        application errors. You could, however, replace this with a custom
        handler if you wish.
        """
        #import traceback
        #ei = sys.exc_info()
        #traceback.print_exception(ei[0], ei[1], ei[2], None, sys.stderr)
        #del ei
        pass

class StreamHandler(Handler):
    """
    A handler class which writes logging records, appropriately formatted,
    to a stream. Note that this class does not close the stream, as
    sys.stdout or sys.stderr may be used.
    """
    def __init__(self, strm=None):
        """
        If strm is not specified, sys.stderr is used.
        """
        Handler.__init__(self)
        if not strm:
            strm = sys.stderr
        self.stream = strm
        self.formatter = None

    def flush(self):
        """
        Flushes the stream.
        """
        self.stream.flush()

    def emit(self, record):
        """
        If a formatter is specified, it is used to format the record.
        The record is then written to the stream with a trailing newline
        [N.B. this may be removed depending on feedback]. If exception
        information is present, it is formatted using
        traceback.print_exception and appended to the stream.
        """
        try:
            msg = self.format(record)
            self.stream.write("%s\n" % msg)
            self.flush()
        except:
            self.handleError()

class FileHandler(StreamHandler):
    """
    A handler class which writes formatted logging records to disk files.
    """
    def __init__(self, filename, mode="a+"):
        """
        Open the specified file and use it as the stream for logging.
        By default, the file grows indefinitely. You can call setRollover()
        to allow the file to rollover at a predetermined size.
        """
        StreamHandler.__init__(self, open(filename, mode))
        self.maxBytes = 0
        self.backupCount = 0
        self.baseFilename = filename
        #self.backupIndex = 0
        self.mode = mode

    def setRollover(self, maxBytes, backupCount):
        """
        Set the rollover parameters so that rollover occurs whenever the
        current log file is nearly maxBytes in length. If backupCount
        is >= 1, the system will successively create new files with the
        same pathname as the base file, but with extensions ".1", ".2"
        etc. appended to it. For example, with a backupCount of 5 and a
        base file name of "app.log", you would get "app.log", "app.log.1",
        "app.log.2", ... through to "app.log.5". When the last file reaches
        its size limit, the logging reverts to "app.log" which is truncated
        to zero length. If maxBytes is zero, rollover never occurs.
        """
        self.maxBytes = maxBytes
        self.backupCount = backupCount
        if maxBytes > 0:
            self.mode = "a+"

    def doRollover(self):
        """
        Do a rollover, as described in setRollover().
        """
#       Old algorithm
#        if self.backupIndex >= self.backupCount:
#            self.backupIndex = 0
#            fn = self.baseFilename
#        else:
#            self.backupIndex = self.backupIndex + 1
#            fn = "%s.%d" % (self.baseFilename, self.backupIndex)
#        self.stream.close()
#        self.stream = open(fn, "w+")
        self.stream.close()
        if self.backupCount > 0:
            for i in range(self.backupCount - 1, 0, -1):
                sfn = "%s.%d" % (self.baseFilename, i)
                dfn = "%s.%d" % (self.baseFilename, i + 1)
                if os.path.exists(sfn):
                    #print "%s -> %s" % (sfn, dfn)
                    if os.path.exists(dfn):
                        os.remove(dfn)
                    os.rename(sfn, dfn)
            dfn = self.baseFilename + ".1"
            if os.path.exists(dfn):
                os.remove(dfn)
            os.rename(self.baseFilename, dfn)
        self.stream = open(self.baseFilename, "w+")

    def emit(self, record):
        """
        Output the record to the file, catering for rollover as described
        in setRollover().
        """
        if self.maxBytes > 0:                   # are we rolling over?
            msg = "%s\n" % self.format(record)
            if self.stream.tell() + len(msg) >= self.maxBytes:
                self.doRollover()
        StreamHandler.emit(self, record)

    def close(self):
        """
        Closes the stream.
        """
        self.stream.close()

class SocketHandler(Handler):
    """
    A handler class which writes logging records, in pickle format, to
    a streaming socket. The socket is kept open across logging calls.
    If the peer resets it, an attempt is made to reconnect on the next call.
    Note that the very simple wire protocol used means that packet sizes
    are expected to be encodable within 16 bits (i.e. < 32767 bytes).
    """

    def __init__(self, host, port):
        """
        Initializes the handler with a specific host address and port.
        """
        Handler.__init__(self)
        self.host = host
        self.port = port
        self.sock = None
        self.closeOnError = 1

    def makeSocket(self):
        """
        A factory method which allows subclasses to define the precise
        type of socket they want.
        """
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect((self.host, self.port))
        return s

    def send(self, s):
        """
        Send a pickled string to the socket. This function allows for
        partial sends which can happen when the network is busy.
        """
        sentsofar = 0
        left = len(s)
        while left > 0:
            sent = self.sock.send(s[sentsofar:])
            sentsofar = sentsofar + sent
            left = left - sent

    def makePickle(self, record):
        """
        Pickles the record in binary format with a length prefix, and
        returns it ready for transmission across the socket.
        """
        s = cPickle.dumps(record.__dict__, 1)
        n = len(s)
        slen = "%c%c" % ((n >> 8) & 0xFF, n & 0xFF)
        return slen + s

    def handleError(self):
        """
        An error has occurred during logging. Most likely cause -
        connection lost. Close the socket so that we can retry on the
        next event.
        """
        if self.closeOnError and self.sock:
            self.sock.close()
            self.sock = None        #try to reconnect next time

    def emit(self, record):
        """
        Pickles the record and writes it to the socket in binary format.
        If there is an error with the socket, silently drop the packet.
        If there was a problem with the socket, re-establishes the
        socket.
        """
        try:
            s = self.makePickle(record)
            if not self.sock:
                self.sock = self.makeSocket()
            self.send(s)
        except:
            self.handleError()

    def close(self):
        """
        Closes the socket.
        """
        if self.sock:
            self.sock.close()
            self.sock = None

class DatagramHandler(SocketHandler):
    """
    A handler class which writes logging records, in pickle format, to
    a datagram socket. Note that the very simple wire protocol used means
    that packet sizes are expected to be encodable within 16 bits
    (i.e. < 32767 bytes).

    """
    def __init__(self, host, port):
        """
        Initializes the handler with a specific host address and port.
        """
        SocketHandler.__init__(self, host, port)
        self.closeOnError = 0

    def makeSocket(self):
        """
        The factory method of SocketHandler is here overridden to create
        a UDP socket (SOCK_DGRAM).
        """
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        return s

    def send(self, s):
        """
        Send a pickled string to a socket. This function allows for
        partial sends which can happen when the network is busy.
        """
        sentsofar = 0
        left = len(s)
        addr = (self.host, self.port)
        while left > 0:
            sent = self.sock.sendto(s[sentsofar:], addr)
            sentsofar = sentsofar + sent
            left = left - sent

class SysLogHandler(Handler):
    """
    A handler class which sends formatted logging records to a syslog
    server. Based on Sam Rushing's syslog module:
    http://www.nightmare.com/squirl/python-ext/misc/syslog.py
    Contributed by Nicolas Untz (after which minor refactoring changes
    have been made).
    """

    # from <linux/sys/syslog.h>:
    # ======================================================================
    # priorities/facilities are encoded into a single 32-bit quantity, where
    # the bottom 3 bits are the priority (0-7) and the top 28 bits are the
    # facility (0-big number). Both the priorities and the facilities map
    # roughly one-to-one to strings in the syslogd(8) source code.  This
    # mapping is included in this file.
    #
    # priorities (these are ordered)

    LOG_EMERG     = 0       #  system is unusable
    LOG_ALERT     = 1       #  action must be taken immediately
    LOG_CRIT      = 2       #  critical conditions
    LOG_ERR       = 3       #  error conditions
    LOG_WARNING   = 4       #  warning conditions
    LOG_NOTICE    = 5       #  normal but significant condition
    LOG_INFO      = 6       #  informational
    LOG_DEBUG     = 7       #  debug-level messages

    #  facility codes
    LOG_KERN      = 0       #  kernel messages
    LOG_USER      = 1       #  random user-level messages
    LOG_MAIL      = 2       #  mail system
    LOG_DAEMON    = 3       #  system daemons
    LOG_AUTH      = 4       #  security/authorization messages
    LOG_SYSLOG    = 5       #  messages generated internally by syslogd
    LOG_LPR       = 6       #  line printer subsystem
    LOG_NEWS      = 7       #  network news subsystem
    LOG_UUCP      = 8       #  UUCP subsystem
    LOG_CRON      = 9       #  clock daemon
    LOG_AUTHPRIV  = 10  #  security/authorization messages (private)

    #  other codes through 15 reserved for system use
    LOG_LOCAL0    = 16      #  reserved for local use
    LOG_LOCAL1    = 17      #  reserved for local use
    LOG_LOCAL2    = 18      #  reserved for local use
    LOG_LOCAL3    = 19      #  reserved for local use
    LOG_LOCAL4    = 20      #  reserved for local use
    LOG_LOCAL5    = 21      #  reserved for local use
    LOG_LOCAL6    = 22      #  reserved for local use
    LOG_LOCAL7    = 23      #  reserved for local use

    priority_names = {
        "alert":    LOG_ALERT,
        "crit":     LOG_CRIT,
        "critical": LOG_CRIT,
        "debug":    LOG_DEBUG,
        "emerg":    LOG_EMERG,
        "err":      LOG_ERR,
        "error":    LOG_ERR,        #  DEPRECATED
        "info":     LOG_INFO,
        "notice":   LOG_NOTICE,
        "panic":    LOG_EMERG,      #  DEPRECATED
        "warn":     LOG_WARNING,    #  DEPRECATED
        "warning":  LOG_WARNING,
        }

    facility_names = {
        "auth":     LOG_AUTH,
        "authpriv": LOG_AUTHPRIV,
        "cron":     LOG_CRON,
        "daemon":   LOG_DAEMON,
        "kern":     LOG_KERN,
        "lpr":      LOG_LPR,
        "mail":     LOG_MAIL,
        "news":     LOG_NEWS,
        "security": LOG_AUTH,       #  DEPRECATED
        "syslog":   LOG_SYSLOG,
        "user":     LOG_USER,
        "uucp":     LOG_UUCP,
        "local0":   LOG_LOCAL0,
        "local1":   LOG_LOCAL1,
        "local2":   LOG_LOCAL2,
        "local3":   LOG_LOCAL3,
        "local4":   LOG_LOCAL4,
        "local5":   LOG_LOCAL5,
        "local6":   LOG_LOCAL6,
        "local7":   LOG_LOCAL7,
        }

    def __init__(self, address=('localhost', SYSLOG_UDP_PORT), facility=LOG_USER):
        """
        If address is specified as a string, UNIX socket is used.
        If facility is not specified, LOG_USER is used.
        """
        Handler.__init__(self)

        self.address = address
        self.facility = facility
        if type(address) == types.StringType:
            self.socket = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
            self.socket.connect(address)
            self.unixsocket = 1
        else:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.unixsocket = 0

        self.formatter = None

    # curious: when talking to the unix-domain '/dev/log' socket, a
    #   zero-terminator seems to be required.  this string is placed
    #   into a class variable so that it can be overridden if
    #   necessary.
    log_format_string = '<%d>%s\000'

    def encodePriority (self, facility, priority):
        """
        Encode the facility and priority. You can pass in strings or
        integers - if strings are passed, the facility_names and
        priority_names mapping dictionaries are used to convert them to
        integers.
        """
        if type(facility) == types.StringType:
            facility = self.facility_names[facility]
        if type(priority) == types.StringType:
            priority = self.priority_names[priority]
        return (facility << 3) | priority

    def close (self):
        """
        Closes the socket.
        """
        if self.unixsocket:
            self.socket.close()

    def emit(self, record):
        """
        The record is formatted, and then sent to the syslog server. If
        exception information is present, it is NOT sent to the server.
        """
        msg = self.format(record)
        """
        We need to convert record level to lowercase, maybe this will
        change in the future.
        """
        msg = self.log_format_string % (
            self.encodePriority(self.facility,
                                string.lower(record.levelname)),
            msg)
        try:
            if self.unixsocket:
                self.socket.send(msg)
            else:
                self.socket.sendto(msg, self.address)
        except:
            self.handleError()

class SMTPHandler(Handler):
    """
    A handler class which sends an SMTP email for each logging event.
    """
    def __init__(self, mailhost, fromaddr, toaddrs, subject):
        """
        Initialize the instance with the from and to addresses and subject
        line of the email. To specify a non-standard SMTP port, use the
        (host, port) tuple format for the mailhost argument.
        """
        Handler.__init__(self)
        if type(mailhost) == types.TupleType:
            host, port = mailhost
            self.mailhost = host
            self.mailport = port
        else:
            self.mailhost = mailhost
            self.mailport = None
        self.fromaddr = fromaddr
        self.toaddrs = toaddrs
        self.subject = subject

    def getSubject(self, record):
        """
        If you want to specify a subject line which is record-dependent,
        override this method.
        """
        return self.subject

    def emit(self, record):
        """
        Format the record and send it to the specified addressees.
        """
        try:
            import smtplib
            port = self.mailport
            if not port:
                port = smtplib.SMTP_PORT
            smtp = smtplib.SMTP(self.mailhost, port)
            msg = self.format(record)
            msg = "From: %s\r\nTo: %s\r\nSubject: %s\r\n\r\n%s" % (
                            self.fromaddr,
                            string.join(self.toaddrs, ","),
                            self.getSubject(record), msg
                            )
            smtp.sendmail(self.fromaddr, self.toaddrs, msg)
            smtp.quit()
        except:
            self.handleError()

class BufferingHandler(Handler):
    """
  A handler class which buffers logging records in memory. Whenever each
  record is added to the buffer, a check is made to see if the buffer should
  be flushed. If it should, then flush() is expected to do the needful.
    """
    def __init__(self, capacity):
        """
        Initialize the handler with the buffer size.
        """
        Handler.__init__(self)
        self.capacity = capacity
        self.buffer = []

    def shouldFlush(self, record):
        """
        Returns true if the buffer is up to capacity. This method can be
        overridden to implement custom flushing strategies.
        """
        return (len(self.buffer) >= self.capacity)

    def emit(self, record):
        """
        Append the record. If shouldFlush() tells us to, call flush() to process
        the buffer.
        """
        self.buffer.append(record)
        if self.shouldFlush(record):
            self.flush()

    def flush(self):
        """
        Override to implement custom flushing behaviour. This version just zaps
        the buffer to empty.
        """
        self.buffer = []

class MemoryHandler(BufferingHandler):
    """
    A handler class which buffers logging records in memory, periodically
    flushing them to a target handler. Flushing occurs whenever the buffer
    is full, or when an event of a certain severity or greater is seen.
    """
    def __init__(self, capacity, flushLevel=ERROR, target=None):
        """
        Initialize the handler with the buffer size, the level at which
        flushing should occur and an optional target. Note that without a
        target being set either here or via setTarget(), a MemoryHandler
        is no use to anyone!
        """
        BufferingHandler.__init__(self, capacity)
        self.flushLevel = flushLevel
        self.target = target

    def shouldFlush(self, record):
        """
        Check for buffer full or a record at the flushLevel or higher.
        """
        return (len(self.buffer) >= self.capacity) or \
                (record.levelno >= self.flushLevel)

    def setTarget(self, target):
        """
        Set the target handler for this handler.
        """
        self.target = target

    def flush(self):
        """
        For a MemoryHandler, flushing means just sending the buffered
        records to the target, if there is one. Override if you want
        different behaviour.
        """
        if self.target:
            for record in self.buffer:
                self.target.handle(record)
            self.buffer = []

    def close(self):
        """
        Flush, set the target to None and lose the buffer.
        """
        self.flush()
        self.target = None
        self.buffer = []

class NTEventLogHandler(Handler):
    """
    A handler class which sends events to the NT Event Log. Adds a
    registry entry for the specified application name. If no dllname is
    provided, win32service.pyd (which contains some basic message
    placeholders) is used. Note that use of these placeholders will make
    your event logs big, as the entire message source is held in the log.
    If you want slimmer logs, you have to pass in the name of your own DLL
    which contains the message definitions you want to use in the event log.
    """
    def __init__(self, appname, dllname=None, logtype="Application"):
        Handler.__init__(self)
        try:
            import win32evtlogutil, win32evtlog
            self.appname = appname
            self._welu = win32evtlogutil
            if not dllname:
                dllname = os.path.split(self._welu.__file__)
                dllname = os.path.split(dllname[0])
                dllname = os.path.join(dllname[0], r'win32service.pyd')
            self.dllname = dllname
            self.logtype = logtype
            self._welu.AddSourceToRegistry(appname, dllname, logtype)
            self.deftype = win32evtlog.EVENTLOG_ERROR_TYPE
            self.typemap = {
                DEBUG   : win32evtlog.EVENTLOG_INFORMATION_TYPE,
                INFO    : win32evtlog.EVENTLOG_INFORMATION_TYPE,
                WARN    : win32evtlog.EVENTLOG_WARNING_TYPE,
                ERROR   : win32evtlog.EVENTLOG_ERROR_TYPE,
                CRITICAL: win32evtlog.EVENTLOG_ERROR_TYPE,
         }
        except ImportError:
            print "The Python Win32 extensions for NT (service, event "\
                        "logging) appear not to be available."
            self._welu = None

    def getMessageID(self, record):
        """
        Return the message ID for the event record. If you are using your
        own messages, you could do this by having the msg passed to the
        logger being an ID rather than a formatting string. Then, in here,
        you could use a dictionary lookup to get the message ID. This
        version returns 1, which is the base message ID in win32service.pyd.
        """
        return 1

    def getEventCategory(self, record):
        """
        Return the event category for the record. Override this if you
        want to specify your own categories. This version returns 0.
        """
        return 0

    def getEventType(self, record):
        """
        Return the event type for the record. Override this if you want
        to specify your own types. This version does a mapping using the
        handler's typemap attribute, which is set up in __init__() to a
        dictionary which contains mappings for DEBUG, INFO, WARN, ERROR
        and CRITICAL. If you are using your own levels you will either need
        to override this method or place a suitable dictionary in the
        handler's typemap attribute.
        """
        return self.typemap.get(record.levelno, self.deftype)

    def emit(self, record):
        """
        Determine the message ID, event category and event type. Then
        log the message in the NT event log.
        """
        if self._welu:
            try:
                id = self.getMessageID(record)
                cat = self.getEventCategory(record)
                type = self.getEventType(record)
                msg = self.format(record)
                self._welu.ReportEvent(self.appname, id, cat, type, [msg])
            except:
                self.handleError()

    def close(self):
        """
        You can remove the application name from the registry as a
        source of event log entries. However, if you do this, you will
        not be able to see the events as you intended in the Event Log
        Viewer - it needs to be able to access the registry to get the
        DLL name.
        """
        #self._welu.RemoveSourceFromRegistry(self.appname, self.logtype)
        pass

class HTTPHandler(Handler):
    """
    A class which sends records to a Web server, using either GET or
    POST semantics.
    """
    def __init__(self, host, url, method="GET"):
        """
        Initialize the instance with the host, the request URL, and the method
        ("GET" or "POST")
        """
        Handler.__init__(self)
        method = string.upper(method)
        if method not in ["GET", "POST"]:
            raise ValueError, "method must be GET or POST"
        self.host = host
        self.url = url
        self.method = method

    def emit(self, record):
        """
        Send the record to the Web server as an URL-encoded dictionary
        """
        try:
            import httplib, urllib
            h = httplib.HTTP(self.host)
            url = self.url
            data = urllib.urlencode(record.__dict__)
            if self.method == "GET":
                if (string.find(url, '?') >= 0):
                    sep = '&'
                else:
                    sep = '?'
                url = url + "%c%s" % (sep, data)
            h.putrequest(self.method, url)
            if self.method == "POST":
                h.putheader("Content-length", str(len(data)))
            h.endheaders()
            if self.method == "POST":
                h.send(data)
            h.getreply()    #can't do anything with the result
        except:
            self.handleError()

#---------------------------------------------------------------------------
#   Manager classes and functions
#---------------------------------------------------------------------------

class PlaceHolder:
    """
    PlaceHolder instances are used in the Manager logger hierarchy to take
    the place of nodes for which no loggers have been defined [FIXME add
    example].
    """
    def __init__(self, alogger):
        """
        Initialize with the specified logger being a child of this placeholder.
        """
        self.loggers = [alogger]

    def append(self, alogger):
        """
        Add the specified logger as a child of this placeholder.
        """
        if alogger not in self.loggers:
            self.loggers.append(alogger)

#
#   Determine which class to use when instantiating loggers.
#
_loggerClass = None

def setLoggerClass(klass):
    """
    Set the class to be used when instantiating a logger. The class should
    define __init__() such that only a name argument is required, and the
    __init__() should call Logger.__init__()
    """
    if klass != Logger:
        if type(klass) != types.ClassType:
            raise TypeError, "setLoggerClass is expecting a class"
        if not issubclass(klass, Logger):
            raise TypeError, "logger not derived from logging.Logger: " + \
                            klass.__name__
    global _loggerClass
    _loggerClass = klass

class Manager:
    """
    There is [under normal circumstances] just one Manager instance, which
    holds the hierarchy of loggers.
    """
    def __init__(self, root):
        """
        Initialize the manager with the root node of the logger hierarchy.
        """
        self.root = root
        self.disable = 0
        self.emittedNoHandlerWarning = 0
        self.loggerDict = {}

    def getLogger(self, name):
        """
        Get a logger with the specified name, creating it if it doesn't
        yet exist. If a PlaceHolder existed for the specified name [i.e.
        the logger didn't exist but a child of it did], replace it with
        the created logger and fix up the parent/child references which
        pointed to the placeholder to now point to the logger.
        """
        rv = None
        _acquireLock()
        try:
            if self.loggerDict.has_key(name):
                rv = self.loggerDict[name]
                if isinstance(rv, PlaceHolder):
                    ph = rv
                    rv = _loggerClass(name)
                    rv.manager = self
                    self.loggerDict[name] = rv
                    self._fixupChildren(ph, rv)
                    self._fixupParents(rv)
            else:
                rv = _loggerClass(name)
                rv.manager = self
                self.loggerDict[name] = rv
                self._fixupParents(rv)
        finally:
            _releaseLock()
        return rv

    def _fixupParents(self, alogger):
        """
        Ensure that there are either loggers or placeholders all the way
        from the specified logger to the root of the logger hierarchy.
        """
        name = alogger.name
        i = string.rfind(name, ".")
        rv = None
        while (i > 0) and not rv:
            substr = name[:i]
            if not self.loggerDict.has_key(substr):
                self.loggerDict[substr] = PlaceHolder(alogger)
            else:
                obj = self.loggerDict[substr]
                if isinstance(obj, Logger):
                    rv = obj
                else:
                    assert isinstance(obj, PlaceHolder)
                    obj.append(alogger)
            i = string.rfind(name, ".", 0, i - 1)
        if not rv:
            rv = self.root
        alogger.parent = rv

    def _fixupChildren(self, ph, alogger):
        """
        Ensure that children of the placeholder ph are connected to the
        specified logger.
        """
        for c in ph.loggers:
            if string.find(c.parent.name, alogger.name) <> 0:
                alogger.parent = c.parent
                c.parent = alogger

#---------------------------------------------------------------------------
#   Logger classes and functions
#---------------------------------------------------------------------------

class Logger(Filterer):
    """
    Instances of the Logger class represent a single logging channel. A
    "logging channel" indicates an area of an application. Exactly how an
    "area" is defined is up to the application developer. Since an
    application can have any number of areas, logging channels are identified
    by a unique string. Application areas can be nested (e.g. an area
    of "input processing" might include sub-areas "read CSV files", "read
    XLS files" and "read Gnumeric files"). To cater for this natural nesting,
    channel names are organized into a namespace hierarchy where levels are
    separated by periods, much like the Java or Python package namespace. So
    in the instance given above, channel names might be "input" for the upper
    level, and "input.csv", "input.xls" and "input.gnu" for the sub-levels.
    There is no arbitrary limit to the depth of nesting.
    """
    def __init__(self, name, level=ALL):
        """
        Initialize the logger with a name and an optional level.
        """
        Filterer.__init__(self)
        self.name = name
        self.level = level
        self.parent = None
        self.propagate = 1
        self.handlers = []
        self.disabled = 0

    def setLevel(self, lvl):
        """
        Set the logging level of this logger.
        """
        self.level = lvl

#   def getRoot(self):
#       """
#       Get the root of the logger hierarchy.
#       """
#       return Logger.root

    def debug(self, msg, *args, **kwargs):
        """
        Log 'msg % args' with severity 'DEBUG'. To pass exception information,
        use the keyword argument exc_info with a true value, e.g.

        logger.debug("Houston, we have a %s", "thorny problem", exc_info=1)
        """
        if self.manager.disable >= DEBUG:
            return
        if DEBUG >= self.getEffectiveLevel():
            apply(self._log, (DEBUG, msg, args), kwargs)

    def info(self, msg, *args, **kwargs):
        """
        Log 'msg % args' with severity 'INFO'. To pass exception information,
        use the keyword argument exc_info with a true value, e.g.

        logger.info("Houston, we have a %s", "interesting problem", exc_info=1)
        """
        if self.manager.disable >= INFO:
            return
        if INFO >= self.getEffectiveLevel():
            apply(self._log, (INFO, msg, args), kwargs)

    def warn(self, msg, *args, **kwargs):
        """
        Log 'msg % args' with severity 'WARN'. To pass exception information,
        use the keyword argument exc_info with a true value, e.g.

        logger.warn("Houston, we have a %s", "bit of a problem", exc_info=1)
        """
        if self.manager.disable >= WARN:
            return
        if self.isEnabledFor(WARN):
            apply(self._log, (WARN, msg, args), kwargs)

    def error(self, msg, *args, **kwargs):
        """
        Log 'msg % args' with severity 'ERROR'. To pass exception information,
        use the keyword argument exc_info with a true value, e.g.

        logger.error("Houston, we have a %s", "major problem", exc_info=1)
        """
        if self.manager.disable >= ERROR:
            return
        if self.isEnabledFor(ERROR):
            apply(self._log, (ERROR, msg, args), kwargs)

    def exception(self, msg, *args):
        """
        Convenience method for logging an ERROR with exception information
        """
        apply(self.error, (msg,) + args, {'exc_info': 1})

    def critical(self, msg, *args, **kwargs):
        """
        Log 'msg % args' with severity 'CRITICAL'. To pass exception
        information, use the keyword argument exc_info with a true value, e.g.

        logger.critical("Houston, we have a %s", "major disaster", exc_info=1)
        """
        if self.manager.disable >= CRITICAL:
            return
        if CRITICAL >= self.getEffectiveLevel():
            apply(self._log, (CRITICAL, msg, args), kwargs)

    fatal = critical

    def log(self, lvl, msg, *args, **kwargs):
        """
        Log 'msg % args' with the severity 'lvl'. To pass exception
        information, use the keyword argument exc_info with a true value, e.g.
        logger.log(lvl, "We have a %s", "mysterious problem", exc_info=1)
        """
        if self.manager.disable >= lvl:
            return
        if self.isEnabledFor(lvl):
            apply(self._log, (lvl, msg, args), kwargs)

    def findCaller(self):
        """
        Find the stack frame of the caller so that we can note the source
        file name and line number.
        """
        rv = (None, None)
        frame = inspect.currentframe().f_back
        while frame:
            sfn = inspect.getsourcefile(frame)
            if sfn:
                sfn = os.path.normcase(sfn)
            if sfn != _srcfile:
                #print frame.f_code.co_code
                lineno = inspect.getlineno(frame)
                rv = (sfn, lineno)
                break
            frame = frame.f_back
        return rv

    def makeRecord(self, name, lvl, fn, lno, msg, args, exc_info):
        """
        A factory method which can be overridden in subclasses to create
        specialized LogRecords.
        """
        return LogRecord(name, lvl, fn, lno, msg, args, exc_info)

    def _log(self, lvl, msg, args, exc_info=None):
        """
        Low-level logging routine which creates a LogRecord and then calls
        all the handlers of this logger to handle the record.
        """
        if inspect and _srcfile:
            _acquireLock()
            try:
                fn, lno = self.findCaller()
            finally:
                _releaseLock()
        else:
            fn, lno = "<unknown file>", 0
        if exc_info:
            exc_info = sys.exc_info()
        record = self.makeRecord(self.name, lvl, fn, lno, msg, args, exc_info)
        self.handle(record)

    def handle(self, record):
        """
        Call the handlers for the specified record. This method is used for
        unpickled records received from a socket, as well as those created
        locally. Logger-level filtering is applied.
        """
        if (not self.disabled) and self.filter(record):
            self.callHandlers(record)

    def addHandler(self, hdlr):
        """
        Add the specified handler to this logger.
        """
        if not (hdlr in self.handlers):
            self.handlers.append(hdlr)

    def removeHandler(self, hdlr):
        """
        Remove the specified handler from this logger.
        """
        if hdlr in self.handlers:
            hdlr.close()
            self.handlers.remove(hdlr)

    def callHandlers(self, record):
        """
        Loop through all handlers for this logger and its parents in the
        logger hierarchy. If no handler was found, output a one-off error
        message to sys.stderr. Stop searching up the hierarchy whenever a
        logger with the "propagate" attribute set to zero is found - that
        will be the last logger whose handlers are called.
        """
        c = self
        found = 0
        while c:
            for hdlr in c.handlers:
                found = found + 1
                if record.levelno >= hdlr.level:
                    hdlr.handle(record)
            if not c.propagate:
                c = None    #break out
            else:
                c = c.parent
        if (found == 0) and not self.manager.emittedNoHandlerWarning:
            sys.stderr.write("No handlers could be found for logger"
                             " \"%s\"\n" % self.name)
            self.manager.emittedNoHandlerWarning = 1

    def getEffectiveLevel(self):
        """
        Loop through this logger and its parents in the logger hierarchy,
        looking for a non-zero logging level. Return the first one found.
        """
        logger = self
        while logger:
            if logger.level:
                return logger.level
            logger = logger.parent
        return ALL

    def isEnabledFor(self, lvl):
        """
        Is this logger enabled for level lvl?
        """
        if self.manager.disable >= lvl:
            return 0
        return lvl >= self.getEffectiveLevel()

class RootLogger(Logger):
    """
    A root logger is not that different to any other logger, except that
    it must have a logging level and there is only one instance of it in
    the hierarchy.
    """
    def __init__(self, lvl):
        """
        Initialize the logger with the name "root".
        """
        Logger.__init__(self, "root", lvl)

_loggerClass = Logger

root = RootLogger(DEBUG)
Logger.root = root
Logger.manager = Manager(Logger.root)

#---------------------------------------------------------------------------
# Configuration classes and functions
#---------------------------------------------------------------------------

BASIC_FORMAT = "%(levelname)s:%(name)s:%(message)s"

def basicConfig():
    """
    Do basic configuration for the logging system by creating a
    StreamHandler with a default Formatter and adding it to the
    root logger.
    """
    if len(root.handlers) == 0:
        hdlr = StreamHandler()
        fmt = Formatter(BASIC_FORMAT)
        hdlr.setFormatter(fmt)
        root.addHandler(hdlr)

def fileConfig(fname):
    """
    Read the logging configuration from a ConfigParser-format file. This can
    be called several times from an application, allowing an end user the
    ability to select from various pre-canned configurations (if the
    developer provides a mechanism to present the choices and load the chosen
    configuration).
    """
    import ConfigParser

    cp = ConfigParser.ConfigParser()
    cp.read(fname)
    #first, do the formatters...
    flist = cp.get("formatters", "keys")
    if len(flist):
        flist = string.split(flist, ",")
        formatters = {}
        for form in flist:
            sectname = "formatter_%s" % form
            opts = cp.options(sectname)
            if "format" in opts:
                fs = cp.get(sectname, "format", 1)
            else:
                fs = None
            if "datefmt" in opts:
                dfs = cp.get(sectname, "datefmt", 1)
            else:
                dfs = None
            f = Formatter(fs, dfs)
            formatters[form] = f
    #next, do the handlers...
    #critical section...
    _acquireLock()
    try:
        try:
            #first, lose the existing handlers...
            _handlers.clear()
            #now set up the new ones...
            hlist = cp.get("handlers", "keys")
            if len(hlist):
                hlist = string.split(hlist, ",")
                handlers = {}
                fixups = [] #for inter-handler references
                for hand in hlist:
                    sectname = "handler_%s" % hand
                    klass = cp.get(sectname, "class")
                    opts = cp.options(sectname)
                    if "formatter" in opts:
                        fmt = cp.get(sectname, "formatter")
                    else:
                        fmt = ""
                    klass = eval(klass)
                    args = cp.get(sectname, "args")
                    args = eval(args)
                    h = apply(klass, args)
                    if "level" in opts:
                        lvl = cp.get(sectname, "level")
                        h.setLevel(_levelNames[lvl])
                    if len(fmt):
                        h.setFormatter(formatters[fmt])
                    #temporary hack for FileHandler and MemoryHandler.
                    if klass == FileHandler:
                        maxsize = 0
                        if "maxsize" in opts:
                            ms = cp.getint(sectname, "maxsize")
                            if ms > 0:
                                maxsize = ms
                        if maxsize:
                            backcount = 0
                            if "backcount" in opts:
                                bc = cp.getint(sectname, "backcount")
                                if bc > 0:
                                    backcount = bc
                            h.setRollover(maxsize, backcount)
                    elif klass == MemoryHandler:
                        if "target" in opts:
                            target = cp.get(sectname,"target")
                        else:
                            target = ""
                        if len(target): #the target handler may not be loaded yet, so keep for later...
                            fixups.append((h, target))
                    handlers[hand] = h
                #now all handlers are loaded, fixup inter-handler references...
                for fixup in fixups:
                    h = fixup[0]
                    t = fixup[1]
                    h.setTarget(handlers[t])
            #at last, the loggers...first the root...
            llist = cp.get("loggers", "keys")
            llist = string.split(llist, ",")
            llist.remove("root")
            sectname = "logger_root"
            log = root
            opts = cp.options(sectname)
            if "level" in opts:
                lvl = cp.get(sectname, "level")
                log.setLevel(_levelNames[lvl])
            for h in root.handlers:
                root.removeHandler(h)
            hlist = cp.get(sectname, "handlers")
            if len(hlist):
                hlist = string.split(hlist, ",")
                for hand in hlist:
                    log.addHandler(handlers[hand])
            #and now the others...
            #we don't want to lose the existing loggers,
            #since other threads may have pointers to them.
            #existing is set to contain all existing loggers,
            #and as we go through the new configuration we
            #remove any which are configured. At the end,
            #what's left in existing is the set of loggers
            #which were in the previous configuration but
            #which are not in the new configuration.
            existing = root.manager.loggerDict.keys()
            #now set up the new ones...
            for log in llist:
                sectname = "logger_%s" % log
                qn = cp.get(sectname, "qualname")
                opts = cp.options(sectname)
                if "propagate" in opts:
                    propagate = cp.getint(sectname, "propagate")
                else:
                    propagate = 1
                logger = getLogger(qn)
                if qn in existing:
                    existing.remove(qn)
                if "level" in opts:
                    lvl = cp.get(sectname, "level")
                    logger.setLevel(_levelNames[lvl])
                for h in logger.handlers:
                    logger.removeHandler(h)
                logger.propagate = propagate
                logger.disabled = 0
                hlist = cp.get(sectname, "handlers")
                if len(hlist):
                    hlist = string.split(hlist, ",")
                    for hand in hlist:
                        logger.addHandler(handlers[hand])
            #Disable any old loggers. There's no point deleting
            #them as other threads may continue to hold references
            #and by disabling them, you stop them doing any logging.
            for log in existing:
                root.manager.loggerDict[log].disabled = 1
        except:
            import traceback
            ei = sys.exc_info()
            traceback.print_exception(ei[0], ei[1], ei[2], None, sys.stderr)
            del ei
    finally:
        _releaseLock()

#---------------------------------------------------------------------------
# Utility functions at module level.
# Basically delegate everything to the root logger.
#---------------------------------------------------------------------------

def getLogger(name=None):
    """
    Return a logger with the specified name, creating it if necessary.
    If no name is specified, return the root logger.
    """
    if name:
        return Logger.manager.getLogger(name)
    else:
        return root

def getRootLogger():
    """
    Return the root logger. Note that getLogger('') now does the same thing,
    so this function is deprecated and may disappear in the future.
    """
    return root

def critical(msg, *args, **kwargs):
    """
    Log a message with severity 'CRITICAL' on the root logger.
    """
    if len(root.handlers) == 0:
        basicConfig()
    apply(root.critical, (msg,)+args, kwargs)

fatal = critical

def error(msg, *args, **kwargs):
    """
    Log a message with severity 'ERROR' on the root logger.
    """
    if len(root.handlers) == 0:
        basicConfig()
    apply(root.error, (msg,)+args, kwargs)

def exception(msg, *args):
    """
    Log a message with severity 'ERROR' on the root logger,
    with exception information.
    """
    apply(error, (msg,)+args, {'exc_info': 1})

def warn(msg, *args, **kwargs):
    """
    Log a message with severity 'WARN' on the root logger.
    """
    if len(root.handlers) == 0:
        basicConfig()
    apply(root.warn, (msg,)+args, kwargs)

def info(msg, *args, **kwargs):
    """
    Log a message with severity 'INFO' on the root logger.
    """
    if len(root.handlers) == 0:
        basicConfig()
    apply(root.info, (msg,)+args, kwargs)

def debug(msg, *args, **kwargs):
    """
    Log a message with severity 'DEBUG' on the root logger.
    """
    if len(root.handlers) == 0:
        basicConfig()
    apply(root.debug, (msg,)+args, kwargs)

def disable(level):
    """
    Disable all logging calls less severe than 'level'.
    """
    root.manager.disable = level

def shutdown():
    """
    Perform any cleanup actions in the logging system (e.g. flushing
    buffers). Should be called at application exit.
    """
    for h in _handlers.keys():
        h.flush()
        h.close()

#
#   The following code implements a socket listener for on-the-fly
#   reconfiguration of logging.
#
#   _listener holds the server object doing the listening
_listener = None

def listen(port=DEFAULT_LOGGING_CONFIG_PORT):
    """
    Start up a socket server on the specified port, and listen for new
    configurations. These will be sent as a file suitable for processing
    by fileConfig(). Returns a Thread object on which you can call start()
    to start the server, and which you can join() when appropriate.
    To stop the server, call stopListening().
    """
    if not thread:
        raise NotImplementedError, "listen() needs threading to work"

    import threading

    class ConfigStreamHandler(StreamRequestHandler):
        """
        Handler for a logging configuration request. It expects a
        completely new logging configuration and uses fileConfig to
        install it.
        """
        def handle(self):
            """
            Each request is expected to be a 2-byte length,
            followed by the config file. Uses fileConfig() to do the
            needful.
            """
            import tempfile
            try:
                conn = self.connection
                chunk = conn.recv(2)
                if len(chunk) == 2:
                    slen = (ord(chunk[0]) << 8) | ord(chunk[1])
                    chunk = self.connection.recv(slen)
                    while len(chunk) < slen:
                        chunk = chunk + conn.recv(slen - len(chunk))
                    #Apply new configuration. We'd like to be able to
                    #create a StringIO and pass that in, but unfortunately
                    #1.5.2 ConfigParser does not support reading file
                    #objects, only actual files. So we create a temporary
                    #file and remove it later.
                    file = tempfile.mktemp(".ini")
                    f = open(file, "w")
                    f.write(chunk)
                    f.close()
                    fileConfig(file)
                    os.remove(file)
            except socket.error, e:
                if type(e.args) != types.TupleType:
                    raise
                else:
                    errcode = e.args[0]
                    if errcode != RESET_ERROR:
                        raise

    class ConfigSocketReceiver(ThreadingTCPServer):
        """
        A simple TCP socket-based logging config receiver.
        """

        allow_reuse_address = 1

        def __init__(self, host='localhost', port=DEFAULT_LOGGING_CONFIG_PORT,
                handler=None):
            ThreadingTCPServer.__init__(self, (host, port), handler)
            _acquireLock()
            self.abort = 0
            _releaseLock()
            self.timeout = 1

        def serve_until_stopped(self):
            import select
            abort = 0
            while not abort:
                rd, wr, ex = select.select([self.socket.fileno()],
                                           [], [],
                                           self.timeout)
                if rd:
                    self.handle_request()
                _acquireLock()
                abort = self.abort
                _releaseLock()

    def serve(rcvr, hdlr):
        server = rcvr(handler=hdlr)
        global _listener
        _acquireLock()
        _listener = server
        _releaseLock()
        server.serve_until_stopped()

    return threading.Thread(target=serve, args=(ConfigSocketReceiver, ConfigStreamHandler))

def stopListening():
    """
    Stop the listening server which was created with a call to listen().
    """
    if _listener:
        _acquireLock()
        _listener.abort = 1
        _listener = None
        _releaseLock()


# bicycle repair man stuff

bike_logger_initialised = 0

def init():
    global bike_logger_initialised
    if not bike_logger_initialised:
        log = getLogger("bike")
        h = StreamHandler()
        h.setFormatter(Formatter(
            fmt="%(pathname)s:%(lineno)s:%(levelname)s   %(message)s"))
        log.addHandler(h)
        bike_logger_initialised = 1

if __name__ == "__main__":
    print __doc__
