package com.example.attendanceapp.Dashboards;


import android.content.Intent;
import android.os.Bundle;
import java.util.Iterator;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.attendanceapp.Fragments.AdminClassesFragment;
import com.example.attendanceapp.Fragments.AdminReportsFragment;
import com.example.attendanceapp.Fragments.AdminTeachersFragment;
import com.example.attendanceapp.LoginScreen.LoginActivity;
import com.example.attendanceapp.ProfileActivity;
import com.example.attendanceapp.R;
import com.example.attendanceapp.TeacherApprovalActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

public class SuperAdminDashboard extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FirebaseFirestore db;
    private TextView tvTeacherCount, tvClassCount, tvAttendancePercent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_superadmin_dashboard);

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupViewPager();
        loadDashboardStats();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.main_superAdmin_layout);
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Quick stats views
        tvTeacherCount = findViewById(R.id.tvTeacherCount);
        tvClassCount = findViewById(R.id.tvClassCount);
        tvAttendancePercent = findViewById(R.id.tvAttendancePercent);

        db = FirebaseFirestore.getInstance();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void setupNavigationDrawer() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupViewPager() {
        AdminPagerAdapter pagerAdapter = new AdminPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Teachers");
                    break;
                case 1:
                    tab.setText("Classes");
                    break;
                case 2:
                    tab.setText("Reports");
                    break;
            }
        }).attach();

        // Add page change callback if needed
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // Handle page changes if needed
            }
        });
    }

    private void loadDashboardStats() {
        // Load teacher count
        db.collection("teachers")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvTeacherCount.setText(String.valueOf(count));
                });

        // Load class count
        db.collection("classes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvClassCount.setText(String.valueOf(count));
                });

        // Load today's attendance percentage
        String today = java.text.DateFormat.getDateInstance().format(new Date());
        db.collection("attendance")
                .document(today)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        double present = documentSnapshot.getDouble("present");
                        double total = documentSnapshot.getDouble("total");
                        if (total > 0) {
                            int percent = (int) ((present / total) * 100);
                            tvAttendancePercent.setText(percent + "%");
                        }
                    }
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Refresh dashboard
            recreate();
        } else if (id == R.id.nav_teachers) {
            // Implement user management
            Toast.makeText(this, "User management functionality", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, TeacherApprovalActivity.class);
            startActivity(i);
        } else if (id == R.id.nav_settings) {
            // Implement system settings
            Toast.makeText(this, "System settings functionality", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_logout) {
            logoutUser();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // ViewPager Adapter
    private static class AdminPagerAdapter extends FragmentStateAdapter {
        public AdminPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1:
                    return new AdminClassesFragment();
                case 2:
                    return new AdminReportsFragment();
                default:
                    return new AdminTeachersFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
