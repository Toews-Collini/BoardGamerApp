package com.example.boardgamer;

import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {

    static class Message {
        String text;
        boolean isOwn;

        Message(String text, boolean isOwn) {
            this.text = text;
            this.isOwn = isOwn;
        }
    }

    public final List<Message> messages = new ArrayList<>();

    public MessageAdapter(@NonNull List<Message> initial) {
        messages.addAll(initial);
    }

    static class VH extends RecyclerView.ViewHolder {
        CardView cvOthers;
        CardView cvOwn;
        TextView tvOthers;
        TextView tvOwn;

        VH(@NonNull View itemView) {
            super(itemView);
            cvOthers = itemView.findViewById(R.id.cardOther);
            cvOwn = itemView.findViewById(R.id.cardOwn);
            tvOthers = itemView.findViewById(R.id.messageTextOther);
            tvOwn = itemView.findViewById(R.id.messageTextOwn);

            tvOthers.setMovementMethod(new ScrollingMovementMethod());;
            tvOthers.setVerticalScrollBarEnabled(true);
            tvOwn.setMovementMethod(new ScrollingMovementMethod());;
            tvOwn.setVerticalScrollBarEnabled(true);

            tvOthers.setOnTouchListener((v, event) -> {
                boolean canScroll = v.canScrollVertically(-1) || v.canScrollVertically(1);
                if (canScroll) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    if (event.getAction() == MotionEvent.ACTION_UP
                            || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
                return false; // TextView soll das Scrollen selbst handeln
            });

            tvOwn.setOnTouchListener((v, event) -> {
                boolean canScroll = v.canScrollVertically(-1) || v.canScrollVertically(1);
                if (canScroll) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    if (event.getAction() == MotionEvent.ACTION_UP
                            || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
                return false; // TextView soll das Scrollen selbst handeln
            });
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_messages_recycler, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Message msg = messages.get(position);

        // Sichtbarkeit steuern
        if (msg.isOwn) {
            h.cvOwn.setVisibility(View.VISIBLE);
            h.cvOthers.setVisibility(View.GONE);
            h.tvOwn.setText(msg.text);
            h.tvOwn.scrollTo(0, 0);
        } else {
            h.cvOthers.setVisibility(View.VISIBLE);
            h.cvOwn.setVisibility(View.GONE);
            h.tvOthers.setText(msg.text);
            h.tvOthers.scrollTo(0, 0);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public int addItem(boolean isOwn, @NonNull String text) {
        Message msg = new Message(text, isOwn);
        messages.add(msg);
        int pos = messages.size() - 1;
        notifyItemInserted(pos);
        return pos;
    }
}
