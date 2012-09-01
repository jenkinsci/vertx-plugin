package org.bravo5.jenkins.plugins;

import hudson.Plugin;
import jenkins.model.Jenkins;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.util.logging.Logger;

public class PluginImpl extends Plugin {
    private final Logger logger = Logger.getLogger(getClass().getName());
    
    private static Vertx vertx;
    
    // {{{ start
    /** {@inheritDoc} */
    @Override
    public void start() throws Exception {
        logger.info("Vert.x: events for everyone!");
        
        vertx = Vertx.newVertx(25000, "10.0.1.9");
        vertx.eventBus().publish("jenkins-vertx", new JsonObject().putString("action", "started"));
    }
    // }}}
    
    // {{{ stop
    /** {@inheritDoc} */
    @Override
    public void stop() {
        logger.info("shutting down");
        
        vertx.eventBus().publish("jenkins-vertx", new JsonObject().putString("action", "stopped"));
    }
    // }}}
    
    // {{{ getInstance
    /** 
     * Getter for instance.
     *
     * @return value for instance
     */
    public static final Vertx getVertx() {
        if (vertx == null) {
            throw new IllegalStateException("plugin not started");
        }
        
        return vertx;
    }
    // }}}
    
}
