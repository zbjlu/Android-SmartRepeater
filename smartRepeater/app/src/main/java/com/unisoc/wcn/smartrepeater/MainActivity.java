package com.unisoc.wcn.smartrepeater;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.unisoc.wcn.smartrepeater.service.MainService;
import com.unisoc.wcn.smartrepeater.ui.fragment.ConnectionFragment;
import com.unisoc.wcn.smartrepeater.ui.fragment.WiFiManagerFragment;

import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static MainActivity sInstance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        sInstance = this;
        switchToConnectionFragment();
        checkPermission();
        MainService.startMainService();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
//            super.onBackPressed();
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
        }
    }

    public static MainActivity getInstance() {
        return sInstance;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
        if (id == R.id.action_version) {
            showAppVersion();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_connection) {
            // Handle the camera action
            switchToConnectionFragment();
        } else if (id == R.id.nav_wifi_manager) {
            switchToWiFiManagerFragment();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void checkPermission() {
        ArrayList<String> permissionList = new ArrayList<String>();
        permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);

        ArrayList<String> list = new ArrayList<>(permissionList);
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String permission = it.next();
            int hasPermission = ContextCompat.checkSelfPermission(this, permission);
            if (hasPermission == PackageManager.PERMISSION_GRANTED) {
                it.remove();
            }
        }
        if (list.size() == 0) {
            return;
        }
        String[] permissions = list.toArray(new String[0]);
        ActivityCompat.requestPermissions(this, permissions, 1);
    }

    private void showAppVersion() {
        String Inform = "unknow";
        Inform = getAppVersion();

        String showSting = "Version: " + Inform + "\n" + "Build Time: " + BuildConfig.BUILD_TIME;

        new AlertDialog.Builder(this)
                .setTitle("App Version")
                .setMessage(showSting)
                .setPositiveButton("YES", null)
                .show();
    }

    private String getAppVersion() {
        Log.d(TAG, "getAppVersion");
        String Inform = "unknow";
        PackageManager manager = getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
            Inform = info.versionName;
            Log.d(TAG, "getAppVersion version --> " + Inform);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch blockd
            e.printStackTrace();
        }
        return Inform;
    }

    private void switchToConnectionFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.content_main, new ConnectionFragment()).commit();
    }

    private void switchToWiFiManagerFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.content_main, new WiFiManagerFragment()).commit();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        MainService.stopMainService();
        finish();
    }
}
