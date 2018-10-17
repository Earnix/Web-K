# Web-K
[![Build Status](https://travis-ci.org/Earnix/Web-K.svg?branch=master)](https://travis-ci.org/Earnix/Web-K)

Web-K is [FlyingSaucer](https://github.com/flyingsaucerproject/flyingsaucer)-based pure java browser and Swing browser component. In addition to FlyingSaucer features it supports:
* a `<script>` tag with limited set of JS features (like Canvas). Nashosh JavaScript runtime is used. See features list below. 
* Not strict HTML. Standard Java XML parser and DOM replaced with modified [JSoup](https://github.com/jhy/jsoup) library. This provides support of HTML5 at parsing level.
* Embedded `<svg>` elements, implemented with [svgSalamander](https://github.com/blackears/svgSalamander).

FlyingSaucer PDF and SWT rendering was removed.

Main usecase - pure Java web view component. Not intended to be used as standalone web browser (at leas at this moment).

### JavaScript runtime
JavaScript APIs implementation based on [WHATWG DOM](https://dom.spec.whatwg.org/) and [WHATWG HTML](specification). It is currently very limited.

##### List of supported browser JS APIs
* HTMLCanvasElement
* CanvasRenderingContext2D (but not images-related functions)
* TextMetrics.width
* Document.getElementsByTagName
* Document.getElementsByClassName
* Document.createElement
* Document.getElementById
* Document.createTextNode
* Document.querySelector
* Document.querySelectorAll
* Document.body
* Document.childElementCount
* Element.tagName
* Element.className
* Element.id
* Element.classList
* Element.hasAttributes
* Element.attributes
* Element.getElementsByTagName
* Element.getElementsByClassName
* Element.previousElementSibling
* Element.nextElementSibling
* Element.children
* Element.style
* Element.innerHTML
* Element.outerHTML (read-only)
* Element.clientWidth
* Element.setAttribute
* Element.toggleAttribute
* Element.removeAttribute
* Element.hasAttribute
* Element.childElementCount
* Node.parentElement
* Node.childNodes
* Node.appendChild
* Node.addEventListener
* Node.removeEventListener
* Node.dispatchEvent
* NodeList
* CSSStyleAttribute
* Attr.name
* Attr.value
* Attr.nodeType
* Attr.nodeName
* Attr.parentElement
* Attr.nodeValue
* Attr.textContent
* Event.type
* HTMLCollection
* DOMTokenList.*
* Window.onload
* Window.document
* Window.console.log
* Window.console.error
* Window.setInterval
* Window.setTimeout
* Window.location
* Window.getComputedStyle

##### For developers: Adding new JS features
1. Package `com.earnix.webk.script.web_idl` contains set of classes which provide apis to describe [Web IDL specifications](https://heycam.github.io/webidl/) with java language features (see `com.earnix.webk.script.html.canvas.CanvasRenderingContext2D` as example interface which is [CanvasRenderingContext2D WebIDL specification](https://html.spec.whatwg.org/multipage/canvas.html#canvasrenderingcontext2d) described with Java). To add new APIs, firstly select / create package of feature (or standard) in `com.earnix.webk.script`, and and describe there new APIs (as Java interfaces based on WebIDL)
2. Inside target package (like `com.earnix.webk.script.html` for general features of [WHATWG HTML standard](https://html.spec.whatwg.org/)), create or locate `impl` package, and implement interfaces created before (see `com.earnix.webk.script.html.canvas.impl.CanvasRenderingContext2DImpl` as example)
3. If something new shoud be added to global `window` object, describe it in `com.earnix.webk.script.ScriptContext#initEngine`. If you just need to expose a constructor it's enough to use `com.earnix.webk.script.web_idl.Exposed` on target WebIDL interface.

The core of analyze and adaptation of WebIDL implementations is done by `com.earnix.webk.script.WebIDLAdapter`, so if you need to support new WebILD feature (like annotation), you need to handle it there. It adapts Nashorn JavaScript object API to concrete WebIDL Java implementation classes.


## license
GNU Lesser General Public License v3.0
