/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.additionalinfo;

import java.util.List;

import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.IDeltaProcessor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.parser.jython.SimpleNode;

/**
 * @author fabioz
 *
 */
public abstract class AbstractAdditionalInfoWithBuild extends AbstractAdditionalDependencyInfo implements IDeltaProcessor<Object>{
    
    
    /**
     * @param callInit
     * @throws MisconfigurationException
     */
    public AbstractAdditionalInfoWithBuild(boolean callInit) throws MisconfigurationException {
        super(callInit);
    }
    
    protected void init() throws MisconfigurationException {
        super.init();
        deltaSaver = createDeltaSaver();
    }

    /**
     * This is the maximum number of deltas that can be generated before saving everything in a big chunk and 
     * clearing the deltas. 50 means that it's something as 25 modules (because usually a module change
     * is composed of a delete and an addition). 
     */
    public static final int MAXIMUN_NUMBER_OF_DELTAS = 50;

    /**
     * If the delta size is big enough, save the current state and discard the deltas.
     */
    private void checkDeltaSize() {
        synchronized (lock) {
            if(deltaSaver.availableDeltas() > MAXIMUN_NUMBER_OF_DELTAS){
                this.save();
            }
        }
    }
    
    /**
     * Used to save things in deltas
     */
    protected DeltaSaver<Object> deltaSaver;
    
    
    @Override
    public void removeInfoFromModule(String moduleName, boolean generateDelta) {
        synchronized (lock) {
            super.removeInfoFromModule(moduleName, generateDelta);
            if(generateDelta){
                this.deltaSaver.addDeleteCommand(moduleName);
                checkDeltaSize();
            }
        }
    }
    

    @Override
    public List<IInfo> addAstInfo(SimpleNode node, String moduleName, IPythonNature nature,
            boolean generateDelta) {
        List<IInfo> addAstInfo = super.addAstInfo(node, moduleName, nature, generateDelta);
        if(generateDelta && addAstInfo.size() > 0){
            deltaSaver.addInsertCommand(addAstInfo);
        }
        return addAstInfo;
    }

    @Override
    protected void restoreSavedInfo(Object o) throws MisconfigurationException {
        synchronized (lock) {
            super.restoreSavedInfo(o);
            //when we do a load, we have to process the deltas that may exist
            if(deltaSaver.availableDeltas() > 0){
                deltaSaver.processDeltas(this);
            }
        }
    }

    protected DeltaSaver<Object> createDeltaSaver() {
        return new DeltaSaver<Object>(
                getPersistingFolder(), 
                "v1_projectinfodelta", 
                new ICallback<Object, String>(){

                    public Object call(String arg) {
                        if(arg.startsWith("STR")){
                            return arg.substring(3);
                        }
                        if(arg.startsWith("LST")){
                            return InfoStrFactory.strToInfo(arg.substring(3));
                        }
                        
                        throw new AssertionError("Expecting string starting with STR or LST");
                    }}, 
                    
                new ICallback<String, Object>() {

                    /**
                     * Here we'll convert the object we added to a string.
                     * 
                     * The objects we can add are:
                     * List<IInfo> -- on addition
                     * String (module name) -- on deletion
                     */
                    public String call(Object arg) {
                        if(arg instanceof String){
                            return "STR"+(String) arg;
                        }
                        if(arg instanceof List){
                            List<IInfo> l = (List<IInfo>) arg;
                            String infoToString = InfoStrFactory.infoToString(l);
                            FastStringBuffer buf = new FastStringBuffer("LST", infoToString.length());
                            buf.append(infoToString);
                            return buf.toString();
                        }
                        throw new AssertionError("Expecting List<IInfo> or String.");
                    }
                }
        );
    }
    

    
    public void processUpdate(Object data) {
        throw new RuntimeException("There is no update generation, only add.");
    }

    public void processDelete(Object data) {
        synchronized (lock) {
            //the moduleName is generated on delete
            this.removeInfoFromModule((String) data, false);
        }
    }
        

    public void processInsert(Object data) {
        synchronized (lock) {
            if(data instanceof IInfo){
                //backward compatibility
                //the IInfo token is generated on insert
                IInfo info = (IInfo) data;
                if(info.getPath() == null || info.getPath().length() == 0){
                    this.add(info, TOP_LEVEL);
                    
                }else{
                    this.add(info, INNER);
                    
                }
            }else if(data instanceof List){
                //current way (saves a list of iinfo)
                for(IInfo info : (List<IInfo>) data){
                    if(info.getPath() == null || info.getPath().length() == 0){
                        this.add(info, TOP_LEVEL);
                        
                    }else{
                        this.add(info, INNER);
                        
                    }
                }
            }
        }
    }

    public void endProcessing() {
        //save it when the processing is finished
        synchronized (lock) {
            this.save();
        }
    }
    
    /**
     * Whenever it's properly saved, clear all the deltas.
     */
    public void save() {
        synchronized (lock) {
            super.save();
            deltaSaver.clearAll();
        }
    }
    
    
}
