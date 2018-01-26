package com.anastasia.app.image3d;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
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
    RadioButton xyRadioButton;

    @FXML
    RadioButton xzRadioButton;

    @FXML
    RadioButton yzRadioButton;

    @FXML
    Button rotateUpButton;

    @FXML
    Button rotateDownButton;

    @FXML
    Button moveLeftButton;

    @FXML
    Button moveRightButton;

    @FXML
    Button moveUpButton;

    @FXML
    Button moveDownButton;

    @FXML
    Button zoomDownButton;

    @FXML
    Button zoomUpButton;

    @FXML
    CheckBox fillPolygonsCheckBox;

    private static boolean NEED_USER_VIEW_TRANSFORM = false;
    private static double RADIUS_COEFF = (NEED_USER_VIEW_TRANSFORM ? 0.5 : 0.3);

    private boolean fillPolygons;
    private static final int N_ALPHA = 30, N_BETA = 30;

    private static final double DELTA_ROTATE_ANGLE = 10 * Math.PI / 180;
    private static final int XY = 0, XZ = 1, YZ = 2;

    private static final int NEGATIVE = 0, POSITIVE = 1;
    private static final AffineTransform[][] DELTA_ROTATE_TRANSFORMS;

    static {
        DELTA_ROTATE_TRANSFORMS = new AffineTransform[2][3];

        int[] signs = { -1, 1 };

        int[][] firstSeconds = {
                { AffineTransforms.X, AffineTransforms.Y },
                { AffineTransforms.X, AffineTransforms.Z },
                { AffineTransforms.Y, AffineTransforms.Z }
        };

        for (int i = 0; i < signs.length; ++i) {
            for (int j = 0; j < 3; ++j) {
                DELTA_ROTATE_TRANSFORMS[i][j] = AffineTransforms.rotate(
                        signs[i] * DELTA_ROTATE_ANGLE,
                        firstSeconds[j][0], firstSeconds[j][1]
                        );
            }
        }
    }

    private int curAxis;
    private AffineTransform rotate;

    // умножаем/делим на (1 + 0.05)
    private static final double ZOOM_DELTA_MULTIPLIER = 0.05;
    private double zoomCoeff = 1;

    private static final double MOVE_DELTA = 100;
    private Point moveShift = new Point(0, 0);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initCanvas();
        initRadioButtons();
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

    private double calculateRadius() {
        return Math.min(imageCanvas.getWidth(), imageCanvas.getHeight()) * RADIUS_COEFF;
    }

    private void initRadioButtons() {
        initRotateRadioButtons();
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
        initMoveButtons();
        initZoomButtons();
    }

    private void rotateImage(int direction){
        rotate = rotate.append(DELTA_ROTATE_TRANSFORMS[direction][curAxis]);
        drawImage();
    }

    private void initRotateRadioButtons() {
        ToggleGroup rotateGroup = new ToggleGroup();

        RadioButton[] rotateRadioButtons = {
                xyRadioButton, xzRadioButton, yzRadioButton
        };

        for (int axis = XY; axis <= YZ; ++axis) {
            int finalAxis = axis;
            rotateRadioButtons[axis].setToggleGroup(rotateGroup);
            rotateRadioButtons[axis].setOnAction(event -> curAxis = finalAxis);
        }

        xyRadioButton.fire();
    }

    private void initRotateButtons() {
        rotate = AffineTransforms.equal();

        rotateUpButton.setOnAction(event -> rotateImage(POSITIVE));
        rotateDownButton.setOnAction(event -> rotateImage(NEGATIVE));
    }

    private void initMoveButtons() {
        moveLeftButton.setOnAction( event -> {
            moveShift.x -= MOVE_DELTA;
            drawImage();
        });

        moveRightButton.setOnAction( event -> {
            moveShift.x += MOVE_DELTA;
            drawImage();
        });

        // у Y знаки поменяны, так как на экране он сверху вниз
        moveDownButton.setOnAction( event -> {
            moveShift.y += MOVE_DELTA;
            drawImage();
        });

        moveUpButton.setOnAction( event -> {
            moveShift.y -= MOVE_DELTA;
            drawImage();
        });
    }

    private void initZoomButtons() {
        zoomDownButton.setOnAction(event -> {
            zoomCoeff /= (1 + ZOOM_DELTA_MULTIPLIER);
            drawImage();
        });

        zoomUpButton.setOnAction(event -> {
            zoomCoeff *= (1 + ZOOM_DELTA_MULTIPLIER);
            drawImage();
        });
    }

    private void drawImage() {
        Polygon[] polygons = generatePolygons(calculateRadius(), N_ALPHA, N_BETA);
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

        polygons = polygonsAffineTransform(polygons, rotate);

        // TODO если нужно преобразование на экран
        if (NEED_USER_VIEW_TRANSFORM) {
            polygons = userViewTransform(polygons);
        }

        polygons = sortByDepth(polygons);
        polygons = screenTransform(polygons);

        polygons = zoomTransform(polygons);
        polygons = moveTransform(polygons);

        return polygons;
    }

    private static Polygon[] polygonsAffineTransform(Polygon[] polygons, AffineTransform transform) {
        for (Polygon polygon : polygons) {
            for (int i = 0; i < polygon.points.length; ++i) {
                polygon.points[i] = transform.process(polygon.points[i]);
            }
        }

        return polygons;
    }

    private static Polygon[] acsonometricTransform(Polygon[] polygons) {
        // мы поворачиваем вокруг оси ОZ на 0 градусов, поэтому ничего не делаем
        AffineTransform transform = AffineTransforms.chain(
                AffineTransforms.rotate(0, AffineTransforms.X, AffineTransforms.Y),
                AffineTransforms.rotate(Math.PI / 2, AffineTransforms.Y, AffineTransforms.Z)
        );

        return polygonsAffineTransform(polygons, transform);
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
        double radius = calculateRadius();

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
        double centerX = imageCanvas.getWidth() / 2;
        double centerY = imageCanvas.getHeight() / 2;

        AffineTransform screenTransform = AffineTransforms.shift(centerX, centerY, 0);
        return polygonsAffineTransform(polygons, screenTransform);
    }

    private Polygon[] zoomTransform(Polygon[] polygons) {
        double centerX = imageCanvas.getWidth() / 2;
        double centerY = imageCanvas.getHeight() / 2;

        AffineTransform zoom = AffineTransforms.chain(
                AffineTransforms.shift(-centerX, -centerY, 0),
                AffineTransforms.zoom(zoomCoeff, zoomCoeff, zoomCoeff),
                AffineTransforms.shift(centerX, centerY, 0)
        );

        return polygonsAffineTransform(polygons, zoom);
    }

    private Polygon[] moveTransform(Polygon[] polygons) {
        AffineTransform move = AffineTransforms.shift(moveShift.x, moveShift.y, 0);
        return polygonsAffineTransform(polygons, move);
    }
}
