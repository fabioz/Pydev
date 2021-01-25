import sys
IS_PY2 = sys.version_info < (3,)
del sys

import importlib
if IS_PY2:
  def _CloneModule(name):
    import imp
    proxy_module = imp.new_module(name)
    real_module = importlib.import_module(name)
    for attr in dir(real_module):
      setattr(proxy_module, attr, getattr(real_module, attr))
    return proxy_module
else:
  def _CloneModule(name):
    import types
    proxy_module = types.ModuleType(name)
    real_module = importlib.import_module(name)
    for attr in dir(real_module):
      setattr(proxy_module, attr, getattr(real_module, attr))
    return proxy_module

# Capture a clone of modules so that if parts of them are mocked out by a
# target, it does not impact the execution of pydev.
# Usage:
#  import x --> x = CloneModule("x")
#  import x as y --> y = CloneModule("x")

threading = _CloneModule("threading")
time = _CloneModule("time")
socket = _CloneModule("socket")
select = _CloneModule("select")

if IS_PY2:
    thread = _CloneModule("thread")
    _queue = _CloneModule("Queue")
    xmlrpclib = _CloneModule("xmlrpclib")
    _pydev_SimpleXMLRPCServer = _CloneModule("SimpleXMLRPCServer")
    BaseHTTPServer = _CloneModule("BaseHTTPServer")
else:
    thread = _CloneModule("_thread")
    _queue = _CloneModule("queue")
    xmlrpclib = _CloneModule("xmlrpc.client")
    _pydev_SimpleXMLRPCServer = _CloneModule("xmlrpc.server")
    BaseHTTPServer = _CloneModule("http.server")

del _CloneModule
del importlib
del IS_PY2
