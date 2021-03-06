package com.earnix.webk.runtime.xhr.impl;

import com.earnix.webk.runtime.ScriptContext;
import com.earnix.webk.runtime.web_idl.Attribute;
import com.earnix.webk.runtime.dom.EventHandler;
import com.earnix.webk.runtime.dom.EventTarget;
import com.earnix.webk.runtime.dom.impl.EventTargetImpl;
import com.earnix.webk.runtime.dom.impl.Level1EventTarget;
import com.earnix.webk.runtime.xhr.XMLHttpRequestUpload;
import lombok.AccessLevel;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;

/**
 * @author Taras Maslov
 * 12/2p0/2018
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class XMLHttpRequestUploadImpl implements XMLHttpRequestUpload {

    @Delegate(types = {EventTarget.class})
    EventTargetImpl eventTargetImpl;
    Level1EventTarget level1EventTarget;

    ScriptContext context;

    public XMLHttpRequestUploadImpl(ScriptContext context) {
        this.context = context;
        eventTargetImpl = new EventTargetImpl(() -> context);
        level1EventTarget = new Level1EventTarget(() -> context, this);
    }

    @Override
    public Attribute<EventHandler> onloadstart() {
        return level1EventTarget.getHandlerAttribute("onloadstart");
    }

    @Override
    public Attribute<EventHandler> onprogress() {
        return level1EventTarget.getHandlerAttribute("onprogress");
    }

    @Override
    public Attribute<EventHandler> onabort() {
        return level1EventTarget.getHandlerAttribute("onabort");
    }

    @Override
    public Attribute<EventHandler> onerror() {
        return level1EventTarget.getHandlerAttribute("onerror");
    }

    @Override
    public Attribute<EventHandler> onload() {
        return level1EventTarget.getHandlerAttribute("onload");
    }

    @Override
    public Attribute<EventHandler> ontimeout() {
        return level1EventTarget.getHandlerAttribute("ontimeout");
    }

    @Override
    public Attribute<EventHandler> onloadend() {
        return level1EventTarget.getHandlerAttribute("onloadend");
    }
}
