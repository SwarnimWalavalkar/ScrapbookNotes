package com.swarnimwalavalkar.scrapbooknotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import net.dankito.richtexteditor.android.RichTextEditor;
import net.dankito.richtexteditor.android.toolbar.AllCommandsEditorToolbar;
import net.dankito.richtexteditor.callback.GetCurrentHtmlCallback;
import net.dankito.richtexteditor.command.CommandName;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EditNoteActivity extends AppCompatActivity implements EditNoteMetadataDialog.DialogListener {
    private static final String TAG = "EditNoteActivity";

    private RichTextEditor editor;
    private EditText titleEditor;

    private AllCommandsEditorToolbar editorToolbar;

    //Note metadata global vars
    private String collection;
    private List<String> labels = new ArrayList<>();

    //A global variable for the document id of the initiated note
    String docid;

    //Cloud Firestore Database Instance
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    //A reference object to the "Notebook" Collection (for simplicity)
    private CollectionReference notesRef;

    //Firebase Auth Object and Auth State Listener
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onStart() {
        super.onStart();
        //Add the Auth State Listener to the Firebase Auth Object
        mAuth.addAuthStateListener(mAuthStateListener);

    }

    //Inflate the options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_note_menu, menu);

        return true;
    }

    //Add an onClick listener to each menu item
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //If the user selects the 'Save Note' Option, call the 'getNoteInfo' Method
            case R.id.save_note:
                getNoteInfo();
                break;
            //If the user selects the 'Edit Note Metadata' Option, show the editing dialog
            case R.id.edit_metadata:
                showDialog();
                break;
            //if the user selects the 'Delete' Option, call the 'deleteNote' Method
            case R.id.delete:
                deleteNote();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        //Initialize the toolbar
        Toolbar toolbar = findViewById(R.id.addNoteToolbar);
        setSupportActionBar(toolbar);

        //Get the document id of the initiated document through an Intent
        Intent intent = getIntent();
        docid = intent.getStringExtra("docid");

        //If the current logged-in user is equal to null, redirect to the login activity
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {
                    startActivity(new Intent(EditNoteActivity.this, LoginActivity.class));
                }
            }
        };

        //Get the uid of the currently logged in user and initialize the notesRef Collection Reference
        String uid = mAuth.getCurrentUser().getUid();
        notesRef = db.collection("users/" + uid + "/notes");

        /*------------------------------------------------HTML Toolbar Setup------------------------------------------------------------*/
        editor = findViewById(R.id.editor);
        titleEditor = findViewById(R.id.titleEditor);

        editorToolbar = findViewById(R.id.editorToolbar);
        editorToolbar.setEditor(editor);

        editor.setEditorFontSize(20);
        editor.setPadding((int) (4 * getResources().getDisplayMetrics().density));


        editorToolbar.removeCommand(CommandName.EXPANDING_SEARCH_VIEWING);
        editorToolbar.removeCommand(CommandName.INSERTIMAGE);
        editorToolbar.removeSearchView();
        /*------------------------------------------------------------------------------------------------------------------------------*/

        //Get the initiated document
        db.document("users/" + uid + "/notes/" + docid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot doc = task.getResult();
                if (doc.exists()) {
                    Note note = doc.toObject(Note.class);
                    collection = note.getCollection();
                    labels = note.getLabels();

                    titleEditor.setText(note.getTitle());
                    editor.getJavaScriptExecutor().insertHtml(note.getDescription());
                } else {
                    Toast.makeText(EditNoteActivity.this, "Note Doesn't exist", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EditNoteActivity.this, "Cannot retrieve document: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (editorToolbar.handlesBackButtonPress() == false) {
            super.onBackPressed();
        }
    }

    private void showDialog() {

        //Send Note's metadata to the 'EditNoteMetadata' Fragment through a Bundle
        Bundle bundle = new Bundle();

        String[] labelArray = new String[labels.size()];
        labelArray = labels.toArray(labelArray);
        bundle.putStringArray("labels", labelArray);
        bundle.putString("collection", collection);

        //Create and show the dialog
        EditNoteMetadataDialog dialog = new EditNoteMetadataDialog();
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "Edit Note Metadata dialog");
    }

    //Get the note's title, description and pass it on to a function that saves it to firestore
    void getNoteInfo() {

        final String[] title = new String[1];
        final String[] description = new String[1];

        editor.getCurrentHtmlAsync(new GetCurrentHtmlCallback() {
            @Override
            public void htmlRetrieved(@NotNull String descriptionHtml) {

                //Save the description html in the description variable
                description[0] = descriptionHtml;
                //save the title text in the title variable
                title[0] = titleEditor.getText().toString();

                //Pass the title and description to the save method
                save(title[0], description[0]);
            }
        });

    }

    private void save(String title, String description) {
        //Create a new note object from the information in the parameters and the global var (for the metadata)
        Note note = new Note(title, description, collection, labels);

        //Create a Reference to the document with the same docid
        DocumentReference noteRef = notesRef.document(docid);

        //Update that document with the current document
        noteRef.set(note).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(EditNoteActivity.this, "Note Saved Successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(EditNoteActivity.this, MainActivity.class));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EditNoteActivity.this, "Something Went Wrong! " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Get the current note and delete it from firestore
    private void deleteNote() {
        //Create a conformation dialog
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Delete Confirmation")
                .setMessage("Are you sure you want to delete this note")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    //If the user selects 'Yes'
                    public void onClick(DialogInterface dialog, int which) {
                        //Delete the note with the docid of the current note
                        notesRef.document(docid).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //Notify the user and redirect to MainActivity
                                Toast.makeText(EditNoteActivity.this, "Note Successfully Deleted!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(EditNoteActivity.this, MainActivity.class));
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(EditNoteActivity.this, "There was a problem deleting the note", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    //If the user selects 'No' Do nothing and close the dialog
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    //Override the getMetadata Method to the get the updated metadata from the editNoteMetadata Fragment
    @Override
    public void getMetadata(String collection, String tags) {
        this.collection = collection;

        //Split the tagInput String after every ',' (comma) and put it into an array
        String[] tagArray = tags.split("\\s*,\\s*");

        //Create a List out of the "tagArray" Array
        this.labels = Arrays.asList(tagArray);
    }
}
