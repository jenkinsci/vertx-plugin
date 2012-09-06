package org.bravo5.jenkins.plugins;

import hudson.model.Cause;

public class VertxCause extends Cause {
    private final String type = "vert.x";

    // {{{ getType
    /** 
     * Getter for type.
     *
     * @return value for type
     */
    public String getType() {
        return type;
    }
    // }}}
    
    // {{{ getShortDescription
    /** {@inheritDoc} */
    @Override
    public String getShortDescription() {
        return "triggered via vert.x";
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
        return getClass().getName().hashCode();
    }
    // }}}
}
