#pylint: disable-msg=C0103,R0904
"""
This module demonstrates a possible problem of pyLint with calling __init__ s
from inherited classes.
Initializations done there are not considered, which results in Error E0203 for
self.cookedq.
"""

__revision__ = 'yo'

import telnetlib

class SeeTelnet(telnetlib.Telnet):
    """
    Extension of telnetlib.
    """   
   
    def __init__(self, host=None, port=0):
        """
        Constructor.
        When called without arguments, create an unconnected instance.
        With a hostname argument, it connects the instance; a port
        number is optional.
        Parameter:
        - host: IP address of the host
        - port: Port number
        """
        telnetlib.Telnet.__init__(self, host, port)

    def readUntilArray(self, matches, _=None):
        """
        Read until a given string is encountered or until timeout.
        ...
        """
        self.process_rawq()
        maxLength = 0
        index = -1
        for match in matches:
            index += 1
            if len(match) > maxLength:
                maxLength = len(match)
