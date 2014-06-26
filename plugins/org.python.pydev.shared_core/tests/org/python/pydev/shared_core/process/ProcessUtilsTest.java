package org.python.pydev.shared_core.process;

import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

public class ProcessUtilsTest extends TestCase {

    public void testMapAsArrayAndBack() throws Exception {
        Map<String, String> env = new TreeMap<>();
        env.put("a", "10");
        env.put("b", "20");
        String[] mapEnvAsArray = ProcessUtils.getMapEnvAsArray(env);
        assertEquals(ProcessUtils.getEnvironmentAsStr(mapEnvAsArray), "a=10\nb=20");

        Map<String, String> asMap = ProcessUtils.getArrayAsMapEnv(mapEnvAsArray);
        assertEquals(env, asMap);

        mapEnvAsArray = ProcessUtils.addOrReplaceEnvVar(mapEnvAsArray, "c", "ra");
        assertEquals(ProcessUtils.getEnvironmentAsStr(mapEnvAsArray), "a=10\nb=20\nc=ra");
        mapEnvAsArray = ProcessUtils.addOrReplaceEnvVar(mapEnvAsArray, "c", "bar");
        assertEquals(ProcessUtils.getEnvironmentAsStr(mapEnvAsArray), "a=10\nb=20\nc=bar");
        mapEnvAsArray = ProcessUtils.addOrReplaceEnvVar(mapEnvAsArray, "c", "=bar");
        assertEquals(ProcessUtils.getEnvironmentAsStr(mapEnvAsArray), "a=10\nb=20\nc==bar");

        asMap = ProcessUtils.getArrayAsMapEnv(mapEnvAsArray);
        env.put("c", "=bar");
        assertEquals(env, asMap);
        mapEnvAsArray = ProcessUtils.getMapEnvAsArray(asMap);
        assertEquals(ProcessUtils.getEnvironmentAsStr(mapEnvAsArray), "a=10\nb=20\nc==bar");
    }
}
