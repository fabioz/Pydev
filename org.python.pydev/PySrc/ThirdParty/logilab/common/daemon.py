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

a daemon mix-in class
"""

__revision__ = '$Id: daemon.py,v 1.1 2004-10-26 12:52:29 fabioz Exp $'

import os, signal, sys, time
from logilab.common.logger import make_logger, LOG_ALERT, LOG_NOTICE

class DaemonMixIn:
    """ mixin to make a daemon from watchers/queriers
    """

    def __init__(self, configmod) :
        self.delay = configmod.DELAY
        self.name = str(self.__class__).split('.')[-1]
        self._pid_file = os.path.join('/tmp', '%s.pid'%self.name)
        if os.path.exists(self._pid_file):
            raise '''Another instance of %s must be running.
If it i not the case, remove the file %s''' % (self.name, self._pid_file)
        self._alive = 1
        self._sleeping = 0
        treshold = configmod.LOG_TRESHOLD
        if configmod.NODETACH:
            configmod.log = make_logger('print', treshold, self.name).log
        else:
            configmod.log = make_logger('syslog', treshold, self.name).log
        self.config = configmod

    def _daemonize(self):
        if not self.config.NODETACH:
            # fork so the parent can exist
            if (os.fork()):
                return -1
            # deconnect from tty and create a new session
            os.setsid()
            # fork again so the parent, (the session group leader), can exit.
            # as a non-session group leader, we can never regain a controlling
            # terminal.
            if (os.fork()):
                return -1
            # move to the root to avoit mount pb
            os.chdir('/')
            # set paranoid umask
            os.umask(077)
            # write pid in a file
            f = open(self._pid_file, 'w')
            f.write(str(os.getpid()))
            f.close()
            # close standard descriptors
            sys.stdin.close()
            sys.stdout.close()
            sys.stderr.close()
            # put signal handler
            signal.signal(signal.SIGTERM, self.signal_handler)
            signal.signal(signal.SIGHUP, self.signal_handler)
		
        
    def run(self):
        """ optionaly go in daemon mode and
        do what concrete classe has to do and pauses for delay between runs
        If self.delay is negative, do a pause before starting
        """
        if self._daemonize() == -1:
            return
        self.config.log(LOG_NOTICE, '%s instance started' % self.name)
        if self.delay < 0:
            self.delay = -self.delay
            time.sleep(self.delay)
        while 1:
            try:
                self._run()
            except Exception, e:
                # display for info, sleep, and hope the problem will be solved
                # later.
                self.config.log(LOG_ALERT, 'Internal error: %s'%(e))
            if not self._alive:
                break
            try:
                self._sleeping = 1
                time.sleep(self.delay)
                self._sleeping = 0
            except SystemExit:
                break
        self.config.log(LOG_NOTICE, '%s instance exited'%self.name)
        # remove pid file
        os.remove(self._pid_file)
        
    def signal_handler(self, sig_num, stack_frame):
        if sig_num == signal.SIGTERM:
            if self._sleeping:
                # we are sleeping so we can exit without fear
                self.config.log(LOG_NOTICE, 'exit on SIGTERM')
                sys.exit(0)
            else:
                self.config.log(LOG_NOTICE, 'exit on SIGTERM (on next turn)')
                self._alive = 0
        elif sig_num == signal.SIGHUP:
            self.config.log(LOG_NOTICE, 'reloading configuration on SIGHUP')
            reload(self.config)

    def _run(self):
        """should be overidden in the mixed class"""
        raise NotImplementedError()
    
## command line utilities ######################################################

L_OPTIONS = ["help", "log=", "delay=", 'no-detach']
S_OPTIONS = 'hl:d:n'

def print_help(modconfig):
    print """  --help or -h
    displays this message
  --log <log_level>
    log treshold (7 record everything, 0 record only emergency.)
    Defaults to %s
  --delay <delay>
    the number of seconds between two runs.
    Defaults to %s""" % (modconfig.LOG_TRESHOLD, modconfig.DELAY)

def handle_option(modconfig, opt_name, opt_value, help_meth):
    if opt_name in ('-h','--help'):            
        help_meth()
        sys.exit(0)
    elif opt_name in ('-l','--log'):
        modconfig.LOG_TRESHOLD = int(opt_value)
    elif opt_name in ('-d', '--delay'):
        modconfig.DELAY = int(opt_value)
    elif opt_name in ('-n', '--no-detach'):
        modconfig.NODETACH = 1
