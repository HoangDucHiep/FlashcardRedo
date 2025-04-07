package com.cntt2.flashcard.ui.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.model.Card;
import com.cntt2.flashcard.utils.ImageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.wasabeef.richeditor.RichEditor;

public class AddCardActivity extends AppCompatActivity {

    private RichEditor edtCardContent;
    private Button btnFront;
    private Button btnBackSide;
    private Button btnBack;
    private Button btnSave;

    private CardRepository cardRepository = App.getInstance().getCardRepository();

    private int deskId;

    private boolean isFrontSide = true; // Track if we're editing the front or back
    private String frontText = "";
    private String backText = "";

    private int historyIndex;
    private List<String> htmlHistory;
    private List<String> imageHistory;
    private Uri photoUri;


    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleImageSelection(uri);
                }
            });

    // ActivityResultLauncher để chụp ảnh
    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            result -> {
                if (result) {
                    handleImageSelection(photoUri);
                }
            });

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

        historyIndex = -1;
        imageHistory = new ArrayList<>();
        htmlHistory = new ArrayList<>();
        updateHistory(edtCardContent.getHtml());

        File photoFile = new File(getFilesDir(), "temp_photo_" + System.currentTimeMillis() + ".jpg");
        photoUri = FileProvider.getUriForFile(this,
                "com.cntt2.flashcard.fileprovider", photoFile);

        deskId = getIntent().getIntExtra("deskId", -1);

        updateToggleState();

        // set up rich editor
        edtCardContent.setPlaceholder("Type in front content");
        edtCardContent.setEditorFontColor(Color.WHITE);
        edtCardContent.setPadding(10, 10, 10, 10);

        edtCardContent.setOnTextChangeListener(text -> {
            updateHistory(text);
        });

        setupEditorControls();

        // Switch to front side
        btnFront.setOnClickListener(v -> {
            if (!isFrontSide) {
                backText = edtCardContent.getHtml();
                isFrontSide = true;
                edtCardContent.setHtml(frontText);
                edtCardContent.setPlaceholder("Type in front content");
                updateToggleState();
            }
        });

        // Switch to back side
        btnBackSide.setOnClickListener(v -> {
            if (isFrontSide) {
                frontText = edtCardContent.getHtml();
                isFrontSide = false;
                edtCardContent.setHtml(backText);
                edtCardContent.setPlaceholder("Type in back content");
                updateToggleState();
            }
        });

        // Back button to return to MainActivity
        btnBack.setOnClickListener(v -> finish());

        // Save button to create a new card
        btnSave.setOnClickListener(v -> saveCard());
        
    }
    
    
    private void setupEditorControls() {
        findViewById(R.id.action_undo).setOnClickListener(v -> {
            if (historyIndex > 0) {
                historyIndex--;
                String previousHtml = htmlHistory.get(historyIndex);
                edtCardContent.setHtml(previousHtml);
            }
        });

        findViewById(R.id.action_redo).setOnClickListener(v -> {
            if (historyIndex < htmlHistory.size() - 1) {
                historyIndex++;
                String nextHtml = htmlHistory.get(historyIndex);
                edtCardContent.setHtml(nextHtml);
            }
        });

        findViewById(R.id.action_bold).setOnClickListener(v -> edtCardContent.setBold());
        findViewById(R.id.action_italic).setOnClickListener(v -> edtCardContent.setItalic());
        findViewById(R.id.action_subscript).setOnClickListener(v -> edtCardContent.setSubscript());
        findViewById(R.id.action_superscript).setOnClickListener(v -> edtCardContent.setSuperscript());
        findViewById(R.id.action_strikethrough).setOnClickListener(v -> edtCardContent.setStrikeThrough());
        findViewById(R.id.action_underline).setOnClickListener(v -> edtCardContent.setUnderline());
        findViewById(R.id.action_heading1).setOnClickListener(v -> edtCardContent.setHeading(1));
        findViewById(R.id.action_heading2).setOnClickListener(v -> edtCardContent.setHeading(2));
        findViewById(R.id.action_heading3).setOnClickListener(v -> edtCardContent.setHeading(3));
        findViewById(R.id.action_heading4).setOnClickListener(v -> edtCardContent.setHeading(4));
        findViewById(R.id.action_heading5).setOnClickListener(v -> edtCardContent.setHeading(5));
        findViewById(R.id.action_heading6).setOnClickListener(v -> edtCardContent.setHeading(6));
        findViewById(R.id.action_txt_color).setOnClickListener(new View.OnClickListener() {
            private boolean isChanged;
            @Override
            public void onClick(View v) {
                edtCardContent.setTextColor(isChanged ? Color.BLACK : Color.RED);
                isChanged = !isChanged;
            }
        });
        findViewById(R.id.action_bg_color).setOnClickListener(new View.OnClickListener() {
            private boolean isChanged;
            @Override
            public void onClick(View v) {
                edtCardContent.setTextBackgroundColor(isChanged ? Color.TRANSPARENT : Color.YELLOW);
                isChanged = !isChanged;
            }
        });
        findViewById(R.id.action_indent).setOnClickListener(v -> edtCardContent.setIndent());
        findViewById(R.id.action_outdent).setOnClickListener(v -> edtCardContent.setOutdent());
        findViewById(R.id.action_align_left).setOnClickListener(v -> edtCardContent.setAlignLeft());
        findViewById(R.id.action_align_center).setOnClickListener(v -> edtCardContent.setAlignCenter());
        findViewById(R.id.action_align_right).setOnClickListener(v -> edtCardContent.setAlignRight());
        findViewById(R.id.action_blockquote).setOnClickListener(v -> edtCardContent.setBlockquote());
        findViewById(R.id.action_insert_bullets).setOnClickListener(v -> edtCardContent.setBullets());
        findViewById(R.id.action_insert_numbers).setOnClickListener(v -> edtCardContent.setNumbers());
        findViewById(R.id.action_insert_youtube).setOnClickListener(v -> edtCardContent.insertYoutubeVideo("https://www.youtube.com/embed/pS5peqApgUA"));
        findViewById(R.id.action_insert_audio).setOnClickListener(v -> edtCardContent.insertAudio("https://file-examples-com.github.io/uploads/2017/11/file_example_MP3_5MG.mp3"));
        findViewById(R.id.action_insert_image).setOnClickListener(v -> showImagePickerDialog());
        findViewById(R.id.action_insert_video).setOnClickListener(v -> edtCardContent.insertVideo("https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_10MB.mp4", 360));
        findViewById(R.id.action_insert_link).setOnClickListener(v -> edtCardContent.insertLink("https://github.com/wasabeef", "wasabeef"));
        findViewById(R.id.action_insert_checkbox).setOnClickListener(v -> edtCardContent.insertTodo());
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


    private void showImagePickerDialog() {
        String[] options = {"Choose from Gallery", "Take a Photo"};
        new AlertDialog.Builder(this)
                .setTitle("Insert Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Chọn ảnh từ thư viện
                        pickImageLauncher.launch("image/*");
                    } else {
                        // Chụp ảnh
                        takePictureLauncher.launch(photoUri);
                    }
                })
                .show();
    }


    private void handleImageSelection(Uri uri) {

    }

    private void updateHistory(String html) {
        // Cắt bỏ lịch sử sau vị trí hiện tại
        while (htmlHistory.size() > historyIndex + 1) {
            htmlHistory.remove(htmlHistory.size() - 1);
        }
        htmlHistory.add(html);
        historyIndex++;
    }

    private void manageImageFiles(String html) {

    }


    private void saveCard() {
        if (isFrontSide) {
            frontText = edtCardContent.getHtml() != null ? edtCardContent.getHtml() : "";
        } else {
            backText = edtCardContent.getHtml() != null ? edtCardContent.getHtml() : "";
        }

        // Kiểm tra validation
        if (frontText.isEmpty() && backText.isEmpty()) {
            // Show error
            return;
        }

        String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        Intent resultIntent = new Intent();
        resultIntent.putExtra("newCardFront", frontText);
        resultIntent.putExtra("newCardBack", backText);
        resultIntent.putExtra("newCardCreatedAt", currentDate);
        setResult(RESULT_OK, resultIntent);
        finish();
    }


}