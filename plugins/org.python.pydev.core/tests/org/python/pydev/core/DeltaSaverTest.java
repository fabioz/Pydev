/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 12/10/2005
 */
package org.python.pydev.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.python.pydev.shared_core.callbacks.ICallback;

import junit.framework.TestCase;

public class DeltaSaverTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DeltaSaverTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        new DeltaSaver<Object>(new File("."), "deltatest", null, null).clearAll(); //leave no traces
    }

    public static class DeltaProcessor implements IDeltaProcessor<String> {

        public List<String> state = new ArrayList<String>();

        public int processed;

        @Override
        public void processUpdate(String data) {
            throw new RuntimeException("should not be called");
        }

        @Override
        public void processDelete(String data) {
            processed += 1;
            state.remove(data);
        }

        @Override
        public void processInsert(String data) {
            processed += 1;
            state.add((String) data);
        }

        @Override
        public void endProcessing() {
        }

    }

    public void testSaveRestore() throws Exception {
        DeltaSaver<String> saver = new DeltaSaver<String>(new File("."), "deltatest", getCallBackStr(), getToFileStr());
        saver.addInsertCommand("ins1");
        saver.addInsertCommand("ins2");
        saver.addDeleteCommand("ins1");

        DeltaSaver<String> restorer = new DeltaSaver<String>(new File("."), "deltatest", getCallBackStr(),
                getToFileStr());
        assertEquals(3, restorer.availableDeltas());
        DeltaProcessor deltaProcessor = new DeltaProcessor();
        restorer.processDeltas(deltaProcessor);
        assertEquals(3, deltaProcessor.processed);
        assertEquals(1, deltaProcessor.state.size());
        assertEquals("ins2", deltaProcessor.state.get(0));

        restorer = new DeltaSaver<String>(new File("."), "deltatest", getCallBackStr(), getToFileStr());
        assertEquals(0, restorer.availableDeltas());

    }

    public static class InsertDeltaProcessor implements IDeltaProcessor<Integer> {

        public List<String> state = new ArrayList<String>();

        public int processed;

        @Override
        public void processUpdate(Integer data) {
            throw new RuntimeException("should not be called");
        }

        @Override
        public void processDelete(Integer data) {
            throw new RuntimeException("should not be called");
        }

        @Override
        public void processInsert(Integer data) {
            assertEquals((Object) processed, (Object) data);
            processed += 1;
        }

        @Override
        public void endProcessing() {
        }

    }

    public void testSaveRestore3() throws Exception {
        //check if the order is correct
        DeltaSaver<Integer> saver = new DeltaSaver<Integer>(new File("."), "deltatest", getCallBack(), getToFile());
        for (int i = 0; i < 50; i++) {
            saver.addInsertCommand(i);
        }
        DeltaSaver<Integer> restorer = new DeltaSaver<Integer>(new File("."), "deltatest", getCallBack(), getToFile());
        assertEquals(50, restorer.availableDeltas());
        restorer.processDeltas(new InsertDeltaProcessor());
    }

    private ICallback<String, Integer> getToFile() {
        return new ICallback<String, Integer>() {

            @Override
            public String call(Integer arg) {
                return Integer.toString(arg);
            }
        };
    }

    private ICallback<Integer, String> getCallBack() {
        return new ICallback<Integer, String>() {

            @Override
            public Integer call(String arg) {
                return Integer.parseInt(arg);
            }
        };
    }

    private ICallback<String, String> getToFileStr() {
        return new ICallback<String, String>() {

            @Override
            public String call(String arg) {
                return arg;
            }
        };
    }

    private ICallback<String, String> getCallBackStr() {
        return new ICallback<String, String>() {

            @Override
            public String call(String arg) {
                return arg;
            }
        };
    }

}
