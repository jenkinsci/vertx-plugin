package org.bravo5.jenkins.vertx;

import org.kohsuke.stapler.export.ExportConfig;
import org.kohsuke.stapler.export.TreePruner;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.Flavor;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.DataWriter;

import org.vertx.java.core.json.JsonObject;

import hudson.model.Run;
import hudson.model.Item;

import java.io.Writer;
import java.io.StringWriter;
import java.io.IOException;

final class SerializeUtil {
    private SerializeUtil() {
        throw new UnsupportedOperationException("don't do that.");
    }

    private static class Serializer<T> {
        private T object;

        private ExportConfig exportConfig;
        private TreePruner pruner;

        // {{{ constructor
        Serializer(final T object) {
            this.object = object;

            exportConfig = new ExportConfig();
            // exportConfig.prettyPrint = true;

            // or NamedPathPruner, or custom
            pruner = new TreePruner.ByDepth(1);
        }
        // }}}

        // {{{ invoke
        JsonObject invoke() {
            JsonObject retVal = null;
            
            if (object != null) {
                Writer writer = new StringWriter();
                
                try {
                    DataWriter dataWriter =
                        Flavor.JSON.createDataWriter(object, writer, exportConfig);
                    
                    Model<T> model = (Model<T>) new ModelBuilder().get(object.getClass());
                    model.writeTo(object, pruner, dataWriter);
                } catch (IOException e) {
                    throw new RuntimeException("error serializing", e);
                }
                
                // logger.severe("stapler serialization: " + writer.toString());
                
                retVal = new JsonObject(writer.toString());
            }

            return retVal;
        }
        // }}}
    }

    // {{{ serializeToJson
    public static JsonObject serializeToJson(final Run run) {
        return new Serializer<Run>(run).invoke();
    }
    // }}}

    // {{{ serializeToJson
    public static JsonObject serializeToJson(final Item item) {
        return new Serializer<Item>(item).invoke();
    }
    // }}}
}
