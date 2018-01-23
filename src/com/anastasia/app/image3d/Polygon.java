package com.anastasia.app.image3d;

class Polygon {

    Point3D[] points;
    private double[] xPoints, yPoints, zPoints;

    Polygon(Point3D... points) {
        this.points = points;
    }

    double[] xPoints() {
        xPoints = new double[size()];
        for (int i = 0; i < xPoints.length; ++i) {
            xPoints[i] = points[i].x;
        }

        return xPoints;
    }

    double[] yPoints() {
        yPoints = new double[size()];
        for (int i = 0; i < yPoints.length; ++i) {
            yPoints[i] = points[i].y;
        }

        return yPoints;
    }

    double[] zPoints() {
        zPoints = new double[size()];
        for (int i = 0; i < zPoints.length; ++i) {
            zPoints[i] = points[i].z;
        }

        return zPoints;
    }

    public int size() {
        return points.length;
    }
}
