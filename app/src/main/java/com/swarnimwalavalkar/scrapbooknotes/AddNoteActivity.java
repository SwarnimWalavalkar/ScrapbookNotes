package com.swarnimwalavalkar.scrapbooknotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import net.dankito.richtexteditor.android.RichTextEditor;
import net.dankito.richtexteditor.android.toolbar.AllCommandsEditorToolbar;
import net.dankito.richtexteditor.callback.GetCurrentHtmlCallback;
import net.dankito.richtexteditor.command.CommandName;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddNoteActivity extends AppCompatActivity implements EditNoteMetadataDialog.DialogListener {
    private static final String TAG = "AddNoteActivity";

    //Local Variables
    private RichTextEditor editor;
    private EditText titleEditor;
    private AllCommandsEditorToolbar editorToolbar;

    //Note metadata global variables
    private String collection;
    private List<String> labels = new ArrayList<>();

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
        getMenuInflater().inflate(R.menu.add_note_menu, menu);

        return true;
    }

    //Add an onClick listener to each menu item
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //If the user selects the save notes menu action, call the getNotesInfo method
            case R.id.save_note:
                getNoteInfo();
                break;
            //If the user selects the edit metadata menu action, call the showMetadataDialog method
            case R.id.edit_metadata:
                showMetadataDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        //Initialize the setup the toolbar
        Toolbar toolbar = findViewById(R.id.addNoteToolbar);
        setSupportActionBar(toolbar);

        //Initialize the variables for the collection and the labels List
        collection = "Default";
        labels.add("");

        //If the current logged-in user is equal to null, redirect to the login activity
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser() == null){
                    startActivity(new Intent(AddNoteActivity.this, LoginActivity.class));
                }
            }
        };

        //Get the uid of the currently logged in user and point the 'notesRef' Collection Reference to the 'notes' sub-collection
        String uid = mAuth.getCurrentUser().getUid();
        notesRef = db.collection("users/"+uid+"/notes");

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
    }

    //Notify the editor toolbar of any backPressed Event
    @Override
    public void onBackPressed() {
        if (editorToolbar.handlesBackButtonPress() == false) {
            super.onBackPressed();
        }
    }

    //Show a dialog box showing the current notes metadata so the user and edit/view it.
    private void showMetadataDialog() {
        EditNoteMetadataDialog dialog = new EditNoteMetadataDialog();
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

        //Push that note object to firestore
        notesRef.add(note).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Toast.makeText(AddNoteActivity.this, "Note Successfully Added", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(AddNoteActivity.this, MainActivity.class));
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddNoteActivity.this, "Something Went Wrong!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onFailure: " + e.toString());
                    }
                });
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
