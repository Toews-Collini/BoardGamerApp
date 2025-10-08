package com.example.boardgamer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

public class VotingAdapter extends RecyclerView.Adapter<VotingAdapter.VH> {

    private final Map<String, Integer> items;
    private final List<String> keys = new ArrayList<>();

    public VotingAdapter() {
        this(new LinkedHashMap<>());
    }

    public VotingAdapter(@NonNull Map<String, Integer> initial) {
        this.items = new LinkedHashMap<>(initial);
        this.keys.addAll(this.items.keySet());
        setHasStableIds(true);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tv;
        final ImageButton btn;
        VH(@NonNull View itemView) {
            super(itemView);
            tv  = itemView.findViewById(R.id.tvVotingTitle);
            btn = itemView.findViewById(R.id.btnVoting);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voting_recycler, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String key = keys.get(position);
        h.tv.setText(key);

        int state = normalize(items.get(key));
        applyState(h.btn, state);

        h.btn.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            String k = keys.get(pos);
            int cur  = items.getOrDefault(k, 0);
            int next = (cur % 3) + 1;
            items.put(k, next);

            applyState(h.btn, next);
            notifyItemChanged(pos);
        });
    }

    private static int normalize(Integer x) { return x == null ? 0 : x; }

    @Override public int getItemCount() { return keys.size(); }

    @Override public long getItemId(int position) { return keys.get(position).hashCode(); }

    public void submitAll(@NonNull Map<String, Integer> data) {
        items.clear();
        items.putAll(data);
        keys.clear();
        keys.addAll(items.keySet());
        notifyDataSetChanged();
    }

    public int addItem(@NonNull String title) {
        if (!items.containsKey(title)) {
            items.put(title, 0);
            keys.add(title);
            int pos = keys.size() - 1;
            notifyItemInserted(pos);
            return pos;
        } else {
            int pos = keys.indexOf(title);
            if (pos >= 0) notifyItemChanged(pos);
            return Math.max(pos, 0);
        }
    }

    public Map<String, Integer> snapshot() {
        return new LinkedHashMap<>(items);
    }

    private void applyState(@NonNull ImageButton btn, int state) {
        btn.setRotation(0f);
        btn.setAlpha(1f);
        androidx.core.widget.ImageViewCompat.setImageTintList(btn, null); // Tint aus

        switch (state) {
            case 1:
                btn.setImageResource(R.drawable.thumbs_up);
                break;
            case 2:
                btn.setImageResource(R.drawable.thumbs_down);
                break;
            case 3:
                btn.setImageResource(R.drawable.thumbs_up);
                btn.setRotation(90f);
                break;
            default:
                btn.setImageResource(R.drawable.thumbs_up);
                btn.setAlpha(0.35f);
                break;
        }
    }
}
