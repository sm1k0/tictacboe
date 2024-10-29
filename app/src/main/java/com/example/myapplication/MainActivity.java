package com.example.myapplication;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences themeSettings, gameStats;
    private SharedPreferences.Editor settingsEditor, statsEditor;

    private ImageButton imageTheme;
    private ToggleButton toggleGameMode;
    private Spinner difficultySpinner;
    private Button restartButton;
    private TextView playerXScoreText, playerOScoreText, drawScoreText;
    private Button[] boardButtons = new Button[9];
    private boolean isPlayerXTurn = true;
    private boolean isBotEnabled = false;
    private boolean isGameActive = true; // Game state flag
    private int playerXScore, playerOScore, drawScore;
    private int botLevel = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeSettings = getSharedPreferences("SETTINGS", MODE_PRIVATE);
        gameStats = getSharedPreferences("STATS", MODE_PRIVATE);
        settingsEditor = themeSettings.edit();
        statsEditor = gameStats.edit();

        loadScores();
        if (themeSettings.getBoolean("MODE_NIGHT_ON", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_main);


        imageTheme = findViewById(R.id.Img);
        toggleGameMode = findViewById(R.id.toggleGameMode);
        difficultySpinner = findViewById(R.id.difficultySpinner);
        restartButton = findViewById(R.id.restartButton);
        playerXScoreText = findViewById(R.id.playerXScore);
        playerOScoreText = findViewById(R.id.playerOScore);
        drawScoreText = findViewById(R.id.drawScore);


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.difficulty_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(adapter);


        imageTheme.setOnClickListener(view -> toggleTheme());
        toggleGameMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isBotEnabled = isChecked;
            resetBoard();
            updateDifficultyVisibility();
        });

        difficultySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                botLevel = position + 1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        restartButton.setOnClickListener(view -> resetBoard());

        updateScoreDisplay();
        initializeBoard();
        updateImageButton();
        updateDifficultyVisibility();
    }

    private void initializeBoard() {
        for (int i = 0; i < boardButtons.length; i++) {
            String buttonID = "button" + i;
            int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
            boardButtons[i] = findViewById(resID);
            boardButtons[i].setOnClickListener(new ButtonClickListener(i));
        }
    }

    private void loadScores() {
        playerXScore = gameStats.getInt("playerXScore", 0);
        playerOScore = gameStats.getInt("playerOScore", 0);
        drawScore = gameStats.getInt("drawScore", 0);
    }

    private void updateScoreDisplay() {
        playerXScoreText.setText("Игрок X: " + playerXScore);
        playerOScoreText.setText("Игрок O: " + playerOScore);
        drawScoreText.setText("Ничьи: " + drawScore);
    }

    private void toggleTheme() {
        boolean isNightMode = themeSettings.getBoolean("MODE_NIGHT_ON", false);
        settingsEditor.putBoolean("MODE_NIGHT_ON", !isNightMode);
        settingsEditor.apply();
        AppCompatDelegate.setDefaultNightMode(isNightMode ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
        updateImageButton();
    }

    private void updateImageButton() {
        boolean isNightMode = themeSettings.getBoolean("MODE_NIGHT_ON", false);
        imageTheme.setImageResource(isNightMode ? R.drawable.sun : R.drawable.icon);
    }

    private void updateDifficultyVisibility() {
        if (isBotEnabled) {
            difficultySpinner.setVisibility(View.VISIBLE);
        } else {
            difficultySpinner.setVisibility(View.GONE);
        }
    }

    private void resetBoard() {
        isGameActive = true; // Reset the game state
        for (Button button : boardButtons) {
            button.setText("");
            button.setEnabled(true);
            button.setBackgroundColor(Color.TRANSPARENT);
        }
        updateScoreDisplay();
        restartButton.setVisibility(View.GONE);
    }

    private class ButtonClickListener implements View.OnClickListener {
        private final int index;

        public ButtonClickListener(int index) {
            this.index = index;
        }

        @Override
        public void onClick(View view) {
            if (!isGameActive) return;

            Button button = (Button) view;
            if (isPlayerXTurn) {
                button.setText("X");
                button.setEnabled(false);
            } else {
                button.setText("O");
                button.setEnabled(false);
            }

            if (checkForWin()) {
                if (isPlayerXTurn) {
                    playerXScore++;
                    Toast.makeText(MainActivity.this, "Игрок X выиграл!", Toast.LENGTH_SHORT).show();
                } else {
                    playerOScore++;
                    Toast.makeText(MainActivity.this, "Игрок O выиграл!", Toast.LENGTH_SHORT).show();
                }
                isGameActive = false;
                showRestartButton();
                updateScoreDisplay();
                return;
            }

            if (checkForDraw()) {
                drawScore++;
                Toast.makeText(MainActivity.this, "Ничья!", Toast.LENGTH_SHORT).show();
                isGameActive = false;
                showRestartButton();
                updateScoreDisplay();
                return;
            }

            isPlayerXTurn = !isPlayerXTurn;
            if (isBotEnabled && !isPlayerXTurn) {
                botMove();
            }
        }
    }

    private boolean checkForWin() {
        String[][] board = new String[3][3];
        for (int i = 0; i < boardButtons.length; i++) {
            board[i / 3][i % 3] = boardButtons[i].getText().toString();
        }


        for (int i = 0; i < 3; i++) {
            if (board[i][0].equals(board[i][1]) && board[i][1].equals(board[i][2]) && !board[i][0].isEmpty()) {
                highlightWinningCombination(i * 3, i * 3 + 1, i * 3 + 2);
                return true;
            }
            if (board[0][i].equals(board[1][i]) && board[1][i].equals(board[2][i]) && !board[0][i].isEmpty()) {
                highlightWinningCombination(i, i + 3, i + 6);
                return true;
            }
        }
        if (board[0][0].equals(board[1][1]) && board[1][1].equals(board[2][2]) && !board[0][0].isEmpty()) {
            highlightWinningCombination(0, 4, 8);
            return true;
        }
        if (board[0][2].equals(board[1][1]) && board[1][1].equals(board[2][0]) && !board[0][2].isEmpty()) {
            highlightWinningCombination(2, 4, 6);
            return true;
        }
        return false;
    }

    private void highlightWinningCombination(int... indices) {
        for (int index : indices) {
            boardButtons[index].setBackgroundColor(Color.RED);
        }
    }

    private boolean checkForDraw() {
        for (Button button : boardButtons) {
            if (button.getText().toString().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void showRestartButton() {
        restartButton.setVisibility(View.VISIBLE);
    }

    private void botMove() {
        switch (botLevel) {
            case 1:
                makeRandomMove();
                break;
            case 2:

                if (!makeWinningMove("O") && !makeWinningMove("X")) {
                    makeRandomMove();
                }
                break;

        }
        isPlayerXTurn = true;
    }

    private void makeRandomMove() {
        Random random = new Random();
        int index;
        do {
            index = random.nextInt(9);
        } while (!boardButtons[index].getText().toString().isEmpty());
        boardButtons[index].setText("O");
        boardButtons[index].setEnabled(false);
        if (checkForWin()) {
            playerOScore++;
            Toast.makeText(this, "Игрок O выиграл!", Toast.LENGTH_SHORT).show();
            isGameActive = false;
            showRestartButton();
            updateScoreDisplay();
        }
    }

    private boolean makeWinningMove(String player) {
        for (int i = 0; i < boardButtons.length; i++) {
            if (boardButtons[i].getText().toString().isEmpty()) {
                boardButtons[i].setText(player);
                if (checkForWin()) {
                    boardButtons[i].setEnabled(false);
                    return true;
                } else {
                    boardButtons[i].setText("");
                }
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        statsEditor.putInt("playerXScore", playerXScore);
        statsEditor.putInt("playerOScore", playerOScore);
        statsEditor.putInt("drawScore", drawScore);
        statsEditor.apply();
    }
}
