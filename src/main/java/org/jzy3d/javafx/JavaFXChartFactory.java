package org.jzy3d.javafx;

import java.awt.image.BufferedImage;
import java.util.Date;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.Pane;
import org.apache.log4j.Logger;
import org.jzy3d.chart.AWTChart;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.keyboard.camera.ICameraKeyController;
import org.jzy3d.chart.controllers.keyboard.screenshot.AWTScreenshotKeyController;
import org.jzy3d.chart.controllers.keyboard.screenshot.IScreenshotKeyController;
import org.jzy3d.chart.controllers.keyboard.screenshot.IScreenshotKeyController.IScreenshotEventListener;
import org.jzy3d.chart.controllers.keyboard.screenshot.NewtScreenshotKeyController;
import org.jzy3d.chart.controllers.mouse.camera.ICameraMouseController;
import org.jzy3d.chart.controllers.mouse.picking.IMousePickingController;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.javafx.controllers.keyboard.JavaFXCameraKeyController;
import org.jzy3d.javafx.controllers.mouse.JavaFXCameraMouseController;
import org.jzy3d.javafx.controllers.mouse.JavaFXMousePickingController;
import org.jzy3d.maths.Utils;
import org.jzy3d.plot3d.rendering.canvas.OffscreenCanvas;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.view.AWTImageRenderer3d.DisplayListener;
import org.jzy3d.plot3d.rendering.view.AWTRenderer3d;
import org.jzy3d.plot3d.rendering.view.Renderer3d;
import org.jzy3d.plot3d.rendering.view.View;

public class JavaFXChartFactory extends AWTChartComponentFactory {

    static Logger LOGGER = Logger.getLogger(JavaFXChartFactory.class);

    public static String SCREENSHOT_FOLDER = "./data/screenshots/";

    public static Chart chart(Quality quality, String toolkit) {
        JavaFXChartFactory f = new JavaFXChartFactory();
        return f.newChart(quality, toolkit);
    }

    public Image getScreenshotAsJavaFXImage(AWTChart chart) {
        chart.screenshot();
        AWTRenderer3d renderer = (AWTRenderer3d) chart.getCanvas().getRenderer();
        BufferedImage i = renderer.getLastScreenshotImage();
        if (i != null) {
            Image image = SwingFXUtils.toFXImage(i, null);
            return image;
        } else {
            // LOGGER.error(this.getClass() + " SCREENSHOT NULL");
            return null;
        }
    }

    /**
     * Return an {@link ImageView} from an {@link AWTChart} expected to render
     * offscreen and to use a {@link JavaFXRenderer3d} poping Images when the
     * chart is redrawn.
     *
     * @param chart
     * @return
     */
    public ImageView bindImageView(AWTChart chart) {
        ImageView imageView = new ImageView();
        imageView.fitHeightProperty();
        imageView.fitWidthProperty();

        bind(imageView, chart);

        // Initialize imageView
        Image image = getScreenshotAsJavaFXImage(chart);
        if (image != null) {
            System.out.println("setting image at init");
            imageView.setImage(image);
        } else {
            // LOGGER.error("image is null at init");
        }

        JavaFXCameraMouseController jfxMouse = (JavaFXCameraMouseController) chart.addMouseCameraController();
        jfxMouse.setNode(imageView);
        // JavaFXNodeMouse.makeDraggable(stage, imgView);

        JavaFXCameraKeyController jfxKey = (JavaFXCameraKeyController) chart.addKeyboardCameraController();
        jfxKey.setNode(imageView);
        imageView.setFocusTraversable(true);
        return imageView;
    }

    /**
     * Register for renderer notifications with a new JavaFX Image
     */
    public void bind(final ImageView imageView, AWTChart chart) {
        if (!(chart.getCanvas().getRenderer() instanceof JavaFXRenderer3d)) {
            LOGGER.error("NOT BINDING IMAGE VIEW TO CHART AS NOT A JAVAFX RENDERER");
            return;
        }

        // Set listener on renderer to update imageView
        JavaFXRenderer3d renderer = (JavaFXRenderer3d) chart.getCanvas().getRenderer();
        renderer.addDisplayListener(image -> {
            if (image != null) {
                imageView.setImage((Image) image);
            } else {
                LOGGER.error("image is null while listening to renderer");
            }
        });
    }

    public void addSceneSizeChangedListener(Chart chart, Pane pane) {
        pane.widthProperty().addListener((observableValue, oldSceneWidth, newSceneWidth) -> resetTo(chart, pane.widthProperty().get(), pane.heightProperty().get()));
        pane.heightProperty().addListener((observableValue, oldSceneHeight, newSceneHeight) -> resetTo(chart, pane.widthProperty().get(), pane.heightProperty().get()));
    }

    protected void resetTo(Chart chart, double width, double height) {
        if (chart.getCanvas() instanceof OffscreenCanvas canvas) {
            canvas.initBuffer(canvas.getCapabilities(), (int) width, (int) height);
            chart.render();
        } else {
            LOGGER.error("NOT AN OFFSCREEN CANVAS!");
        }
    }

    //custom function
    public void resetSize(Chart chart, double width, double height) {
        if (chart.getCanvas() instanceof OffscreenCanvas canvas) {
            canvas.initBuffer(canvas.getCapabilities(), (int) width, (int) height);
            chart.render();
        } else {
            LOGGER.error("NOT AN OFFSCREEN CANVAS!");
        }
    }

    @Override
    public Renderer3d newRenderer(View view, boolean traceGL, boolean debugGL) {
        return new JavaFXRenderer3d(view, traceGL, debugGL);
    }

    @Override
    public ICameraMouseController newMouseCameraController(Chart chart) {
        return new JavaFXCameraMouseController(chart, null);
    }

    @Override
    public IMousePickingController newMousePickingController(Chart chart, int clickWidth) {
        return new JavaFXMousePickingController(chart, clickWidth);
    }

    @Override
    public ICameraKeyController newKeyboardCameraController(Chart chart) {
        return new JavaFXCameraKeyController(chart, null);
    }

    /**
     * TODO : replace by a JavaFXScreenshotKeyController
     */
    @Override
    public IScreenshotKeyController newKeyboardScreenshotController(Chart chart) {
        // trigger screenshot on 's' letter
        String file = SCREENSHOT_FOLDER + "capture-" + Utils.dat2str(new Date(), "yyyy-MM-dd-HH-mm-ss") + ".png";
        IScreenshotKeyController screenshot;

        if (!chart.getWindowingToolkit().equals("newt"))
            screenshot = new AWTScreenshotKeyController(chart, file);
        else
            screenshot = new NewtScreenshotKeyController(chart, file);

        screenshot.addListener(new IScreenshotEventListener() {
            @Override
            public void failedScreenshot(String file, Exception e) {
                System.out.println("Failed to save screenshot:");
                e.printStackTrace();
            }

            @Override
            public void doneScreenshot(String file) {
                System.out.println("Screenshot: " + file);
            }
        });
        return screenshot;
    }

}
