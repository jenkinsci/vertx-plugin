package org.bravo5.jenkins.plugins;

import hudson.model.listeners.ItemListener;
import hudson.model.Item;

import hudson.Extension;

import java.util.logging.Logger;

import java.util.Map;

import org.vertx.java.core.json.JsonObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;

@Extension
public class GlobalItemListener extends ItemListener {
    private final Logger logger = Logger.getLogger(getClass().getName());

    private final ObjectMapper objectMapper =
        new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
            .configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true);
    
    // {{{ onLoaded
    /** {@inheritDoc} */
    @Override
    public void onLoaded() {
        PluginImpl
            .getVertx()
            .eventBus()
            .publish(
                "jenkins.item",
                new JsonObject().putString("action", "allLoaded")
            );
    }
    // }}}

    // {{{ onCreated
    /** {@inheritDoc} */
    @Override
    public void onCreated(final Item item) {
        PluginImpl
            .getVertx()
            .eventBus()
            .publish(
                "jenkins.item",
                new JsonObject()
                    .putString("action", "created")
                    .putObject("item", new JsonObject(objectMapper.convertValue(item, Map.class)))
            );
    }
    // }}}
    
    // {{{ onUpdated
    /** {@inheritDoc} */
    @Override
    public void onUpdated(final Item item) {
        PluginImpl
            .getVertx()
            .eventBus()
            .publish(
                "jenkins.item",
                new JsonObject()
                    .putString("action", "updated")
                    .putObject("item", new JsonObject(objectMapper.convertValue(item, Map.class)))
            );
    }
    // }}}
    
    // {{{ onCopied
    /** {@inheritDoc} */
    @Override
    public void onCopied(final Item src, final Item item) {
        PluginImpl
            .getVertx()
            .eventBus()
            .publish(
                "jenkins.item",
                new JsonObject()
                    .putString("action", "copied")
                    .putObject("src", new JsonObject(objectMapper.convertValue(src, Map.class)))
                    .putObject("item", new JsonObject(objectMapper.convertValue(item, Map.class)))
            );
    }
    // }}}
    
    // {{{ onRenamed
    /** {@inheritDoc} */
    @Override
    public void onRenamed(final Item item, final String oldName, final String newName) {
        PluginImpl
            .getVertx()
            .eventBus()
            .publish(
                "jenkins.item",
                new JsonObject()
                    .putString("action", "renamed")
                    .putString("oldName", oldName)
                    .putString("newName", newName)
                    .putObject("item", new JsonObject(objectMapper.convertValue(item, Map.class)))
            );
    }
    // }}}
    
    // {{{ onDeleted
    /** {@inheritDoc} */
    @Override
    public void onDeleted(final Item item) {
        PluginImpl
            .getVertx()
            .eventBus()
            .publish(
                "jenkins.item",
                new JsonObject()
                    .putString("action", "deleted")
                    .putObject("item", new JsonObject(objectMapper.convertValue(item, Map.class)))
            );
    }
    // }}}
}
