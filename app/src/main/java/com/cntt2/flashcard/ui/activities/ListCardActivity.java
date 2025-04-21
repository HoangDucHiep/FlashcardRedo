package com.cntt2.flashcard.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.dto.DeskDto;
import com.cntt2.flashcard.data.repository.DeskRepository;
import com.cntt2.flashcard.ui.fragments.CardsFragment;
import com.cntt2.flashcard.ui.fragments.StatisticsFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListCardActivity extends AppCompatActivity {

    private FloatingActionButton btnAdd;
    private Button btnCard;
    private Button btnThongKe;
    private Button btnBack;
    private TextView txtDeskName;
    private ProgressBar progressBar;
    private String deskId;
    private DeskRepository deskRepository = App.getInstance().getDeskRepository();

    private static final int ADD_CARD_REQUEST_CODE = 100;
    private static final int EDIT_CARD_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_card);

        txtDeskName = findViewById(R.id.txtDeskName);
        btnAdd = findViewById(R.id.btnAdd);
        btnCard = findViewById(R.id.btnCard);
        btnThongKe = findViewById(R.id.btnThongKe);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        deskId = getIntent().getStringExtra("deskId");

        if (deskId == null) {
            finish();
            return;
        }

        loadDesk();

        btnCard.setOnClickListener(view -> {
            resetButtonBackground();
            btnCard.setBackgroundResource(R.drawable.toggle_button_background);

            CardsFragment cardsFragment = CardsFragment.newInstance(deskId);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, cardsFragment);
            transaction.commit();
            btnAdd.setVisibility(View.VISIBLE);
        });

        btnThongKe.setOnClickListener(view -> {
            resetButtonBackground();
            btnThongKe.setBackgroundResource(R.drawable.toggle_button_background);

            StatisticsFragment statisticsFragment = StatisticsFragment.newInstance(deskId);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, statisticsFragment);
            transaction.commit();
            btnAdd.setVisibility(View.GONE);
        });

        if (savedInstanceState == null) {
            btnCard.performClick();
        }

        btnAdd.setOnClickListener(view -> {
            Intent intent = new Intent(ListCardActivity.this, AddCardActivity.class);
            intent.putExtra("isEditMode", false);
            intent.putExtra("deskId", deskId);
            startActivityForResult(intent, ADD_CARD_REQUEST_CODE);
        });

        btnBack.setOnClickListener(view -> finish());
    }

    private void loadDesk() {
        progressBar.setVisibility(View.VISIBLE);
        deskRepository.getAllDesks(new Callback<List<DeskDto>>() {
            @Override
            public void onResponse(Call<List<DeskDto>> call, Response<List<DeskDto>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    DeskDto desk = response.body().stream()
                            .filter(d -> d.getId().equals(deskId))
                            .findFirst()
                            .orElse(null);
                    if (desk != null) {
                        txtDeskName.setText(desk.getName());
                    } else {
                        Toast.makeText(ListCardActivity.this, "Desk not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(ListCardActivity.this, "Failed to load desk", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<List<DeskDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ListCardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void resetButtonBackground() {
        btnCard.setBackgroundResource(R.drawable.toggle_background);
        btnThongKe.setBackgroundResource(R.drawable.toggle_background);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == ADD_CARD_REQUEST_CODE || requestCode == EDIT_CARD_REQUEST_CODE) && resultCode == RESULT_OK) {
            CardsFragment fragment = (CardsFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (fragment != null) {
                fragment.updateCardList();
            }
        }
    }
}