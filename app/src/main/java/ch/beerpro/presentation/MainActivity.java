package ch.beerpro.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.firebase.ui.auth.AuthUI;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.beerpro.R;
import ch.beerpro.presentation.beerMap.FragmentBeerMap;
import ch.beerpro.presentation.explore.BeerCategoriesFragment;
import ch.beerpro.presentation.explore.BeerManufacturersFragment;
import ch.beerpro.presentation.explore.ExploreFragment;
import ch.beerpro.presentation.profile.ProfileFragment;
import ch.beerpro.presentation.ratings.RatingsFragment;
import ch.beerpro.presentation.splash.SplashScreenActivity;
import ch.beerpro.presentation.utils.ViewPagerAdapter;

/**
 * The {@link MainActivity} is the entry point for logged-in users (actually, users start at the
 * {@link SplashScreenActivity}, but if they are still logged in they will get redirected to this activity.
 * <p>
 * The Activity has three tabs, each of which implemented by a fragment and held together by a {@link ViewPager}.
 */
public class MainActivity extends AppCompatActivity
        implements BeerCategoriesFragment.OnItemSelectedListener, BeerManufacturersFragment.OnItemSelectedListener {

    /**
     * We use ButterKnife's view injection instead of having to call findViewById repeatedly.
     */
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.viewpager)
    ViewPager viewPager;
    @BindView(R.id.tablayout)
    TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        /*
         * The following ceremony is need to have the app logo set as the home button.
         * */
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setLogo(R.drawable.beer_glass_icon);

        setupViewPager(viewPager, tabLayout);

        /*
         * Just a placeholder for your own ideas...
         * */
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());
    }

    private void setupViewPager(ViewPager viewPager, TabLayout tabLayout) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new ExploreFragment());
        adapter.addFragment(new RatingsFragment());
        adapter.addFragment(new ProfileFragment());
        adapter.addFragment(new FragmentBeerMap());
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_search_black_24dp);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_people_black_24dp);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_person_black_24dp);
        tabLayout.getTabAt(3).setIcon(android.R.drawable.ic_dialog_map);

        /*
         * We want to change the title of the activity depending on the selected fragment. We can do this by
         * listening to the tabLayout's changes and setting the title accordingly:
         * */
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                super.onTabSelected(tab);
                switch (tab.getPosition()) {
                    case 0:
                        getSupportActionBar().setTitle("Entdecken");
                        break;
                    case 1:
                        getSupportActionBar().setTitle("Bewertungen");
                        break;
                    case 2:
                        getSupportActionBar().setTitle("Mein Profil");
                        break;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_home_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void logout() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener(task -> {
            Intent intent = new Intent(MainActivity.this, SplashScreenActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBeerCategorySelected(String name) {
        // TODO implement
    }

    @Override
    public void onBeerManufacturerSelected(String name) {
        // TODO implement
    }
}
