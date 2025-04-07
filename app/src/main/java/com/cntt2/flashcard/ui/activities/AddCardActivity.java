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
import android.widget.EditText;
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
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

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
    private List<Set<String>> imageHistory; // Danh sách các tập hợp đường dẫn ảnh
    private Set<String> initialImages; // Lưu các ảnh ban đầu
    private Uri photoUri;

    private static final int REQUEST_PERMISSIONS = 100;

    private void checkPermissions() {
        // Danh sách quyền cần xin
        List<String> permissionsNeeded = new ArrayList<>();

        // Kiểm tra quyền dựa trên phiên bản Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ dùng READ_MEDIA_IMAGES thay vì READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Các phiên bản Android cũ hơn
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // Kiểm tra quyền camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        // Xin quyền nếu cần
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

        checkPermissions();

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
        initialImages = new HashSet<>();

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
        try {
            // Tạo tên file ảnh dựa trên timestamp để tránh trùng lặp
            String fileName = "img_" + System.currentTimeMillis() + ".jpg";
            File destFile = new File(getFilesDir(), fileName);

            // Sao chép ảnh từ Uri vào bộ nhớ ứng dụng
            InputStream in = getContentResolver().openInputStream(uri);
            FileOutputStream out = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();

            // Đường dẫn đầy đủ của ảnh mới
            String fullImagePath = destFile.getAbsolutePath();
            Log.d("ImageTracking", "Added new image: " + fullImagePath);

            // Tạo URL cho ảnh và thêm vào editor
            String imagePath = "file://" + fullImagePath;
            edtCardContent.insertImage(imagePath, "Image " + fileName);

            // Lấy HTML hiện tại và cập nhật lịch sử
            String html = edtCardContent.getHtml();
            updateHistory(html);

        } catch (IOException e) {
            Log.e("AddCardActivity", "Error handling image: " + e.getMessage());
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateHistory(String html) {
        // Cắt bỏ lịch sử sau vị trí hiện tại
        while (htmlHistory.size() > historyIndex + 1) {
            htmlHistory.remove(htmlHistory.size() - 1);
            if (imageHistory.size() > historyIndex + 1) {
                imageHistory.remove(imageHistory.size() - 1);
            }
        }

        // Thêm HTML mới vào lịch sử
        htmlHistory.add(html);

        // Cập nhật danh sách ảnh hiện tại đang được sử dụng
        Set<String> currentImages = extractImagePathsFromHtml(html);
        imageHistory.add(currentImages); // Sửa lỗi tại đây

        historyIndex++;
    }

    private Set<String> extractImagePathsFromHtml(String html) {
        Set<String> imagePaths = new HashSet<>();
        if (html == null || html.isEmpty()) {
            return imagePaths;
        }

        // Tìm tất cả các thẻ img và trích xuất thuộc tính src
        Pattern pattern = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>");
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            String srcPath = matcher.group(1);
            // Xử lý đường dẫn file://
            if (srcPath.startsWith("file://")) {
                srcPath = srcPath.substring(7); // Bỏ "file://"
            }

            // Thêm đường dẫn vào tập hợp
            imagePaths.add(srcPath);
            Log.d("ImagePath", "Found image path: " + srcPath);
        }

        return imagePaths;
    }

    private void manageImageFiles(String combinedHtml) {
        try {
            // Lấy tất cả các đường dẫn ảnh hiện đang được sử dụng trong HTML
            Set<String> usedImages = extractImagePathsFromHtml(combinedHtml);
            Log.d("ImageCleanup", "Found " + usedImages.size() + " images in use");

            // Tìm tất cả ảnh đã thêm mới trong phiên làm việc này
            Set<String> allImagesAddedInSession = new HashSet<>();

            // Kết hợp tất cả ảnh từ mọi bước trong lịch sử
            for (Set<String> imagesAtStep : imageHistory) {
                allImagesAddedInSession.addAll(imagesAtStep);
            }

            // Loại bỏ các ảnh đã có từ ban đầu
            allImagesAddedInSession.removeAll(initialImages);

            // Duyệt qua danh sách ảnh đã thêm mới trong phiên
            for (String addedImagePath : allImagesAddedInSession) {
                boolean isUsed = false;

                // Kiểm tra xem ảnh có đang được sử dụng hay không
                for (String usedImagePath : usedImages) {
                    if (addedImagePath.equals(usedImagePath) ||
                            addedImagePath.endsWith(usedImagePath) ||
                            usedImagePath.contains(new File(addedImagePath).getName())) {
                        isUsed = true;
                        break;
                    }
                }

                // Nếu ảnh không còn được sử dụng, xóa nó
                if (!isUsed) {
                    File fileToDelete = new File(addedImagePath);
                    boolean deleted = fileToDelete.delete();
                    Log.d("ImageCleanup", "Deleted unused image: " +
                            fileToDelete.getName() + ", success: " + deleted);
                }
            }

        } catch (Exception e) {
            Log.e("ImageCleanup", "Error cleaning up images: " + e.getMessage(), e);
        }
    }


    private void saveCard() {
        if (isFrontSide) {
            frontText = edtCardContent.getHtml() != null ? edtCardContent.getHtml() : "";
        } else {
            backText = edtCardContent.getHtml() != null ? edtCardContent.getHtml() : "";
        }

        // Kiểm tra nếu cả hai mặt đều trống
        if (frontText.isEmpty() && backText.isEmpty()) {
            Toast.makeText(this, "Card content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        manageImageFiles(frontText + backText);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("newCardFront", frontText);
        resultIntent.putExtra("newCardBack", backText);
        resultIntent.putExtra("newCardCreatedAt", currentDate);
        setResult(RESULT_OK, resultIntent);
        finish();
    }


}