from pydev_imports import SimpleXMLRPCServer
import select
import sys
import traceback

select_fn = select.select
if sys.platform.startswith('java'):
    select_fn = select.cpython_compatible_select

class InputHookedXMLRPCServer(SimpleXMLRPCServer):
    ''' An XML-RPC Server that can run hooks while polling for new requests.

        This code was designed to allow Debug framework to have a place to run
        commands during idle.
    '''
    def __init__(self, *args, **kwargs):
        SimpleXMLRPCServer.__init__(self, *args, **kwargs)
        self.debug_hook = None

    def setDebugHook(self, debug_hook):
        self.debug_hook = debug_hook

    def serve_forever(self):
        ''' Serve forever, running debug hooks regularly and when idle.
            Does not support shutdown '''
        while True:
            try:
                # Block for default 1/2 second
                timeout = 0.5
                if self.debug_hook:
                    self.debug_hook()
                    timeout = 0.1
                r, unused_w, unused_e = select_fn([self], [], [], timeout)
                if self in r:
                    try:
                        self._handle_request_noblock()
                    except AttributeError:
                        # Older libraries do not support _handle_request_noblock, so fall
                        # back to the handle_request version
                        self.handle_request()
            except (Exception, KeyboardInterrupt):
                self.handle_error(None, None)

    def shutdown(self):
        raise NotImplementedError('InputHookedXMLRPCServer does not support shutdown')

    def handle_error(self, request, client_address):
        type_, value = sys.exc_info()[:2]
        if issubclass(type_, KeyboardInterrupt):
            sys.stderr.write('\n'.join(traceback.format_exception_only(type_, value)))
        else:
            SimpleXMLRPCServer.handle_error(self, request, client_address)
