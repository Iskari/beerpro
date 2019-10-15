package ch.beerpro.presentation.details.createrating;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Collections;
import java.util.Date;

import ch.beerpro.domain.models.Beer;
import ch.beerpro.domain.models.BeerPlace;
import ch.beerpro.domain.models.Rating;
import ch.beerpro.presentation.utils.EntityClassSnapshotParser;

public class CreateRatingViewModel extends ViewModel {

    private static final String TAG = "CreateRatingViewModel";

    private EntityClassSnapshotParser<Rating> parser = new EntityClassSnapshotParser<>(Rating.class);
    private Beer item;
    private Uri photo;
    private BeerPlace beerPlace;

    public Beer getItem() {
        return item;
    }

    public void setItem(Beer item) {
        this.item = item;
    }

    public Uri getPhoto() {
        return photo;
    }

    public void setPhoto(Uri photo) {
        this.photo = photo;
    }

    public void setBeerPlace(BeerPlace bP){
        beerPlace = bP;
    }

    public BeerPlace getBeerPlace() {
        return beerPlace;
    }

    public Task<Rating> saveRating(Beer item, float rating, String comment, Uri localPhotoUri, BeerPlace localBeerPlace) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;

        return uploadFileToFirebaseStorage(localPhotoUri).continueWithTask(task -> {
            String photoUrl = null;

            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                photoUrl = downloadUri.toString();
            } else if (task.isCanceled()) {
                // the user did not take a photo, that's ok!
            } else {
                throw task.getException();
            }

            Rating newRating = new Rating(null, item.getId(), item.getName(), user.getUid(), user.getDisplayName(),
                    user.getPhotoUrl().toString(), photoUrl, beerPlace, rating, comment, Collections.emptyMap(), new Date());
            Log.i(TAG, "Adding new rating: " + newRating.toString());
            return FirebaseFirestore.getInstance().collection("ratings").add(newRating);

        }).continueWithTask(task -> {
            if (task.isSuccessful()) {
                return task.getResult().get();
            } else {
                throw task.getException();
            }
        }).continueWithTask(task -> {

            if (task.isSuccessful()) {
                return Tasks.forResult(parser.parseSnapshot(task.getResult()));
            } else {
                throw task.getException();
            }
        });
    }

    private Task<Uri> uploadFileToFirebaseStorage(Uri localPhotoUri) {

        if (localPhotoUri == null) {
            return Tasks.forCanceled();
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        StorageReference imageRef = storageRef.child("images/" + localPhotoUri.getLastPathSegment());
        Log.i(TAG, "Uploading image " + localPhotoUri);

        return imageRef.putFile(localPhotoUri).addOnFailureListener(exception -> {
            Log.e(TAG, "Uploading image failed", exception);
        }).addOnSuccessListener(taskSnapshot -> {
            Log.i(TAG, "Uploading image successful: " + taskSnapshot.getMetadata().getName());
        }).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return imageRef.getDownloadUrl();
        });
    }
}