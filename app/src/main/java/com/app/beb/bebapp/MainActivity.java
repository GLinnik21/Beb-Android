package com.app.beb.bebapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import static androidx.navigation.ui.NavigationUI.setupWithNavController;

public class MainActivity extends AppCompatActivity implements NavController.OnNavigatedListener {

    private Toolbar _mainToolbar;
    private NavController _navController;
    private BottomNavigationView _bottomNavigationView;
    private Menu _menu;
    private Boolean _hideEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _bottomNavigationView = findViewById(R.id.navigation);

        _navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        _navController.addOnNavigatedListener(this);
        setupWithNavController(_bottomNavigationView, _navController);

        _mainToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(_mainToolbar);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.appbar, menu);
        _menu = menu;
        return true;
    }

    @SuppressLint("ResourceType")
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int edit_id = R.id.action_edit_profile;

        if (menu.findItem(edit_id) != null && _hideEdit) {
            menu.removeItem(edit_id);
        } else if (menu.findItem(edit_id) == null && !_hideEdit) {
            menu.add(edit_id);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return _navController.navigateUp();
    }

    @Override
    public void onNavigated(@NonNull NavController controller, @NonNull NavDestination destination) {
        switch (destination.getId()) {
            case R.id.profile:
                _hideEdit = false;
                break;
            default:
                _hideEdit = true;
                break;
        }
        invalidateOptionsMenu();
    }
}
