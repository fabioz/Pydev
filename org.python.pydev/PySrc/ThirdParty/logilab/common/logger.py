# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.
#
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
""" Copyright (c) 2002-2003 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr

Define a logger interface and two concrete loggers : one which prints
everything on stdout, the other using syslog.
"""

__revision__ = "$Id: logger.py,v 1.2 2004-10-26 14:18:34 fabioz Exp $"


import sys
import traceback
import time

LOG_EMERG   = 0
LOG_ALERT   = 1
LOG_CRIT    = 2
LOG_ERR     = 3
LOG_WARN    = 4
LOG_NOTICE  = 5
LOG_INFO    = 6
LOG_DEBUG   = 7

INDICATORS = ['emergency', 'alert', 'critical', 'error',
              'warning', 'notice', 'info', 'debug']


def make_logger(method='print', threshold=LOG_DEBUG, sid=None):
    """return a logger for the given method
    
    known methods are 'print', 'eprint' and syslog'
    """
    if method == 'print':
        return PrintLogger(threshold, sid=sid)
    elif method == 'eprint':
        return PrintLogger(threshold, sys.stderr, sid)
    elif method == 'syslog':
        return SysLogger(threshold, sid)
    else:
        raise 'UnknownLogger'


class AbstractLogger:
    """logger interface.
    Priorities allow to filter on the importance of events
    An event gets logged if it's priority is lower than the threshold"""

    def __init__(self, threshold=LOG_DEBUG, priority_indicator=1):
        self.threshold = threshold
        self.priority_indicator = priority_indicator
        
    def log(self, priority=LOG_DEBUG, message='', substs=None):
        """log a message with priority <priority>
        substs are optional substrings
        """
        #print 'LOG', self, priority, self.threshold, message
        if priority <= self.threshold :
            if substs is not None:
                message = message % substs
            if self.priority_indicator:
                message = '[%s] %s' % (INDICATORS[priority], message)
            self._writelog(priority, message)

    def _writelog(self, priority, message):
        """Override this method in concrete class """
        raise NotImplementedError()

    def log_traceback(self, priority=LOG_ERR, tb_info=None):
        """log traceback information with priority <priority>
        """
        assert tb_info is not None
        e_type, value, tbck = tb_info
        stacktb = traceback.extract_tb(tbck)
        l = ['Traceback (most recent call last):']
        for stackentry in stacktb :
            if stackentry[3]:
                plus = '\n    %s' % stackentry[3]
            else:
                plus = ''
            l.append('filename="%s" line_number="%s" function_name="%s"%s' %
                     (stackentry[0], stackentry[1], stackentry[2], plus))
        l.append('%s: %s' % (e_type, value))
        self.log(priority, '\n'.join(l))


class PrintLogger(AbstractLogger):
    """logger implementation

    log everything to a file, using the standard output by default
    """
    
    def __init__(self, threshold, output=sys.stdout, sid=None):
        AbstractLogger.__init__(self, threshold)
        self.output = output
        self.sid = sid
        
    def _writelog(self, priority, message):
        """overriden from AbstractLogger"""
        if self.sid is not None:
            self.output.write('[%s] [%s] %s\n' % (time.asctime(), self.sid,
                                                  message))
        else:
            self.output.write('[%s] %s\n' % (time.asctime(), message))


class SysLogger(AbstractLogger):
    """ logger implementation

    log everything to syslog daemon
    use the LOCAL_7 facility
    """

    def __init__(self, threshold, sid):
        import syslog
        AbstractLogger.__init__(self, threshold)
        if sid is None:
            sid = 'syslog'
        syslog.openlog(sid, syslog.LOG_PID)
        
    def _writelog(self, priority, message):
        """overriden from AbstractLogger"""
        import syslog
        syslog.syslog(priority | syslog.LOG_LOCAL7, message)

