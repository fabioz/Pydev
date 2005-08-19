'''
@author Fabio Zadrozny 
'''
#ok, let,s fix some things that may not work on jython depending on the version
try:
    __setFalse = False
except NameError:
    False = 0
    True = 1

import sys
_sys_path = [p for p in sys.path]

_sys_modules = {}
for name,mod in sys.modules.items():
    _sys_modules[name] = mod
    
from java.lang import Thread
import time
import urllib
import jyimportsTipper


DEBUG = False

def dbg(s):
    if DEBUG == True:
        print 'JY_SERVER', s
        
HOST = '127.0.0.1'               # Symbolic name meaning the local host

MSG_KILL_SERVER         = '@@KILL_SERVER_END@@'
MSG_COMPLETIONS         = '@@COMPLETIONS'
MSG_END                 = 'END@@'
MSG_INVALID_REQUEST     = '@@INVALID_REQUEST'
MSG_CHANGE_DIR          = '@@CHANGE_DIR:'
MSG_OK                  = '@@MSG_OK_END@@'
MSG_PROCESSING          = '@@PROCESSING_END@@'
MSG_PROCESSING_PROGRESS = '@@PROCESSING:%sEND@@'
MSG_IMPORTS             = '@@IMPORTS:'
MSG_PYTHONPATH          = '@@PYTHONPATH_END@@'
MSG_CHANGE_PYTHONPATH   = '@@CHANGE_PYTHONPATH:'

BUFFER_SIZE = 1024



currDirModule = None

def CompleteFromDir(dir):
    '''
    This is necessary so that we get the imports from the same dir where the file
    we are completing is located.
    '''
    global currDirModule
    if currDirModule is not None:
        del sys.path[currDirModule]

    sys.path.insert(0, dir)


def ReloadModules():
    '''
    Reload all the modules in sys.modules
    '''
    sys.modules.clear()
    for name,mod in _sys_modules.items():
        sys.modules[name] = mod

def ChangePythonPath(pythonpath):
    '''Changes the pythonpath (clears all the previous pythonpath)
    
    @param pythonpath: string with paths separated by |
    '''
    
    split = pythonpath.split('|')
    sys.path = []
    for path in split:
        path = path.strip()
        if len(path) > 0:
            sys.path.append(path)
    
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
                s = MSG_PROCESSING_PROGRESS % urllib.quote_plus( self.processMsgFunc( ) )
                self.socket.send( s )
            else:
                self.socket.send( MSG_PROCESSING )
            time.sleep( 0.1 )

        dbg( 'sending '+ self.lastMsg)
        self.socket.send( self.lastMsg )
        
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
        if msg:
            return urllib.quote_plus( msg )
        return ' '
    
    def formatCompletionMessage( self, completionsList ):
        '''
        Format the completions suggestions in the following format:
        @@COMPLETIONS((token,description),(token,description),(token,description))END@@
        '''
        compMsg = []
        for tup in completionsList:
            if len( compMsg ) > 0:
                compMsg.append( ',' )
                
            compMsg.append( '(' )
            compMsg.append( str( self.removeInvalidChars( tup[0] ) ) ) #token
            compMsg.append( ',' )
            compMsg.append( self.removeInvalidChars( tup[1] ) ) #description

            if(len(tup) > 2):
                compMsg.append( ',' )
                compMsg.append( self.removeInvalidChars( tup[2] ) ) #args - only if function.
                
            if(len(tup) > 3):
                compMsg.append( ',' )
                compMsg.append( self.removeInvalidChars( tup[3] ) ) #TYPE
                
            compMsg.append( ')' )
        
        return '%s(%s)%s'%( MSG_COMPLETIONS, ''.join( compMsg ), MSG_END )
    
    def getCompletionsMessage( self, completionsList ):
        '''
        get message with completions.
        '''
        return self.formatCompletionMessage( completionsList )
    
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
        import socket
        
        s = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
        s.bind( ( HOST, self.thisPort ) )
        s.listen( 1 ) #socket to receive messages.
        

        #we stay here until we are connected.
        #we only accept 1 client. 
        #the exit message for the server is @@KILL_SERVER_END@@
        conn, addr = s.accept( )
        time.sleep( 0.5 ) #wait a little before connecting to JAVA server

        #after being connected, create a socket as a client.
        self.connectToServer( )
        
        dbg('jycompletionserver Connected by ' + str(addr))
        
        
        while 1:
            data = ''
            returnMsg = ''
            keepAliveThread = KeepAliveThread( self.socket )
            
            while not data.endswith( MSG_END ):
                data += conn.recv( BUFFER_SIZE )
                dbg('received data '+str(data))

            dbg('ok, out of the while... treating received msg')
            
            try:
                try:
                    dbg('making test')
                    if data.find(MSG_KILL_SERVER) != -1:
                        #break if we received kill message.
                        break;
        
                    dbg('starting keep alive thread')
                    keepAliveThread.start( )
                    
                    if data.find(MSG_PYTHONPATH) != -1:
                        comps = []
                        for p in _sys_path:
                            comps.append( ( p, ' ' ) )
                        returnMsg = self.getCompletionsMessage( comps )

                    else:
                        data = data[:data.rfind( MSG_END )]
                    
                        if data.startswith( MSG_IMPORTS ):
                            dbg('ok, generating tips for import msg: '+str(data))
                            data = data.replace( MSG_IMPORTS, '' )
                            data = urllib.unquote_plus( data )
                            comps = jyimportsTipper.GenerateTip( data )
                            returnMsg = self.getCompletionsMessage( comps )
    
                        elif data.startswith( MSG_CHANGE_PYTHONPATH ):
                            data = data.replace( MSG_CHANGE_PYTHONPATH, '' )
                            data = urllib.unquote_plus( data )
                            ChangePythonPath( data )
                            returnMsg = MSG_OK
    
                        elif data.startswith( MSG_CHANGE_DIR ):
                            data = data.replace( MSG_CHANGE_DIR, '' )
                            data = urllib.unquote_plus( data )
                            CompleteFromDir( data )
                            returnMsg = MSG_OK
                            
                        else:
                            returnMsg = MSG_INVALID_REQUEST
                except :
                    dbg('exception ocurred')
                    import traceback
                    import StringIO

                    s = StringIO.StringIO( )
                    exc_info = sys.exc_info( )

                    traceback.print_exception( exc_info[0], exc_info[1], exc_info[2], limit=None, file = s )
                    returnMsg = self.getCompletionsMessage( [( 'ERROR:', '%s'%( s.getvalue( ) ), '' )] )
                    dbg('exception ocurred and returned (%s)' % s.getvalue())
                
            finally:
                keepAliveThread.lastMsg = returnMsg
            
        conn.close( )
        self.ended = True

if __name__ == '__main__':

    thisPort = int( sys.argv[1] )  #this is from where we want to receive messages.
    serverPort = int( sys.argv[2] )#this is where we want to write messages.
    
    t = T( thisPort, serverPort )
    dbg( 'will start' )
    t.start( )

