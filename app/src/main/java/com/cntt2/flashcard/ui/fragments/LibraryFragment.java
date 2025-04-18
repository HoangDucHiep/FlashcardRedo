package com.cntt2.flashcard.ui.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.ApiClient;
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.remote.dto.PublicDeskDto;
import com.cntt2.flashcard.ui.adapters.PublicDeskAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText edtSearch;
    private PublicDeskAdapter adapter;
    private List<PublicDeskDto> publicDesks = new ArrayList<>();
    private List<PublicDeskDto> filteredDesks = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView);
        edtSearch = view.findViewById(R.id.edtSearch);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PublicDeskAdapter(filteredDesks, desk -> {
            // Navigate to CardsFragment to display the cards of the selected desk
            PublicCardFragment cardsFragment = PublicCardFragment.newInstance(desk.getId(), true);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, cardsFragment)
                    .addToBackStack(null)
                    .commit();
        });
        recyclerView.setAdapter(adapter);

        // Fetch public desks
        fetchPublicDesks();

        // Set up search functionality
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDesks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void fetchPublicDesks() {
        ApiService apiService = ApiClient.getApiService();
        Call<List<PublicDeskDto>> call = apiService.getPublicDesks();
        call.enqueue(new Callback<List<PublicDeskDto>>() {
            @Override
            public void onResponse(Call<List<PublicDeskDto>> call, Response<List<PublicDeskDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    publicDesks.clear();
                    publicDesks.addAll(response.body());
                    filteredDesks.clear();
                    filteredDesks.addAll(publicDesks);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(requireContext(), "Failed to load public desks", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<PublicDeskDto>> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("LibraryFragment", "Failed to fetch public desks", t);
            }
        });
    }

    private void filterDesks(String query) {
        filteredDesks.clear();
        if (query.isEmpty()) {
            filteredDesks.addAll(publicDesks);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (PublicDeskDto desk : publicDesks) {
                if (desk.getName().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                    filteredDesks.add(desk);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }


}