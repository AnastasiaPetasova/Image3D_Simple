package com.anastasia.app.image3d;

/**
 * Created by Anastasia on 25.01.2018.
 */
public class AffineTransform {
    private double[][] matrix;

    public AffineTransform(double[][] matrix) {
        this.matrix = matrix;
    }

    AffineTransform append(AffineTransform next) {
        double[][] nextMatrix = next.matrix;
        double[][] result = new double[matrix.length][matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                for (int k = 0; k < matrix.length; k++) {
                    result[i][j] += nextMatrix[i][k] * matrix[k][j];
                }
            }
        }

        return new AffineTransform(result);
    }

    Point3D process(Point3D point) {
        double[] coeffs = { point.x, point.y, point.z, 1 };
        double[] result = new double[coeffs.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < coeffs.length; j++) {
                result[i] += matrix[i][j] * coeffs[j];
            }
        }
        return new Point3D(result);
    }

}
