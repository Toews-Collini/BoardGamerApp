package com.example.boardgamer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FoodChoiceAdapter extends RecyclerView.Adapter<FoodChoiceAdapter.VH>{
    public class Item {
        String guestName;
        String guestChoice;

        public Item(String name, String choice) {
            guestName = name;
            guestChoice = choice;
        }
    }
    private final List<Item> items;

    public FoodChoiceAdapter(List<Item> items) {
        this.items = items;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvGuestName;
        TextView tvGuestChoice;
        VH(View itemView) {
            super(itemView);
            tvGuestName = itemView.findViewById(R.id.guestName);
            tvGuestChoice = itemView.findViewById(R.id.guestChoice);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_foodchoice_recyler, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.tvGuestName.setText(items.get(position).guestName);
        holder.tvGuestChoice.setText(items.get(position).guestChoice);
    }

    @Override
    public int getItemCount() { return items.size(); }

    public int addFoodChoice(String name, String choice) {
        Item item = new Item(name, choice);
        int pos = items.size();
        items.add(item);
        notifyItemInserted(pos);
        return pos;
    }
}
