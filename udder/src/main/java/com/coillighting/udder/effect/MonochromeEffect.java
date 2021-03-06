package com.coillighting.udder.effect;

import com.coillighting.udder.mix.TimePoint;
import com.coillighting.udder.model.Pixel;

/** Show a single color on all pixels. */
public class MonochromeEffect extends EffectBase {

    private boolean dirty = false;
    private Pixel color = null;

    public MonochromeEffect(Pixel color) {
        this.setColor(color);
    }

    public Class getStateClass() {
        return Pixel.class;
    }

    public Object getState() {
        if(this.color == null) {
            return null;
        } else {
            return new Pixel(this.color);
        }
    }

    public void setState(Object state) throws ClassCastException {
        Pixel newColor = (Pixel) state;
        newColor.clip();
        this.setColor(newColor);
    }

    public void setColor(Pixel color) {
        // Do not dirty this effect if color hasn't actually changed.
        if(color == null) {
            throw new NullPointerException("MonochromeEffect requires a color.");
        } else if(this.color == null || !this.color.equals(color)) {
            this.color = new Pixel(color);
            this.dirty = true;
        }
    }

    /** Draw pictures only when needed. */
    public void animate(TimePoint timePoint) {
        if(this.dirty) {
            if(this.pixels != null) {
                for(int i=0; i<this.pixels.length; i++) {
                    this.pixels[i].setColor(this.color);
                }
            }
            this.dirty = false;
        }
    }

}
