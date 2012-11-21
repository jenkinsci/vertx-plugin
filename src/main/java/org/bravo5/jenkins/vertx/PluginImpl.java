package org.bravo5.jenkins.vertx;

import hudson.Plugin;
import jenkins.model.Jenkins;
import hudson.model.queue.QueueTaskDispatcher;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point into the plugin.  Loaded before all @Extensions.
 */
public class PluginImpl extends Plugin {
    /**
     * Reference to the ClassLoader for this class.
     */
    private static final ClassLoader CLASS_LOADER = PluginImpl.class.getClassLoader();

    private final Logger logger = LoggerFactory.getLogger(getClass());

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
        // can't be destroyed!  Maybe someday…
        // https://github.com/vert-x/vert.x/issues/355
        vertx = Vertx.newVertx(25000, "0.0.0.0");

        handler = new JenkinsEventBusHandler(vertx.eventBus(), jenkins);

        // hello, world.
        vertx.eventBus().publish(
            "jenkins-vertx",
            buildAction("started")
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
        
        queueTaskDispatcher.setJenkins(jenkins);
        queueTaskDispatcher.setEventBus(vertx.eventBus());
        queueTaskDispatcher.init();
    }
    // }}}
    
    // {{{ stop
    /** {@inheritDoc} */
    @Override
    public void stop() {
        logger.info("shutting down");

        if (vertx != null) {
            vertx.eventBus().publish(
                "jenkins-vertx",
                buildAction("stopped")
            );
        }        

        if (handler != null) {
            handler.close();
        }

        if (queueTaskDispatcher != null) {
            queueTaskDispatcher.close();
        }
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
    
    // {{{ buildAction
    private JsonObject buildAction(final String action) {
        return new JsonObject()
            .putString("action", action)
            // Mailer isn't initialized, yet; throws NPE
            // .putString("jenkins_url", jenkins.getRootUrl())
        ;
    }
    // }}}
}
