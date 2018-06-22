package org.xhtmlrenderer.js.whatwg_dom;

import org.xhtmlrenderer.js.web_idl.Optional;
import org.xhtmlrenderer.js.web_idl.*;

/**
 * @author Taras Maslov
 * 6/21/2018
 */
@Constructor
@Exposed(Window.class)
@Stringifier
public interface Range extends AbstractRange {
    @ReadonlyAttribute
    Node commonAncestorContainer();

    void setStart(Node node, @Unsigned long offset);

    void setEnd(Node node, @Unsigned long offset);

    void setStartBefore(Node node);

    void setStartAfter(Node node);

    void setEndBefore(Node node);

    void setEndAfter(Node node);

    void collapse(@Optional @DefaultBoolean(false) boolean toStart);

    void selectNode(Node node);

    void selectNodeContents(Node node);

    @Unsigned short START_TO_START = 0;
    @Unsigned short START_TO_END = 1;
    @Unsigned short END_TO_END = 2;
    @Unsigned short END_TO_START = 3;

    short compareBoundaryPoints(@Unsigned short how, Range sourceRange);

    @CEReactions
    void deleteContents();

    @CEReactions
    @NewObject
    DocumentFragment extractContents();

    @CEReactions
    @NewObject
    DocumentFragment cloneContents();

    @CEReactions
    void insertNode(Node node);

    @CEReactions
    void surroundContents(Node newParent);

    @NewObject
    Range cloneRange();

    void detach();

    boolean isPointInRange(Node node, @Unsigned long offset);

    short comparePoint(Node node, @Unsigned long offset);

    boolean intersectsNode(Node node);

}
