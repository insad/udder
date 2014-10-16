package com.coillighting.udder;


import com.coillighting.udder.Device;

/** This class is used as a JSON schema spec and output datatype for the Boon
 *  JsonFactory when it deserializes JSON patch sheets exported from Eric's
 *  visualizer. This is just an intermediate representation. We immediately
 *  convert these PatchElements to Devices after parsing.
 *
 *  Example input consisting of three JSON-serialized PatchElements:
 *
 *      [{
 *          "point": [-111, 92.33984355642154, -78.20986874321204],
 *          "group": 0,
 *          "address": 57
 *      }, {
 *          "point": [-111, 93.58520914575311, -78.05527879900943],
 *          "group": 0,
 *          "address": 56
 *      }, {
 *          "point": [-111, 94.70067095432645, -77.91681404627441],
 *          "group": 0,
 *          "address": 55
 *      }]
 *
 *  Like Command and PatchElement, this class is structured for compatibility
 *  with the Boon JsonFactory, which automatically binds JSON bidirectionally
 *  to Java classes which adhere to its preferred (bean-style) layout.
 */
public class PatchElement {

    private double[] point;
    private int group;
    private int address;

    // TODO validate parameter ranges
    public PatchElement(double[] point, int group, int address) {
        this.point = point;
        this.group = group;
        this.address = address;
        this.getZ(); // validates point is between 1 and 3 dimensions
    }

    /* A 1D layout is mandatory. 2D is optional. Return y=0 if unspecified. */
    private double getY() {
        double y=0.0;
        if(this.point == null) {
            throw new IllegalArgumentException(
                "You must provide a point in space for each output device in the patch sheet.");
        } else if(this.point.length >= 2) {
            y = this.point[1];
        } else {
            throw new IllegalArgumentException("Udder does not support "
                + this.point.length + "-dimensional shows.");
        }
        return y;
    }

    /* A 1D layout is mandatory. 3D is optional. Return z=0 if unspecified. */
    private double getZ() {
        double z=0.0;
        if(this.point == null) {
            throw new IllegalArgumentException(
                "You must provide a point in space for each output device in the patch sheet.");
        } else if(this.point.length == 3) {
            z = this.point[2];
        } else {
            throw new IllegalArgumentException("Udder does not support "
                + this.point.length + "-dimensional shows.");
        }
        return z;
    }

    /** Convert this intermediate representation into a full-fledged 3D Udder
     *  Device.
     */
    public Device toDevice() {
        double z = this.getZ();
        double y = this.getY();
        double x = point[0];
        return new Device(this.address, this.group, x, y, z);
    }

    public String toString() {
        double z = this.getZ();
        double y = this.getY();
        double x = point[0];
        return "PatchElement([" + x + "," + y + "," + z + "], " + group + ")";
    }

    public double[] getPoint() {
        return this.point;
    }

    public int getGroup() {
        return this.group;
    }

}
