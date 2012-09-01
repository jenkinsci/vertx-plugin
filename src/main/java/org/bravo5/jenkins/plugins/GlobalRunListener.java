package org.bravo5.jenkins.plugins;

import hudson.model.listeners.RunListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.Extension;

import java.util.logging.Logger;

import org.vertx.java.core.json.JsonObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;

import java.util.Map;

@Extension
public class GlobalRunListener extends RunListener<Run> {
    private final Logger logger = Logger.getLogger(getClass().getName());

    private final ObjectMapper objectMapper =
        new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
            .configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true)
            .configure(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS, false);
    
    // {{{ runToJson
    private JsonObject runToJson(final Run r) {
        String resultStr = "unknown";
        
        if (r.getResult() != null) {
            resultStr = r.getResult().toString();
        }
        
        return new JsonObject()
            .putString("id", r.getId())
            .putNumber("num", r.getNumber())
            .putString("result", resultStr)
            .putNumber("timestamp", r.getTimeInMillis())
            .putString("url", r.getUrl())
        ;
    }
    // }}}
    
    // {{{ onStarted
    /** {@inheritDoc} */
    @Override
    public void onStarted(final Run r, final TaskListener listener) {
        listener.getLogger().println(String.format(
            "in onStarted(%s, …)", r
        ));

        PluginImpl
            .getVertx()
            .eventBus()
            .publish(
                "jenkins.run",
                new JsonObject()
                    .putString("action", "started")
                    .putObject("run", runToJson(r))
            );
    }
    // }}}
    
    // {{{ onCompleted
    /** {@inheritDoc} */
    @Override
    public void onCompleted(final Run r, final TaskListener listener) {
        listener.getLogger().println(String.format(
            "in onCompleted(%s, …)", r
        ));

        PluginImpl
            .getVertx()
            .eventBus()
            .publish(
                "jenkins.run",
                new JsonObject()
                    .putString("action", "completed")
                    .putObject("run", runToJson(r))
            );
    }
    // }}}
    
    // {{{ onFinalized
    /** {@inheritDoc} */
    @Override
    public void onFinalized(final Run r) {
        PluginImpl
            .getVertx()
            .eventBus()
            .publish(
                "jenkins.run",
                new JsonObject()
                    .putString("action", "finalized")
                    .putObject("run", runToJson(r))
            );

    }
    // }}}
    
    // {{{ onDeleted
    /** {@inheritDoc} */
    @Override
    public void onDeleted(final Run r) {
        PluginImpl
            .getVertx()
            .eventBus()
            .publish(
                "jenkins.run",
                new JsonObject()
                    .putString("action", "deleted")
                    .putObject("run", runToJson(r))
            );

    }
    // }}}
}
