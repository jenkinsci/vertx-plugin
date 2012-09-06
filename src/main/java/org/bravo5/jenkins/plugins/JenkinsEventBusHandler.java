package org.bravo5.jenkins.plugins;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.Handler;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import jenkins.model.Jenkins;
import hudson.model.AbstractProject;

import hudson.model.Action;
import hudson.model.ParametersAction;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;


public class JenkinsEventBusHandler implements Handler<Message<JsonObject>>{
    private final Logger logger = Logger.getLogger(getClass().getName());

    private final Jenkins jenkins = Jenkins.getInstance();
    private Collection<String> registeredHandlerIds = new HashSet<>();

    private EventBus eventBus;

    // {{{ constructor
    public JenkinsEventBusHandler(final EventBus eventBus) {
        this.eventBus = eventBus;

        registeredHandlerIds.add(eventBus.registerHandler("jenkins", this));
    }
    // }}}

    // {{{ close
    public void close() {
        for (String handlerId : registeredHandlerIds) {
            eventBus.unregisterHandler(handlerId);
        }
    }
    // }}}

    // {{{ handle
    /** {@inheritDoc} */
    public void handle(final Message<JsonObject> msg) {
        String action = msg.body.getString("action");

        if (action == null) {
            sendError(msg, "no action provided");
        } else {
            try {
                switch (action) {
                    case "scheduleBuild":
                        scheduleBuild(msg);
                        break;

                    default:
                        sendError(msg, "unknown action " + action);
                        break;
                }
            } catch (Exception e) {
                sendError(msg, "error invoking " + action + ": " + e.getMessage(), e);
            }
        }
    }
    // }}}

    // {{{ sendError
    private void sendError(final Message<JsonObject> message, final String error) {
        sendError(message, error, null);
    }
    // }}}

    // {{{ sendError
    private void sendError(final Message<JsonObject> message,
                           final String error,
                           final Exception e)
    {
        logger.log(Level.SEVERE, error, e); // RAAAAAAGE
        
        JsonObject json = new JsonObject()
            .putString("status", "error")
            .putString("message", error);

        message.reply(json);
    }
    // }}}

    // {{{ sendOk
    private void sendOk(final Message<JsonObject> message) {
        sendOk(message, null);
    }
    // }}}

    // {{{ sendOk
    private void sendOk(final Message<JsonObject> message, final JsonObject obj) {
        sendStatus("ok", message, obj);
    }
    // }}}

    // {{{ sendStatus
    private void sendStatus(final String status,
                            final Message<JsonObject> message,
                            final JsonObject obj)
    {
        JsonObject resp = new JsonObject()
            .putString("status", status);

        if (obj != null) {
            resp.putObject("result", obj);
        }

        message.reply(resp);
    }
    // }}}

    // {{{ scheduleBuild
    private void scheduleBuild(final Message<JsonObject> msg) {
        /*
        {
            "data" : {
                "projectName" : "foo"
                // optional
                , "params" : {
                    "param1key": "value"
                    , …
                }
            }
        }
        */
        JsonObject json = msg.body.getObject("data");

        if (json == null) {
            sendError(msg, "missing job data");
        } else {
            AbstractProject project =
                (AbstractProject) jenkins.getItem(json.getString("projectName"));

            Action[] actions = new Action[]{};

            JsonObject params = json.getObject("params");
            if (params != null) {
                List<ParameterValue> paramVals = new ArrayList<>();

                for (String fieldName : params.getFieldNames()) {
                    paramVals.add(
                        new StringParameterValue(fieldName,
                                                 params.getString(fieldName))
                    );
                }

                actions = new Action[] { new ParametersAction(paramVals) };
            }

            if (project.scheduleBuild(0, new VertxCause(), actions)) {
                sendOk(msg);
            } else {
                sendError(msg, "failed to schedule");
            }
        }
    }
    // }}}
}
