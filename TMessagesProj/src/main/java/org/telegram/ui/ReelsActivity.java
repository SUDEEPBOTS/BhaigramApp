package org.telegram.ui;

import android.os.Bundle;

import android.app.Activity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.PagerSnapHelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.R;

import java.util.ArrayList;
import java.util.List;

public class ReelsActivity extends Activity {

    private RecyclerView recyclerViewReels;
    private android.widget.ProgressBar progressBar;
    private ReelsAdapter reelsAdapter;
    private List<ReelModel> reelList = new ArrayList<>();
    private String telegramId = "123456";
    private LinearLayoutManager layoutManager;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_reels);

            recyclerViewReels = findViewById(R.id.recyclerViewReels);
            progressBar = findViewById(R.id.progressBar);
            
            android.view.View btnBack = findViewById(R.id.btnBack);
            btnBack.setOnClickListener(v -> finish());
            
            layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            recyclerViewReels.setLayoutManager(layoutManager);
            
            PagerSnapHelper snapHelper = new PagerSnapHelper();
            snapHelper.attachToRecyclerView(recyclerViewReels);

            reelsAdapter = new ReelsAdapter(this, reelList);
            recyclerViewReels.setAdapter(reelsAdapter);
            
            // To simulate setOffscreenPageLimit(3) you could set initial prefetch, but we'll keep it simple
            recyclerViewReels.setItemViewCacheSize(3);

            fetchReelsFromBackend();

            recyclerViewReels.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        int position = layoutManager.findFirstCompletelyVisibleItemPosition();
                        if (position >= 0) {
                            reelsAdapter.playVideoAt(position);
                            if (position >= reelList.size() - 5) {
                                fetchReelsFromBackend();
                            }
                        }
                    }
                }
            });

            android.view.View btnNavChats = findViewById(R.id.btnNavChats);
            if (btnNavChats != null) {
                btnNavChats.setOnClickListener(v -> finish());
            }
        } catch (Throwable e) {
            e.printStackTrace();
            android.widget.Toast.makeText(this, "Reels Error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void fetchReelsFromBackend() {
        if (isLoading) return;
        isLoading = true;
        String urlString = "https://reels.yukiapi.site/feed/" + telegramId + "?limit=10";
        
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray reels = jsonResponse.getJSONArray("reels");
                
                runOnUiThread(() -> {
                    try {
                        for (int i = 0; i < reels.length(); i++) {
                            JSONObject obj = reels.getJSONObject(i);
                            reelList.add(new ReelModel(
                                obj.getString("id"),
                                obj.getString("author"),
                                obj.getString("caption"),
                                obj.getString("video_url"),
                                obj.getInt("like_count"),
                                obj.getInt("comment_count")
                            ));
                        }
                        progressBar.setVisibility(android.view.View.GONE);
                        reelsAdapter.notifyDataSetChanged();
                        isLoading = false;
                        
                        // Play the first video after a slight delay to allow layout
                        recyclerViewReels.postDelayed(() -> {
                            if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                                reelsAdapter.playVideoAt(0);
                            }
                        }, 500);
                    } catch (Exception e) {
                        e.printStackTrace();
                        isLoading = false;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                isLoading = false;
            }
        }).start();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        reelsAdapter.pauseAll();
    }
}
