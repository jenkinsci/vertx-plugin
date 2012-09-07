package org.bravo5.jenkins.plugins;

import hudson.model.listeners.ItemListener;
import hudson.model.Item;

import hudson.Extension;

import java.util.logging.Logger;

import java.util.Map;

import org.vertx.java.core.json.JsonObject;

import hudson.util.XStream2;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;

@Extension
public class GlobalItemListener extends ItemListener {
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

    // {{{ itemToJson
    private JsonObject itemToJson(final Item item) {
        JsonObject json = serializeToJson(item)
            .putString("name", item.getName())
            .putString("fullName", item.getFullName())
            .putString("url", item.getUrl())
        ;

        return json;
    }
    // }}}

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
                .putObject("item", itemToJson(item))
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
                .putObject("item", itemToJson(item))
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
                .putObject("src", itemToJson(src))
                .putObject("item", itemToJson(item))
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
                .putObject("item", itemToJson(item))
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
                .putObject("item", itemToJson(item))
        );
    }
    // }}}
}
