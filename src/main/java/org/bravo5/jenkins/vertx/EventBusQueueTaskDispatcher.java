package org.bravo5.jenkins.vertx;

import hudson.Extension;
import hudson.model.queue.QueueTaskDispatcher;

import jenkins.model.Jenkins;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.Queue;
import hudson.model.Action;
import hudson.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.Handler;

import static org.bravo5.jenkins.vertx.SerializeUtil.serializeToJson;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * QueueTaskDispatcher that gets exposed to the EventBus.
 */
@Extension
public class EventBusQueueTaskDispatcher
    extends QueueTaskDispatcher
    implements Handler<Message<JsonObject>>
{
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private static final String QTD_ADDR = "jenkins.queueTaskDispatcher";

    private EventBus eventBus;

    /**
     * Address of EventBus handler that has registered for QueueTaskDispatcher 
     * actions.
     */
    private String registeredHandlerId;
    
    /**
     * A little short-hand.
     */
    private final Jenkins jenkins = Jenkins.getInstance();

    // {{{ setEventBus
    /** 
     * Setter for eventBus.
     *
     * @param eventBus new value for eventBus
     */
    public void setEventBus(final EventBus eventBus) {
        this.eventBus = eventBus;
    }
    // }}}
    
    // {{{ init
    public void init() {
        eventBus.registerHandler(QTD_ADDR, this);
    }
    // }}}
    
    // {{{ close
    /**
     * Called by {@link PluginImpl} to shut us down.
     */
    public void close() {
        eventBus.unregisterHandler(QTD_ADDR, this);
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
                    case "register":
                        registerHandler(msg);
                        break;

                    case "unregister":
                        unregisterHandler(msg);
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
    
    // {{{ registerHandler
    private void registerHandler(final Message<JsonObject> msg) {
        /*
        {
            "action": "register",
            "handlerAddress" : queueTaskDispatcherId
        }
        */
        
        String handlerAddr = msg.body.getString("handlerAddress");
        
        if (handlerAddr == null) {
            sendError(msg, "missing handlerAddress");
        } else {
            if (registeredHandlerId != null) {
                logger.warn(
                    "replacing existing handler {} with {}",
                    registeredHandlerId, handlerAddr
                );
            }

            registeredHandlerId = handlerAddr;
            sendOk(msg);
        }
    }
    // }}}
    
    // {{{ unregisterHandler
    private void unregisterHandler(final Message<JsonObject> msg) {
        /*
        {
            "action": "unregister",
            "handlerAddress" : queueTaskDispatcherId
        }
        */
        
        String handlerAddr = msg.body.getString("handlerAddress");
        
        if (handlerAddr == null) {
            sendError(msg, "missing handlerAddress");
        } else {
            if (handlerAddr.equals(registeredHandlerId)) {
                registeredHandlerId = null;
                sendOk(msg);
            } else {
                sendError(msg, "handler ID mismatch");
            }
        }
    }
    // }}}
    
    // {{{ canRun
    /** {@inheritDoc} */
    @Override
    public CauseOfBlockage canRun(final Queue.Item queueItem) {
        final String _handlerId = registeredHandlerId;

        CauseOfBlockage cause = null;
        
        if (registeredHandlerId == null) {
            logger.debug("no handler registered");
        } else {
            // invoke EventBus#send() in a thread and use a BlockingQueue to
            // retrieve the reply.  This appears to be the only way to work with
            // Vert.x in a synchronous fashion.
            
            final BlockingQueue<JsonObject> replyQueue = new ArrayBlockingQueue<>(1);
            
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        // can't use serializeToJson() here; that'll cause an
                        // infinite loop!  I think it's due to "why".

                        /*
                        {
                            "action":"canRun",
                            "item":{
                                "actions":[
                                    {
                                        "causes":[{"shortDescription":"Started by user anonymous","userId":null,"userName":"anonymous"}]}
                                ],
                                "blocked":false,
                                "buildable":true,
                                "id":5,
                                "inQueueSince":1348006835051,
                                "params":"",
                                "stuck":false,
                                "task":{"name":"foo","url":"http://localhost:8080/job/foo/","color":"blue"},
                                "why":"Waiting for next available executor",
                                "buildableStartMilliseconds":1348006840462
                            }
                        }
                        */

                        JsonObject payload = 
                            new JsonObject()
                                .putString("action", "canRun")
                                .putObject(
                                    "item",
                                    new JsonObject()
                                        .putBoolean("blocked", queueItem.isBlocked())
                                        .putBoolean("buildable", queueItem.isBuildable())
                                        .putNumber("id", queueItem.id)
                                        .putNumber("inQueueSince", queueItem.getInQueueSince())
                                        .putString("params", queueItem.getParams())
                                        .putBoolean("stuck", queueItem.isStuck())
                                        .putObject(
                                            "task",
                                            new JsonObject()
                                                .putString("name", queueItem.task.getName())
                                                .putString("url", Util.encode(jenkins.getRootUrl() + queueItem.task.getUrl()))
                                        )
                                );
                        
                        // need to serialize actions by hand
                        JsonArray jsonActions = new JsonArray();
                        for (Action action : queueItem.getActions()) {
                            jsonActions.addObject(serializeToJson(action));
                        }

                        payload
                            .getObject("item")
                            .putArray("actions", jsonActions);

                        if (queueItem instanceof Queue.NotWaitingItem) {
                            payload
                                .getObject("item")
                                .putNumber(
                                    "buildableStartMilliseconds",
                                    ((Queue.NotWaitingItem) queueItem).buildableStartMilliseconds
                                );
                        } else if (queueItem instanceof Queue.WaitingItem) {
                            payload
                                .getObject("item")
                                .putNumber(
                                    "timestamp",
                                    ((Queue.WaitingItem) queueItem).timestamp.getTimeInMillis()
                                );
                        }

                        eventBus.send(_handlerId, payload,
                            new Handler<Message<JsonObject>>() {
                                public void handle(final Message<JsonObject> msg) {
                                    replyQueue.add(msg.body);
                                }
                            }
                        );
                    } catch (Exception e) {
                        logger.error("unable to send message", e);
                        replyQueue.add(new JsonObject().putString("error", "got exception"));
                    }
                }
            });
            
            t.setContextClassLoader(getClass().getClassLoader());
            t.start();

            try {
                final JsonObject reply = replyQueue.poll(10, TimeUnit.SECONDS);
                
                if (reply == null) {
                    logger.warn("timeout waiting for reply from {}", registeredHandlerId);
                } else {
                    if (! reply.getBoolean("canRun", true)) {
                        cause = new CauseOfBlockage() {
                            @Override
                            public String getShortDescription() {
                                return reply.getString("reason", "reason not specified");
                            }
                        };
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("interrupted waiting for reply");
            }
        }
        
        return cause;
    }
    // }}}

    // ========================================================== private stuff

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
        logger.error(error, e);
        
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
}
