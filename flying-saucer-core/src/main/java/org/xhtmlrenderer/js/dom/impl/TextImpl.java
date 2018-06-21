package org.xhtmlrenderer.js.dom.impl;

import org.xhtmlrenderer.js.dom.DOMException;
import org.xhtmlrenderer.js.web_idl.DOMString;
import org.xhtmlrenderer.js.dom.Text;
import org.xhtmlrenderer.js.web_idl.Attribute;

/**
 * @author Taras Maslov
 * 6/1/2018
 */
public class TextImpl extends CharacterDataImpl implements Text {
    
    private final org.w3c.dom.Text text;
    
    public TextImpl(org.w3c.dom.Text text) {
        super(text);
        this.text = text;
    }

    @Override
    public Text splitText(long offset) throws DOMException {
        return new TextImpl(text.splitText((int) offset));
    }

    @Override
    public Attribute<Boolean> isElementContentWhitespace() {
        return Attribute.<Boolean>readOnly().give(text::isElementContentWhitespace);
    }

    @Override
    public Attribute<DOMString> wholeText() {
        return Attribute.readOnly(new DOMStringImpl(text.getWholeText()));
    }

    @Override
    public Text replaceWholeText(DOMString content) throws DOMException {
        return new TextImpl(text.replaceWholeText(content.toString()));
    }
}