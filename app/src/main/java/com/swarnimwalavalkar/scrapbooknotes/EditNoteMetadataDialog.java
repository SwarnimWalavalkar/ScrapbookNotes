package com.swarnimwalavalkar.scrapbooknotes;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class EditNoteMetadataDialog extends AppCompatDialogFragment {
    private static final String TAG = "EditNoteMetadataDialog";

    //Local Variables
    private EditText tagsEditText;
    private Spinner collectionSelector;

    //The Collections List
    private ArrayList<String> collectionsList;

    //The Array Adapter
    ArrayAdapter<String> adapter;

    //Cloud Firestore Database Instance
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    //A reference to the user document
    private DocumentReference userRef;

    //Firebase Auth Object and Auth State Listener
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    //Listener to report changes to the parent Activity
    private DialogListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.edit_note_metadata_dialog, null);

        builder.setView(view)
                .setTitle("Edit note Metadata")
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String collection = collectionSelector.getSelectedItem().toString();
                        String tags = tagsEditText.getText().toString();

                        listener.getMetadata(collection, tags);
                    }
                });

        tagsEditText = view.findViewById(R.id.tagsEditText);
        collectionSelector = view.findViewById(R.id.collectionSelector);

        String uid = mAuth.getCurrentUser().getUid();

        userRef = db.document("users/" + uid);

        //Getting the Collections array and adding it to the CollectionSelector Spinner
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    //Get The Document Snapshot from the task Object
                    DocumentSnapshot document = task.getResult();
                    collectionsList = (ArrayList<String>) document.get("collections");

                    /*Array Adapter Code*/
                    adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, collectionsList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    collectionSelector.setAdapter(adapter);

                    final String collection;
                    try {
                        collection = getArguments().getString("collection");
                        if(collection != null){
                            collectionSelector.setSelection(collectionsList.indexOf(collection));
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "User Doc get onComplete: Something Went wrong " + task.getException().getLocalizedMessage());
                }
            }
        });

        String[] labelArray;
        try {
            labelArray = getArguments().getStringArray("labels");

            if(labelArray != null) {
                StringBuilder sb = new StringBuilder();

                for (String s : labelArray) {
                    sb.append(s).append(",");
                }

                String tagText = sb.deleteCharAt(sb.length() - 1).toString();

                tagsEditText.setText(tagText);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (DialogListener) context;
        } catch (Exception e) {
            throw new ClassCastException(context.toString() + " must implement Dialog Listener");
        }
    }

    public interface DialogListener {
        void getMetadata(String collection, String tags);
    }
}
