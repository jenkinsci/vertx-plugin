package org.bravo5.jenkins.plugins;

import hudson.model.listeners.RunListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.Extension;

import java.util.logging.Logger;

import org.vertx.java.core.json.JsonObject;

import hudson.util.XStream2;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;

import java.util.Map;

@Extension
public class GlobalRunListener extends RunListener<Run> {
    private final Logger logger = Logger.getLogger(getClass().getName());

    // {{{ runToJson
    private JsonObject runToJson(final Run r) {
        XStream2 xstream = new XStream2(new JsonHierarchicalStreamDriver());

        JsonObject json = new JsonObject()
            .putString("id", r.getId())
            .putNumber("num", r.getNumber())
            .putNumber("scheduledTimestamp", r.getTimeInMillis())
            .putString("url", r.getUrl())
            .putString("fullDisplayName", r.getFullDisplayName())
            .putString("externalizableId", r.getExternalizableId())

            // artifacts aren't serializing in a usable fashion
            // .putObject("artifacts", new JsonObject(xstream.toXML(r.getArtifacts())))

            // actions
            // causes
            // duration
            .putObject("build", new JsonObject(xstream.toXML(r)))
            .putObject("nextBuild", new JsonObject(xstream.toXML(r.getNextBuild())))
            .putObject("previousBuild", new JsonObject(xstream.toXML(r.getPreviousBuild())))

            .putObject(
                "parent",
                new JsonObject(xstream.toXML(r.getParent()))
                    .putString("name", r.getParent().getName())
                    .putString("fullName", r.getParent().getFullName())
                    .putString("url", r.getParent().getUrl())
            )
        ;

        return json;
    }
    // }}}
    
    // {{{ onStarted
    /** {@inheritDoc} */
    @Override
    public void onStarted(final Run r, final TaskListener listener) {
        listener.getLogger().println(String.format(
            "in onStarted(%s, …)", r
        ));

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
        listener.getLogger().println(String.format(
            "in onCompleted(%s, …)", r
        ));

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
