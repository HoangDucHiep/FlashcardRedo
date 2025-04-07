package com.cntt2.flashcard.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.ui.fragments.CardsFragment;
import com.cntt2.flashcard.ui.fragments.LearnFragment;
import com.cntt2.flashcard.ui.fragments.StatisticsFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ListCardActivity extends AppCompatActivity {

    private FloatingActionButton btnAdd;
    private Button btnLearn;
    private Button btnCard;
    private Button btnThongKe;

    private static final int ADD_CARD_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_card);

        btnAdd = findViewById(R.id.btnAdd);
        btnLearn = findViewById(R.id.btnLearn);
        btnCard = findViewById(R.id.btnCard);
        btnThongKe = findViewById(R.id.btnThongKe);


        btnLearn.setOnClickListener(view -> {
            resetButtonBackground();
            btnLearn.setBackgroundResource(R.drawable.toggle_button_background);

            // Show LearnModeFragment
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, new LearnFragment());
            transaction.commit();
        });

        btnCard.setOnClickListener(view -> {
            resetButtonBackground();
            btnCard.setBackgroundResource(R.drawable.toggle_button_background);

            // Show CardFragment
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, new CardsFragment());
            transaction.commit();
        });

        btnThongKe.setOnClickListener(view -> {
            resetButtonBackground();
            btnThongKe.setBackgroundResource(R.drawable.toggle_button_background);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, new StatisticsFragment());
            transaction.commit();
        });


        // Initial Fragment (CardFragment)
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, new CardsFragment());
            transaction.commit();
        }

        btnAdd.setOnClickListener(view -> {
            //if (cardList.size() >= 200) return;

            Intent intent = new Intent(ListCardActivity.this, AddCardActivity.class);
            startActivityForResult(intent, ADD_CARD_REQUEST_CODE);
        });
    }

    private void resetButtonBackground() {
        btnLearn.setBackgroundResource(R.drawable.toggle_background);
        btnCard.setBackgroundResource(R.drawable.toggle_background);
        btnThongKe.setBackgroundResource(R.drawable.toggle_background);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_CARD_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Extract the new card details from the Intent
            String front = data.getStringExtra("newCardFront");
            String back = data.getStringExtra("newCardBack");
            String createdAt = data.getStringExtra("newCardCreatedAt");

            // Add the new card to the list and update the fragment
            Card newCard = new Card(front, back, createdAt);
            CardsFragment fragment = (CardsFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (fragment != null) {
                fragment.addNewCard(newCard);
            }
        }
    }
}
