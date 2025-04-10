package com.cntt2.flashcard;

import android.app.Application;

import com.cntt2.flashcard.data.local.DatabaseHelper;
import com.cntt2.flashcard.data.remote.ApiClient;
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.repository.CardRepository;
import com.cntt2.flashcard.data.repository.DeskRepository;
import com.cntt2.flashcard.data.repository.FolderRepository;
import com.cntt2.flashcard.data.repository.LearningSessionRepository;
import com.cntt2.flashcard.data.repository.ReviewRepository;

public class App extends Application {
    private static App instance;
    private DatabaseHelper databaseHelper;
    private FolderRepository folderRepository;
    private DeskRepository deskRepository;
    private CardRepository cardRepository;
    private ReviewRepository reviewRepository;
    private LearningSessionRepository learningSessionRepository;
    private ApiService apiService;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        ApiClient.init(this);
        apiService = ApiClient.getApiService();

        reviewRepository = new ReviewRepository(this);
        learningSessionRepository = new LearningSessionRepository(this);

        databaseHelper = new DatabaseHelper(this);
        cardRepository = new CardRepository(this);
        deskRepository = new DeskRepository(this);
        folderRepository = new FolderRepository(this);
    }

    public static App getInstance() {
        return instance;
    }

    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

    public FolderRepository getFolderRepository() {
        return folderRepository;
    }

    public DeskRepository getDeskRepository() {
        return deskRepository;
    }

    public CardRepository getCardRepository() {
        return cardRepository;
    }

    public ReviewRepository getReviewRepository() {
        return reviewRepository;
    }

    public LearningSessionRepository getLearningSessionRepository() {
        return learningSessionRepository;
    }

    public ApiService getApiService() {
        return apiService;
    }
}