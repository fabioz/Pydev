#@PydevCodeAnalysisIgnore
'''
@author Fabio Zadrozny 
'''

try:
    import java.lang
    True = 1
    False = 0
    IS_JYTHON = True
    SERVER_NAME = 'jycompletionserver'
    from java.lang import Thread
    import jyimportsTipper as importsTipper

except:
    
    #it is python
    IS_JYTHON = False
    SERVER_NAME = 'pycompletionserver'
    from threading import Thread
    import refactoring
    import importsTipper


import sys
#initial sys.path
_sys_path = [p for p in sys.path]

#initial sys.modules
_sys_modules = {}
for name, mod in sys.modules.items():
    _sys_modules[name] = mod


import traceback
import StringIO
import time
import urllib

INFO1 = 1
INFO2 = 2
WARN = 4
ERROR = 8

DEBUG = INFO1 | ERROR

def dbg( s, prior ):
    if prior & DEBUG != 0:
        print s
#        f = open('c:/temp/test.txt', 'a')
#        print >> f, s
#        f.close()
        
HOST = '127.0.0.1'               # Symbolic name meaning the local host

MSG_KILL_SERVER             = '@@KILL_SERVER_END@@'
MSG_COMPLETIONS             = '@@COMPLETIONS'
MSG_END                     = 'END@@'
MSG_INVALID_REQUEST         = '@@INVALID_REQUEST'
MSG_JYTHON_INVALID_REQUEST  = '@@JYTHON_INVALID_REQUEST'
MSG_RELOAD_MODULES          = '@@RELOAD_MODULES_END@@'
MSG_CHANGE_DIR              = '@@CHANGE_DIR:'
MSG_OK                      = '@@MSG_OK_END@@'
MSG_BIKE                    = '@@BIKE'
MSG_PROCESSING              = '@@PROCESSING_END@@'
MSG_PROCESSING_PROGRESS     = '@@PROCESSING:%sEND@@'
MSG_IMPORTS                 = '@@IMPORTS:'
MSG_PYTHONPATH              = '@@PYTHONPATH_END@@'
MSG_CHANGE_PYTHONPATH       = '@@CHANGE_PYTHONPATH:'
MSG_SEARCH                  = '@@SEARCH'

BUFFER_SIZE = 1024



currDirModule = None

def CompleteFromDir( dir ):
    '''
    This is necessary so that we get the imports from the same dir where the file
    we are completing is located.
    '''
    global currDirModule
    if currDirModule is not None:
        del sys.path[currDirModule]

    sys.path.insert( 0, dir )


def ReloadModules():
    '''
    Reload all the modules in sys.modules
    '''
    sys.modules.clear()
    for name, mod in _sys_modules.items():
        sys.modules[name] = mod

def ChangePythonPath( pythonpath ):
    '''Changes the pythonpath (clears all the previous pythonpath)
    
    @param pythonpath: string with paths separated by |
    '''
    
    split = pythonpath.split( '|' )
    sys.path = []
    for path in split:
        path = path.strip()
        if len( path ) > 0:
            sys.path.append( path )
    
class KeepAliveThread( Thread ):
    def __init__( self, socket ):
        Thread.__init__( self )
        self.socket = socket
        self.processMsgFunc = None
        self.lastMsg = None
    
    def run( self ):
        time.sleep( 0.1 )
        while self.lastMsg == None:
            
            if self.processMsgFunc != None:
                s = MSG_PROCESSING_PROGRESS % urllib.quote_plus( self.processMsgFunc() )
                sent = self.socket.send( s )
            else:
                sent = self.socket.send( MSG_PROCESSING )
            if sent == 0:
                sys.exit(0) #connection broken
            time.sleep( 0.1 )

        sent = self.socket.send( self.lastMsg )
        if sent == 0:
            sys.exit(0) #connection broken
        
class T( Thread ):

    def __init__( self, thisP, serverP ):
        Thread.__init__( self )
        self.thisPort   = thisP
        self.serverPort = serverP
        self.socket = None #socket to send messages.
        

    def connectToServer( self ):
        import socket
        
        self.socket = s = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
        s.connect( ( HOST, self.serverPort ) )

    def removeInvalidChars( self, msg ):
        msg = str(msg)
        if msg:
            try:
                return urllib.quote_plus( msg )
            except:
                print 'error making quote plus in', msg
                raise
        return ' '
    
    def formatCompletionMessage( self, defFile, completionsList ):
        '''
        Format the completions suggestions in the following format:
        @@COMPLETIONS(modFile(token,description),(token,description),(token,description))END@@
        '''
        compMsg = []
        compMsg.append( '%s' % defFile )
        for tup in completionsList:
            compMsg.append( ',' )
                
            compMsg.append( '(' )
            compMsg.append( str( self.removeInvalidChars( tup[0] ) ) ) #token
            compMsg.append( ',' )
            compMsg.append( self.removeInvalidChars( tup[1] ) ) #description

            if( len( tup ) > 2 ):
                compMsg.append( ',' )
                compMsg.append( self.removeInvalidChars( tup[2] ) ) #args - only if function.
                
            if( len( tup ) > 3 ):
                compMsg.append( ',' )
                compMsg.append( self.removeInvalidChars( tup[3] ) ) #TYPE
                
            compMsg.append( ')' )
        
        return '%s(%s)%s'%( MSG_COMPLETIONS, ''.join( compMsg ), MSG_END )
    
    def getCompletionsMessage( self, defFile, completionsList ):
        '''
        get message with completions.
        '''
        return self.formatCompletionMessage( defFile, completionsList )
    
    def getTokenAndData( self, data ):
        '''
        When we receive this, we have 'token):data'
        '''
        token = ''
        for c in data:
            if c != ')':
                token += c
            else:
                break;
        
        return token, data.lstrip( token+'):' )

    
    def run( self ):
        # Echo server program
        try:
            import socket
            
            dbg( SERVER_NAME+' creating socket' , INFO1 )
            s = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
            s.bind( ( HOST, self.thisPort ) )
            s.listen( 1 ) #socket to receive messages.
            
    
            #we stay here until we are connected.
            #we only accept 1 client. 
            #the exit message for the server is @@KILL_SERVER_END@@
            dbg( SERVER_NAME+' waiting for connection' , INFO1 )
            conn, addr = s.accept()
            time.sleep( 0.5 ) #wait a little before connecting to JAVA server
    
            dbg( SERVER_NAME+' waiting to java client' , INFO1 )
            #after being connected, create a socket as a client.
            self.connectToServer()
            
            dbg( SERVER_NAME+' Connected by ' + str( addr ), INFO1 )
            
            
            while 1:
                data = ''
                returnMsg = ''
                keepAliveThread = KeepAliveThread( self.socket )
                
                while data.find( MSG_END ) == -1:
                    received = conn.recv( BUFFER_SIZE )
                    if len(received) == 0:
                        sys.exit(0) #ok, connection ended
                    data += received
    
                try:
                    try:
                        if data.find( MSG_KILL_SERVER ) != -1:
                            dbg( SERVER_NAME+' kill message received', INFO1 )
                            #break if we received kill message.
                            self.ended = True
                            sys.exit(0)
            
                        dbg( SERVER_NAME+' starting keep alive thread', INFO2 )
                        keepAliveThread.start()
                        
                        if data.find( MSG_PYTHONPATH ) != -1:
                            comps = []
                            for p in _sys_path:
                                comps.append( ( p, ' ' ) )
                            returnMsg = self.getCompletionsMessage( None, comps )
    
                        elif data.find( MSG_RELOAD_MODULES ) != -1:
                            ReloadModules()
                            returnMsg = MSG_OK
                        
                        else:
                            data = data[:data.rfind( MSG_END )]
                        
                            if data.startswith( MSG_IMPORTS ):
                                data = data.replace( MSG_IMPORTS, '' )
                                data = urllib.unquote_plus( data )
                                defFile, comps = importsTipper.GenerateTip( data )
                                returnMsg = self.getCompletionsMessage( defFile, comps )
        
                            elif data.startswith( MSG_CHANGE_PYTHONPATH ):
                                data = data.replace( MSG_CHANGE_PYTHONPATH, '' )
                                data = urllib.unquote_plus( data )
                                ChangePythonPath( data )
                                returnMsg = MSG_OK
        
                            elif data.startswith( MSG_SEARCH ):
                                data = data.replace( MSG_SEARCH, '' )
                                data = urllib.unquote_plus( data )
                                (f, line, col), foundAs = importsTipper.Search(data)
                                returnMsg = self.getCompletionsMessage(f, [(line, col, foundAs)])
                                
                            elif data.startswith( MSG_CHANGE_DIR ):
                                data = data.replace( MSG_CHANGE_DIR, '' )
                                data = urllib.unquote_plus( data )
                                CompleteFromDir( data )
                                returnMsg = MSG_OK
                                
                            elif data.startswith( MSG_BIKE ): 
                                if IS_JYTHON:
                                    returnMsg = MSG_JYTHON_INVALID_REQUEST
                                else:
                                    data = data.replace( MSG_BIKE, '' )
                                    data = urllib.unquote_plus( data )
                                    returnMsg = refactoring.HandleRefactorMessage( data, keepAliveThread )
                                
                            else:
                                returnMsg = MSG_INVALID_REQUEST
                    except :
                        dbg( SERVER_NAME+' exception ocurred', ERROR )
                        s = StringIO.StringIO()
                        traceback.print_exc(file = s)
    
                        err = s.getvalue()
                        dbg( SERVER_NAME+' received error: '+str(err), ERROR )
                        returnMsg = self.getCompletionsMessage( None, [( 'ERROR:', '%s'%( err ), '' )] )
                    
                finally:
                    keepAliveThread.lastMsg = returnMsg
                
            conn.close()
            self.ended = True
            sys.exit(0) #connection broken
            
            
        except:
            s = StringIO.StringIO()
            exc_info = sys.exc_info()

            traceback.print_exception( exc_info[0], exc_info[1], exc_info[2], limit=None, file = s )
            err = s.getvalue()
            dbg( SERVER_NAME+' received error: '+str( err ), ERROR )
            raise

if __name__ == '__main__':

    thisPort = int( sys.argv[1] )  #this is from where we want to receive messages.
    serverPort = int( sys.argv[2] )#this is where we want to write messages.
    
    t = T( thisPort, serverPort )
    dbg( SERVER_NAME+' will start', INFO1 )
    t.start()

