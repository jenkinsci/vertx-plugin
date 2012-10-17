package org.bravo5.jenkins.vertx;

import hudson.Plugin;
import jenkins.model.Jenkins;
import hudson.model.queue.QueueTaskDispatcher;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.util.logging.Logger;

/**
 * Entry point into the plugin.  Loaded before all @Extensions.
 */
public class PluginImpl extends Plugin {
    /**
     * Reference to the ClassLoader for this class.
     */
    private static final ClassLoader CLASS_LOADER = PluginImpl.class.getClassLoader();

    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * A little short-hand.
     */
    private final Jenkins jenkins = Jenkins.getInstance();
    
    /**
     * Need just one of these (can't stop it, anyway) for use by other classes.
     */
    private static Vertx vertx;

    /**
     * Main request dispatcher.
     */
    private JenkinsEventBusHandler handler;
    
    /**
     * Exposes a QueueTaskDispatcher to the EventBus.
     */
    private EventBusQueueTaskDispatcher queueTaskDispatcher;
    
    // {{{ start
    /** {@inheritDoc} */
    @Override
    public void start() throws Exception {
        logger.info("Vert.x: events for everyone!");
        
        // Hm, this could be made configurable, except that the Vertx instance
        // can't be destroyed!
        vertx = Vertx.newVertx(25000, "0.0.0.0");

        handler = new JenkinsEventBusHandler(vertx.eventBus());

        // hello, world.
        vertx.eventBus().publish(
            "jenkins-vertx",
            new JsonObject()
                .putString("action", "started")
        );
    }
    // }}}

    // {{{ postInitialize
    /** {@inheritDoc} */
    @Override
    public void postInitialize() {
        // initialize EventBusQueueTaskDispatcher
        queueTaskDispatcher = jenkins
            .getExtensionList(QueueTaskDispatcher.class)
            .get(EventBusQueueTaskDispatcher.class);
        
        queueTaskDispatcher.setEventBus(vertx.eventBus());
        queueTaskDispatcher.init();
    }
    // }}}
    
    // {{{ stop
    /** {@inheritDoc} */
    @Override
    public void stop() {
        logger.info("shutting down");

        handler.close();
        queueTaskDispatcher.close();
        
        vertx.eventBus().publish(
            "jenkins-vertx",
            new JsonObject()
                .putString("action", "stopped")
        );
    }
    // }}}
    
    // {{{ ebPublish
    /**
     * Publishes an EventBus message, possibly from another thread.
     *
     * @param addr the destination address
     * @param msg the message to publish
     */
    static void ebPublish(final String addr, final JsonObject msg) {
        if (vertx == null) {
            throw new IllegalStateException("plugin not started");
        }

        ClassLoader oldContextClassLoader =
            Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(CLASS_LOADER);

        try { 
            vertx.eventBus().publish(addr, msg);
        } finally { 
           Thread.currentThread().setContextClassLoader(oldContextClassLoader); 
        }
    }
    // }}}
}
