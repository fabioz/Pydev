package org.python.pydev.shared_core.yaml;

import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class YamlWrapper {

    public static String dumpAsMap(@SuppressWarnings("rawtypes") Map map) {
        return new Yaml().dumpAsMap(map);
    }

}
