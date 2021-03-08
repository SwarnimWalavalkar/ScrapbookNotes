package com.swarnimwalavalkar.scrapbooknotes;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class AddNewCollectionDialog extends AppCompatDialogFragment {
    private static final String TAG = "AddNewCollectionDialog";

    private EditText collectionEditText;

    //Cloud Firestore Database Instance
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    //A reference to the user document
    private DocumentReference userRef;

    //Firebase Auth Object and Auth State Listener
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private Context mContext;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.add_new_collection_dialog, null);

        builder.setView(view)
                .setTitle("Add New Collection")
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addCollection(collectionEditText.getText().toString());
                    }
                });

        collectionEditText = view.findViewById(R.id.collectionNameEditText);

        String uid = mAuth.getCurrentUser().getUid();

        userRef = db.document("users/" + uid);

        return builder.create();
    }

    private void addCollection(final String collectionName){
        //Check if the editText is empty, Decline The Operation and Notify the user
        if(collectionName != ""){
            //Check if a collection with the same name exists... If true, do not allow operation and notify user
            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        //Get The Document Snapshot from the task Object
                        DocumentSnapshot document = task.getResult();
                        ArrayList<String> collectionsList = (ArrayList<String>) document.get("collections");

                        if(collectionsList.contains(collectionName)){
                            Toast.makeText(mContext, "Collection Already Exists", Toast.LENGTH_SHORT).show();
                        }else{
                            //Add The Collection to the "collections" array in the user document
                            userRef.update("collections", FieldValue.arrayUnion(collectionName));

                            Toast.makeText(mContext, "Collection Added Successfully", Toast.LENGTH_SHORT).show();
                        }

                    }else{
                        Log.d(TAG, "User Doc get onComplete: Something Went wrong " + task.getException().getLocalizedMessage());
                    }
                }
            });
        }else{
            Toast.makeText(mContext, "Name cannot be empty", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }
}
