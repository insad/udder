package com.coillighting.udder.geometry;

/** A 3D bounding box.
 *
 *  See notes about ditching JavaFX (and possibly switching
 *  to Toxic) in Point3D.java.
 */
public class BoundingCube {

    protected double minX = 0.0;
    protected double minY = 0.0;
    protected double minZ = 0.0;

    protected double maxX = 0.0;
    protected double maxY = 0.0;
    protected double maxZ = 0.0;

    protected double width = 0.0;
    protected double height = 0.0;
    protected double depth = 0.0;

    /** Create a 3D bounding box with the given location and dimensions. */
    public BoundingCube(double minX, double minY, double minZ,
                        double width, double height, double depth)
    {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;

        this.width = width;
        this.height = height;
        this.depth = depth;

        this.maxX = width + minX;
        this.maxY = height + minY;
        this.maxZ = depth + minZ;
    }

    /** Create a 2D bounding box as a BoundingCube in the XY plane. */
    public BoundingCube(double minX, double minY,
                        double width, double height)
    {
        this(minX, minY, 0.0, width, height, 0.0);
    }

    public final boolean isEmpty() {
        return width < 0.0 || height < 0.0 || depth < 0.0;
    }

    public final boolean contains(double x, double y, double z) {
        if(this.isEmpty()) {
            return false;
        } else {
            return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
        }
    }

    public final boolean contains(Point3D pt) {
        if(pt != null) {
            return this.contains(pt.getX(), pt.getY(), pt.getZ());
        } else {
            return false;
        }
    }

    public String toString() {
        return "x:[" + minX + ", " + maxX + "] y:[" + minY + ", " + maxY + "] z:[" + minZ + ", " + maxZ + "] w:" + width + " h:" + height + " d:" + depth + "w/h:" + (width/height);
    }

    public final double getMinX() {
        return minX;
    }

    public final void setMinX(double minX) {
        this.minX = minX;
    }

    public final double getMinY() {
        return minY;
    }

    public final void setMinY(double minY) {
        this.minY = minY;
    }

    public final double getMinZ() {
        return minZ;
    }

    public final void setMinZ(double minZ) {
        this.minZ = minZ;
    }

    public final double getWidth() {
        return width;
    }

    public final void setWidth(double width) {
        this.width = width;
    }

    public final double getHeight() {
        return height;
    }

    public final void setHeight(double height) {
        this.height = height;
    }

    public final double getDepth() {
        return depth;
    }

    public final void setDepth(double depth) {
        this.depth = depth;
    }

    public final double getMaxX() {
        return minX + width;
    }

    public final double getMaxY() {
        return minY + height;
    }

    public final double getMaxZ() {
        return minZ + depth;
    }
}
