package com.coillighting.udder.scene;

import com.coillighting.udder.effect.woven.WovenEffect;
import com.coillighting.udder.geometry.Interpolator;
import com.coillighting.udder.mix.StatefulAnimator;
import com.coillighting.udder.mix.Layer;
import com.coillighting.udder.mix.Mixer;
import com.coillighting.udder.mix.TimePoint;

import java.awt.geom.Point2D;

import static com.coillighting.udder.geometry.Interpolator.Interpolation;

/** Implements a 'shuffle' mode specifically tailored to the Boulder Dairy
 * Archway's scene. Cross-fades layers in groups that look good together,
 * periodically stopping to fade everything out and fade in the Woven effect,
 * with synchronized startup of the Woven effect from its opening queue
 * (a blackout of several seconds).
 *
 * When not displaying the Woven scene, shows 1 incoming look + 1 primary
 * look + 1 outgoing look. Fade-in of the incoming look and fade-out of
 * the outgoing look have their own easing curves, randomly selected from
 * the available modes.
 */
public class DairyShuffler implements StatefulAnimator {

    protected Mixer mixer;
    protected Interpolator interpolator;

    int wovenLayerIndex;
    int shuffleLayerStartIndex; // inclusive
    int shuffleLayerEndIndex; // inclusive
    long cueStartTimeMillis;
    long textureCueDurationMillis;
    long wovenCueDurationMillis;
    long cueDurationMillis;

    private DairyShufflerFadeTiming[] fadeTimings;

    boolean enabled;
    boolean wovenMode;
    protected Interpolation interpolationModeIncoming;
    protected Interpolation interpolationModeOutgoing;
    protected int incomingLayerIndex;
    protected double incomingLevel;
    protected double primaryLevel;
    protected double outgoingLevel;
    protected double wovenLevel;

    // temp variables we don't want to keep reallocating in every event
    private Point2D.Double off;
    private Point2D.Double current;
    private Point2D.Double on;

    public DairyShuffler(Mixer mixer, int wovenLayerIndex, int shuffleLayerStartIndex, int shuffleLayerEndIndex,
                         DairyShufflerFadeTiming[] timings)
    {
        if(mixer == null) {
            throw new NullPointerException("DairyShuffler requires a Mixer before it can shuffle.");
        } else {
            int ct = mixer.size();
            if(wovenLayerIndex < 0 || wovenLayerIndex >= ct) {
                throw new IllegalArgumentException(
                    "This mixer contains no layer at wovenLayerIndex="
                    + wovenLayerIndex);

            } else if(shuffleLayerStartIndex < 0 || shuffleLayerStartIndex >= ct) {
                throw new IllegalArgumentException(
                    "This mixer contains no layer at shuffleLayerStartIndex="
                    + shuffleLayerStartIndex);

            } else if(shuffleLayerEndIndex < 0 || shuffleLayerEndIndex >= ct) {
                throw new IllegalArgumentException(
                    "This mixer contains no layer at shuffleLayerEndIndex="
                    + shuffleLayerStartIndex);

            } else if(shuffleLayerEndIndex <= shuffleLayerStartIndex) {
                throw new IllegalArgumentException(
                    "A shuffler's shuffleLayerStartIndex may not exceed its shuffleLayerEndIndex.");

            } else if(shuffleLayerStartIndex <= wovenLayerIndex
                    && wovenLayerIndex <= shuffleLayerEndIndex) {
                throw new IllegalArgumentException(
                    "A shuffler's woven layer may not also be a shuffled layer.");

            } else if(timings == null) {
                throw new IllegalArgumentException("A list of fade times is required.");
            } else if(timings.length != ct) {
                throw new IllegalArgumentException("Exactly " + ct +
                        " fade times slots are required, because that's how many layers are in your mixer, but you supplied "
                        + timings.length + " of them.");
            } else {
                for(int i=0; i<timings.length; i++) {
                    boolean inRange = i >= shuffleLayerStartIndex && i <= shuffleLayerEndIndex;
                    if(timings[i] == null && inRange) {
                        throw new IllegalArgumentException("Every shuffled layer must have a DairyShufflerFadeTiming "
                                + "object which is not null, but timings[" + i + "] is null.");
                    } else if(timings[i] != null && ! inRange) {
                        throw new IllegalArgumentException("A non-shuffled layer must never have a DairyShufflerFadeTiming "
                                + "object, but timings[" + i + "] is not null.");
                    }
                }
                this.fadeTimings = timings;
            }
        }

        // These interpolator curve settings favor a strong three-way mix
        // over what looks almost like a one-way mix.
        // 0.15: fade in quick, linger for a while and fade out quick
        // 2.5: relatively brighter, more gradual fade in and out in POWER mode
        this.interpolator = new Interpolator(0.15, 2.5);
        this.mixer = mixer;
        this.wovenLayerIndex = wovenLayerIndex;
        this.shuffleLayerStartIndex = shuffleLayerStartIndex;
        this.shuffleLayerEndIndex = shuffleLayerEndIndex;
        textureCueDurationMillis = 75000;
        this.reset();
        off = new Point2D.Double(0.0, 0.0);
        current = new Point2D.Double(0.0, 0.0);
        on = new Point2D.Double(1.0, 1.0);
        enabled = true;
        this.mixer.subscribeAnimator(this);
    }

    public void reset() throws ClassCastException {
        wovenMode = true;
        Layer wovenLayer = (Layer) mixer.getLayer(wovenLayerIndex);
        WovenEffect wovenEffect = (WovenEffect) wovenLayer.getEffect();
        wovenCueDurationMillis = wovenEffect.getDurationMillis();
        wovenEffect.reset();
        cueDurationMillis = wovenCueDurationMillis;
        incomingLayerIndex = -1; // < 0: nothing incoming
        incomingLevel = 0.0;
        primaryLevel = 0.0;
        outgoingLevel=0.0;
        wovenLevel = 0.0;
        interpolationModeIncoming = Interpolation.SINUSOIDAL;
        interpolationModeOutgoing = Interpolation.SINUSOIDAL;
        cueStartTimeMillis = -1; // < 0: not started
    }

    // switch off woven vs. other layers only at the transition point,
    // so human operators can play around with transient looks,
    // like woven + textures
    public void animate(TimePoint timePoint) {
        if(enabled) {
            // Step forward or rewind and start over if needed.
            long now = timePoint.sceneTimeMillis();
            if (cueStartTimeMillis < 0) {
                cueStartTimeMillis = now;
            }
            long end = cueStartTimeMillis + cueDurationMillis;
            if (end < now) {
                // Step forward to the next track in the playlist.
                if (wovenMode) {
                    // Switch from woven to texture mode
                    cueDurationMillis = textureCueDurationMillis;
                    wovenMode = false;
                    wovenLevel = 0.0f;
                    mixer.getLayer(wovenLayerIndex).setLevel(wovenLevel);
                    incomingLayerIndex = shuffleLayerStartIndex;
                    interpolationModeIncoming = Interpolation.ROOT; // fade in quick, don't leave it black
                } else if (incomingLayerIndex >= shuffleLayerEndIndex + 2) {
                    // Switch to woven mode
                    for (int i = shuffleLayerStartIndex; i <= shuffleLayerEndIndex; i++) {
                        this.setTextureLevelConditionally(0.0f, i);
                    }
                    this.reset();
                    // level will be set below on transition into Woven effect
                } else {
                    // Start fading in the next texture.
                    // Choose a new easing curve each time.

                    // We used to favor a brighter, three-way mix, but since
                    // we added many open patterns, we've increased the probability
                    // of a thinner, mostly solo or duet mix, by increasing the
                    // chance of a POWER mix to 50/50.
                    interpolationModeIncoming = interpolator.randomMode(35, 55, 95);
                    interpolationModeOutgoing = interpolator.randomMode(35, 55, 95);

                    cueDurationMillis = textureCueDurationMillis;

                    if(incomingLayerIndex > shuffleLayerEndIndex) {
                        // At this point there is only one outgoing layer, so
                        // make it quick to avoid spooking the client.
                        //
                        // (The gallery staff gets antsy that the rig has failed
                        // and calls BV whenever they observe a long-tail
                        // fade to black, so we cut the final cue short in order
                        // to loop promptly back to the Woven effect.)
                        cueDurationMillis *= 0.20;
                    }

                    // finish fading out the outgoing track if needed:
                    this.setTextureLevelConditionally(0.0f, incomingLayerIndex - 2);
                    outgoingLevel = primaryLevel;
                    primaryLevel = incomingLevel;
                    incomingLevel = 0.0f;
                    incomingLayerIndex++;
                }
                cueStartTimeMillis = now;
            }

            if (wovenMode) {
                // Don't crossfade into the Woven cue. It builds from black
                // without any help from this Shuffler.
                if (cueStartTimeMillis == now) {
                    wovenLevel = 1.0;
                    mixer.getLayer(wovenLayerIndex).setLevel(wovenLevel);
                }
            } else {
                // texture mode only, not in woven mode
                int li = incomingLayerIndex;
                DairyShufflerFadeTiming inTiming, outTiming;

                // N.B. Fade in and out are not applied to Woven mode thanks to
                // the range check in setTextureLevelConditionally.
                // TODO the following code is a recent edition, probably not too robust. TEST.
                if(li - 2 < shuffleLayerStartIndex) {
                    // won't matter, but must be not null
                    outTiming = fadeTimings[shuffleLayerStartIndex];
                } else {
                    outTiming = fadeTimings[li - 2];
                }

                if(li > shuffleLayerEndIndex) {
                    // likewise won't matter, but must be not null
                    inTiming = fadeTimings[shuffleLayerEndIndex];
                } else {
                    inTiming = fadeTimings[li];
                }

                // Crossfade out of the outgoing cue and into the incoming cue.
                // Do not change the primary cue level.
                double cuePct = (double) (now - cueStartTimeMillis) / cueDurationMillis;

                // To fade out faster, make this number smaller.
                // Range: 0 - 1.0 for 0% to 100% of cue time spent fading.
                double cueFadeOutPct = outTiming.out;
                double outPct;

                // To fade in faster, make this number smaller.
                double cueFadeInPct = inTiming.in;
                double inPct;

                if(cuePct >= cueFadeOutPct || cueFadeOutPct == 0.0) {
                    // Done fading out already.
                    outPct = 1.0;
                } else {
                    // Keep fading out, aiming to be done cueFadeOutPct of the
                    // way along the cue's timeline.
                    outPct = cuePct / cueFadeOutPct;
                }

                double inThreshold = 1.0 - cueFadeInPct;
                if(cuePct <= inThreshold || cueFadeInPct == 0.0) {
                    // Don't start fading in yet.
                    inPct = 0.0;
                } else {
                    // Continue fading in, beginning cueFadeInPct of the way
                    // along the cue's timeline.
                    inPct = (cuePct - inThreshold) / cueFadeInPct;
                }

                // incoming look (if applicable)
                interpolator.interpolate2D(interpolationModeIncoming, inPct, off, current, on);
                // FUTURE: implement a 1D Interpolator API, but for now just
                // piggyback on the existing 2D API and ignore y.
                // FIXME: convert all layer levels to doubles.
                incomingLevel = current.x;
                this.setTextureLevelConditionally(incomingLevel, li);

                // primary look (if applicable)
                li -= 1;
                primaryLevel = 1.0;
                this.setTextureLevelConditionally(primaryLevel, li);

                // outgoing look (if applicable)
                li -= 1;
                interpolator.interpolate2D(interpolationModeOutgoing, outPct, on, current, off);
                outgoingLevel = current.x;
                this.setTextureLevelConditionally(outgoingLevel, li);
            }
        }
    }

    /** Only try to fade in or out a texture layer. Allow Woven to handle its
     *  own dynamics, and ignore out of range layers. Simplifies the impl of
     *  animate somewhat.
     */
    private void setTextureLevelConditionally(double level, int layerIndex) {
        if(layerIndex >= shuffleLayerStartIndex
                && layerIndex <= shuffleLayerEndIndex) {
            mixer.getLayer(layerIndex).setLevel(level);
        }
    }

    public Class getStateClass() {
        return DairyShufflerState.class;
    }

    public Object getState() {
        return new DairyShufflerState(enabled, textureCueDurationMillis);
    }

    public void setState(Object state) throws ClassCastException {
        DairyShufflerState command = (DairyShufflerState) state;
        this.enabled = command.getEnabled();
        long millis = command.getCueDurationMillis();
        if(millis > 0) {
            this.textureCueDurationMillis = millis;
        }
    }

}
