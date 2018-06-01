package org.xhtmlrenderer.js.dom.impl;

import org.xhtmlrenderer.js.dom.DOMString;
import org.xhtmlrenderer.js.dom.DOMStringList;
import org.xhtmlrenderer.js.web_idl.Attribute;

import java.util.ArrayList;

/**
 * @author Taras Maslov
 * 6/1/2018
 */
public class DOMStringListImpl implements DOMStringList {

    private ArrayList<DOMString> items = new ArrayList<>();

    @Override
    public DOMString item(long index) {
        assert index < Integer.MAX_VALUE;
        return items.get((int) index);
    }

    @Override
    public Attribute<Long> length() {
        //noinspection unchecked
        return Attribute.readOnly().give(() -> items.size());
    }

    @Override
    public boolean contains(DOMString str) {
        return items.contains(str);
    }
}
