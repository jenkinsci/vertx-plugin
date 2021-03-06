package org.bravo5.jenkins.vertx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kohsuke.stapler.export.ExportConfig;
import org.kohsuke.stapler.export.TreePruner;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.Flavor;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.DataWriter;

import org.vertx.java.core.json.JsonObject;

import hudson.model.Run;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.Action;

import java.io.Writer;
import java.io.StringWriter;

/**
 * Utility class for serializing Jenkins objects using the same method as the
 * remote API.
 */
final class SerializeUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializeUtil.class);

    /**
     * Utility classes don't get constructors.
     */
    private SerializeUtil() {
        throw new UnsupportedOperationException("don't do that.");
    }
    
    /**
     * Internal class for performing the serialization.  Jenkins is so over-
     * templated…
     */
    private static class Serializer<T> {
        /** The object to be serialized. */
        private T object;

        private ExportConfig exportConfig;
        private TreePruner pruner;

        // {{{ constructor
        /**
         * @param object the object to be serialized
         */
        Serializer(final T object) {
            this.object = object;

            exportConfig = new ExportConfig();
            // exportConfig.prettyPrint = true;

            // or NamedPathPruner, or custom
            pruner = new TreePruner.ByDepth(1);
        }
        // }}}

        // {{{ invoke
        /**
         * Performs the serialization.
         *
         * @return a JsonObject instance containing the serialized data.
         */
        JsonObject invoke() {
            JsonObject retVal = null;
            
            if (object != null) {
                Writer writer = new StringWriter();
                
                try {
                    DataWriter dataWriter =
                        Flavor.JSON.createDataWriter(object, writer, exportConfig);
                    
                    Model<T> model = (Model<T>) new ModelBuilder().get(object.getClass());
                    model.writeTo(object, pruner, dataWriter);
                    
                    retVal = new JsonObject(writer.toString());
                } catch (Exception e) {
                    // Catching Exception is only allowed in very limited
                    // circumstances.  This is one of them.  Regardless of the
                    // error, we want the caller to know about it.
                    retVal = new JsonObject()
                        .putString("error", "could not serialize to JSON")
                        .putString("message", e.getMessage())
                        .putString("class", object.getClass().getName());

                    LOGGER.error(
                        "could not serialize {} to JSON",
                        object.getClass().getName(), e
                    );
                }
            }

            return retVal;
        }
        // }}}
    }

    // {{{ serializeToJson
    /**
     * Serializes a Run to a JsonObject.
     *
     * @param run the Run to serialize
     * @return JsonObject representation
     */
    public static JsonObject serializeToJson(final Run run) {
        return new Serializer<Run>(run).invoke();
    }
    // }}}

    // {{{ serializeToJson
    /**
     * Serializes an Item to a JsonObject.
     *
     * @param item the Item to serialize
     * @return JsonObject representation
     */
    public static JsonObject serializeToJson(final Item item) {
        return new Serializer<Item>(item).invoke();
    }
    // }}}
    
    // {{{ serializeToJson
    public static JsonObject serializeToJson(final Queue.Item queueItem) {
        return new Serializer<Queue.Item>(queueItem).invoke();
    }
    // }}}
    
    // {{{ serializeToJson
    public static JsonObject serializeToJson(final Action action) {
        return new Serializer<Action>(action).invoke();
    }
    // }}}
}
