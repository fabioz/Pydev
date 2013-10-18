from pydev_imports import SimpleXMLRPCServer
from pydev_ipython.inputhook import get_inputhook, set_stdin_file
import select
import sys

select_fn = select.select
if sys.platform.startswith('java'):
    select_fn = select.cpython_compatible_select

class InputHookedXMLRPCServer(SimpleXMLRPCServer):
    ''' An XML-RPC Server that can run hooks while polling for new requests.

        This code was designed to work with IPython's inputhook methods and
        to allow Debug framework to have a place to run commands during idle
        too.
    '''
    def __init__(self, *args, **kwargs):
        SimpleXMLRPCServer.__init__(self, *args, **kwargs)
        set_stdin_file(self)
        self.debug_hook = None
    def setDebugHook(self, debug_hook):
        self.debug_hook = debug_hook

    def serve_forever(self):
        ''' Serve forever, running defined hooks regularly and when idle.
            Does not support shutdown '''
        inputhook = get_inputhook()
        while True:
            # Block for default 1/2 second when no GUI is in progress
            timeout = 0.5
            if self.debug_hook:
                self.debug_hook()
                timeout = 0.1
            if inputhook:
                try:
                    inputhook()
                    # The GUI has given us an opportunity to try receiving, normally
                    # this happens because the input hook has already polled the
                    # server has knows something is waiting
                    timeout = 0.020
                except:
                    inputhook = None
            r, unused_w, unused_e = select_fn([self], [], [], timeout)
            if self in r:
                try:
                    self._handle_request_noblock()
                except AttributeError:
                    # Older libraries do not support _handle_request_noblock, so fall
                    # back to the handle_request version
                    self.handle_request()
                # Running the request may have changed the inputhook in use
                inputhook = get_inputhook()

    def shutdown(self):
        raise NotImplementedError('InputHookedXMLRPCServer does not support shutdown')
