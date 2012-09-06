package org.bravo5.jenkins.plugins;

import hudson.Plugin;
import jenkins.model.Jenkins;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.util.logging.Logger;

public class PluginImpl extends Plugin {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private static final ClassLoader CLASS_LOADER = PluginImpl.class.getClassLoader();

    private static Vertx vertx;
    
    private JenkinsEventBusHandler handler;

    // {{{ start
    /** {@inheritDoc} */
    @Override
    public void start() throws Exception {
        logger.info("Vert.x: events for everyone!");
        
        vertx = Vertx.newVertx(25000, "0.0.0.0");

        handler = new JenkinsEventBusHandler(vertx.eventBus());

        vertx.eventBus().publish("jenkins-vertx", new JsonObject().putString("action", "started"));
    }
    // }}}
    
    // {{{ stop
    /** {@inheritDoc} */
    @Override
    public void stop() {
        logger.info("shutting down");

        handler.close();
        vertx.eventBus().publish("jenkins-vertx", new JsonObject().putString("action", "stopped"));
    }
    // }}}
    
    // {{{ ebPublish
    static void ebPublish(final String addr, final JsonObject obj) {
        ClassLoader oldContextClassLoader =
            Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(CLASS_LOADER);

        try { 
            getVertx().eventBus().publish(addr, obj);
        } finally { 
           Thread.currentThread().setContextClassLoader(oldContextClassLoader); 
        } 

    }
    // }}}

    // {{{ getInstance
    /** 
     * Getter for instance.
     *
     * @return value for instance
     */
    private static final Vertx getVertx() {
        if (vertx == null) {
            throw new IllegalStateException("plugin not started");
        }
        
        return vertx;
    }
    // }}}
}
