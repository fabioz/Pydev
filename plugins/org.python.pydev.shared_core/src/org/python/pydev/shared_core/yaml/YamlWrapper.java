package org.python.pydev.shared_core.yaml;

import java.util.Map;

import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.LoadSettingsBuilder;
import org.snakeyaml.engine.v2.common.FlowStyle;

public class YamlWrapper {

    public static String dumpAsMap(@SuppressWarnings("rawtypes") Map map) {
        Dump dump = new Dump(
                DumpSettings.builder()
                        .setBestLineBreak("\n")
                        .setMultiLineFlow(true)
                        .setIndent(2)
                        .setDefaultFlowStyle(FlowStyle.BLOCK)
                        .build());
        String output = dump.dumpToString(map);
        return output;
    }

    public static Object load(String yamlContents) {
        LoadSettingsBuilder builder = LoadSettings.builder();
        LoadSettings settings = builder
                .setMaxAliasesForCollections(10)
                .build();
        Load load = new Load(settings);
        return load.loadFromString(yamlContents);
    }

}
