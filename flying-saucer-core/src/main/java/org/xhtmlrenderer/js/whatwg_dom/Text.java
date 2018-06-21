package org.xhtmlrenderer.js.whatwg_dom;

import org.xhtmlrenderer.js.Optional;
import org.xhtmlrenderer.js.web_idl.*;

/**
 * @author Taras Maslov
 * 6/21/2018
 */
@Constructor(true)
@Exposed(Window.class)
public interface Text extends Slotable {
    
    void construct(@Optional @DefaultString("") DOMString data);

    @NewObject
    Text splitText(@Unsigned long offset);

    @ReadonlyAttribute
    DOMString wholeText();
}