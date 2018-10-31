package com.earnix.webk.dom.nodes;

import com.earnix.webk.dom.SerializationException;
import com.earnix.webk.dom.helper.Validate;
import com.earnix.webk.dom.internal.StringUtil;
import com.earnix.webk.dom.select.NodeFilter;
import com.earnix.webk.dom.select.NodeTraversor;
import com.earnix.webk.dom.select.NodeVisitor;
import com.earnix.webk.script.impl.NodeImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The base, abstract Node model. Elements, Documents, Comments etc are all Node instances.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public abstract class NodeModel implements Cloneable {
    static final String EmptyString = "";
    NodeModel parentNode;
    int siblingIndex;
    NodeImpl scriptNode;

    /**
     * Default constructor. Doesn't setup base uri, children, or attributes; use with caution.
     */
    protected NodeModel() {
    }

    /**
     * Get the node name of this node. Use for debugging purposes and not logic switching (for that, use instanceof).
     *
     * @return node name
     */
    public abstract String nodeName();

    /**
     * Check if this Node has an actual Attributes object.
     */
    protected abstract boolean hasAttributes();

    public boolean hasParent() {
        return parentNode != null;
    }

    /**
     * Get an attribute's value by its key. <b>Case insensitive</b>
     * <p>
     * To get an absolute URL from an attribute that may be a relative URL, prefix the key with <code><b>abs</b></code>,
     * which is a shortcut to the {@link #absUrl} method.
     * </p>
     * E.g.:
     * <blockquote><code>String url = a.attr("abs:href");</code></blockquote>
     *
     * @param attributeKey The attribute key.
     * @return The attribute, or empty string if not present (to avoid nulls).
     * @see #attributes()
     * @see #hasAttr(String)
     * @see #absUrl(String)
     */
    public String attr(String attributeKey) {
        Validate.notNull(attributeKey);
        if (!hasAttributes())
            return EmptyString;

        String val = attributes().getIgnoreCase(attributeKey);
        if (val.length() > 0)
            return val;
        else if (attributeKey.startsWith("abs:"))
            return absUrl(attributeKey.substring("abs:".length()));
        else return "";
    }

    /**
     * Get all of the element's attributes.
     *
     * @return attributes (which implements iterable, in same order as presented in original HTML).
     */
    public abstract AttributesModel attributes();

    /**
     * Set an attribute (key=value). If the attribute already exists, it is replaced. The attribute key comparison is
     * <b>case insensitive</b>. The key will be set with case sensitivity as set in the parser settings.
     *
     * @param attributeKey   The attribute key.
     * @param attributeValue The attribute value.
     * @return this (for chaining)
     */
    public NodeModel attr(String attributeKey, String attributeValue) {
        attributeKey = NodeModelUtils.parser(this).settings().normalizeAttribute(attributeKey);
        attributes().putIgnoreCase(attributeKey, attributeValue);
        return this;
    }

    /**
     * Test if this element has an attribute. <b>Case insensitive</b>
     *
     * @param attributeKey The attribute key to check.
     * @return true if the attribute exists, false if not.
     */
    public boolean hasAttr(String attributeKey) {
        Validate.notNull(attributeKey);

        if (attributeKey.startsWith("abs:")) {
            String key = attributeKey.substring("abs:".length());
            if (attributes().hasKeyIgnoreCase(key) && !absUrl(key).equals(""))
                return true;
        }
        return attributes().hasKeyIgnoreCase(attributeKey);
    }

    /**
     * Remove an attribute from this element.
     *
     * @param attributeKey The attribute to remove.
     * @return this (for chaining)
     */
    public NodeModel removeAttr(String attributeKey) {
        Validate.notNull(attributeKey);
        attributes().removeIgnoreCase(attributeKey);
        return this;
    }

    /**
     * Clear (remove) all of the attributes in this node.
     *
     * @return this, for chaining
     */
    public NodeModel clearAttributes() {
        Iterator<AttributeModel> it = attributes().iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        return this;
    }

    /**
     * Get the base URI of this node.
     *
     * @return base URI
     */
    public abstract String baseUri();

    /**
     * Set the baseUri for just this node (not its descendants), if this Node tracks base URIs.
     *
     * @param baseUri new URI
     */
    protected abstract void doSetBaseUri(String baseUri);

    /**
     * Update the base URI of this node and all of its descendants.
     *
     * @param baseUri base URI to set
     */
    public void setBaseUri(final String baseUri) {
        Validate.notNull(baseUri);

        traverse(new NodeVisitor() {
            public void head(NodeModel node, int depth) {
                node.doSetBaseUri(baseUri);
            }

            public void tail(NodeModel node, int depth) {
            }
        });
    }

    /**
     * Get an absolute URL from a URL attribute that may be relative (i.e. an <code>&lt;a href&gt;</code> or
     * <code>&lt;img src&gt;</code>).
     * <p>
     * E.g.: <code>String absUrl = linkEl.absUrl("href");</code>
     * </p>
     * <p>
     * If the attribute value is already absolute (i.e. it starts with a protocol, like
     * <code>http://</code> or <code>https://</code> etc), and it successfully parses as a URL, the attribute is
     * returned directly. Otherwise, it is treated as a URL relative to the element's {@link #baseUri}, and made
     * absolute using that.
     * </p>
     * <p>
     * As an alternate, you can use the {@link #attr} method with the <code>abs:</code> prefix, e.g.:
     * <code>String absUrl = linkEl.attr("abs:href");</code>
     * </p>
     *
     * @param attributeKey The attribute key
     * @return An absolute URL if one could be made, or an empty string (not null) if the attribute was missing or
     * could not be made successfully into a URL.
     * @see #attr
     * @see java.net.URL#URL(java.net.URL, String)
     */
    public String absUrl(String attributeKey) {
        Validate.notEmpty(attributeKey);

        if (!hasAttr(attributeKey)) {
            return ""; // nothing to make absolute with
        } else {
            return StringUtil.resolve(baseUri(), attr(attributeKey));
        }
    }

    protected abstract List<NodeModel> ensureChildNodes();

    /**
     * Get a child node by its 0-based index.
     *
     * @param index index of child node
     * @return the child node at this index. Throws a {@code IndexOutOfBoundsException} if the index is out of bounds.
     */
    public NodeModel childNode(int index) {
        return ensureChildNodes().get(index);
    }

    /**
     * Get this node's children. Presented as an unmodifiable list: new children can not be added, but the child nodes
     * themselves can be manipulated.
     *
     * @return list of children. If no children, returns an empty list.
     */
    public List<NodeModel> childNodes() {
        return Collections.unmodifiableList(ensureChildNodes());
    }

    /**
     * Returns a deep copy of this node's children. Changes made to these nodes will not be reflected in the original
     * nodes
     *
     * @return a deep copy of this node's children
     */
    public List<NodeModel> childNodesCopy() {
        final List<NodeModel> nodes = ensureChildNodes();
        final ArrayList<NodeModel> children = new ArrayList<>(nodes.size());
        for (NodeModel node : nodes) {
            children.add(node.clone());
        }
        return children;
    }

    /**
     * Get the number of child nodes that this node holds.
     *
     * @return the number of child nodes that this node holds.
     */
    public abstract int childNodeSize();

    protected NodeModel[] childNodesAsArray() {
        return ensureChildNodes().toArray(new NodeModel[childNodeSize()]);
    }

    /**
     * Gets this node's parent node.
     *
     * @return parent node; or null if no parent.
     */
    public NodeModel parent() {
        return parentNode;
    }

    /**
     * Gets this node's parent node. Not overridable by extending classes, so useful if you really just need the Node type.
     *
     * @return parent node; or null if no parent.
     */
    public final NodeModel parentNode() {
        return parentNode;
    }

    /**
     * Get this node's root node; that is, its topmost ancestor. If this node is the top ancestor, returns {@code this}.
     *
     * @return topmost ancestor.
     */
    public NodeModel root() {
        NodeModel node = this;
        while (node.parentNode != null)
            node = node.parentNode;
        return node;
    }

    /**
     * Gets the Document associated with this Node.
     *
     * @return the Document associated with this Node, or null if there is no such Document.
     */
    public DocumentModel ownerDocument() {
        NodeModel root = root();
        return (root instanceof DocumentModel) ? (DocumentModel) root : null;
    }

    /**
     * Remove (delete) this node from the DOM tree. If this node has children, they are also removed.
     */
    public void remove() {
        Validate.notNull(parentNode);
        parentNode.removeChild(this);
    }

    /**
     * Insert the specified HTML into the DOM before this node (i.e. as a preceding sibling).
     *
     * @param html HTML to add before this node
     * @return this node, for chaining
     * @see #after(String)
     */
    public NodeModel before(String html) {
        addSiblingHtml(siblingIndex, html);
        return this;
    }

    /**
     * Insert the specified node into the DOM before this node (i.e. as a preceding sibling).
     *
     * @param node to add before this node
     * @return this node, for chaining
     * @see #after(NodeModel)
     */
    public NodeModel before(NodeModel node) {
        Validate.notNull(node);
        Validate.notNull(parentNode);

        parentNode.addChildren(siblingIndex, node);
        return this;
    }

    /**
     * Insert the specified HTML into the DOM after this node (i.e. as a following sibling).
     *
     * @param html HTML to add after this node
     * @return this node, for chaining
     * @see #before(String)
     */
    public NodeModel after(String html) {
        addSiblingHtml(siblingIndex + 1, html);
        return this;
    }

    /**
     * Insert the specified node into the DOM after this node (i.e. as a following sibling).
     *
     * @param node to add after this node
     * @return this node, for chaining
     * @see #before(NodeModel)
     */
    public NodeModel after(NodeModel node) {
        Validate.notNull(node);
        Validate.notNull(parentNode);

        parentNode.addChildren(siblingIndex + 1, node);
        return this;
    }

    private void addSiblingHtml(int index, String html) {
        Validate.notNull(html);
        Validate.notNull(parentNode);

        ElementModel context = parent() instanceof ElementModel ? (ElementModel) parent() : null;
        List<NodeModel> nodes = NodeModelUtils.parser(this).parseFragmentInput(html, context, baseUri());
        parentNode.addChildren(index, nodes.toArray(new NodeModel[nodes.size()]));
    }

    /**
     * Wrap the supplied HTML around this node.
     *
     * @param html HTML to wrap around this element, e.g. {@code <div class="head"></div>}. Can be arbitrarily deep.
     * @return this node, for chaining.
     */
    public NodeModel wrap(String html) {
        Validate.notEmpty(html);

        ElementModel context = parent() instanceof ElementModel ? (ElementModel) parent() : null;
        List<NodeModel> wrapChildren = NodeModelUtils.parser(this).parseFragmentInput(html, context, baseUri());
        NodeModel wrapNode = wrapChildren.get(0);
        if (!(wrapNode instanceof ElementModel)) // nothing to wrap with; noop
            return null;

        ElementModel wrap = (ElementModel) wrapNode;
        ElementModel deepest = getDeepChild(wrap);
        parentNode.replaceChild(this, wrap);
        deepest.addChildren(this);

        // remainder (unbalanced wrap, like <div></div><p></p> -- The <p> is remainder
        if (wrapChildren.size() > 0) {
            //noinspection ForLoopReplaceableByForEach (beacause it allocates an Iterator which is wasteful here)
            for (int i = 0; i < wrapChildren.size(); i++) {
                NodeModel remainder = wrapChildren.get(i);
                remainder.parentNode.removeChild(remainder);
                wrap.appendChild(remainder);
            }
        }
        return this;
    }

    /**
     * Removes this node from the DOM, and moves its children up into the node's parent. This has the effect of dropping
     * the node but keeping its children.
     * <p>
     * For example, with the input html:
     * </p>
     * <p>{@code <div>One <span>Two <b>Three</b></span></div>}</p>
     * Calling {@code element.unwrap()} on the {@code span} element will result in the html:
     * <p>{@code <div>One Two <b>Three</b></div>}</p>
     * and the {@code "Two "} {@link TextNodeModel} being returned.
     *
     * @return the first child of this node, after the node has been unwrapped. Null if the node had no children.
     * @see #remove()
     * @see #wrap(String)
     */
    public NodeModel unwrap() {
        Validate.notNull(parentNode);
        final List<NodeModel> childNodes = ensureChildNodes();
        NodeModel firstChild = childNodes.size() > 0 ? childNodes.get(0) : null;
        parentNode.addChildren(siblingIndex, this.childNodesAsArray());
        this.remove();

        return firstChild;
    }

    private ElementModel getDeepChild(ElementModel el) {
        List<ElementModel> children = el.children();
        if (children.size() > 0)
            return getDeepChild(children.get(0));
        else
            return el;
    }

    void nodelistChanged() {
        // Element overrides this to clear its shadow children elements
    }

    /**
     * Replace this node in the DOM with the supplied node.
     *
     * @param in the node that will will replace the existing node.
     */
    public void replaceWith(NodeModel in) {
        Validate.notNull(in);
        Validate.notNull(parentNode);
        parentNode.replaceChild(this, in);
    }

    protected void setParentNode(NodeModel parentNode) {
        Validate.notNull(parentNode);
        if (this.parentNode != null)
            this.parentNode.removeChild(this);
        this.parentNode = parentNode;
    }

    protected void replaceChild(NodeModel out, NodeModel in) {
        Validate.isTrue(out.parentNode == this);
        Validate.notNull(in);
        if (in.parentNode != null)
            in.parentNode.removeChild(in);

        final int index = out.siblingIndex;
        ensureChildNodes().set(index, in);
        in.parentNode = this;
        in.setSiblingIndex(index);
        out.parentNode = null;
    }

    protected void removeChild(NodeModel out) {
        Validate.isTrue(out.parentNode == this);
        final int index = out.siblingIndex;
        ensureChildNodes().remove(index);
        reindexChildren(index);
        out.parentNode = null;
    }

    protected void addChildren(NodeModel... children) {
        //most used. short circuit addChildren(int), which hits reindex children and array copy
        final List<NodeModel> nodes = ensureChildNodes();

        for (NodeModel child : children) {
            reparentChild(child);
            nodes.add(child);
            child.setSiblingIndex(nodes.size() - 1);
        }
    }

    protected void addChildren(int index, NodeModel... children) {
        Validate.noNullElements(children);
        final List<NodeModel> nodes = ensureChildNodes();

        for (NodeModel child : children) {
            reparentChild(child);
        }
        nodes.addAll(index, Arrays.asList(children));
        reindexChildren(index);
    }

    protected void reparentChild(NodeModel child) {
        child.setParentNode(this);
    }

    private void reindexChildren(int start) {
        final List<NodeModel> childNodes = ensureChildNodes();

        for (int i = start; i < childNodes.size(); i++) {
            childNodes.get(i).setSiblingIndex(i);
        }
    }

    /**
     * Retrieves this node's sibling nodes. Similar to {@link #childNodes()  node.parent.childNodes()}, but does not
     * include this node (a node is not a sibling of itself).
     *
     * @return node siblings. If the node has no parent, returns an empty list.
     */
    public List<NodeModel> siblingNodes() {
        if (parentNode == null)
            return Collections.emptyList();

        List<NodeModel> nodes = parentNode.ensureChildNodes();
        List<NodeModel> siblings = new ArrayList<>(nodes.size() - 1);
        for (NodeModel node : nodes)
            if (node != this)
                siblings.add(node);
        return siblings;
    }

    /**
     * Get this node's next sibling.
     *
     * @return next sibling, or null if this is the last sibling
     */
    public NodeModel nextSibling() {
        if (parentNode == null)
            return null; // root

        final List<NodeModel> siblings = parentNode.ensureChildNodes();
        final int index = siblingIndex + 1;
        if (siblings.size() > index)
            return siblings.get(index);
        else
            return null;
    }

    /**
     * Get this node's previous sibling.
     *
     * @return the previous sibling, or null if this is the first sibling
     */
    public NodeModel previousSibling() {
        if (parentNode == null)
            return null; // root

        if (siblingIndex > 0)
            return parentNode.ensureChildNodes().get(siblingIndex - 1);
        else
            return null;
    }

    /**
     * Get the list index of this node in its node sibling list. I.e. if this is the first node
     * sibling, returns 0.
     *
     * @return position in node sibling list
     * @see ElementModel#elementSiblingIndex()
     */
    public int siblingIndex() {
        return siblingIndex;
    }

    protected void setSiblingIndex(int siblingIndex) {
        this.siblingIndex = siblingIndex;
    }

    /**
     * Perform a depth-first traversal through this node and its descendants.
     *
     * @param nodeVisitor the visitor callbacks to perform on each node
     * @return this node, for chaining
     */
    public NodeModel traverse(NodeVisitor nodeVisitor) {
        Validate.notNull(nodeVisitor);
        NodeTraversor.traverse(nodeVisitor, this);
        return this;
    }

    /**
     * Perform a depth-first filtering through this node and its descendants.
     *
     * @param nodeFilter the filter callbacks to perform on each node
     * @return this node, for chaining
     */
    public NodeModel filter(NodeFilter nodeFilter) {
        Validate.notNull(nodeFilter);
        NodeTraversor.filter(nodeFilter, this);
        return this;
    }

    /**
     * Get the outer HTML of this node. For example, on a {@code p} element, may return {@code <p>Para</p>}.
     *
     * @return outer HTML
     * @see ElementModel#html()
     * @see ElementModel#text()
     */
    public String outerHtml() {
        StringBuilder accum = StringUtil.borrowBuilder();
        outerHtml(accum);
        return StringUtil.releaseBuilder(accum);
    }

    protected void outerHtml(Appendable accum) {
        NodeTraversor.traverse(new OuterHtmlVisitor(accum, NodeModelUtils.outputSettings(this)), this);
    }

    /**
     * Get the outer HTML of this node.
     *
     * @param accum accumulator to place HTML into
     * @throws IOException if appending to the given accumulator fails.
     */
    abstract void outerHtmlHead(final Appendable accum, int depth, final DocumentModel.OutputSettings out) throws IOException;

    abstract void outerHtmlTail(final Appendable accum, int depth, final DocumentModel.OutputSettings out) throws IOException;

    /**
     * Write this node and its children to the given {@link Appendable}.
     *
     * @param appendable the {@link Appendable} to write to.
     * @return the supplied {@link Appendable}, for chaining.
     */
    public <T extends Appendable> T html(T appendable) {
        outerHtml(appendable);
        return appendable;
    }

    /**
     * Gets this node's outer HTML.
     *
     * @return outer HTML.
     * @see #outerHtml()
     */
    public String toString() {
        return outerHtml();
    }

    protected void indent(Appendable accum, int depth, DocumentModel.OutputSettings out) throws IOException {
        accum.append('\n').append(StringUtil.padding(depth * out.indentAmount()));
    }

    /**
     * Check if this node is the same instance of another (object identity test).
     *
     * @param o other object to compare to
     * @return true if the content of this node is the same as the other
     * @see NodeModel#hasSameValue(Object) to compare nodes by their value
     */
    @Override
    public boolean equals(Object o) {
        // implemented just so that javadoc is clear this is an identity test
        return this == o;
    }

    /**
     * Check if this node is has the same content as another node. A node is considered the same if its name, attributes and content match the
     * other node; particularly its position in the tree does not influence its similarity.
     *
     * @param o other object to compare to
     * @return true if the content of this node is the same as the other
     */
    public boolean hasSameValue(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return this.outerHtml().equals(((NodeModel) o).outerHtml());
    }

    /**
     * Create a stand-alone, deep copy of this node, and all of its children. The cloned node will have no siblings or
     * parent node. As a stand-alone object, any changes made to the clone or any of its children will not impact the
     * original node.
     * <p>
     * The cloned node may be adopted into another Document or node structure using {@link ElementModel#appendChild(NodeModel)}.
     *
     * @return a stand-alone cloned node, including clones of any children
     * @see #shallowClone()
     */
    @Override
    public NodeModel clone() {
        NodeModel thisClone = doClone(null); // splits for orphan

        // Queue up nodes that need their children cloned (BFS).
        final LinkedList<NodeModel> nodesToProcess = new LinkedList<>();
        nodesToProcess.add(thisClone);

        while (!nodesToProcess.isEmpty()) {
            NodeModel currParent = nodesToProcess.remove();

            final int size = currParent.childNodeSize();
            for (int i = 0; i < size; i++) {
                final List<NodeModel> childNodes = currParent.ensureChildNodes();
                NodeModel childClone = childNodes.get(i).doClone(currParent);
                childNodes.set(i, childClone);
                nodesToProcess.add(childClone);
            }
        }

        return thisClone;
    }

    /**
     * Create a stand-alone, shallow copy of this node. None of its children (if any) will be cloned, and it will have
     * no parent or sibling nodes.
     *
     * @return a single independent copy of this node
     * @see #clone()
     */
    public NodeModel shallowClone() {
        return doClone(null);
    }

    /*
     * Return a clone of the node using the given parent (which can be null).
     * Not a deep copy of children.
     */
    protected NodeModel doClone(NodeModel parent) {
        NodeModel clone;

        try {
            clone = (NodeModel) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        clone.parentNode = parent; // can be null, to create an orphan split
        clone.siblingIndex = parent == null ? 0 : siblingIndex;

        return clone;
    }

    private static class OuterHtmlVisitor implements NodeVisitor {
        private Appendable accum;
        private DocumentModel.OutputSettings out;

        OuterHtmlVisitor(Appendable accum, DocumentModel.OutputSettings out) {
            this.accum = accum;
            this.out = out;
            out.prepareEncoder();
        }

        public void head(NodeModel node, int depth) {
            try {
                node.outerHtmlHead(accum, depth, out);
            } catch (IOException exception) {
                throw new SerializationException(exception);
            }
        }

        public void tail(NodeModel node, int depth) {
            if (!node.nodeName().equals("#text")) { // saves a void hit.
                try {
                    node.outerHtmlTail(accum, depth, out);
                } catch (IOException exception) {
                    throw new SerializationException(exception);
                }
            }
        }
    }

    public NodeImpl getScriptNode() {
        return scriptNode;
    }

    public NodeModel setScriptNode(NodeImpl scriptNode) {
        this.scriptNode = scriptNode;
        return this;
    }
}
