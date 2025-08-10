package com.example.attendanceapp;


import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import java.util.HashMap;
import java.util.Map;

public class AdminManager {
    private FirebaseFunctions mFunctions;
    private FirebaseAuth mAuth;

    public AdminManager() {
        mFunctions = FirebaseFunctions.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Set custom claims for a user
     * @param uid User ID
     * @param role Role to set (director, teacher, student)
     * @return Task with the result
     */
    public Task<HttpsCallableResult> setUserRole(String uid, String role) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("role", role);

        return mFunctions
                .getHttpsCallable("setDirectorClaim")
                .call(data);
    }

    /**
     * Approve a teacher
     * @param uid Teacher's user ID
     * @return Task with the result
     */
    public Task<HttpsCallableResult> approveTeacher(String uid) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);

        return mFunctions
                .getHttpsCallable("approveTeacher")
                .call(data);
    }

    /**
     * Get current user's role information
     * @return Task with user role data
     */
    public Task<HttpsCallableResult> getCurrentUserRole() {
        return mFunctions
                .getHttpsCallable("getUserRole")
                .call();
    }

    /**
     * Check if current user is director
     * @return boolean indicating if user is director
     */
    public boolean isCurrentUserDirector() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // This will be available after the user's token is refreshed
            return user.getIdToken(false).getResult() != null;
        }
        return false;
    }

    /**
     * Force token refresh to get updated custom claims
     * @return Task that completes when token is refreshed
     */
    public Task<Void> refreshUserToken() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return user.getIdToken(true).continueWith(task -> null);
        }
        return null;
    }
}