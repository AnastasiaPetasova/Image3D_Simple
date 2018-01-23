package com.anastasia.app.image3d;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.paint.Color;

import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class Image3dController implements Initializable {
    @FXML
    Canvas imageCanvas;

    @FXML
    Button rotateUpButton;

    @FXML
    Button rotateDownButton;

    @FXML
    CheckBox fillPolygonsCheckBox;

    private boolean fillPolygons;

    private static final double RADIUS = 300;
    private static final int N_ALPHA = 30, N_BETA = 30;

    private static final double DELTA_ROTATE_ANGLE = 15 * Math.PI / 180;
    private double rotateAngle = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initCanvas();
        initButtons();
        initCheckBox();

        drawImage();
    }

    private void clearCanvas() {
        GraphicsContext graphics = imageCanvas.getGraphicsContext2D();
        graphics.setFill(Color.WHITE);
        graphics.fillRect(0, 0, imageCanvas.getWidth(), imageCanvas.getHeight());
    }

    private void initCanvas() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int sizeCoeff = Image3dApplication.SIZE_COEFF;

        imageCanvas.setWidth(screenSize.width * sizeCoeff / (sizeCoeff + 1) - 100);
        imageCanvas.setHeight(screenSize.height * sizeCoeff / (sizeCoeff + 1) - 100);

        clearCanvas();
    }

    private void initCheckBox() {
        fillPolygons = true;

        fillPolygonsCheckBox.setOnAction(event -> {
            fillPolygons = !fillPolygons;
            drawImage();
        });
        fillPolygonsCheckBox.setAllowIndeterminate(false);

        fillPolygonsCheckBox.setSelected(fillPolygons);
    }

    private void initButtons() {
        initRotateButtons();
    }

    private void rotateImage(double deltaRotateAngle){
        rotateAngle += deltaRotateAngle;
        if (rotateAngle > 2 * Math.PI) rotateAngle -= 2 * Math.PI;
        if (rotateAngle < 0) rotateAngle += 2 * Math.PI;

        drawImage();
    }

    private void initRotateButtons() {
        rotateUpButton.setOnAction(event -> rotateImage(DELTA_ROTATE_ANGLE));

        rotateDownButton.setOnAction(event -> rotateImage(-DELTA_ROTATE_ANGLE));
    }

    private void drawImage() {
        Polygon[] polygons = generatePolygons(RADIUS, N_ALPHA, N_BETA);
        polygons = prepareToDraw(polygons);
        drawImage(polygons);
    }

    private void drawImage(Polygon[] polygons) {
        GraphicsContext graphicsContext = imageCanvas.getGraphicsContext2D();

        clearCanvas();

        graphicsContext.setStroke(Color.BLACK);

        final double lineWidth = 1;
        graphicsContext.setLineWidth(lineWidth);

        graphicsContext.setFill(Color.WHITE);

        for (Polygon polygon : polygons) {
            double[] xPoints = polygon.xPoints();
            double[] yPoints = polygon.yPoints();
            int n = polygon.size();

            graphicsContext.strokePolygon(
                    xPoints, yPoints, n
            );

            if (fillPolygons) {
                graphicsContext.fillPolygon(
                        xPoints, yPoints, n
                );
            }
        }
    }

    private static Point3D generatePoint(double radius, double alpha, double beta) {
        double x = radius * Math.sin(alpha) * Math.cos(beta);
        double y = radius * Math.sin(alpha) * Math.sin(beta);

        double z = radius * Math.cos(alpha);
        if (z > 0) z += radius / 2;
        else z -= radius / 2;

        return new Point3D(x, y, z);
    }

    private static Polygon[] generatePolygons(double radius, int nAlpha, int nBeta) {

        List<Polygon> polygons = new ArrayList<>();

        double dAlpha = Math.PI / Math.max(nAlpha + 1, 1);
        double dBeta = 2 * Math.PI / Math.max(nBeta + 1, 1);

        for (double alpha = 0; alpha < Math.PI; alpha += dAlpha) {
            double nextAlpha = alpha + dAlpha;
            for (double beta = 0; beta < 2 * Math.PI; beta += dBeta) {
                double nextBeta = beta + dBeta;

                Point3D a = generatePoint(radius, alpha, beta);
                Point3D b = generatePoint(radius, alpha, nextBeta);
                Point3D c = generatePoint(radius, nextAlpha, nextBeta);
                Point3D d = generatePoint(radius, nextAlpha, beta);

                Polygon polygon = new Polygon(a, b, c, d);
                polygons.add(polygon);
            }
        }

        return polygons.toArray(new Polygon[0]);
    }

    private Polygon[] prepareToDraw(Polygon[] polygons) {

        polygons = acsonometricTransform(polygons);
        polygons = rotatePolygonsYZ(polygons, rotateAngle);

        // TODO если нужно преобразование на экран
//        polygons = userViewTransform(polygons);

        polygons = sortByDepth(polygons);
        polygons = screenTransform(polygons);

        return polygons;
    }

    // Поворот в плоскости oYZ вокруг oX
    private static Polygon[] rotatePolygonsYZ(Polygon[] polygons, double angle) {
        for (Polygon polygon : polygons) {
            for (Point3D point : polygon.points) {
                double oldY = point.y, oldZ = point.z;

                point.y = oldY * Math.cos(angle) - oldZ * Math.sin(angle);
                point.z = oldY * Math.sin(angle) + oldZ * Math.cos(angle);
            }
        }

        return polygons;
    }

    private static Polygon[] acsonometricTransform(Polygon[] polygons) {
        // мы поворачиваем вокруг оси ОZ на 0 градусов, поэтому ничего не делаем
        return rotatePolygonsYZ(polygons, Math.PI / 2);
    }

    /**
     * MAX_H = (-R .. R + R) = 3R
     * Экран -> x = [-1.5R, 1.5R], y = [0..MAX_H], z = 1.5R
     * Пользователь -> (0, MAX_H + DELTA_H, 3R] -> tg rotate -> DELTA_H / Y ( OR Y / DELTA_H)
     *
     * double x = xPoints[j] * (zUser - zScreen) / (zUser - zPoints[j]);
     * double y = yPoints[j] * (zUser - zScreen) / (zUser - zPoints[j]);
     * double z = zPoints[j] - zScreen;
     */
    private Polygon[] userViewTransform(Polygon[] polygons) {
        double radius = RADIUS;

        double zUser = 3 * radius;
        double zScreen = 1.5 * radius;

        for (Polygon polygon : polygons) {
            for (Point3D point : polygon.points) {
                double coeff = (zUser - zScreen) / (zUser - point.z);

                point.x *= coeff;
                point.y *= coeff;
                point.z -= zScreen;
            }
        }

        return polygons;
    }

    private Polygon[] sortByDepth(Polygon[] polygons) {
        Arrays.sort(polygons, (a, b) -> {
            double aZSum = 0, bZSum = 0;
            for (double z : a.zPoints()) aZSum += z;
            for (double z : b.zPoints()) bZSum += z;

            return Double.compare(aZSum, bZSum);
        });

        return polygons;
    }

    private Polygon[] screenTransform(Polygon[] polygons) {

        double radius = RADIUS;

        double xMinScreen = Integer.MAX_VALUE;
        double xMaxScreen = Integer.MIN_VALUE;

        double yMinScreen = Integer.MAX_VALUE;
        double yMaxScreen = Integer.MIN_VALUE;

        for (Polygon polygon : polygons) {
            for (double x : polygon.xPoints()) {
                xMinScreen = Math.min(xMinScreen, x);
                xMaxScreen = Math.max(xMaxScreen, x);
            }

            for (double y : polygon.yPoints()) {
                yMinScreen = Math.min(yMinScreen, y);
                yMaxScreen = Math.max(yMaxScreen, y);
            }
        }

        xMinScreen -= 0.5 * radius;
        xMaxScreen += 0.5 * radius;

        yMinScreen -= 0.1 * radius;
        yMaxScreen += 0.1 * radius;

        double coeffX = imageCanvas.getWidth() / (xMaxScreen - xMinScreen);
        double coeffY = imageCanvas.getHeight() / (yMaxScreen - yMinScreen);

        for (Polygon polygon : polygons) {
            for (Point3D point : polygon.points) {
                point.x = ((point.x - xMinScreen) * coeffX);
                point.y = ((yMaxScreen - point.y) * coeffY);
                point.z = 0;
            }
        }

        return polygons;
    }
}
