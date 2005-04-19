"""M2Crypto.SSL.Connection

Copyright (c) 1999-2004 Ng Pheng Siong. All rights reserved."""

RCS_id='$Id: Connection1.py,v 1.2 2005-04-19 14:39:15 fabioz Exp $'

#Some code deleted here

class Connection:

    """An SSL connection."""

    def __init__(self, ctx, sock=None):
        print 'init Connection'
