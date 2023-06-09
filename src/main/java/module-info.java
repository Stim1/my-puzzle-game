module com.example.puzzlestest {
    requires javafx.controls;
    requires javafx.fxml;


    opens puzzlestestchallenge to javafx.fxml;
    exports puzzlestestchallenge;
}