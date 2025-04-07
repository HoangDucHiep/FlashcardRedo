package com.cntt2.flashcard.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.model.Card;

public class AddCardActivity extends AppCompatActivity {

    private EditText edtCardContent;
    private Button btnFront;
    private Button btnBackSide;
    private Button btnBack;
    private Button btnSave;

    private boolean isFrontSide = true; // Track if we're editing the front or back
    private String frontText = "";
    private String backText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);

        // Initialize UI components
        edtCardContent = findViewById(R.id.edtCardContent);
        btnFront = findViewById(R.id.btnFront);
        btnBackSide = findViewById(R.id.btnBackSide);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);

        // Set initial state
        updateToggleState();
        edtCardContent.setHint("Nhập nội dung mặt trước");

        // Switch to front side
        btnFront.setOnClickListener(v -> {
            if (!isFrontSide) {
                backText = edtCardContent.getText().toString();
                isFrontSide = true;
                edtCardContent.setText(frontText);
                edtCardContent.setHint("Nhập nội dung mặt trước");
                updateToggleState();
            }
        });

        // Switch to back side
        btnBackSide.setOnClickListener(v -> {
            if (isFrontSide) {
                frontText = edtCardContent.getText().toString();
                isFrontSide = false;
                edtCardContent.setText(backText);
                edtCardContent.setHint("Nhập nội dung mặt sau");
                updateToggleState();
            }
        });

        // Back button to return to MainActivity
        btnBack.setOnClickListener(v -> finish());

        // Save button to create a new card
        btnSave.setOnClickListener(v -> {
            // Ensure both sides are saved
            if (isFrontSide) {
                frontText = edtCardContent.getText().toString();
            } else {
                backText = edtCardContent.getText().toString();
            }

            // Create a new card
            Card newCard = new Card(frontText, backText, "");

            // Return the new card to MainActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("newCardFront", newCard.getFront());
            resultIntent.putExtra("newCardBack", newCard.getBack());
            resultIntent.putExtra("newCardCreatedAt", newCard.getCreatedAt());
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void updateToggleState() {
        if (isFrontSide) {
            btnFront.setBackgroundResource(R.drawable.toggle_button_background);
            btnBackSide.setBackgroundResource(android.R.color.transparent);
        } else {
            btnFront.setBackgroundResource(android.R.color.transparent);
            btnBackSide.setBackgroundResource(R.drawable.toggle_button_background);
        }
    }
}