/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.python.pydev.shared_core.net.SocketUtil;
import org.python.pydev.shared_core.string.StringUtils;

public class PyUnitServer implements IPyUnitServer {

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

    private abstract static class Dispatch {

        private final int expectedParameters;

        Dispatch(int expectedParameters) {
            this.expectedParameters = expectedParameters;
        }

        protected abstract void dispatch(IRequest request);

        public void handle(IRequest request) {
            int parameterCount = request.getParameterCount();
            if (parameterCount != this.expectedParameters) {
                Log.log("Error. Expected " + this.expectedParameters + " parameters in notifyTest. Received: "
                        + parameterCount);
            } else {
                this.dispatch(request);
            }
        }

    }

    private static interface IRequest {

        public String getMethodName();

        public int getParameterCount();

        public Object getParameter(int i);

    }

    private String getAsStr(Object obj) {
        if (obj instanceof byte[]) {
            return StringUtils.safeDecodeByteArray((byte[]) obj, "ISO-8859-1"); //same from server
        }
        return obj.toString();
    }

    /**
     * This is where the handling of xml-rpc methods from the servers is handled and properly translated for listeners.
     */
    private XmlRpcHandler handler = new XmlRpcHandler() {

        public Object execute(final XmlRpcRequest request) throws XmlRpcException {
            return execute(new IRequest() {

                public int getParameterCount() {
                    return request.getParameterCount();
                }

                public Object getParameter(int i) {
                    return request.getParameter(i);
                }

                public String getMethodName() {
                    return request.getMethodName();
                }
            });
        }

        public Object execute(IRequest request) throws XmlRpcException {
            try {
                String method = request.getMethodName();

                Dispatch actual = dispatch.get(method);
                if (actual != null) {
                    actual.handle(request);
                } else {
                    Log.log("Unhandled notification: " + method);
                }

            } catch (Throwable e) {
                //Never return any error here (we don't want to stop running the tests because of some error here).
                Log.log(e);
            }
            return "OK";
        }

    };

    private final HashMap<String, Dispatch> dispatch = new HashMap<String, Dispatch>();

    private void initializeDispatches() {
        dispatch.put("notifyTest", new Dispatch(6) {

            @Override
            public void dispatch(IRequest request) {
                String status = getAsStr(request.getParameter(0));

                String capturedOutput = getAsStr(request.getParameter(1));
                String errorContents = getAsStr(request.getParameter(2));
                String location = getAsStr(request.getParameter(3));
                String test = getAsStr(request.getParameter(4));
                String time = getAsStr(request.getParameter(5));

                for (IPyUnitServerListener listener : listeners) {
                    listener.notifyTest(status, location, test, capturedOutput, errorContents, time);
                }
            }

        });
        dispatch.put("notifyStartTest", new Dispatch(2) {

            @Override
            public void dispatch(IRequest request) {
                String location = getAsStr(request.getParameter(0));
                String test = getAsStr(request.getParameter(1));
                for (IPyUnitServerListener listener : listeners) {
                    listener.notifyStartTest(location, test);
                }

            }
        });
        dispatch.put("notifyTestsCollected", new Dispatch(1) {

            @Override
            public void dispatch(IRequest request) {
                String totalTestsCount = getAsStr(request.getParameter(0));
                for (IPyUnitServerListener listener : listeners) {
                    listener.notifyTestsCollected(totalTestsCount);
                }
            }
        });
        dispatch.put("notifyConnected", new Dispatch(0) {

            @Override
            public void dispatch(IRequest request) {
                // Ignore this one
            }
        });
        dispatch.put("notifyTestRunFinished", new Dispatch(1) {

            @Override
            public void dispatch(IRequest request) {
                for (IPyUnitServerListener listener : listeners) {
                    Object seconds = request.getParameter(0);
                    listener.notifyFinished(getAsStr(seconds));
                }
            }
        });
        dispatch.put("notifyCommands", new Dispatch(1) { //the list of commands as a parameter

                    @Override
                    public void dispatch(IRequest request) {
                        Object requestParam = request.getParameter(0);
                        if (!(requestParam instanceof Object[])) {
                            if (requestParam == null) {
                                Log.log("Expected Object[]. Found: null");
                            } else {
                                Log.log("Expected Object[]. Found: " + requestParam.getClass());
                            }
                            return;
                        }

                        Object[] parameters = (Object[]) requestParam;

                        for (int i = 0; i < parameters.length; i++) {
                            Object param = parameters[i];
                            if (!(param instanceof Object[])) {
                                if (param == null) {
                                    Log.log("Expected Object[]. Found: null");
                                } else {
                                    Log.log("Expected Object[]. Found: " + param.getClass());
                                }
                                return;
                            }

                            final Object[] methodAndParams = (Object[]) param;
                            if (methodAndParams.length != 2) {
                                Log.log("Expected Object[] of len == 2. Found len: " + methodAndParams.length);
                                continue;
                            }
                            if (!(methodAndParams[1] instanceof Object[])) {
                                Log.log("Expected methodAndParams[1] to be Object[]. Found: "
                                        + methodAndParams[1].getClass());
                                continue;
                            }

                            final String methodName = getAsStr(methodAndParams[0]);
                            final Object[] params = (Object[]) methodAndParams[1];

                            Dispatch d = dispatch.get(methodName);
                            if (d != null) {
                                d.handle(new IRequest() {

                                    public int getParameterCount() {
                                        return params.length;
                                    }

                                    public Object getParameter(int i) {
                                        return params[i];
                                    }

                                    public String getMethodName() {
                                        return methodName;
                                    }
                                });
                            }
                        }
                    }
                });
    }

    /**
     * When the launch is removed or terminated, we'll promptly dispose of the server.
     */
    private ILaunchesListener2 launchListener = new ILaunchesListener2() {

        public void launchesRemoved(ILaunch[] launches) {
            if (!disposed) {
                for (ILaunch iLaunch : launches) {
                    if (iLaunch == launch) {
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
            if (!disposed) {
                for (ILaunch iLaunch : launches) {
                    if (iLaunch == launch) {
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
    public PyUnitServer(PythonRunnerConfig config, ILaunch launch) throws IOException {
        initializeDispatches();
        port = SocketUtil.findUnusedLocalPorts(1)[0];
        this.webServer = new WebServer(port);
        XmlRpcServer serverToHandleRawInput = this.webServer.getXmlRpcServer();
        serverToHandleRawInput.setHandlerMapping(new XmlRpcHandlerMapping() {

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
        if (!this.disposed) {
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
    public void dispose() {
        if (!disposed) {
            disposed = true;
            try {
                ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
                launchManager.removeLaunchListener(this.launchListener);
            } catch (Throwable e1) {
                Log.log(e1);
            }

            if (this.webServer != null) {
                try {
                    this.webServer.shutdown();
                } catch (Throwable e) {
                    //Ignore anything here
                }
                this.webServer = null;
            }

            for (IPyUnitServerListener listener : this.listeners) {
                listener.notifyDispose();
            }
            this.listeners.clear();
        }
    }

    public IPyUnitLaunch getPyUnitLaunch() {
        return new PyUnitLaunch(launch, configuration);
    }

}
