package org.xhtmlrenderer.swing;

import lombok.AllArgsConstructor;
import org.xhtmlrenderer.js.canvas.impl.HTMLCanvasElementImpl;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Taras Maslov
 * 5/29/2018
 */
public class CanvasReplacedElement extends SwingReplacedElement {

//    private HTMLCanvasElementImpl impl;

    public CanvasReplacedElement(HTMLCanvasElementImpl impl) {
        super(new CanvasPanel(impl.getContextImpl().getBufferedImage()));
        getJComponent().setSize(impl.getContextImpl().getWidth(), impl.getContextImpl().getHeight());
//        this.impl = impl;
    }


    @AllArgsConstructor
    static class CanvasPanel extends JPanel {

        private BufferedImage bufferedImage;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(bufferedImage, 0, 0, null);
        }
    }

    @Override
    public boolean isRequiresInteractivePaint() {
        return true;
    }
}