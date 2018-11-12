package com.earnix.webk.script.whatwg_dom;

import com.earnix.webk.script.web_idl.Exposed;
import com.earnix.webk.script.web_idl.ReadonlyAttribute;

/**
 * @author Taras Maslov
 * 6/19/2018
 */
@Exposed(Window.class)
public interface ShadowRoot extends DocumentOrShadowRoot {
    @ReadonlyAttribute
    ShadowRootMode mode();

    @ReadonlyAttribute
    Element host();
}