"""log utilities

Copyright (c) 2003-2004 LOGILAB S.A. (Paris, FRANCE), all rights reserved.
http://www.logilab.fr/ -- mailto:contact@logilab.fr
"""

__revision__ = "$Id: logservice.py,v 1.2 2005-01-21 17:42:03 fabioz Exp $"

from logilab.common.logger import make_logger, LOG_ERR, LOG_WARN, LOG_NOTICE, \
     LOG_INFO, LOG_DEBUG

def init_log(treshold, method='eprint', sid='common-log-service',
             logger=None):
    """init the logging system and and log methods to builtins"""
    #print 'INIT LOG', treshold, logger
    if logger is None:
        logger = make_logger(method, treshold, sid)
    # add log functions and constants to builtins
    #print 'LOGGER -->', logger
    __builtins__.update({'log': logger.log,
                         'log_traceback' : logger.log_traceback,
                         'LOG_ERR':    LOG_ERR,
                         'LOG_WARN':   LOG_WARN,
                         'LOG_NOTICE': LOG_NOTICE,
                         'LOG_INFO' :  LOG_INFO,
                         'LOG_DEBUG':  LOG_DEBUG,
                         })

init_log(LOG_ERR)


