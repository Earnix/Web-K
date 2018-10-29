/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.earnix.webk.simple;

import com.earnix.webk.dom.nodes.DocumentModel;
import com.earnix.webk.extend.UserAgentCallback;
import com.earnix.webk.layout.SharedContext;
import com.earnix.webk.render.RenderingContext;
import com.earnix.webk.simple.extend.FormSubmissionListener;
import com.earnix.webk.simple.extend.XhtmlNamespaceHandler;
import com.earnix.webk.swing.BasicPanel;
import com.earnix.webk.swing.CursorListener;
import com.earnix.webk.swing.HoverListener;
import com.earnix.webk.swing.LinkListener;
import com.earnix.webk.util.Configuration;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * <p/>
 * <p/>
 * XHTMLPanel is a simple Swing component that renders valid XHTML content in a
 * Java program. It is scrolling aware so you can safely drop it into a
 * {@link javax.swing.JScrollPane}. The most common usage is to stuff a {@link URL}
 * into it and then add it to your JFrame. Ex:</p>
 * <pre>
 * import com.earnix.webk.simple.*;
 * .....
 * <p/>
 * public static void main(String[] args) {
 * <p/>
 * // set up the xhtml panel XHTMLPanel xhtml = new XHTMLPanel();
 * xhtml.setDocument(new URL("http://myserver.com/page.xhtml"));
 * <p/>
 * JScrollPane scroll = new JScrollPane(xhtml);
 * JFrame frame = new JFrame("Demo");
 * frame.getContentPane().add(scroll);
 * frame.pack();
 * frame.setSize(500,600);
 * frame.show();
 * }
 * </pre>
 * <p/>
 * <p>XHTMLPanel renders XHTML and XML which can be loaded as valid {@link DocumentModel}
 * instances. You should make sure the document you want to render is well-formed. For XHTML,
 * there is always a default stylesheet available, even if no CSS is attached to the
 * XHTML you are loading. For XML, there is no default stylesheet, so you should have
 * one attached to your XML before trying to render it using the xml-stylesheer processing
 * instruction. XHTMLPanel has methods to load
 * documents from a uri ({@link #setDocument(String uri)}),
 * from a Document instance ({@link #setDocument(DocumentModel)}) or from an InputStream
 * ({@link BasicPanel#setDocument(java.io.InputStream, String)}).</p>
 * <p/>
 * <p/>
 * <p/>
 * XHTMLPanel also lets you make simple changes with simple methods like
 * {@link #setFontScalingFactor(float)}. If you want to make other changes you will
 * need to get the rendering context ({@link #getSharedContext()}) and call methods on
 * that. Ex: </p> <p/>
 * <p/>
 * <pre>
 * XHTMLPanel xhtml = new XHTMLPanel();
 * RenderingContext ctx = xhtml.getRenderingContext();
 * ctx.setLogging(true); // turn on logging
 * ctx.setValidating(true); // turn on doctype validation
 * ctx.addFont(fnt,"Arial"); // redefine a font
 * ctx.setDomImplementation("com.cooldom.DomImpl");
 * </pre>
 * <p/>
 * <p>XHTMLPanel comes with a pre-installed MouseListener which handles :hover events used for rollovers
 * ( @see com.earnix.webk.swing.HoverListener ). XHTMLPanel also comes with a pre-installed LinkListener
 * used to follow links.  ( @see com.earnix.webk.swing.LinkListener )
 * If you want to disable these for some reason you can
 * get the list of mouse listeners and remove them all.
 * </p>
 *
 * @author Joshua Marinacci (joshy@joshy.net)
 * @see <a href="http://xhtmlrenderer.dev.java.net">The Flying Saucer Home Page</a>
 * @see RenderingContext
 */
public class XHTMLPanel extends BasicPanel {
    private static final long serialVersionUID = 1L;

    private float fontScalingFactor = 1.2F;
    private float minFontScale = 0.50F;
    private float maxFontScale = 3.0F;

    /**
     * Instantiates an XHTMLPanel with no {@link DocumentModel} loaded by default.
     */
    public XHTMLPanel() {
        setupListeners();
    }

    /**
     * Instantiates a panel with a custom {@link UserAgentCallback}
     * implementation.
     *
     * @param uac The custom UserAgentCallback implementation.
     */
    public XHTMLPanel(UserAgentCallback uac) {
        super(uac);
        setupListeners();
    }

    private void setupListeners() {
        if (Configuration.isTrue("xr.use.listeners", true)) {
            addMouseTrackingListener(new HoverListener());
            addMouseTrackingListener(new LinkListener());
            addMouseTrackingListener(new CursorListener());
            setFormSubmissionListener(new FormSubmissionListener() {
                public void submit(String query) {
                    XHTMLPanel.this.setDocumentRelative(query);
                }
            });
        }
    }

    private void resetListeners() {
        if (Configuration.isTrue("xr.use.listeners", true)) {
            resetMouseTracker();
        }
    }

    /**
     * Lays out the current document again, and re-renders.
     */
    public void relayout() {
        sharedContext.flushFonts();
        super.relayout();
    }

    /**
     * Loads and renders a Document given a uri.
     * The uri is resolved by the UserAgentCallback
     *
     * @param uri
     */
    public void setDocument(String uri) {
        setDocument(loadDocument(uri), uri);
    }

    /**
     * Renders an XML Document instance.
     * Make sure that no relative resources are needed
     *
     * @param doc The document to render.
     */
    public void setDocument(DocumentModel doc) {
        setDocument(doc, "");
    }

    /**
     * Renders a Document using a URL as a base URL for relative
     * paths.
     *
     * @param doc The new document value
     * @param url The new document value
     */
    public void setDocument(DocumentModel doc, String url) {
        resetListeners();
        setDocument(doc, url, new XhtmlNamespaceHandler());
    }

    /**
     * Renders a Document read from an InputStream using a URL
     * as a base URL for relative paths.
     *
     * @param stream The stream to read the Document from.
     * @param url    The URL used to resolve relative path references.
     */
    // TODO: should throw more specific exception (PWW 25/07/2006)
    public void setDocument(InputStream stream, String url)
            throws Exception {
        resetListeners();
        setDocument(stream, url, new XhtmlNamespaceHandler());
    }

    /**
     * Renders a Document read from an InputStream using a URL
     * as a base URL for relative paths.
     *
     * @param file The file to read the Document from. Relative paths
     *             will be resolved based on the file's parent directory.
     */
    // TODO: should throw more specific exception (PWW 25/07/2006)
    public void setDocument(File file)
            throws Exception {
        resetListeners();
        File parent = file.getAbsoluteFile().getParentFile();
        String parentURL = (parent == null ? "" : parent.toURI().toURL().toExternalForm());
        setDocument(
                loadDocument(file.toURI().toURL().toExternalForm()),
                parentURL
        );
    }

    /**
     * Sets the {@link RenderingContext} attribute of the XHTMLPanel object. Generally
     * you should not use this unless you have a heavily customized context to
     * use. To modify just some rendering behavior, consider using
     * {@link #getSharedContext()} to retrieve the current context, and using
     * mutators to change its behavior.
     *
     * @param ctx A new RenderingContext to use for rendering.
     */
    public void setSharedContext(SharedContext ctx) {
        super.setSharedContext(ctx);
    }

    /**
     * Sets the scaling factor used by {@link #incrementFontSize()} and
     * {@link #decrementFontSize()}--both scale the font up or down by this
     * scaling factor. The scaling roughly modifies the font size as a multiplier
     * or divisor. A scaling factor of 1.2 applied against a font size of 10pt
     * results in a scaled font of 12pt. The default scaling factor is
     * 1.2F.
     */
    public void setFontScalingFactor(float scaling) {
        fontScalingFactor = scaling;
    }

    /**
     * Increments all rendered fonts on the current document by the current
     * scaling factor for the panel. Scaling applies culmulatively, which means that
     * multiple calls to this method scale fonts larger and larger by applying the
     * current scaling factor against itself. You can modify the scaling factor by
     * {@link #setFontScalingFactor(float)}, and reset to the document's specified
     * font size with {@link #resetFontSize()}.
     */
    public void incrementFontSize() {
        scaleFont(fontScalingFactor);
    }

    /**
     * Resets all rendered fonts on the current document to the font size
     * specified in the document's styling instructions.
     */
    public void resetFontSize() {
        SharedContext rc = getSharedContext();
        rc.getTextRenderer().setFontScale(1.0F);
        setDocument(getDocument());
    }

    /**
     * Decrements all rendered fonts on the current document by the current
     * scaling factor for the panel. Scaling applies culmulatively, which means that
     * multiple calls to this method scale fonts smaller and smaller by applying the
     * current scaling factor against itself. You can modify the scaling factor by
     * {@link #setFontScalingFactor(float)}, and reset to the document's specified
     * font size with {@link #resetFontSize()}.
     */
    public void decrementFontSize() {
        scaleFont(1 / fontScalingFactor);
    }

    /**
     * Applies a change in scale for fonts using the rendering context's text
     * renderer.
     */
    private void scaleFont(float scaleBy) {
        SharedContext rc = getSharedContext();
        float fs = rc.getTextRenderer().getFontScale() * scaleBy;
        if (fs < minFontScale || fs > maxFontScale) return;
        rc.getTextRenderer().setFontScale(fs);
        setDocument(getDocument());
    }

    /**
     * Returns the maximum font scaling that may be applied, e.g. 3 times assigned font size.
     */
    public float getMaxFontScale() {
        return maxFontScale;
    }

    /**
     * Returns the minimum font scaling that may be applied, e.g. 0.5 times assigned font size.
     */
    public float getMinFontScale() {
        return minFontScale;
    }

    /**
     * Sets the maximum font scaling that may be applied, e.g. 3 times assigned font size. Calling incrementFontSize()
     * after this scale has been reached doesn't have an effect.
     */
    public void setMaxFontScale(float f) {
        maxFontScale = f;
    }

    /**
     * Sets the minimum font scaling that may be applied, e.g. 3 times assigned font size. Calling decrementFontSize()
     * after this scale has been reached doesn't have an effect.
     */
    public void setMinFontScale(float f) {
        minFontScale = f;
    }
}
