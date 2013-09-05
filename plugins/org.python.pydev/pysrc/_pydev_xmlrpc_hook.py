from pydev_imports import SimpleXMLRPCServer

from pydev_versioncheck import versionok
if not versionok():
    # We don't hook into GUI loops unless the version of Python supports it
    InputHookedXMLRPCServer = SimpleXMLRPCServer
else:

    import select
    from pydev_ipython.inputhook import get_inputhook, set_stdin_file

    class InputHookedXMLRPCServer(SimpleXMLRPCServer):
        ''' An XML-RPC Server that can run hooks while polling for new requests.

            This code was designed to work with IPython's inputhook methods.
        '''
        def __init__(self, *args, **kwargs):
            SimpleXMLRPCServer.__init__(self, *args, **kwargs)
            set_stdin_file(self)

        def serve_forever(self):
            ''' Serve forever, running defined hooks regularly and when idle.
                Does not support shutdown '''
            inputhook = get_inputhook()
            while True:
                # Block for default 1/2 second when no GUI is in progress
                timeout = 0.5
                if inputhook:
                    try:
                        inputhook()
                        # The GUI has given us an opportunity to try receiving, normally
                        # this happens because the input hook has already polled the
                        # server has knows something is waiting
                        timeout = 0.020
                    except:
                        inputhook = None
                r, unused_w, unused_e = select.select([self], [], [], timeout)
                if self in r:
                    self._handle_request_noblock()
                    # Running the request may have changed the inputhook in use
                    inputhook = get_inputhook()

        def shutdown(self):
            raise NotImplementedError('InputHookedXMLRPCServer does not support shutdown')
