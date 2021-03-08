package com.swarnimwalavalkar.scrapbooknotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity{
    private static final String TAG = "MainActivity";

    //Number of columns for the notes display grid
    private static final int NUM_COLUMNS = 2;

    //Notes ArrayList to pass on to the recyclerView adapter
    ArrayList<Note> notesArrayList = new ArrayList<>();

    //Cloud Firestore Database Instance
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    //A reference object to the "Notes" Sub-Collection (for simplicity)
    private CollectionReference notesRef;

    //Firebase Auth Object and Auth State Listener
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    //Local Variables
    RecyclerView recyclerView;
    RecyclerViewAdapter adapter;
    FloatingActionButton fab;

    @Override
    protected void onStart() {
        super.onStart();
        //Add the Auth State Listener to the Firebase Auth Object
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    //Inflate the options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    //Add an onClick listener to each menu item
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //If the user clicks on the refresh notes item, call the update notes method.
            case R.id.refresh_notes:
                updateNotes();
                break;
            //If the user clicks on the add collection item, Initiate the 'Add new Collection Dialog fragment'
            case R.id.add_collection:
                AddNewCollectionDialog dialog = new AddNewCollectionDialog();
                dialog.show(getSupportFragmentManager(), "Edit Note Metadata dialog");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize the toolbar and set it up
        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("All Notes");

        //Setup the Firestore Database
        setupFirestore();
        //Load notes from Firestore
        loadNotes();
        //Setup the recyclerView adapter and pass on the notes
        setupRecyclerView();

        //Setup the Floating action button and its onClick Listener
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddNoteActivity.class));
            }
        });

    }

    private void setupFirestore() {
        //If the current logged-in user is equal to null, redirect to the login activity
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
            }
        };


        //Create a new document in the 'users' collection for the current logged in user

        final String uid = mAuth.getCurrentUser().getUid();

        //Check if a document with the id of the currently logged in user's UID exists..
        //Only create it if it doesn't already exists
        DocumentReference userRef = db.document("users/" + uid);
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    //Check if the document with the same uid exists
                    if (!document.exists()) {
                        //If it doesn't create a new Map containing the currently logged in user info
                        Map<String, Object> user = new HashMap<>();
                        user.put("username", mAuth.getCurrentUser().getDisplayName());
                        user.put("collections", Arrays.asList("Default"));

                        //Push that map to the database, as a document for the user
                        db.collection("users").document(uid).set(user, SetOptions.merge());
                    }
                }
            }
        });

        //Initialize the notesRef variable to point to the 'notes' sub-collection in the individual user document
        notesRef = db.collection("users/" + uid + "/notes");
    }

    private void setupRecyclerView() {
        //Initialize the recyclerView
        recyclerView = findViewById(R.id.recyclerView);
        //Set a layout manager to display the notes in a grid
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(NUM_COLUMNS, LinearLayoutManager.VERTICAL));
    }

    public void loadNotes() {
        //Query the "notes" Collection (notesRef)
        notesRef.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        //Loop through all document snapshot objects retrieved
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {

                            //Convert the "QueryDocumentSnapshot" object to a "Note" object
                            Note note = documentSnapshot.toObject(Note.class);

                            //And extract the appropriate documentID from the"QueryDocumentSnapshot" object to set as a property of the Note object
                            note.setDocumentId(documentSnapshot.getId());

                            //Push the current note in the notesArrayList
                            notesArrayList.add(note);
                        }
                        //Initialize the adapter object and set it to the RecyclerView
                        adapter = new RecyclerViewAdapter(MainActivity.this, notesArrayList);
                        recyclerView.setAdapter(adapter);

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Database could not be contacted (Most likely a network error)", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "loadNotes onFailure: " + e.toString());
            }
        });

    }

    //Refreshes the notes when the user taps on the corresponding menu action
    public void updateNotes() {
        //Clear the notes arrraylist and repopulate it with new data and notify the adapter.
        notesArrayList.clear();
        notesRef.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            //Convert the "QueryDocumentSnapshot" object to a "Note" object
                            //And extract the appropriate documentID from the"QueryDocumentSnapshot" object to set as a property of the Note object
                            Note note = documentSnapshot.toObject(Note.class);
                            note.setDocumentId(documentSnapshot.getId());

                                notesArrayList.add(note);
                                adapter.notifyDataSetChanged();

                        }
                    }
                });
    }

}
