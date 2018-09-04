package org.xhtmlrenderer.script.html5.canvas.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.var;
import lombok.val;
import org.xhtmlrenderer.script.html5.canvas.CanvasLineCap;
import org.xhtmlrenderer.script.html5.canvas.CanvasLineJoin;
import org.xhtmlrenderer.script.html5.canvas.CanvasTextBaseline;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * @author Taras Maslov
 * 5/31/2018
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class G2DState implements Cloneable {

    AffineTransform transform = new AffineTransform();
    Color fillColor;
    Color strokeColor;
    double lineWidth = 1;
    int fontSize = 12;
    float globalAlpha = 1.0f;
    CanvasTextBaseline canvasTextBaseline;
    CanvasLineCap lineCap = CanvasLineCap.butt;
    CanvasLineJoin lineJoin;
    double miterLimit;
    List lineDash;

    void apply(Graphics2D graphics2D) {
        graphics2D.setTransform(transform);
        graphics2D.setStroke(createStroke());
        graphics2D.setFont(new Font("sans-serif", Font.PLAIN, fontSize)); // todo cache
    }
    
    
    private Stroke createStroke(){
        
        final int swingCap;
        switch (lineCap) {
            case butt:
                swingCap = BasicStroke.CAP_BUTT;
                break;
            case round: 
                swingCap = BasicStroke.CAP_ROUND;
                break;
            case square:
                swingCap = BasicStroke.CAP_SQUARE;
                break;
            default:
                swingCap = BasicStroke.CAP_BUTT;
        }
        
        return new BasicStroke((float) lineWidth, swingCap, BasicStroke.JOIN_MITER);
    }
    
    void apply(Graphics2D graphics2D, boolean fill) {
        apply(graphics2D);
        var color = fill ? fillColor : strokeColor;
        if(globalAlpha != 1.0){
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(), globalAlpha);
        }
        graphics2D.setColor(color);
    }

    public void scale(double x, double y) {
        transform.scale(x, y);
    }

    void rotate(double angle) {
        transform.rotate(angle);
    }

    void translate(double x, double y) {
        transform.translate(x, y);
    }

    void transform(double a, double b, double c, double d, double e, double f) {
        transform.concatenate(new AffineTransform(a, b, c, d, e, f));
    }

    void setTransform(double a, double b, double c, double d, double e, double f) {
        transform.setTransform(new AffineTransform(a, b, c, d, e, f));
    }
    
    void setTransform(AffineTransform transform) {
        this.transform = transform;
    }
    
    public void setFillColor(Color color) {
        this.fillColor = color;
    }

    public void setStrokeColor(Color color) {
        this.strokeColor = color;
    }

    public void setLineWidth(double value) {
        this.lineWidth = value;
    }

    public G2DState setFontSize(int fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public G2DState setGlobalAlpha(float globalAlpha) {
        this.globalAlpha = globalAlpha;
        return this;
    }

    @Override
    public G2DState clone()  {
        try {
            val res = (G2DState) super.clone();
            res.fillColor = fillColor;
            res.lineWidth = lineWidth;
            res.globalAlpha = globalAlpha;
            res.transform = new AffineTransform(this.transform);
            res.canvasTextBaseline = canvasTextBaseline;
            res.lineCap = lineCap;
            return res;
        } catch (CloneNotSupportedException e){
            throw new RuntimeException(e);
        }
    }


    public void setTextBaseline(CanvasTextBaseline canvasTextBaseline) {
        this.canvasTextBaseline = canvasTextBaseline;
    }

    public void setLineCap(CanvasLineCap canvasLineCap) {
        this.lineCap = canvasLineCap;    
    }
    
    public CanvasLineCap getLineCap(){
        return lineCap;
    }
    
}
