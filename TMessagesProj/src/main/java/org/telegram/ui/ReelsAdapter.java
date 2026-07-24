package org.telegram.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import android.view.TextureView;
import com.google.android.exoplayer2.video.VideoSize;
import com.google.android.exoplayer2.Player;
import android.graphics.Matrix;
import org.telegram.messenger.R;

import java.util.List;

public class ReelsAdapter extends RecyclerView.Adapter<ReelsAdapter.ReelViewHolder> {

    private Context context;
    private List<ReelModel> reelList;
    private java.util.List<ReelViewHolder> activePlayers = new java.util.ArrayList<>();

    public ReelsAdapter(Context context, List<ReelModel> reelList) {
        this.context = context;
        this.reelList = reelList;
    }

    @NonNull
    @Override
    public ReelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reel, parent, false);
        return new ReelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReelViewHolder holder, int position) {
        ReelModel reel = reelList.get(position);
        holder.setReelData(reel);
        if (!activePlayers.contains(holder)) {
            activePlayers.add(holder);
        }
    }

    @Override
    public int getItemCount() {
        return reelList.size();
    }

    @Override
    public void onViewRecycled(@NonNull ReelViewHolder holder) {
        super.onViewRecycled(holder);
        activePlayers.remove(holder);
        if (holder.exoPlayer != null) {
            holder.exoPlayer.release();
            holder.exoPlayer = null;
        }
    }

    public void playVideoAt(int position) {
        for (ReelViewHolder holder : activePlayers) {
            if (holder.exoPlayer != null) {
                if (holder.getAdapterPosition() == position) {
                    holder.exoPlayer.setPlayWhenReady(true);
                } else {
                    holder.exoPlayer.setPlayWhenReady(false);
                }
            }
        }
    }

    public void pauseAll() {
        for (ReelViewHolder holder : activePlayers) {
            if (holder.exoPlayer != null) {
                holder.exoPlayer.setPlayWhenReady(false);
            }
        }
    }

    class ReelViewHolder extends RecyclerView.ViewHolder {
        TextureView playerView;
        ExoPlayer exoPlayer;
        TextView tvAuthor, tvCaption, tvLikeCount;
        ImageView btnShare;
        android.widget.ProgressBar loadingProgress;

        public ReelViewHolder(@NonNull View itemView) {
            super(itemView);
            playerView = itemView.findViewById(R.id.playerView);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvCaption = itemView.findViewById(R.id.tvCaption);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            btnShare = itemView.findViewById(R.id.btnShare);
            loadingProgress = itemView.findViewById(R.id.loadingProgress);
            
            btnShare.setOnClickListener(v -> {
                String link = "https://reels.yukiapi.site/reel/" + tvAuthor.getText().toString();
                android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Check out this reel on Bhaigram!\n" + link);
                shareIntent.setPackage(context.getPackageName());
                context.startActivity(shareIntent);
            });
            
            tvCaption.setOnClickListener(v -> {
                if (tvCaption.getMaxLines() == 2) {
                    tvCaption.setMaxLines(100);
                } else {
                    tvCaption.setMaxLines(2);
                }
            });
        }

        void setReelData(ReelModel reel) {
            tvAuthor.setText("@" + reel.author);
            tvCaption.setText(reel.caption);
            tvLikeCount.setText(formatCount(reel.likeCount));
            loadingProgress.setVisibility(View.VISIBLE);

            if (exoPlayer != null) {
                exoPlayer.release();
            }

            exoPlayer = new ExoPlayer.Builder(context).build();
            exoPlayer.setVideoTextureView(playerView);
            
            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onVideoSizeChanged(VideoSize videoSize) {
                    if (videoSize.width == 0 || videoSize.height == 0) return;
                    
                    float videoProportion = (float) videoSize.width / (float) videoSize.height;
                    int viewWidth = playerView.getWidth();
                    int viewHeight = playerView.getHeight();
                    if (viewWidth == 0 || viewHeight == 0) return;
                    
                    float viewProportion = (float) viewWidth / (float) viewHeight;
                    Matrix matrix = new Matrix();
                    
                    if (videoProportion > viewProportion) {
                        float scale = videoProportion / viewProportion;
                        matrix.postScale(scale, 1f, viewWidth / 2f, viewHeight / 2f);
                    } else {
                        float scale = viewProportion / videoProportion;
                        matrix.postScale(1f, scale, viewWidth / 2f, viewHeight / 2f);
                    }
                    playerView.setTransform(matrix);
                }

                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_READY) {
                        loadingProgress.setVisibility(View.GONE);
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        loadingProgress.setVisibility(View.VISIBLE);
                    }
                }
            });

            MediaItem mediaItem = MediaItem.fromUri(reel.videoUrl);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.setPlayWhenReady(false); // Don't auto-play, wait for playVideoAt
            exoPlayer.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
        }
    }

    private String formatCount(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1000000) return String.format(java.util.Locale.US, "%.1fk", count / 1000.0f);
        return String.format(java.util.Locale.US, "%.1fm", count / 1000000.0f);
    }
}
