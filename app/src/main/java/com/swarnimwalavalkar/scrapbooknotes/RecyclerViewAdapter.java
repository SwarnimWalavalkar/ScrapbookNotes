package com.swarnimwalavalkar.scrapbooknotes;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import net.dankito.richtexteditor.android.RichTextEditor;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder>{
    private static final String TAG = "RecyclerViewAdapter";

    private ArrayList<Note> notes;
    private Context MainActivity;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView noteTitle;
        RichTextEditor noteContent;
        public MyViewHolder(View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.noteTitle);
            noteContent = itemView.findViewById(R.id.noteContent);
        }
    }
    // Provide a suitable constructor (depends on the kind of dataset)
    public RecyclerViewAdapter(Context context, ArrayList<Note> notes) {
        this.notes = notes;
        this.MainActivity = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_item, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }
    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.noteTitle.setText(notes.get(position).getTitle());
        holder.noteContent.setEnabled(false);
        holder.noteContent.getJavaScriptExecutor().insertHtml(notes.get(position).getDescription());

        holder.noteTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity, EditNoteActivity.class);
                intent.putExtra("docid", notes.get(position).getDocumentId());
                MainActivity.startActivity(intent);
            }
        });

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return notes.size();
    }

}
