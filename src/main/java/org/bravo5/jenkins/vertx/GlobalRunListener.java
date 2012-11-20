package org.bravo5.jenkins.vertx;

import static org.bravo5.jenkins.vertx.SerializeUtil.serializeToJson;

import hudson.model.listeners.RunListener;
import hudson.model.Run;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.Extension;
import hudson.Util;
import jenkins.model.Jenkins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.JsonArray;

/**
 * Broadcasts notifications regarding Runs to the EventBus.
 */
@Extension
public class GlobalRunListener extends RunListener<Run> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // {{{ runToJson
    private JsonObject runToJson(final Run r) {
        JsonObject json = new JsonObject()
            .putObject("build", serializeToJson(r))
            .putObject(
                "parent",
                new JsonObject()
                    .putString("name", r.getParent().getName())
                    .putString("fullName", r.getParent().getFullName())
                    .putString("url", Util.encode(Jenkins.getInstance().getRootUrl() + r.getParent().getUrl()))
            )
        ;

        // JsonObject can't handle putObject("foo", null)
        JsonObject nextBuild = serializeToJson(r.getNextBuild());
        JsonObject previousBuild = serializeToJson(r.getPreviousBuild());

        if (nextBuild == null) {
            json.putString("nextBuild", null);
        } else {
            json.putObject("nextBuild", nextBuild);
        }
        
        if (previousBuild == null) {
            json.putString("previousBuild", null);
        } else {
            json.putObject("previousBuild", previousBuild);
        }
        
        return json;
    }
    // }}}
    
    // {{{ onStarted
    /** {@inheritDoc} */
    @Override
    public void onStarted(final Run r, final TaskListener listener) {
        PluginImpl.ebPublish(
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
        PluginImpl.ebPublish(
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
        PluginImpl.ebPublish(
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
        PluginImpl.ebPublish(
            "jenkins.run",
            new JsonObject()
                .putString("action", "deleted")
                .putObject("run", runToJson(r))
        );
    }
    // }}}
}
