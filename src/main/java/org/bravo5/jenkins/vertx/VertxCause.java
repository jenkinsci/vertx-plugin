package org.bravo5.jenkins.vertx;

import hudson.model.Cause;
import hudson.model.TaskListener;
import org.kohsuke.stapler.export.Exported;

import org.vertx.java.core.json.JsonObject;
import java.util.Map;

/**
 * Cause attached to a build when invoked by the Vert.x plugin.
 */
public class VertxCause extends Cause {
    /** Arbitrary JSON payload attached to the cause. */
    private JsonObject payload;

    // {{{ constructor
    /**
     * Constructor.
     *
     * @param payload the JSON payload, or null if none
     */
    public VertxCause(final JsonObject payload) {
        this.payload = payload;
    }
    // }}}

    // {{{ getPayload
    /** 
     * API-friendly getter for payload.
     *
     * @return value for payload
     */
    @Exported(visibility=3)
    public Map<String,Object> getPayload() {
        return payload != null ? payload.toMap() : null;
    }
    // }}}

    // {{{ getPayloadAsJson
    /**
     * Native getter for payload.
     */
    public JsonObject getPayloadAsJson() {
        return payload;
    }
    // }}}
    
    // {{{ getShortDescription
    /** {@inheritDoc} */
    @Override
    public String getShortDescription() {
        return "triggered via vert.x";
    }
    // }}}

    // {{{ print
    /** {@inheritDoc} */
    @Override
    public void print(final TaskListener listener) {
        listener.getLogger().println(String.format(
            "%s with payload: %s",
            getShortDescription(), payload.encode()
        ));
    }
    // }}}

    // {{{ equals
    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        return other instanceof VertxCause;
    }
    // }}}

    // {{{ hashCode
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int hash = 41; // 42 - 1, of course
        hash = 23 * hash + getClass().getName().hashCode();
        hash = 23 * hash + (payload != null ? payload.hashCode() : 0);

        return hash;
    }
    // }}}
}
