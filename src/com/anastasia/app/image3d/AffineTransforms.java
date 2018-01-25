package com.anastasia.app.image3d;

/**
 * Created by Anastasia on 25.01.2018.
 */
public class AffineTransforms {
    public static final int X = 0, Y = 1, Z = 2, ONE = 3;

    private static double[][] equalMatrix() {
        double[][] matrix = new double[4][4];
        for (int i = 0; i <= ONE; i++) {
            matrix[i][i] = 1;
        }
        return matrix;
    }

    static AffineTransform equal() {
        double[][] matrix = equalMatrix();
        return new AffineTransform(matrix);
    }

    static AffineTransform shift(double dx, double dy, double dz) {
        double[][] matrix = equalMatrix();

        matrix[X][ONE] = dx;
        matrix[Y][ONE] = dy;
        matrix[Z][ONE] = dz;

        return new AffineTransform(matrix);
    }

    static AffineTransform rotate(double angle, int first, int second) {
        if(first > second) {
            int tmp = first;
            first = second;
            second = tmp;
        }
        double[][] matrix = equalMatrix();

        matrix[first][first] *= Math.cos(angle);
        matrix[first][second] *= -Math.sin(angle);
        matrix[second][first] *= Math.sin(angle);
        matrix[second][second] *= Math.cos(angle);

        return new AffineTransform(matrix);
    }

    static AffineTransform zoom(double kx, double ky, double kz) {
        double[][] matrix = equalMatrix();
        matrix[X][X] *= kx;
        matrix[Y][Y] *= ky;
        matrix[Z][Z] *= kz;

        return new AffineTransform(matrix);
    }

    static AffineTransform chain(AffineTransform... transforms) {
        AffineTransform result = equal();
        for (AffineTransform transform : transforms) {
            result = result.append(transform);
        }

        return result;
    }
}
