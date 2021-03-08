package com.swarnimwalavalkar.scrapbooknotes;
import com.google.firebase.firestore.Exclude;
import java.util.List;

public class Note {
    public String documentId;
    private String title;
    private String description;
    private String collection;

    List<String> labels;

    public Note (){
        //public no-arg constructor needed..
    }

    public Note(String title, String description, String collection, List<String> labels) {
        this.title = title;
        this.description = description;
        this.collection = collection;
        this.labels = labels;
    }

    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCollection() {
        return collection;
    }

    public List<String> getLabels() {
        return labels;
    }


}
