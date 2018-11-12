package com.earnix.webk.dom.parser;

import com.earnix.webk.dom.helper.Validate;
import com.earnix.webk.dom.internal.StringUtil;
import com.earnix.webk.dom.nodes.CDataNodeModel;
import com.earnix.webk.dom.nodes.CommentModel;
import com.earnix.webk.dom.nodes.DataNodeModel;
import com.earnix.webk.dom.nodes.DocumentModel;
import com.earnix.webk.dom.nodes.ElementModel;
import com.earnix.webk.dom.nodes.FormElement;
import com.earnix.webk.dom.nodes.NodeModel;
import com.earnix.webk.dom.nodes.TextNodeModel;
import com.earnix.webk.dom.select.Elements;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static com.earnix.webk.dom.internal.StringUtil.inSorted;

/**
 * HTML Tree Builder; creates a DOM from Tokens.
 */
public class HtmlTreeBuilder extends TreeBuilder {
    // tag searches. must be sorted, used in inSorted. MUST update HtmlTreeBuilderTest if more arrays are added.
    static final String[] TagsSearchInScope = new String[]{"applet", "caption", "html", "marquee", "object", "table", "td", "th"};
    static final String[] TagSearchList = new String[]{"ol", "ul"};
    static final String[] TagSearchButton = new String[]{"button"};
    static final String[] TagSearchTableScope = new String[]{"html", "table"};
    static final String[] TagSearchSelectScope = new String[]{"optgroup", "option"};
    static final String[] TagSearchEndTags = new String[]{"dd", "dt", "li", "optgroup", "option", "p", "rp", "rt"};
    static final String[] TagSearchSpecial = new String[]{"address", "applet", "area", "article", "aside", "base", "basefont", "bgsound",
            "blockquote", "body", "br", "button", "caption", "center", "col", "colgroup", "command", "dd",
            "details", "dir", "div", "dl", "dt", "embed", "fieldset", "figcaption", "figure", "footer", "form",
            "frame", "frameset", "h1", "h2", "h3", "h4", "h5", "h6", "head", "header", "hgroup", "hr", "html",
            "iframe", "img", "input", "isindex", "li", "link", "listing", "marquee", "menu", "meta", "nav",
            "noembed", "noframes", "noscript", "object", "ol", "p", "param", "plaintext", "pre", "script",
            "section", "select", "style", "summary", "table", "tbody", "td", "textarea", "tfoot", "th", "thead",
            "title", "tr", "ul", "wbr", "xmp"};

    public static final int MaxScopeSearchDepth = 100; // prevents the parser bogging down in exceptionally broken pages

    private HtmlTreeBuilderState state; // the current state
    private HtmlTreeBuilderState originalState; // original / marked state

    private boolean baseUriSetFromDoc;
    private ElementModel headElement; // the current head element
    private FormElement formElement; // the current form element
    private ElementModel contextElement; // fragment parse context -- could be null even if fragment parsing
    private ArrayList<ElementModel> formattingElements; // active (open) formatting elements
    private List<String> pendingTableCharacters; // chars in table to be shifted out
    private Token.EndTag emptyEnd; // reused empty end tag

    private boolean framesetOk; // if ok to go into frameset
    private boolean fosterInserts; // if next inserts should be fostered
    private boolean fragmentParsing; // if parsing a fragment of html

    ParseSettings defaultSettings() {
        return ParseSettings.htmlDefault;
    }

    @Override
    protected void initialiseParse(Reader input, String baseUri, Parser parser) {
        super.initialiseParse(input, baseUri, parser);

        // this is a bit mucky. todo - probably just create new parser objects to ensure all reset.
        state = HtmlTreeBuilderState.Initial;
        originalState = null;
        baseUriSetFromDoc = false;
        headElement = null;
        formElement = null;
        contextElement = null;
        formattingElements = new ArrayList<>();
        pendingTableCharacters = new ArrayList<>();
        emptyEnd = new Token.EndTag();
        framesetOk = true;
        fosterInserts = false;
        fragmentParsing = false;
    }

    List<NodeModel> parseFragment(String inputFragment, ElementModel context, String baseUri, Parser parser) {
        // context may be null
        state = HtmlTreeBuilderState.Initial;
        initialiseParse(new StringReader(inputFragment), baseUri, parser);
        contextElement = context;
        fragmentParsing = true;
        ElementModel root = null;

        if (context != null) {
            if (context.ownerDocument() != null) // quirks setup:
                doc.quirksMode(context.ownerDocument().quirksMode());

            // initialise the tokeniser state:
            String contextTag = context.tagName();
            if (StringUtil.in(contextTag, "title", "textarea"))
                tokeniser.transition(TokeniserState.Rcdata);
            else if (StringUtil.in(contextTag, "iframe", "noembed", "noframes", "style", "xmp"))
                tokeniser.transition(TokeniserState.Rawtext);
            else if (contextTag.equals("script"))
                tokeniser.transition(TokeniserState.ScriptData);
            else if (contextTag.equals(("noscript")))
                tokeniser.transition(TokeniserState.Data); // if scripting enabled, rawtext
            else if (contextTag.equals("plaintext"))
                tokeniser.transition(TokeniserState.Data);
            else
                tokeniser.transition(TokeniserState.Data); // default

            root = new ElementModel(Tag.valueOf("html", settings), baseUri);
            doc.appendChild(root);
            stack.add(root);
            resetInsertionMode();

            // setup form element to nearest form on context (up ancestor chain). ensures form controls are associated
            // with form correctly
            Elements contextChain = context.parents();
            contextChain.add(0, context);
            for (ElementModel parent : contextChain) {
                if (parent instanceof FormElement) {
                    formElement = (FormElement) parent;
                    break;
                }
            }
        }

        runParser();
        if (context != null)
            return root.childNodes();
        else
            return doc.childNodes();
    }

    @Override
    protected boolean process(Token token) {
        currentToken = token;
        return this.state.process(token, this);
    }

    boolean process(Token token, HtmlTreeBuilderState state) {
        currentToken = token;
        return state.process(token, this);
    }

    void transition(HtmlTreeBuilderState state) {
        this.state = state;
    }

    HtmlTreeBuilderState state() {
        return state;
    }

    void markInsertionMode() {
        originalState = state;
    }

    HtmlTreeBuilderState originalState() {
        return originalState;
    }

    void framesetOk(boolean framesetOk) {
        this.framesetOk = framesetOk;
    }

    boolean framesetOk() {
        return framesetOk;
    }

    DocumentModel getDocument() {
        return doc;
    }

    String getBaseUri() {
        return baseUri;
    }

    void maybeSetBaseUri(ElementModel base) {
        if (baseUriSetFromDoc) // only listen to the first <base href> in parse
            return;

        String href = base.absUrl("href");
        if (href.length() != 0) { // ignore <base target> etc
            baseUri = href;
            baseUriSetFromDoc = true;
            doc.setBaseUri(href); // set on the doc so doc.createElement(Tag) will get updated base, and to update all descendants
        }
    }

    boolean isFragmentParsing() {
        return fragmentParsing;
    }

    void error(HtmlTreeBuilderState state) {
        if (parser.getErrors().canAddError())
            parser.getErrors().add(new ParseError(reader.pos(), "Unexpected token [%s] when in state [%s]", currentToken.tokenType(), state));
    }

    ElementModel insert(Token.StartTag startTag) {
        // handle empty unknown tags
        // when the spec expects an empty tag, will directly hit insertEmpty, so won't generate this fake end tag.
        if (startTag.isSelfClosing()) {
            ElementModel el = insertEmpty(startTag);
            stack.add(el);
            tokeniser.transition(TokeniserState.Data); // handles <script />, otherwise needs breakout steps from script data
            tokeniser.emit(emptyEnd.reset().name(el.tagName()));  // ensure we get out of whatever state we are in. emitted for yielded processing
            return el;
        }

        ElementModel el = new ElementModel(Tag.valueOf(startTag.name(), settings), baseUri, settings.normalizeAttributes(startTag.attributes));
        insert(el);
        return el;
    }

    ElementModel insertStartTag(String startTagName) {
        ElementModel el = new ElementModel(Tag.valueOf(startTagName, settings), baseUri);
        insert(el);
        return el;
    }

    void insert(ElementModel el) {
        insertNode(el);
        stack.add(el);
    }

    ElementModel insertEmpty(Token.StartTag startTag) {
        Tag tag = Tag.valueOf(startTag.name(), settings);
        ElementModel el = new ElementModel(tag, baseUri, startTag.attributes);
        insertNode(el);
        if (startTag.isSelfClosing()) {
            if (tag.isKnownTag()) {
                if (!tag.isEmpty())
                    tokeniser.error("Tag cannot be self closing; not a void tag");
            } else // unknown tag, remember this is self closing for output
                tag.setSelfClosing();
        }
        return el;
    }

    FormElement insertForm(Token.StartTag startTag, boolean onStack) {
        Tag tag = Tag.valueOf(startTag.name(), settings);
        FormElement el = new FormElement(tag, baseUri, startTag.attributes);
        setFormElement(el);
        insertNode(el);
        if (onStack)
            stack.add(el);
        return el;
    }

    void insert(Token.Comment commentToken) {
        CommentModel comment = new CommentModel(commentToken.getData());
        insertNode(comment);
    }

    void insert(Token.Character characterToken) {
        final NodeModel node;
        final ElementModel el = currentElement();
        final String tagName = el.tagName();
        final String data = characterToken.getData();

        if (characterToken.isCData())
            node = new CDataNodeModel(data);
        else if (tagName.equals("script") || tagName.equals("style"))
            node = new DataNodeModel(data);
        else
            node = new TextNodeModel(data);
        el.appendChild(node); // doesn't use insertNode, because we don't foster these; and will always have a stack.
    }

    private void insertNode(NodeModel node) {
        // if the stack hasn't been set up yet, elements (doctype, comments) go into the doc
        if (stack.size() == 0)
            doc.appendChild(node);
        else if (isFosterInserts())
            insertInFosterParent(node);
        else
            currentElement().appendChild(node);

        // connect form controls to their form element
        if (node instanceof ElementModel && ((ElementModel) node).tag().isFormListed()) {
            if (formElement != null)
                formElement.addElement((ElementModel) node);
        }
    }

    ElementModel pop() {
        int size = stack.size();
        return stack.remove(size - 1);
    }

    void push(ElementModel element) {
        stack.add(element);
    }

    ArrayList<ElementModel> getStack() {
        return stack;
    }

    boolean onStack(ElementModel el) {
        return isElementInQueue(stack, el);
    }

    private boolean isElementInQueue(ArrayList<ElementModel> queue, ElementModel element) {
        for (int pos = queue.size() - 1; pos >= 0; pos--) {
            ElementModel next = queue.get(pos);
            if (next == element) {
                return true;
            }
        }
        return false;
    }

    ElementModel getFromStack(String elName) {
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
            ElementModel next = stack.get(pos);
            if (next.nodeName().equals(elName)) {
                return next;
            }
        }
        return null;
    }

    boolean removeFromStack(ElementModel el) {
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
            ElementModel next = stack.get(pos);
            if (next == el) {
                stack.remove(pos);
                return true;
            }
        }
        return false;
    }

    void popStackToClose(String elName) {
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
            ElementModel next = stack.get(pos);
            stack.remove(pos);
            if (next.nodeName().equals(elName))
                break;
        }
    }

    // elnames is sorted, comes from Constants
    void popStackToClose(String... elNames) {
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
            ElementModel next = stack.get(pos);
            stack.remove(pos);
            if (inSorted(next.nodeName(), elNames))
                break;
        }
    }

    void popStackToBefore(String elName) {
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
            ElementModel next = stack.get(pos);
            if (next.nodeName().equals(elName)) {
                break;
            } else {
                stack.remove(pos);
            }
        }
    }

    void clearStackToTableContext() {
        clearStackToContext("table");
    }

    void clearStackToTableBodyContext() {
        clearStackToContext("tbody", "tfoot", "thead", "template");
    }

    void clearStackToTableRowContext() {
        clearStackToContext("tr", "template");
    }

    private void clearStackToContext(String... nodeNames) {
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
            ElementModel next = stack.get(pos);
            if (StringUtil.in(next.nodeName(), nodeNames) || next.nodeName().equals("html"))
                break;
            else
                stack.remove(pos);
        }
    }

    ElementModel aboveOnStack(ElementModel el) {
        assert onStack(el);
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
            ElementModel next = stack.get(pos);
            if (next == el) {
                return stack.get(pos - 1);
            }
        }
        return null;
    }

    void insertOnStackAfter(ElementModel after, ElementModel in) {
        int i = stack.lastIndexOf(after);
        Validate.isTrue(i != -1);
        stack.add(i + 1, in);
    }

    void replaceOnStack(ElementModel out, ElementModel in) {
        replaceInQueue(stack, out, in);
    }

    private void replaceInQueue(ArrayList<ElementModel> queue, ElementModel out, ElementModel in) {
        int i = queue.lastIndexOf(out);
        Validate.isTrue(i != -1);
        queue.set(i, in);
    }

    void resetInsertionMode() {
        boolean last = false;
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
            ElementModel node = stack.get(pos);
            if (pos == 0) {
                last = true;
                node = contextElement;
            }
            String name = node.nodeName();
            if ("select".equals(name)) {
                transition(HtmlTreeBuilderState.InSelect);
                break; // frag
            } else if (("td".equals(name) || "th".equals(name) && !last)) {
                transition(HtmlTreeBuilderState.InCell);
                break;
            } else if ("tr".equals(name)) {
                transition(HtmlTreeBuilderState.InRow);
                break;
            } else if ("tbody".equals(name) || "thead".equals(name) || "tfoot".equals(name)) {
                transition(HtmlTreeBuilderState.InTableBody);
                break;
            } else if ("caption".equals(name)) {
                transition(HtmlTreeBuilderState.InCaption);
                break;
            } else if ("colgroup".equals(name)) {
                transition(HtmlTreeBuilderState.InColumnGroup);
                break; // frag
            } else if ("table".equals(name)) {
                transition(HtmlTreeBuilderState.InTable);
                break;
            } else if ("head".equals(name)) {
                transition(HtmlTreeBuilderState.InBody);
                break; // frag
            } else if ("body".equals(name)) {
                transition(HtmlTreeBuilderState.InBody);
                break;
            } else if ("frameset".equals(name)) {
                transition(HtmlTreeBuilderState.InFrameset);
                break; // frag
            } else if ("html".equals(name)) {
                transition(HtmlTreeBuilderState.BeforeHead);
                break; // frag
            } else if (last) {
                transition(HtmlTreeBuilderState.InBody);
                break; // frag
            }
        }
    }

    // todo: tidy up in specific scope methods
    private String[] specificScopeTarget = {null};

    private boolean inSpecificScope(String targetName, String[] baseTypes, String[] extraTypes) {
        specificScopeTarget[0] = targetName;
        return inSpecificScope(specificScopeTarget, baseTypes, extraTypes);
    }

    private boolean inSpecificScope(String[] targetNames, String[] baseTypes, String[] extraTypes) {
        // https://html.spec.whatwg.org/multipage/parsing.html#has-an-element-in-the-specific-scope
        final int bottom = stack.size() - 1;
        final int top = bottom > MaxScopeSearchDepth ? bottom - MaxScopeSearchDepth : 0;
        // don't walk too far up the tree

        for (int pos = bottom; pos >= top; pos--) {
            final String elName = stack.get(pos).nodeName();
            if (inSorted(elName, targetNames))
                return true;
            if (inSorted(elName, baseTypes))
                return false;
            if (extraTypes != null && inSorted(elName, extraTypes))
                return false;
        }
        //Validate.fail("Should not be reachable"); // would end up false because hitting 'html' at root (basetypes)
        return false;
    }

    boolean inScope(String[] targetNames) {
        return inSpecificScope(targetNames, TagsSearchInScope, null);
    }

    boolean inScope(String targetName) {
        return inScope(targetName, null);
    }

    boolean inScope(String targetName, String[] extras) {
        return inSpecificScope(targetName, TagsSearchInScope, extras);
        // todo: in mathml namespace: mi, mo, mn, ms, mtext annotation-xml
        // todo: in svg namespace: forignOjbect, desc, title
    }

    boolean inListItemScope(String targetName) {
        return inScope(targetName, TagSearchList);
    }

    boolean inButtonScope(String targetName) {
        return inScope(targetName, TagSearchButton);
    }

    boolean inTableScope(String targetName) {
        return inSpecificScope(targetName, TagSearchTableScope, null);
    }

    boolean inSelectScope(String targetName) {
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
            ElementModel el = stack.get(pos);
            String elName = el.nodeName();
            if (elName.equals(targetName))
                return true;
            if (!inSorted(elName, TagSearchSelectScope)) // all elements except
                return false;
        }
        Validate.fail("Should not be reachable");
        return false;
    }

    void setHeadElement(ElementModel headElement) {
        this.headElement = headElement;
    }

    ElementModel getHeadElement() {
        return headElement;
    }

    boolean isFosterInserts() {
        return fosterInserts;
    }

    void setFosterInserts(boolean fosterInserts) {
        this.fosterInserts = fosterInserts;
    }

    FormElement getFormElement() {
        return formElement;
    }

    void setFormElement(FormElement formElement) {
        this.formElement = formElement;
    }

    void newPendingTableCharacters() {
        pendingTableCharacters = new ArrayList<>();
    }

    List<String> getPendingTableCharacters() {
        return pendingTableCharacters;
    }

    /**
     * 11.2.5.2 Closing elements that have implied end tags<p/>
     * When the steps below require the UA to generate implied end tags, then, while the current node is a dd element, a
     * dt element, an li element, an option element, an optgroup element, a p element, an rp element, or an rt element,
     * the UA must pop the current node off the stack of open elements.
     *
     * @param excludeTag If a step requires the UA to generate implied end tags but lists an element to exclude from the
     *                   process, then the UA must perform the above steps as if that element was not in the above list.
     */
    void generateImpliedEndTags(String excludeTag) {
        while ((excludeTag != null && !currentElement().nodeName().equals(excludeTag)) &&
                inSorted(currentElement().nodeName(), TagSearchEndTags))
            pop();
    }

    void generateImpliedEndTags() {
        generateImpliedEndTags(null);
    }

    boolean isSpecial(ElementModel el) {
        // todo: mathml's mi, mo, mn
        // todo: svg's foreigObject, desc, title
        String name = el.nodeName();
        return inSorted(name, TagSearchSpecial);
    }

    ElementModel lastFormattingElement() {
        return formattingElements.size() > 0 ? formattingElements.get(formattingElements.size() - 1) : null;
    }

    ElementModel removeLastFormattingElement() {
        int size = formattingElements.size();
        if (size > 0)
            return formattingElements.remove(size - 1);
        else
            return null;
    }

    // active formatting elements
    void pushActiveFormattingElements(ElementModel in) {
        int numSeen = 0;
        for (int pos = formattingElements.size() - 1; pos >= 0; pos--) {
            ElementModel el = formattingElements.get(pos);
            if (el == null) // marker
                break;

            if (isSameFormattingElement(in, el))
                numSeen++;

            if (numSeen == 3) {
                formattingElements.remove(pos);
                break;
            }
        }
        formattingElements.add(in);
    }

    private boolean isSameFormattingElement(ElementModel a, ElementModel b) {
        // same if: same namespace, tag, and attributes. Element.equals only checks tag, might in future check children
        return a.nodeName().equals(b.nodeName()) &&
                // a.namespace().equals(b.namespace()) &&
                a.attributes().equals(b.attributes());
        // todo: namespaces
    }

    void reconstructFormattingElements() {
        ElementModel last = lastFormattingElement();
        if (last == null || onStack(last))
            return;

        ElementModel entry = last;
        int size = formattingElements.size();
        int pos = size - 1;
        boolean skip = false;
        while (true) {
            if (pos == 0) { // step 4. if none before, skip to 8
                skip = true;
                break;
            }
            entry = formattingElements.get(--pos); // step 5. one earlier than entry
            if (entry == null || onStack(entry)) // step 6 - neither marker nor on stack
                break; // jump to 8, else continue back to 4
        }
        while (true) {
            if (!skip) // step 7: on later than entry
                entry = formattingElements.get(++pos);
            Validate.notNull(entry); // should not occur, as we break at last element

            // 8. create new element from element, 9 insert into current node, onto stack
            skip = false; // can only skip increment from 4.
            ElementModel newEl = insertStartTag(entry.nodeName()); // todo: avoid fostering here?
            // newEl.namespace(entry.namespace()); // todo: namespaces
            newEl.attributes().addAll(entry.attributes());

            // 10. replace entry with new entry
            formattingElements.set(pos, newEl);

            // 11
            if (pos == size - 1) // if not last entry in list, jump to 7
                break;
        }
    }

    void clearFormattingElementsToLastMarker() {
        while (!formattingElements.isEmpty()) {
            ElementModel el = removeLastFormattingElement();
            if (el == null)
                break;
        }
    }

    void removeFromActiveFormattingElements(ElementModel el) {
        for (int pos = formattingElements.size() - 1; pos >= 0; pos--) {
            ElementModel next = formattingElements.get(pos);
            if (next == el) {
                formattingElements.remove(pos);
                break;
            }
        }
    }

    boolean isInActiveFormattingElements(ElementModel el) {
        return isElementInQueue(formattingElements, el);
    }

    ElementModel getActiveFormattingElement(String nodeName) {
        for (int pos = formattingElements.size() - 1; pos >= 0; pos--) {
            ElementModel next = formattingElements.get(pos);
            if (next == null) // scope marker
                break;
            else if (next.nodeName().equals(nodeName))
                return next;
        }
        return null;
    }

    void replaceActiveFormattingElement(ElementModel out, ElementModel in) {
        replaceInQueue(formattingElements, out, in);
    }

    void insertMarkerToFormattingElements() {
        formattingElements.add(null);
    }

    void insertInFosterParent(NodeModel in) {
        ElementModel fosterParent;
        ElementModel lastTable = getFromStack("table");
        boolean isLastTableParent = false;
        if (lastTable != null) {
            if (lastTable.parent() != null) {
                fosterParent = lastTable.parent();
                isLastTableParent = true;
            } else
                fosterParent = aboveOnStack(lastTable);
        } else { // no table == frag
            fosterParent = stack.get(0);
        }

        if (isLastTableParent) {
            Validate.notNull(lastTable); // last table cannot be null by this point.
            lastTable.before(in);
        } else
            fosterParent.appendChild(in);
    }

    @Override
    public String toString() {
        return "TreeBuilder{" +
                "currentToken=" + currentToken +
                ", state=" + state +
                ", currentElement=" + currentElement() +
                '}';
    }
}