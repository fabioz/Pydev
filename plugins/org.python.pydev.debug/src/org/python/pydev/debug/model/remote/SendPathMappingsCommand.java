package org.python.pydev.debug.model.remote;

import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.json.eclipsesource.JsonArray;
import org.python.pydev.json.eclipsesource.JsonObject;

public class SendPathMappingsCommand extends AbstractDebuggerCommand {

    private final String jsonPathMapping;
    private final boolean debugPathMappings;
    private final boolean force;

    public SendPathMappingsCommand(AbstractDebugTarget debugger, String jsonPathMapping, boolean debugPathMappings,
            boolean force) {
        super(debugger);
        this.jsonPathMapping = jsonPathMapping.strip();
        this.debugPathMappings = debugPathMappings;
        this.force = force;
    }

    @Override
    public String getOutgoing() {
        JsonObject root = new JsonObject();
        root.add("debug", this.debugPathMappings);
        root.add("force", this.force);

        JsonArray pathMappings;
        try {
            if (jsonPathMapping.length() == 0) {
                pathMappings = JsonArray.readFrom("[]");
            } else {
                pathMappings = JsonArray.readFrom(jsonPathMapping);
            }
            root.add("pathMappings", pathMappings);
        } catch (Exception e) {
            Log.log(e);
        }

        return makeCommand(AbstractDebuggerCommand.CMD_SET_PATH_MAPPING_JSON, sequence, root.toString());
    }

}
