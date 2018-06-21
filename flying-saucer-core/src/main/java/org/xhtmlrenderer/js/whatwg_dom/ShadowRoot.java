package org.xhtmlrenderer.js.whatwg_dom;

import org.xhtmlrenderer.js.web_idl.Exposed;
import org.xhtmlrenderer.js.web_idl.ReadonlyAttribute;

/**
 * @author Taras Maslov
 * 6/19/2018
 */
@Exposed(Window.class)
public interface ShadowRoot extends DocumentOrShadowRoot {
    @ReadonlyAttribute ShadowRootMode mode();
    @ReadonlyAttribute Element host();
}
