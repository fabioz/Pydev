"""A set of utility function to ease the use of OmniORBpy."""

__revision__ = '$Id: corbautils.py,v 1.3 2005-01-21 17:42:05 fabioz Exp $'

from omniORB import CORBA,PortableServer
import CosNaming

orb = None

def get_orb():
    """
    returns a reference to the ORB.
    The first call to the method initialized the ORB
    This method is mainly used internally in the module.
    """
    
    global orb
    if orb is None:
        import sys
        orb = CORBA.ORB_init(sys.argv,CORBA.ORB_ID)
    return orb

def get_root_context():
    """
    returns a reference to the NameService object.
    This method is mainly used internally in the module.
    """
    
    orb = get_orb()
    rootContext = orb.resolve_initial_references("NameService")._narrow(CosNaming.NamingContext)
    assert rootContext is not None,"Failed to narrow root naming context"
    return rootContext

def register_object_name(object, namepath):
    """
    Registers a object in the NamingService.
    The name path is a list of 2-uples (id,kind) giving the path.

    For instance if the path of an object is [('foo',''),('bar','')],
    it is possible to get a reference to the object using the URL
    'corbaname::hostname#foo/bar'.
    [('logilab','rootmodule'),('chatbot','application'),('chatter','server')]
    is mapped to 'corbaname::hostname#logilab.rootmodule/chatbot.application/chatter.server'.
    The get_object_reference() function can be used to resolve such a URL.
    """
    context = get_root_context()
    for id, kind in namepath[:-1]:
        name = [CosNaming.NameComponent(id,kind)]
        try:
            context = context.bind_new_context(name)
        except CosNaming.NamingContext.AlreadyBound, ex:
            context = context.resolve(name)._narrow(CosNaming.NamingContext)
            assert context is not None, 'test context exists but is not a NamingContext'

    id,kind = namepath[-1]
    name = [CosNaming.NameComponent(id,kind)]
    try:
        context.bind(name,object._this())
    except CosNaming.NamingContext.AlreadyBound, ex:
        context.rebind(name,object._this())

def activate_POA():
    """
    This methods activates the Portable Object Adapter.
    You need to call it to enable the reception of messages in your code,
    on both the client and the server.
    """
    orb = get_orb()
    poa = orb.resolve_initial_references('RootPOA')
    poaManager = poa._get_the_POAManager()
    poaManager.activate()

def run_orb():
    """
    Enters the ORB mainloop on the server.
    You should not call this method on the client.
    """
    get_orb().run()

def get_object_reference(url):
    """
    Resolves a corbaname URL to an object proxy.
    See register_object_name() for examples URLs
    """
    return get_orb().string_to_object(url)

def get_object_string(host, namepath):
    """given an host name and a name path as described in register_object_name,
    return a corba string identifier
    """
    strname = '/'.join(['.'.join(path_elt) for path_elt in namepath])
    return 'corbaname::%s#%s' % (host, strname)
