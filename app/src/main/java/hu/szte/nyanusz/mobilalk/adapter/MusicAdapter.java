package hu.szte.nyanusz.mobilalk.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.utilities.CorePalette;

import java.lang.reflect.Array;
import java.util.ArrayList;

import hu.szte.nyanusz.mobilalk.R;
import hu.szte.nyanusz.mobilalk.activity.HomeActivity;
import hu.szte.nyanusz.mobilalk.model.Music;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> implements Filterable {

    private ArrayList<Music> musicList;
    private ArrayList<Music> filteredMusicList;
    private Context context;
    private int lastPosition = -1;

    public MusicAdapter(Context context, ArrayList<Music> musicList) {
        this.musicList = musicList;
        this.filteredMusicList = musicList;
        this.context = context;
    }

    @NonNull
    @Override
    public MusicAdapter.MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MusicViewHolder(LayoutInflater.from(context).inflate(R.layout.music_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        Music currentItem = filteredMusicList.get(position);

        holder.bindTo(currentItem);

        if (holder.getAdapterPosition() > lastPosition) {
            holder.itemView.setAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in));
            lastPosition = holder.getAdapterPosition();
        }
    }

    @Override
    public int getItemCount() {
        return filteredMusicList.size();
    }

    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                ArrayList<Music> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filterResults.count = musicList.size();
                    filterResults.values = musicList;
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (Music music : musicList) {
                        if (music.getCim().toLowerCase().contains(filterPattern) || music.getEloado().toLowerCase().contains(filterPattern)) {
                            filteredList.add(music);
                        }
                    }

                    filterResults.count = filteredList.size();
                    filterResults.values = filteredList;
                }

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredMusicList = (ArrayList<Music>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public static class MusicViewHolder extends RecyclerView.ViewHolder {

        private final ImageView albumArt;
        private final TextView songTitle;
        private final TextView songArtist;
        private final TextView songDuration;
        private final View songCard;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);

            albumArt = itemView.findViewById(R.id.albumArtImage);
            songTitle = itemView.findViewById(R.id.songTitle);
            songArtist = itemView.findViewById(R.id.songArtist);
            songDuration = itemView.findViewById(R.id.songDuration);
            songCard = itemView.findViewById(R.id.songCard);

        }

        @SuppressLint("DefaultLocale")
        void bindTo(@NonNull Music music) {
            songTitle.setText(music.getCim());
            songArtist.setText(music.getEloado());
            songDuration.setText(String.format("%02d:%02d", music.getHossz() / 60, music.getHossz() % 60));
            System.out.println(songDuration);
            albumArt.setImageResource(R.drawable.baseline_album_64);
            songCard.setBackgroundColor(0x000000);
            itemView.setOnClickListener(view -> {
                ((HomeActivity) view.getContext()).showMusicInfo(music);
            });
            /*
            if (music.getAlbumArtUri() != null) {
                Glide.with(itemView.getContext())
                        .asBitmap()
                        .load(music.getAlbumArtUri())
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                albumArt.setImageBitmap(resource);
                                Palette.from(resource).generate(palette -> {
                                    int defaultValue = 0x000000;
                                    int mutedColor = palette != null ? palette.getMutedColor(defaultValue) : defaultValue;
                                    songCard.setBackgroundColor(mutedColor);
                                });
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                albumArt.setImageDrawable(placeholder);
                            }
                        });
            } else {
                albumArt.setImageResource(R.drawable.baseline_album_64);
            }
            itemView.setOnClickListener(view -> {
                //((HomeActivity) view.getContext()).showMusicInfo(music);
            });

            */
        }
    }
}
