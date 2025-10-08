package com.example.boardgamer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.VH> {
    private final List<String> items;

    public SuggestionAdapter(List<String> items) {
        this.items = items;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        VH(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvSuggestion);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggestion, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.tv.setText(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public int addSuggestion(String s) {
        int pos = items.size();
        items.add(s);
        notifyItemInserted(pos);
        return pos;
    }
}
