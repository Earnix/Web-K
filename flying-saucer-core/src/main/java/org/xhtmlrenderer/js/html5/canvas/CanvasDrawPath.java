package org.xhtmlrenderer.js.html5.canvas;

import org.xhtmlrenderer.js.web_idl.Optional;
import org.xhtmlrenderer.js.web_idl.DefaultString;
import org.xhtmlrenderer.js.web_idl.Mixin;
import org.xhtmlrenderer.js.web_idl.Unrestricted;

/**
 * @author Taras Maslov
 * 6/20/2018
 */
@Mixin
public interface CanvasDrawPath {
    // path API (see also CanvasPath)
    void beginPath();

    void fill(@Optional @DefaultString("nonzero") CanvasFillRule fillRule);

    void fill(Path2D path, @Optional @DefaultString("nonzero") CanvasFillRule fillRule);

    void stroke();

    void stroke(Path2D path);

    void clip(@Optional @DefaultString("nonzero") CanvasFillRule fillRule);

    void clip(Path2D path, @Optional @DefaultString("nonzero") CanvasFillRule fillRule);

    void resetClip();

    boolean isPointInPath(@Unrestricted double x, @Unrestricted double y, @Optional @DefaultString("nonzero") CanvasFillRule fillRule);

    boolean isPointInPath(Path2D path, @Unrestricted double x, @Unrestricted double y, @Optional @DefaultString("nonzero") CanvasFillRule fillRule);

    boolean isPointInStroke(@Unrestricted double x, @Unrestricted double y);

    boolean isPointInStroke(Path2D path, @Unrestricted double x, @Unrestricted double y);
}
