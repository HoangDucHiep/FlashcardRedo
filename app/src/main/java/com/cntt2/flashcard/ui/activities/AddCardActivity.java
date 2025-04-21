package com.cntt2.flashcard.ui.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.dto.CardDto;
import com.cntt2.flashcard.data.repository.CardRepository;
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
import java.util.TimeZone;

import jp.wasabeef.richeditor.RichEditor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.Manifest;

public class AddCardActivity extends AppCompatActivity {

    private RichEditor edtCardContent;
    private Button btnFront;
    private Button btnBackSide;
    private Button btnBack;
    private Button btnSave;
    private ProgressBar progressBar;

    private CardRepository cardRepository = App.getInstance().getCardRepository();

    private String deskId;
    private boolean isFrontSide = true;
    private String frontText = "<style>img { max-width: 100%; height: auto; }</style>";
    private String backText = "<style>img { max-width: 100%; height: auto; }</style>";

    private int frontHistoryIndex = -1;
    private List<String> frontHtmlHistory = new ArrayList<>();
    private int backHistoryIndex = -1;
    private List<String> backHtmlHistory = new ArrayList<>();

    private Set<String> initialImages;
    private Set<String> sessionImages;
    private Uri photoUri;
    private File imagesDir;

    private boolean isEditMode = false;
    private String cardId;

    private static final int REQUEST_PERMISSIONS = 100;
    private static final String BASE_URL = "http://10.0.2.2:5029/";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "Permissions required to use camera and select images",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleImageSelection(uri);
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            result -> {
                if (result) {
                    try {
                        Uri contentUri = photoUri;
                        Log.d("ImageHandling", "Photo URI: " + contentUri);
                        String fileName = contentUri.getLastPathSegment();
                        File photoFile = new File(imagesDir, fileName);
                        sessionImages.add(photoFile.getAbsolutePath());
                        edtCardContent.insertImage(contentUri.toString(), "Photo");
                        String html = edtCardContent.getHtml();
                        updateHistory(html);
                        Log.d("ImageHandling", "Successfully added photo: " + contentUri + ", Absolute path: " + photoFile.getAbsolutePath());
                    } catch (Exception e) {
                        Log.e("AddCardActivity", "Error processing camera image: " + e.getMessage(), e);
                        Toast.makeText(AddCardActivity.this, "Failed to process photo", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("ImageHandling", "User cancelled taking photo");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);

        checkPermissions();

        edtCardContent = findViewById(R.id.edtCardContent);
        btnFront = findViewById(R.id.btnFront);
        btnBackSide = findViewById(R.id.btnBackSide);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        initialImages = new HashSet<>();
        sessionImages = new HashSet<>();

        imagesDir = new File(getFilesDir(), "images");
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }

        Intent intent = getIntent();
        isEditMode = intent.getBooleanExtra("isEditMode", false);
        cardId = intent.getStringExtra("cardId");
        deskId = intent.getStringExtra("deskId");

        if (deskId == null || deskId.isEmpty()) {
            Toast.makeText(this, "Invalid desk ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (isEditMode && cardId != null) {
            loadCardData();
        } else {
            updateFrontHistory("");
        }

        updateToggleState();

        edtCardContent.setPlaceholder("Type in front content");
        edtCardContent.setEditorFontColor(Color.WHITE);
        edtCardContent.setPadding(10, 10, 10, 10);
        String css = "<style>img { max-width: 100%; height: auto; }</style>";
        String initHtml = edtCardContent.getHtml() == null ? "" : edtCardContent.getHtml();
        edtCardContent.setHtml(css + initHtml);

        edtCardContent.setOnTextChangeListener(text -> {
            if (text == null || text.isEmpty()) {
                edtCardContent.setHtml(css + "");
            }
            updateHistory(text);
        });

        setupEditorControls();

        btnFront.setOnClickListener(v -> {
            if (!isFrontSide) {
                backText = edtCardContent.getHtml();
                updateBackHistory(backText);
                isFrontSide = true;
                edtCardContent.setHtml(frontText);
                edtCardContent.setPlaceholder("Type in front content");
                updateToggleState();
            }
        });

        btnBackSide.setOnClickListener(v -> {
            if (isFrontSide) {
                frontText = edtCardContent.getHtml();
                updateFrontHistory(frontText);
                isFrontSide = false;
                edtCardContent.setHtml(backText);
                edtCardContent.setPlaceholder("Type in back content");
                updateToggleState();
            }
        });

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveCard());
    }

    private void setupEditorControls() {
        findViewById(R.id.action_undo).setOnClickListener(v -> {
            if (isFrontSide) {
                if (frontHistoryIndex > 0) {
                    frontHistoryIndex--;
                    String previousHtml = frontHtmlHistory.get(frontHistoryIndex);
                    edtCardContent.setHtml(previousHtml);
                }
            } else {
                if (backHistoryIndex > 0) {
                    backHistoryIndex--;
                    String previousHtml = backHtmlHistory.get(backHistoryIndex);
                    edtCardContent.setHtml(previousHtml);
                }
            }
        });

        findViewById(R.id.action_redo).setOnClickListener(v -> {
            if (isFrontSide) {
                if (frontHistoryIndex < frontHtmlHistory.size() - 1) {
                    frontHistoryIndex++;
                    String nextHtml = frontHtmlHistory.get(frontHistoryIndex);
                    edtCardContent.setHtml(nextHtml);
                }
            } else {
                if (backHistoryIndex < backHtmlHistory.size() - 1) {
                    backHistoryIndex++;
                    String nextHtml = backHtmlHistory.get(backHistoryIndex);
                    edtCardContent.setHtml(nextHtml);
                }
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
                        pickImageLauncher.launch("image/*");
                    } else {
                        String fileName = "img_" + System.currentTimeMillis() + ".jpg";
                        File imagesDir = new File(getFilesDir(), "images");
                        if (!imagesDir.exists()) {
                            imagesDir.mkdirs();
                        }
                        File photoFile = new File(imagesDir, fileName);
                        photoUri = FileProvider.getUriForFile(this,
                                "com.cntt2.flashcard.fileprovider", photoFile);
                        takePictureLauncher.launch(photoUri);
                    }
                })
                .show();
    }

    private void handleImageSelection(Uri uri) {
        new Thread(() -> {
            try {
                String fileName = "img_" + System.currentTimeMillis() + ".jpg";
                File destFile = new File(imagesDir, fileName);
                InputStream in = getContentResolver().openInputStream(uri);
                FileOutputStream out = new FileOutputStream(destFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                in.close();
                out.close();
                Uri contentUri = FileProvider.getUriForFile(
                        this,
                        "com.cntt2.flashcard.fileprovider",
                        destFile
                );
                runOnUiThread(() -> {
                    sessionImages.add(destFile.getAbsolutePath());
                    edtCardContent.insertImage(contentUri.toString(), "Gallery Image");
                    String html = edtCardContent.getHtml();
                    updateHistory(html);
                    Log.d("ImageHandling", "Successfully added gallery image: " + contentUri + ", Absolute path: " + destFile.getAbsolutePath());
                });
            } catch (IOException e) {
                Log.e("AddCardActivity", "Error handling image: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateHistory(String html) {
        if (isFrontSide) {
            updateFrontHistory(html);
        } else {
            updateBackHistory(html);
        }
    }

    private void updateFrontHistory(String html) {
        while (frontHtmlHistory.size() > frontHistoryIndex + 1) {
            frontHtmlHistory.remove(frontHtmlHistory.size() - 1);
        }
        frontHtmlHistory.add(html);
        frontHistoryIndex++;
    }

    private void updateBackHistory(String html) {
        while (backHtmlHistory.size() > backHistoryIndex + 1) {
            backHtmlHistory.remove(backHtmlHistory.size() - 1);
        }
        backHtmlHistory.add(html);
        backHistoryIndex++;
    }

    private String convertRelativeToAbsoluteUrls(String html) {
        if (html == null) return null;
        // Replace relative URLs (e.g., /Uploads/img_...) with absolute URLs
        return html.replaceAll("(<img[^>]+src=\")/Uploads/([^\"]+)(\")", "$1" + BASE_URL + "Uploads/$2$3");
    }

    private String convertAbsoluteToRelativeUrls(String html) {
        if (html == null) return null;
        // Replace absolute URLs (e.g., http://10.0.2.2:5029/Uploads/img_...) with relative URLs
        return html.replaceAll("(<img[^>]+src=\")" + BASE_URL.replace(".", "\\.") + "Uploads/([^\"]+)(\")", "$1/Uploads/$2$3");
    }

    private void loadCardData() {
        progressBar.setVisibility(View.VISIBLE);
        cardRepository.getCardsByDeskId(deskId, new Callback<List<CardDto>>() {
            @Override
            public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    CardDto card = response.body().stream()
                            .filter(c -> c.getId().equals(cardId))
                            .findFirst()
                            .orElse(null);
                    if (card != null) {
                        frontText = convertRelativeToAbsoluteUrls(card.getFront());
                        backText = convertRelativeToAbsoluteUrls(card.getBack());
                        updateFrontHistory(frontText);
                        updateBackHistory(backText);
                        edtCardContent.setHtml(frontText);
                        initialImages = ImageManager.extractImagePathsFromHtml(frontText + backText, AddCardActivity.this);
                        Log.d("AddCardActivity", "Loaded card front: " + frontText);
                        Log.d("AddCardActivity", "Loaded card back: " + backText);
                        Log.d("AddCardActivity", "Initial images: " + initialImages);
                    } else {
                        Toast.makeText(AddCardActivity.this, "Card not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(AddCardActivity.this, "Failed to load card", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<List<CardDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddCardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void saveCard() {
        if (isFrontSide) {
            frontText = edtCardContent.getHtml() != null ? edtCardContent.getHtml() : "";
            updateFrontHistory(frontText);
        } else {
            backText = edtCardContent.getHtml() != null ? edtCardContent.getHtml() : "";
            updateBackHistory(backText);
        }

        if (frontText.isEmpty() || backText.isEmpty()) {
            Toast.makeText(this, "Card content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        // Convert absolute URLs to relative URLs before saving
        String frontTextRelative = convertAbsoluteToRelativeUrls(frontText);
        String backTextRelative = convertAbsoluteToRelativeUrls(backText);
        Log.d("AddCardActivity", "Saving card with front (relative): " + frontTextRelative);
        Log.d("AddCardActivity", "Saving card with back (relative): " + backTextRelative);

        CardDto cardDto = new CardDto();
        cardDto.setDeskId(deskId);
        cardDto.setFront(frontTextRelative);
        cardDto.setBack(backTextRelative);
        cardDto.setCreatedAt(new Date());
        cardDto.setLastModified(new Date());

        List<String> localImagePaths = new ArrayList<>(sessionImages);
        Log.d("AddCardActivity", "Saving card with local image paths: " + localImagePaths);

        if (isEditMode && cardId != null) {
            cardDto.setId(cardId);
            cardRepository.updateCard(cardId, cardDto, localImagePaths, new Callback<CardDto>() {
                @Override
                public void onResponse(Call<CardDto> call, Response<CardDto> response) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    if (response.isSuccessful()) {
                        manageImageFiles(frontText + backText);
                        Toast.makeText(AddCardActivity.this, "Card updated successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(AddCardActivity.this, "Failed to update card", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<CardDto> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(AddCardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            cardRepository.insertCard(cardDto, localImagePaths, new Callback<CardDto>() {
                @Override
                public void onResponse(Call<CardDto> call, Response<CardDto> response) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    if (response.isSuccessful()) {
                        manageImageFiles(frontText + backText);
                        Toast.makeText(AddCardActivity.this, "Card created successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(AddCardActivity.this, "Failed to create card", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<CardDto> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(AddCardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void manageImageFiles(String combinedHtml) {
        try {
            Set<String> usedImages = ImageManager.extractImagePathsFromHtml(combinedHtml, this);
            Log.d("ImageCleanup", "Used images: " + usedImages);

            Set<String> allImagesAddedInSession = new HashSet<>(sessionImages);
            Log.d("ImageCleanup", "All images added in session: " + allImagesAddedInSession);

            allImagesAddedInSession.removeAll(initialImages);
            Log.d("ImageCleanup", "Images added in session after removing initial: " + allImagesAddedInSession);

            Set<String> unusedImages = new HashSet<>(allImagesAddedInSession);
            unusedImages.removeAll(usedImages);
            Log.d("ImageCleanup", "Unused images to delete: " + unusedImages);

            ImageManager.deleteImageFiles(unusedImages, this);
            Log.d("ImageCleanup", "Deleted " + unusedImages.size() + " unused images");
        } catch (Exception e) {
            Log.e("ImageCleanup", "Error cleaning up images: " + e.getMessage(), e);
        }
    }
}