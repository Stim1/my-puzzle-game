package puzzlestestchallenge;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PuzzleGame extends Application {
    private static final int WIDTH = 200;
    private static final int HEIGHT = 200;
    private static final int ROWS = 4;
    private static final int COLS = 4;

    private ImageView[][] puzzleGrid;
    private ImageView[][] initialPuzzleGrid;
    private ImageView selectedPiece;
    private double puzzlePieceOffsetX;
    private double puzzlePieceOffsetY;
    private Image puzzleImage;
    private File imageFile;
    private Button shuffleButton;
    private Button solveButton;

    @Override
    public void start(Stage primaryStage) {
        Pane puzzlePane = new Pane();
        puzzlePane.setPrefSize(COLS * WIDTH, ROWS * HEIGHT);

        puzzleImage = chooseImage();
        if (puzzleImage == null) {
            primaryStage.close();
            return;
        }

        puzzleGrid = createPuzzle();

        addPuzzlePiecesToPane(puzzlePane);

        shuffleButton = new Button("Mix puzzles");
        shuffleButton.setLayoutX(10);
        shuffleButton.setLayoutY(ROWS * HEIGHT + 20);
        shuffleButton.setOnAction(event -> {
            shuffle();
            addPuzzlePiecesToPane(puzzlePane);
            shuffleButton.setDisable(true);
            solveButton.setDisable(false);
            savePuzzleInfo();
        });

        solveButton = new Button("Solve puzzles");
        solveButton.setLayoutX(120);
        solveButton.setLayoutY(ROWS * HEIGHT + 20);
        solveButton.setDisable(true);
        solveButton.setOnAction(event -> {
            solvePuzzle();
            shuffleButton.setDisable(false);
            solveButton.setDisable(true);
            savePuzzleInfo();
        });

        Scene scene = new Scene(new Pane(puzzlePane, shuffleButton, solveButton));
        primaryStage.setTitle("Puzzle Game");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Image chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Image");
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "Image Files", "*.jpg", "*.png", "*.jpeg");
        fileChooser.getExtensionFilters().add(imageFilter);

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            imageFile = selectedFile;
            return new Image(selectedFile.toURI().toString());
        }
        return null;
    }

    public ImageView[][] createPuzzle() {
        ImageView[][] puzzleGrid = new ImageView[ROWS][COLS];
        initialPuzzleGrid = new ImageView[ROWS][COLS];

        double imageWidth = puzzleImage.getWidth();
        double imageHeight = puzzleImage.getHeight();
        double pieceWidth = imageWidth / COLS;
        double pieceHeight = imageHeight / ROWS;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                ImageView puzzlePiece = new ImageView();
                puzzlePiece.setImage(puzzleImage);

                puzzlePiece.setFitWidth(WIDTH);
                puzzlePiece.setFitHeight(HEIGHT);

                puzzlePiece.setViewport(new Rectangle2D(col * pieceWidth, row * pieceHeight, pieceWidth, pieceHeight));
                puzzlePiece.setOnMousePressed(event -> {
                    selectedPiece = (ImageView) event.getSource();
                    puzzlePieceOffsetX = event.getSceneX() - selectedPiece.getTranslateX();
                    puzzlePieceOffsetY = event.getSceneY() - selectedPiece.getTranslateY();
                });
                puzzlePiece.setOnMouseDragged(event -> {
                    if (selectedPiece != null) {
                        selectedPiece.setTranslateX(event.getSceneX() - puzzlePieceOffsetX);
                        selectedPiece.setTranslateY(event.getSceneY() - puzzlePieceOffsetY);
                    }
                });
                puzzlePiece.setOnMouseReleased(event -> {
                    if (selectedPiece != null) {
                        snapPieceToGrid(selectedPiece);
                        selectedPiece = null;
                        checkPuzzleCompletion();
                        savePuzzleInfo();
                    }
                });
                puzzleGrid[row][col] = puzzlePiece;
                initialPuzzleGrid[row][col] = puzzlePiece;
            }

        }
        return puzzleGrid;
    }

    private void addPuzzlePiecesToPane(Pane puzzlePane) {
        puzzlePane.getChildren().clear();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                ImageView puzzlePiece = puzzleGrid[row][col];

                if (puzzlePiece != null) {
                    double targetX = col * WIDTH;
                    double targetY = row * HEIGHT;

                    puzzlePiece.setTranslateX(targetX);
                    puzzlePiece.setTranslateY(targetY);
                    puzzlePane.getChildren().add(puzzlePiece);
                }
            }
        }
    }

    private void snapPieceToGrid(ImageView puzzlePiece) {
        double pieceX = puzzlePiece.getTranslateX();
        double pieceY = puzzlePiece.getTranslateY();

        int targetRow = (int) (pieceY / HEIGHT);
        int targetCol = (int) (pieceX / WIDTH);

        double targetX = targetCol * WIDTH;
        double targetY = targetRow * HEIGHT;

        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.2), puzzlePiece);
        transition.setToX(targetX);
        transition.setToY(targetY);
        transition.play();

        puzzleGrid[targetRow][targetCol] = puzzlePiece;
        checkPuzzleCompletion();
    }

    private void shuffle() {
        puzzleGrid = clonePuzzleGrid(initialPuzzleGrid);
        List<ImageView> puzzlePieces = new ArrayList<>();

        for (int row = 0; row < ROWS; row++) {
            System.arraycopy(initialPuzzleGrid[row], 0, puzzleGrid[row], 0, COLS);
        }

        for (int row = 0; row < ROWS; row++) {
            puzzlePieces.addAll(Arrays.asList(puzzleGrid[row]).subList(0, COLS));
        }

        Collections.shuffle(puzzlePieces);

        int index = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                ImageView puzzlePiece = puzzlePieces.get(index);
                puzzleGrid[row][col] = puzzlePiece;
                puzzlePiece.setTranslateX(col * WIDTH);
                puzzlePiece.setTranslateY(row * HEIGHT);
                index++;
            }
        }
    }

    private ImageView[][] clonePuzzleGrid(ImageView[][] puzzleGrid) {
        ImageView[][] clonedGrid = new ImageView[ROWS][COLS];

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                ImageView puzzlePiece = puzzleGrid[row][col];
                ImageView clonedPiece = new ImageView(puzzlePiece.getImage());
                clonedPiece.setFitWidth(WIDTH);
                clonedPiece.setFitHeight(HEIGHT);
                clonedPiece.setTranslateX(puzzlePiece.getTranslateX());
                clonedPiece.setTranslateY(puzzlePiece.getTranslateY());
                clonedGrid[row][col] = clonedPiece;
            }
        }
        return clonedGrid;
    }

    public void solvePuzzle() {
        for (int row = 0; row < ROWS; row++) {
            System.arraycopy(initialPuzzleGrid[row], 0, puzzleGrid[row], 0, COLS);
        }

        double imageWidth = puzzleImage.getWidth();
        double imageHeight = puzzleImage.getHeight();
        double pieceWidth = imageWidth / COLS;
        double pieceHeight = imageHeight / ROWS;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                ImageView puzzlePiece = puzzleGrid[row][col];

                if (puzzlePiece != null) {
                    puzzlePiece.setFitWidth(WIDTH);
                    puzzlePiece.setFitHeight(HEIGHT);

                    puzzlePiece.setViewport(new Rectangle2D(col * pieceWidth,
                            row * pieceHeight, pieceWidth, pieceHeight));

                    puzzlePiece.setTranslateX(col * WIDTH);
                    puzzlePiece.setTranslateY(row * HEIGHT);
                    checkPuzzleCompletion();
                }
            }
        }
    }

    private void checkPuzzleCompletion() {
        boolean isCompleted = true;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                ImageView puzzlePiece = puzzleGrid[row][col];

                if (puzzlePiece != null) {
                    double targetX = col * WIDTH;
                    double targetY = row * HEIGHT;

                    if (puzzlePiece.getTranslateX() != targetX
                            || puzzlePiece.getTranslateY() != targetY) {
                        isCompleted = false;
                        break;
                    }
                }
            }
        }

        if (isCompleted) {
            showCompletionAlert();
        }
    }

    private void showCompletionAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Puzzle Completed");
        alert.setHeaderText(null);
        alert.setContentText("Congratulations! You completed the puzzle.");
        alert.showAndWait();
    }

    private void savePuzzleInfo() {
        try {
            PrintWriter writer = new PrintWriter("puzzle_info.txt");
            writer.println("Image File: " + imageFile.getPath());
            writer.println("Puzzle Pieces:");

            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    ImageView puzzlePiece = puzzleGrid[row][col];

                    if (puzzlePiece != null) {
                        double pieceX = puzzlePiece.getTranslateX();
                        double pieceY = puzzlePiece.getTranslateY();
                        writer.println("Row: " + row + ", Col: " + col + ", X: " + pieceX + ", Y: " + pieceY);
                    }
                }
            }

            writer.close();
            System.out.println("Puzzle information saved successfully.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
