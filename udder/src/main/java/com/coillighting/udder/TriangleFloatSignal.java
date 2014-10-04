package com.coillighting.udder;


/** A linearly interpolated signal that continuously oscillates between two
 *  values, with sharp corners at the values themselves.
 */
 public class TriangleFloatSignal extends FloatSignalBase {

    public TriangleFloatSignal(float start, float end, long period) {
        super(start, end, period);
    }

    public float interpolate(float x) {
        float x0;
        float x1;
        if(x <= 0.5f) {
            x0 = start;
            x1 = end;
        } else {
            x -= 0.5f;
            x0 = end;
            x1 = start;
        }
        x *= 2.0f;
        return Util.crossfadeLinear(x, x0, x1);
    }

}
