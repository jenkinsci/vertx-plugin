package org.bravo5.jenkins.vertx;

import hudson.model.listeners.RunListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.Extension;

import java.util.logging.Logger;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.JsonArray;

import hudson.util.XStream2;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;

import java.util.Map;

@Extension
public class GlobalRunListener extends RunListener<Run> {
    private final Logger logger = Logger.getLogger(getClass().getName());

    // {{{ serializeToJson
    private JsonObject serializeToJson(final Object obj) {
        XStream2 xstream = new XStream2(new JsonHierarchicalStreamDriver());

        /*
        from: {
            "some.Class" : {
                "field1":"value",
                …
            }
        }
        to: {
            "@class":"some.Class",
            "field1":"value",
            …
        }
        */

        JsonObject tmpJson = new JsonObject(xstream.toXML(obj));
        
        String className = tmpJson.getFieldNames().iterator().next();

        JsonObject json = tmpJson.getObject(className);
        json.putString("@class", className);

        return json;
    }
    // }}}

    // {{{ runToJson
    private JsonObject runToJson(final Run r) {
        JsonObject json = new JsonObject()
            .putString("id", r.getId())
            .putNumber("num", r.getNumber())
            .putNumber("scheduledTimestamp", r.getTimeInMillis())
            .putString("url", r.getUrl())
            .putString("fullDisplayName", r.getFullDisplayName())
            .putString("externalizableId", r.getExternalizableId())
            .putObject("build", serializeToJson(r))
            .putObject("nextBuild", serializeToJson(r.getNextBuild()))
            .putObject("previousBuild", serializeToJson(r.getPreviousBuild()))
            .putObject(
                "parent",
                serializeToJson(r.getParent())
                    .putString("name", r.getParent().getName())
                    .putString("fullName", r.getParent().getFullName())
                    .putString("url", r.getParent().getUrl())
            )
        ;

        // build up artifacts by hand
        JsonArray artifactsArr = new JsonArray();
        json.putArray("artifacts", artifactsArr);

        if (r.getArtifacts() != null) {
            for (Object artObj : r.getArtifacts()) {
                Run.Artifact artifact = (Run.Artifact) artObj;

                artifactsArr.addObject(
                    new JsonObject()
                        .putString("displayPath", artifact.getDisplayPath())
                        .putString("fileName", artifact.getFileName())
                        // .putNumber("fileSize", artifact.getFileSize())
                        .putString("href", artifact.getHref())
                );
            }
        }

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
