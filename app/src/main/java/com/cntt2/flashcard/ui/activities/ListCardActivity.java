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
    private Button btnBack;

    private int deskId;

    private static final int ADD_CARD_REQUEST_CODE = 100;
    private static final int EDIT_CARD_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_card);

        btnAdd = findViewById(R.id.btnAdd);
        btnLearn = findViewById(R.id.btnLearn);
        btnCard = findViewById(R.id.btnCard);
        btnThongKe = findViewById(R.id.btnThongKe);
        btnBack = findViewById(R.id.btnBack);

        deskId = getIntent().getIntExtra("deskId", -1);

        if (deskId == -1)
        {
            finish();
            return;
        }


        btnLearn.setOnClickListener(view -> {
            resetButtonBackground();
            btnLearn.setBackgroundResource(R.drawable.toggle_button_background);

            LearnFragment learnFragment = new LearnFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("deskId", deskId);
            learnFragment.setArguments(bundle);

            // Show LearnModeFragment
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, learnFragment);
            transaction.commit();
        });

        btnCard.setOnClickListener(view -> {
            resetButtonBackground();
            btnCard.setBackgroundResource(R.drawable.toggle_button_background);

            // Show CardFragment
            CardsFragment cardsFragment = new CardsFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("deskId", deskId);
            cardsFragment.setArguments(bundle);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, cardsFragment);
            transaction.commit();
        });

        btnThongKe.setOnClickListener(view -> {
            resetButtonBackground();
            btnThongKe.setBackgroundResource(R.drawable.toggle_button_background);

            StatisticsFragment statisticsFragment = new StatisticsFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("deskId", deskId);
            statisticsFragment.setArguments(bundle);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, statisticsFragment);
            transaction.commit();
        });


        // Initial Fragment (CardFragment)
        if (savedInstanceState == null) {
            CardsFragment cardsFragment = new CardsFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("deskId", deskId);
            cardsFragment.setArguments(bundle);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, cardsFragment);
            transaction.commit();
        }

        btnAdd.setOnClickListener(view -> {
            Intent intent = new Intent(ListCardActivity.this, AddCardActivity.class);
            intent.putExtra("isEditMode", false);
            intent.putExtra("deskId", deskId);
            startActivityForResult(intent, ADD_CARD_REQUEST_CODE);
        });

        btnBack.setOnClickListener(view -> {
            finish();
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

        if (requestCode == ADD_CARD_REQUEST_CODE && resultCode == RESULT_OK ) {

            CardsFragment fragment = (CardsFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (fragment != null) {
                fragment.addNewCard();
            }
        }
    }
}
