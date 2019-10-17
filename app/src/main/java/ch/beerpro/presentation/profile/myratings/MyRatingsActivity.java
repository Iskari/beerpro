package ch.beerpro.presentation.profile.myratings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.beerpro.R;
import ch.beerpro.domain.models.Rating;
import ch.beerpro.domain.models.Wish;
import ch.beerpro.presentation.details.DetailsActivity;

public class MyRatingsActivity extends AppCompatActivity implements OnMyRatingItemInteractionListener {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    private MyRatingsViewModel model;
    private MyRatingsRecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_ratings);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.title_activity_myratings));


        model = ViewModelProviders.of(this).get(MyRatingsViewModel.class);
        model.getMyRatingsWithWishes().observe(this, this::updateMyRatings);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new MyRatingsRecyclerViewAdapter(this, model.getCurrentUser(), this);

        recyclerView.setAdapter(adapter);

    }

    private void updateMyRatings(List<Pair<Rating, Wish>> entries) {
        adapter.submitList(entries);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                supportFinishAfterTransition();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onMoreClickedListener(Rating item) {
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra(DetailsActivity.ITEM_ID, item.getBeerId());
        startActivity(intent);
    }

    @Override
    public void onWishClickedListener(Rating item) {
        model.toggleItemInWishlist(item.getBeerId());
    }
}
