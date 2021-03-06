/*
 * {{{ header & license
 * Copyright (c) 2004 Joshua Marinacci
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
package com.earnix.webk.browser;

import com.earnix.webk.browser.actions.ZoomAction;
import com.earnix.webk.render.Box;
import com.earnix.webk.swing.BasicPanel;
import com.earnix.webk.swing.DOMInspector;
import com.earnix.webk.swing.FSMouseListener;
import com.earnix.webk.swing.LinkListener;
import com.earnix.webk.util.Configuration;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.List;

import static java.awt.event.KeyEvent.VK_F;
import static java.awt.event.KeyEvent.VK_I;
import static java.awt.event.KeyEvent.VK_L;

@FieldDefaults(level = AccessLevel.PRIVATE)
class BrowserMenuBar extends JMenuBar {

    private final static Logger log = LoggerFactory.getLogger(BrowserMenuBar.class);

    WebKApplication root;
    
    JMenu file;
    JMenu edit;
    JMenu view;
    JMenu go;
    JMenuItem viewSource;
    JMenu debug;
    JMenu demos;

    private String lastDemoOpened;

//    private Map allDemos;
    private JMenu help;

    BrowserMenuBar(WebKApplication root) {
        this.root = root;
    }


    void init() {
        val actions = root.getActions();

        file = new JMenu("Browser");
        file.setMnemonic('B');

        debug = new JMenu("Debug");
        debug.setMnemonic('U');

        demos = new JMenu("Demos");
        demos.setMnemonic('D');

        view = new JMenu("View");
        view.setMnemonic('V');

        help = new JMenu("Help");
        help.setMnemonic('H');

        viewSource = new JMenuItem("Page Source");
        viewSource.setEnabled(false);
        view.add(actions.getStop());
        view.add(actions.getRefresh());
        view.add(actions.getReload());
        view.add(new JSeparator());
        JMenu textSize = new JMenu("Text Size");
        textSize.setMnemonic('T');
        textSize.add(actions.getIncreaseFont());
        textSize.add(actions.getDecreaseFont());
        textSize.add(new JSeparator());
        textSize.add(actions.getResetFont());
        view.add(textSize);

        go = new JMenu("Go");
        go.setMnemonic('G');
    }


    void createLayout() {
        val panel = root.getPanel().getView();
        val actions = root.getActions();
        file.add(actions.getOpenFile());
        file.add(new JSeparator());
        file.add(actions.getQuit());
        add(file);


        // TODO: we can get the document and format it, but need syntax highlighting
        // and a tab or separate window, dialog, etc.
        viewSource.setAction(new ViewSourceAction(panel));
        view.add(viewSource);


        JMenu zoom = new JMenu("Zoom");
        zoom.setMnemonic('Z');
        ScaleFactor[] factors = this.initializeScales();
        ButtonGroup zoomGroup = new ButtonGroup();
        for (int i = 0; i < factors.length; i++) {
            ScaleFactor factor = factors[i];
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(new ZoomAction(panel, factor));

            if (factor.isNotZoomed()) item.setSelected(true);

            zoomGroup.add(item);
            zoom.add(item);
        }
        view.add(new JSeparator());
        view.add(zoom);
        view.add(new JSeparator());
        view.add(new JCheckBoxMenuItem(actions.getPrintPreview()));
        add(view);

        go.add(actions.getForward());
        go.add(actions.getBackward());

        add(go);
        demos.add(new JSeparator());

        add(demos);

        JMenu debugShow = new JMenu("Show");
        debug.add(debugShow);
        debugShow.setMnemonic('S');

        debugShow.add(new JCheckBoxMenuItem(new BoxOutlinesAction()));
        debugShow.add(new JCheckBoxMenuItem(new LineBoxOutlinesAction()));
        debugShow.add(new JCheckBoxMenuItem(new InlineBoxesAction()));
        debugShow.add(new JCheckBoxMenuItem(new FontMetricsAction()));

        JMenu anti = new JMenu("Anti Aliasing");
        ButtonGroup anti_level = new ButtonGroup();
        addLevel(anti, anti_level, "None", -1);
        addLevel(anti, anti_level, "Low", 25).setSelected(true);
        addLevel(anti, anti_level, "Medium", 12);
        addLevel(anti, anti_level, "High", 0);
        debug.add(anti);


        debug.add(new ShowDOMInspectorAction());
        debug.add(new AbstractAction("Validation Console") {
            public void actionPerformed(ActionEvent evt) {
                if (root.getValidationConsole() == null) {
                    root.setValidationConsole(new JFrame("Validation Console"));
                    JFrame frame = root.getValidationConsole();
                    JTextArea jta = new JTextArea();

                    root.getValidationHandler().setTextArea(jta);

                    jta.setEditable(false);
                    jta.setLineWrap(true);
                    jta.setText("Validation Console: XML Parsing Error Messages");

                    frame.getContentPane().setLayout(new BorderLayout());
                    frame.getContentPane().add(new JScrollPane(jta), "Center");
                    JButton close = new JButton("Close");
                    frame.getContentPane().add(close, "South");
                    close.addActionListener(evt1 -> root.getValidationConsole().setVisible(false));

                    frame.pack();
                    frame.setSize(400, 300);
                }
                root.getValidationConsole().setVisible(true);
            }
        });

        add(debug);

        help.add(root.getActions().getUsersManual());
        help.add(new JSeparator());
        help.add(root.getActions().getAboutPage());
        add(help);
    }

    private JRadioButtonMenuItem addLevel(JMenu menu, ButtonGroup group, String title, int level) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(new AntiAliasedAction(title, level));
        group.add(item);
        menu.add(item);
        return item;
    }

    void createActions() {
        if (Configuration.isTrue("xr.use.listeners", true)) {
            List l = root.getPanel().getView().getMouseTrackingListeners();
            for (Iterator i = l.iterator(); i.hasNext(); ) {
                FSMouseListener listener = (FSMouseListener) i.next();
                if (listener instanceof LinkListener) {
                    root.getPanel().getView().removeMouseTrackingListener(listener);
                }
            }

            root.getPanel().getView().addMouseTrackingListener(new LinkListener() {
                @Override
                public void onMouseOver(BasicPanel panel, Box box) {
                    super.onMouseOver(panel, box);
                    if (box.getElement().is("a")) {
                        root.getPanel().getStatus().getText().setText(box.getElement().attr("href"));
                    }
                }
            });
        }
    }

    private ScaleFactor[] initializeScales() {
        ScaleFactor[] scales = new ScaleFactor[11];
        int i = 0;
        scales[i++] = new ScaleFactor(1.0d, "Normal (100%)");
        scales[i++] = new ScaleFactor(2.0d, "200%");
        scales[i++] = new ScaleFactor(1.5d, "150%");
        scales[i++] = new ScaleFactor(0.85d, "85%");
        scales[i++] = new ScaleFactor(0.75d, "75%");
        scales[i++] = new ScaleFactor(0.5d, "50%");
        scales[i++] = new ScaleFactor(0.33d, "33%");
        scales[i++] = new ScaleFactor(0.25d, "25%");
        scales[i++] = new ScaleFactor(ScaleFactor.PAGE_WIDTH, "Page width");
        scales[i++] = new ScaleFactor(ScaleFactor.PAGE_HEIGHT, "Page height");
        scales[i++] = new ScaleFactor(ScaleFactor.PAGE_WHOLE, "Whole page");
        return scales;
    }

    class ShowDOMInspectorAction extends AbstractAction {

        private DOMInspector inspector;

        private JFrame inspectorFrame;

        ShowDOMInspectorAction() {
            super("DOM Tree Inspector");
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
        }

        public void actionPerformed(ActionEvent evt) {
            if (inspectorFrame == null) {
                inspectorFrame = new JFrame("DOM Tree Inspector");
            }
            if (inspector == null) {
                inspector = new DOMInspector(root.getPanel().getView().getDocument(), root.getPanel().getView().getSharedContext(), root.getPanel().getView().getSharedContext().getCss());

                inspectorFrame.getContentPane().add(inspector);

                inspectorFrame.pack();
                inspectorFrame.setSize(500, 600);
                inspectorFrame.show();
            } else {
                inspector.setForDocument(root.getPanel().getView().getDocument(), root.getPanel().getView().getSharedContext(), root.getPanel().getView().getSharedContext().getCss());
            }
            inspectorFrame.show();
        }
    }

    class BoxOutlinesAction extends AbstractAction {

        BoxOutlinesAction() {
            super("Show Box Outlines");
            putValue(MNEMONIC_KEY, KeyEvent.VK_B);
        }

        public void actionPerformed(ActionEvent evt) {
            root.getPanel().getView().getSharedContext().setDebug_draw_boxes(!root.getPanel().getView().getSharedContext().debugDrawBoxes());
            root.getPanel().getView().repaint();
        }
    }

    class LineBoxOutlinesAction extends AbstractAction {

        LineBoxOutlinesAction() {
            super("Show Line Box Outlines");
            putValue(MNEMONIC_KEY, VK_L);
        }

        public void actionPerformed(ActionEvent evt) {
            root.getPanel().getView().getSharedContext().setDebug_draw_line_boxes(!root.getPanel().getView().getSharedContext().debugDrawLineBoxes());
            root.getPanel().getView().repaint();
        }
    }

    class InlineBoxesAction extends AbstractAction {
        InlineBoxesAction() {
            super("Show Inline Boxes");
            putValue(MNEMONIC_KEY, VK_I);
        }

        public void actionPerformed(ActionEvent evt) {
            root.getPanel().getView().getSharedContext().setDebug_draw_inline_boxes(!root.getPanel().getView().getSharedContext().debugDrawInlineBoxes());
            root.getPanel().getView().repaint();
        }
    }

    class FontMetricsAction extends AbstractAction {
        FontMetricsAction() {
            super("Show Font Metrics");
            putValue(MNEMONIC_KEY, VK_F);
        }

        public void actionPerformed(ActionEvent evt) {
            root.getPanel().getView().getSharedContext().setDebug_draw_font_metrics(!root.getPanel().getView().getSharedContext().debugDrawFontMetrics());
            root.getPanel().getView().repaint();
        }
    }

    class LoadAction extends AbstractAction {

        protected String url;

        private String pageName;

        public LoadAction(String name, String url) {
            super(name);
            pageName = name;
            this.url = url;
        }

        public void actionPerformed(ActionEvent evt) {
            try {
                root.getPanel().loadPage(url);
                lastDemoOpened = pageName;
            } catch (Exception ex) {
                log.error("actionPerformed", ex);
            }
        }

    }

    class AntiAliasedAction extends AbstractAction {
        int fontSizeThreshold;

        AntiAliasedAction(String text, int fontSizeThreshold) {
            super(text);
            this.fontSizeThreshold = fontSizeThreshold;
        }

        public void actionPerformed(ActionEvent evt) {
            root.getPanel().getView().getSharedContext().getTextRenderer().setSmoothingThreshold(fontSizeThreshold);
            root.getPanel().getView().repaint();
        }
    }

}

