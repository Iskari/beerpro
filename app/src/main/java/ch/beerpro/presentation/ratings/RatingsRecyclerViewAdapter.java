package ch.beerpro.presentation.ratings;

import android.content.Intent;
import android.net.Uri;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseUser;

import java.text.DateFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.beerpro.GlideApp;
import ch.beerpro.R;
import ch.beerpro.domain.models.Rating;
import ch.beerpro.domain.models.Wish;
import ch.beerpro.presentation.utils.EntityPairDiffItemCallback;

import static ch.beerpro.presentation.utils.DrawableHelpers.setDrawableTint;


public class RatingsRecyclerViewAdapter extends ListAdapter<Pair<Rating, Wish>, RatingsRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RatingsRecyclerViewAdap";

    private static final DiffUtil.ItemCallback<Pair<Rating, Wish>> DIFF_CALLBACK = new EntityPairDiffItemCallback<>();

    private final OnRatingsItemInteractionListener listener;
    private Fragment fragment;
    private final FirebaseUser user;

    public RatingsRecyclerViewAdapter(OnRatingsItemInteractionListener listener, Fragment fragment, FirebaseUser user) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.fragment = fragment;
        this.user = user;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.fragment_ratings_listentry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        Pair<Rating, Wish> item = getItem(position);
        holder.bind(item.first, item.second, listener);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.comment)
        TextView comment;

        @BindView(R.id.beerName)
        TextView beerName;

        @BindView(R.id.avatar)
        ImageView avatar;

        @BindView(R.id.ratingBar)
        RatingBar ratingBar;

        @BindView(R.id.authorName)
        TextView authorName;

        @BindView(R.id.date)
        TextView date;

        @BindView(R.id.numLikes)
        TextView numLikes;

        @BindView(R.id.details)
        Button details;

        @BindView(R.id.wishlist)
        Button wishlist;

        @BindView(R.id.like)
        Button like;

        @BindView(R.id.photo)
        ImageView photo;

        @BindView(R.id.beerPlace)
        TextView beerPlaceText;

        @BindView(R.id.placeIcon)
        ImageView placeIcon;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, itemView);
        }

        void bind(Rating item, Wish wish, OnRatingsItemInteractionListener listener) {
            // TODO This code is almost the same in MyBeersRecyclerViewAdapter.. could be simplified
            // with databinding!
            beerName.setText(item.getBeerName());
            comment.setText(item.getComment());

            if(item.getBeerPlace() != null) {
                String placeText = item.getBeerPlace().getName() + ", " + item.getBeerPlace().getAddress();
                beerPlaceText.setText(placeText);
                Uri gmmIntentUri;
                if(item.getBeerPlace().getId() != null){
                    gmmIntentUri = Uri.parse("geo:" + item.getBeerPlace().getLatitude() + "," + item.getBeerPlace().getLongitude() + "?q=" + item.getBeerPlace().getName() + ", " + item.getBeerPlace().getAddress());
                }else{
                    //Should put a label but does not currently because of https://issuetracker.google.com/issues/129726279
                    gmmIntentUri = Uri.parse("geo:0,0?q=" + item.getBeerPlace().getLatitude() + "," + item.getBeerPlace().getLongitude() + "(" + item.getBeerPlace().getName() + item.getBeerPlace().getAddress() + ")");
                }

                beerPlaceText.setOnClickListener((v) -> {
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    fragment.startActivity(mapIntent);
                });
            }else{
                beerPlaceText.setText("");
                placeIcon.setVisibility(View.INVISIBLE);
            }

            ratingBar.setNumStars(5);
            ratingBar.setRating(item.getRating());
            String formattedDate =
                    DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(item.getCreationDate());
            date.setText(formattedDate);

            if (item.getPhoto() != null) {
                // Take a look at https://bumptech.github.io/glide/int/recyclerview.html
                GlideApp.with(fragment).load(item.getPhoto()).into(photo);
                photo.setVisibility(View.VISIBLE);
            } else {
                GlideApp.with(fragment).clear(photo);
                photo.setVisibility(View.GONE);
            }

            authorName.setText(item.getUserName());
            GlideApp.with(fragment).load(item.getUserPhoto()).apply(new RequestOptions().circleCrop()).into(avatar);

            numLikes.setText(itemView.getResources().getString(R.string.fmt_num_ratings, item.getLikes().size()));

            if (item.getLikes().containsKey(user.getUid())) {
                int color = fragment.getResources().getColor(R.color.colorPrimary);
                setDrawableTint(like, color);
            } else {
                int color = fragment.getResources().getColor(android.R.color.darker_gray);
                setDrawableTint(like, color);
            }

            if (wish != null) {
                int color = fragment.getResources().getColor(R.color.colorPrimary);
                setDrawableTint(wishlist, color);
            } else {
                int color = fragment.getResources().getColor(android.R.color.darker_gray);
                setDrawableTint(wishlist, color);
            }

            if (listener != null) {
                like.setOnClickListener(v -> listener.onRatingLikedListener(item));
                details.setOnClickListener(v -> listener.onMoreClickedListener(item));
                wishlist.setOnClickListener(v -> listener.onWishClickedListener(item));
            }
        }
    }
}
