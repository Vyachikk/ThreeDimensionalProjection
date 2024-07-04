package com.example;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ThreeDimensional_Projection extends Application {

    private static final double WIDTH = 800;
    private static final double HEIGHT = 600;
    private static final double ROTATION_SPEED = 1.0;
    private static final int SIZE = 200, X = 200, Y = 300, Z = -400;
    private static final double[] ROTATION_CENTER = {X, Y, Z};
    private static final double[][] TRIMETRIC_MATRIX = {
            {Math.cos(Math.toRadians(45)), 0, -Math.sin(Math.toRadians(45))},
            {Math.sin(Math.toRadians(-45)) * Math.sin(Math.toRadians(45)), Math.cos(Math.toRadians(-45)), Math.sin(Math.toRadians(-45)) * Math.cos(Math.toRadians(45))},
            {Math.cos(Math.toRadians(-45)) * Math.sin(Math.toRadians(45)), -Math.sin(Math.toRadians(-45)), Math.cos(Math.toRadians(-45)) * Math.cos(Math.toRadians(45))}
    };

    private double rotationAngle = 1;
    private int biasX = 350, biasY = -100, biasZ = -700;
    private Group sceneRoot = new Group();
    
    @Override
    public void start(Stage primaryStage) {

        Scene scene = new Scene(sceneRoot, WIDTH, HEIGHT, Color.WHITE);
        PerspectiveCamera camera = new PerspectiveCamera();
        scene.setCamera(camera);

        primaryStage.setTitle("Trimetric Projection");
        primaryStage.setScene(scene);
        primaryStage.show();

        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(16),
                event -> {
                    rotationAngle += ROTATION_SPEED;
                    if (rotationAngle >= 360) {
                        rotationAngle -= 360;
                    }
                    updatePrism();
                }
        ));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        drawPrism(SIZE, X, Y, Z);
    }

    private void drawPrism(double size, int x, int y, int z) {
        double[][] prismVertices = {
                {x - size / 2, y - size / 2, z - size / 2},
                {x + size / 2, y - size / 2, z - size / 2},
                {x + size / 2, y + size / 2, z - size / 2},
                {x - size / 2, y + size / 2, z - size / 2},
                {x - size / 2, y - size / 2, z + size / 2},
                {x + size / 2, y - size / 2, z + size / 2},
                {x + size / 2, y + size / 2, z + size / 2},
                {x - size / 2, y + size / 2, z + size / 2}
        };

        double[][] projectedVertices = new double[prismVertices.length][3];
        for (int i = 0; i < prismVertices.length; i++) {
            projectedVertices[i] = trimetricProjection(prismVertices[i], rotationAngle);
        }
        drawAxes();
        drawWireframe(projectedVertices);
    }

    private void updatePrism() {
        sceneRoot.getChildren().clear();
        drawPrism(SIZE, X, Y, Z);
    }

    private void drawAxes() {
        double[][] axesVertices = {
                {biasX, biasY, biasZ},
                {800 + biasX, biasY, biasZ},
                {biasX, -600 + biasY, biasZ},
                {biasX, biasY, 800 + biasZ}
        };

        for (int i = 0; i < axesVertices.length; i++) {
            axesVertices[i] = trimetricProjection(axesVertices[i], 220);
        }
    }

    private void drawWireframe(double[][] vertices) {
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };

        for (int[] edge : edges) {
            int startIdx = edge[0];
            int endIdx = edge[1];

            double startZ = vertices[startIdx][2];
            double endZ = vertices[endIdx][2];

            if (isZVisible(startZ) || isZVisible(endZ)) {
                LinearGradient gradient = createGradient(vertices[startIdx], vertices[endIdx]);
                Line line = createLine(vertices[startIdx], vertices[endIdx], gradient);
                line.setStrokeWidth(3);
                sceneRoot.getChildren().add(line);
            }
        }
    }

    private LinearGradient createGradient(double[] start, double[] end) {
        double nearClip = -113;
        double farClip = 113;
    
        double startZ = (start[2] - nearClip) / (farClip - nearClip);
        double endZ = (end[2] - nearClip) / (farClip - nearClip);
    
        startZ = Math.max(0, Math.min(1, startZ));
        endZ = Math.max(0, Math.min(1, endZ));
    
        Color startColor = Color.gray(1 - startZ);
        Color endColor = Color.gray(1 - endZ);
    
        return new LinearGradient(
                start[0] / WIDTH, start[1] / HEIGHT, end[0] / WIDTH, end[1] / HEIGHT, 
                true, CycleMethod.NO_CYCLE, 
                new Stop(0, startColor), 
                new Stop(1, endColor)
        );
    }    

    private Line createLine(double[] start, double[] end, LinearGradient gradient) {
        Line line = new Line(start[0], start[1], end[0], end[1]);
        line.setStroke(gradient);
        return line;
    }

    private boolean isZVisible(double z) {
        double nearClip = -113;
        double farClip = 113;
        return z >= nearClip && z <= farClip;
    }

    private double[] trimetricProjection(double[] point, double angle) {
        double[][] rotationMatrix = {
                {Math.cos(Math.toRadians(angle)), 0, -Math.sin(Math.toRadians(angle))},
                {0, 1, 0},
                {Math.sin(Math.toRadians(angle)), 0, Math.cos(Math.toRadians(angle))}
        };

        double[] translatedPoint = subtractVectors(point, ROTATION_CENTER);
        translatedPoint = multiplyMatrixVector(rotationMatrix, translatedPoint);
        point = addVectors(translatedPoint, ROTATION_CENTER);

        return multiplyMatrixVector(TRIMETRIC_MATRIX, point);
    }

    private double[] subtractVectors(double[] a, double[] b) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] - b[i];
        }
        return result;
    }

    private double[] addVectors(double[] a, double[] b) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] + b[i];
        }
        return result;
    }

    private double[] multiplyMatrixVector(double[][] matrix, double[] vector) {
        double[] result = new double[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < vector.length; j++) {
                result[i] += matrix[i][j] * vector[j];
            }
        }
        return result;
    }

    public static void main(String[] args) {
        launch(args);
    }
}