package hu.szte.nyanusz.mobilalk.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import hu.szte.nyanusz.mobilalk.R;
import hu.szte.nyanusz.mobilalk.activity.HomeActivity;
import hu.szte.nyanusz.mobilalk.model.Music;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> implements Filterable {

    private final HomeActivity context;
    private List<Music> musicList;
    private List<Music> musicListFull;

    public MusicAdapter(HomeActivity context, ArrayList<Music> musicList) {
        this.context = context;
        this.musicList = musicList;
        this.musicListFull = new ArrayList<>(musicList);
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_item, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        Music music = musicList.get(position);
        holder.titleText.setText(music.getCim());
        holder.artistText.setText(music.getEloado());
        holder.durationText.setText(formatDuration(music.getDuration()));

        String albumArtUri = music.getAlbumArtUri();
        if (albumArtUri != null && !albumArtUri.isEmpty()) {
            Glide.with(context)
                    .load(albumArtUri)
                    .error(R.drawable.baseline_album_64)
                    .into(holder.albumArtImage);
        } else {
            holder.albumArtImage.setImageResource(R.drawable.baseline_album_64);
        }

        holder.itemView.setOnClickListener(v -> context.showMusicInfo(music));
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    private String formatDuration(int duration) {
        // A duration másodpercben van, átalakítjuk perc:másodperc formátummá
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    public Filter getFilter() {
        return musicFilter;
    }

    private final Filter musicFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Music> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(musicListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Music item : musicListFull) {
                    if (item.getCim().toLowerCase().contains(filterPattern) ||
                            item.getEloado().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            musicList.clear();
            musicList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    static class MusicViewHolder extends RecyclerView.ViewHolder {
        ImageView albumArtImage;
        TextView titleText, artistText, durationText;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            albumArtImage = itemView.findViewById(R.id.albumArtImage);
            titleText = itemView.findViewById(R.id.songTitle);
            artistText = itemView.findViewById(R.id.songArtist);
            durationText = itemView.findViewById(R.id.songDuration);
        }
    }
}