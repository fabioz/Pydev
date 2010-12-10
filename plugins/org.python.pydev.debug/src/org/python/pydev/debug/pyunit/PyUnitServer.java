package org.python.pydev.debug.pyunit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcNoSuchHandlerException;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.webserver.WebServer;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.SocketUtil;

public class PyUnitServer implements IPyUnitServer  {
    
    /**
     * Server used to get information on the tests running.
     */
    protected WebServer webServer;
    
    /**
     * The port to be used to communicate with the python server.
     */
    protected int port;
    
    /**
     * The configuration used to do the launch.
     */
    protected ILaunchConfiguration configuration;
    
    /**
     * The launch to which we're connecting.
     */
    protected ILaunch launch;
    
    /**
     * Whether or not we've been already disposed.
     */
    protected boolean disposed = false;
    
    /**
     * Listeners interested in knowing what happens in the python process should be registered here.
     */
    protected List<IPyUnitServerListener> listeners = new ArrayList<IPyUnitServerListener>();

    /**
     * This is where the handling of xml-rpc methods from the servers is handled and properly translated for listeners.
     */
    private XmlRpcHandler handler = new XmlRpcHandler() {
        

        public Object execute(XmlRpcRequest request) throws XmlRpcException {
            try{
                String method = request.getMethodName();
                int parameterCount = request.getParameterCount();
                if("notifyTest".equals(method)){
                    if(parameterCount != 6){
                        PydevPlugin.log("Error. Expected 6 parameters in notifyTest. Received: "+parameterCount);
                    }else{
                        String status = request.getParameter(0).toString();
                        String capturedOutput = request.getParameter(1).toString();
                        String errorContents = request.getParameter(2).toString();
                        String location = request.getParameter(3).toString();
                        String test = request.getParameter(4).toString();
                        String time = request.getParameter(5).toString();
                        
                        for(IPyUnitServerListener listener:listeners){
                            listener.notifyTest(status, location, test, capturedOutput, errorContents, time);
                        }
                    }
                    
                }else if("notifyTestsCollected".equals(method)){
                    if(parameterCount != 1){
                        PydevPlugin.log("Error. Expected 1 parameters in notifyTestsCollected. Received: "+parameterCount);
                    }else{
                        String totalTestsCount = request.getParameter(0).toString();
                        for(IPyUnitServerListener listener:listeners){
                            listener.notifyTestsCollected(totalTestsCount);
                        }
                    }
                    
                }else if("notifyConnected".equals(method)){
                    //ignore this one
                    
                }else if("notifyTestRunFinished".equals(method)){
                    for(IPyUnitServerListener listener:listeners){
                        listener.notifyFinished();
                    }
                    
                }else{
                    Log.log("Unhandled notification: "+method);
                }
            }catch(Throwable e){
                //Never return any error here (we don't want to stop running the tests because of some error here).
                PydevPlugin.log(e);
            }
            return "OK";
        }
        
    };
    
    
    /**
     * When the launch is removed or terminated, we'll promptly dispose of the server.
     */
    private ILaunchesListener2 launchListener = new ILaunchesListener2() {
        

        public void launchesRemoved(ILaunch[] launches) {
            if(!disposed){
                for (ILaunch iLaunch : launches) {
                    if(iLaunch == launch){
                        dispose();
                    }
                }
            }
        }

        public void launchesAdded(ILaunch[] launches) {
        }

        public void launchesChanged(ILaunch[] launches) {
        }

        public void launchesTerminated(ILaunch[] launches) {
            if(!disposed){
                for (ILaunch iLaunch : launches) {
                    if(iLaunch == launch){
                        dispose();
                    }
                }
            }
        }
    };

    
    /**
     * As we need to be able to relaunch, we store the configuration and launch here (although the relaunch won't be
     * actually done in this class, this is the place to get information on it).
     * 
     * @param config used to get the configuration of the launch.
     * @param launch the actual launch
     * @throws IOException
     */
    public PyUnitServer(PythonRunnerConfig config, ILaunch launch) throws IOException{
        port = SocketUtil.findUnusedLocalPorts(1)[0];
        SocketUtil.checkValidPort(port);
        this.webServer = new WebServer(port);
        XmlRpcServer serverToHandleRawInput = this.webServer.getXmlRpcServer();
        serverToHandleRawInput.setHandlerMapping(new XmlRpcHandlerMapping(){


            public XmlRpcHandler getHandler(String handlerName) throws XmlRpcNoSuchHandlerException, XmlRpcException {
                return handler;
            }
        });
        
        this.webServer.start();

        
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        launchManager.addLaunchListener(this.launchListener);
        this.launch = launch;
        this.configuration = config.getLaunchConfiguration();
    }

    /**
     * Want to hear about what happens in the test running session?
     */
    public void registerOnNotifyTest(IPyUnitServerListener listener) {
        if(!this.disposed){
            this.listeners.add(listener);
        }
    }
    
    /**
     * @return the port being used to communicate with the python side.
     */
    public int getPort() {
        return port;
    }

    /**
     * Disposes of the pyunit server. When the launch is terminated or removed from the launch manager, it's
     * automatically disposed.
     */
    public void dispose(){
        if(!disposed){
            disposed = true;
            try {
                ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
                launchManager.removeLaunchListener(this.launchListener);
            } catch (Throwable e1) {
                PydevPlugin.log(e1);
            }
            
            if(this.webServer != null){
                try {
                    this.webServer.shutdown();
                } catch (Throwable e) {
                    //Ignore anything here
                }
                this.webServer = null;
            }
            
            for(IPyUnitServerListener listener:this.listeners){
                listener.notifyDispose();
            }
            this.listeners.clear();
        }
    }
    

    
    public IPyUnitLaunch getPyUnitLaunch() {
        return new PyUnitLaunch(launch, configuration);
    }

    
}
