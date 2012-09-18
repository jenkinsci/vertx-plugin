package org.bravo5.jenkins.vertx;

import hudson.model.listeners.ItemListener;
import hudson.model.Item;

import hudson.Extension;

import java.util.logging.Logger;

import org.vertx.java.core.json.JsonObject;
import static org.bravo5.jenkins.vertx.SerializeUtil.serializeToJson;

/**
 * Broadcasts notifications regarding Items to the EventBus.
 */
@Extension
public class GlobalItemListener extends ItemListener {
    private final Logger logger = Logger.getLogger(getClass().getName());
    
    // {{{ onLoaded
    /** {@inheritDoc} */
    @Override
    public void onLoaded() {
        PluginImpl.ebPublish(
            "jenkins.item",
            new JsonObject().putString("action", "allLoaded")
        );
    }
    // }}}

    // {{{ onCreated
    /** {@inheritDoc} */
    @Override
    public void onCreated(final Item item) {
        PluginImpl.ebPublish(
            "jenkins.item",
            new JsonObject()
                .putString("action", "created")
                .putObject("item", serializeToJson(item))
        );
    }
    // }}}
    
    // {{{ onUpdated
    /** {@inheritDoc} */
    @Override
    public void onUpdated(final Item item) {
        PluginImpl.ebPublish(
            "jenkins.item",
            new JsonObject()
                .putString("action", "updated")
                .putObject("item", serializeToJson(item))
        );
    }
    // }}}
    
    // {{{ onCopied
    /** {@inheritDoc} */
    @Override
    public void onCopied(final Item src, final Item item) {
        PluginImpl.ebPublish(
            "jenkins.item",
            new JsonObject()
                .putString("action", "copied")
                .putObject("src", serializeToJson(src))
                .putObject("item", serializeToJson(item))
        );
    }
    // }}}
    
    // {{{ onRenamed
    /** {@inheritDoc} */
    @Override
    public void onRenamed(final Item item, final String oldName, final String newName) {
        PluginImpl.ebPublish(
            "jenkins.item",
            new JsonObject()
                .putString("action", "renamed")
                .putObject("item", serializeToJson(item))
                .putString("oldName", oldName)
                .putString("newName", newName)
        );
    }
    // }}}
    
    // {{{ onDeleted
    /** {@inheritDoc} */
    @Override
    public void onDeleted(final Item item) {
        PluginImpl.ebPublish(
            "jenkins.item",
            new JsonObject()
                .putString("action", "deleted")
                .putObject("item", serializeToJson(item))
        );
    }
    // }}}
}
