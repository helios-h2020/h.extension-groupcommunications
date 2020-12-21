package eu.h2020.helios_social.modules.groupcommunications.preferences;

import android.content.SharedPreferences;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

public class SharedPreferencesHelper {

    private SharedPreferences sharedPreferences;

    @Inject
    public SharedPreferencesHelper(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void putString(@NotNull String key, @NotNull String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    public String getString(@NotNull String key) {
        return sharedPreferences.getString(key, null);
    }

    public void putBoolean(@NotNull String key, @NotNull boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(@NotNull String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    public void putInt(@NotNull String key, @NotNull boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public int getInt(@NotNull String key) {
        return sharedPreferences.getInt(key, -1);
    }

    public void putFloat(@NotNull String key, @NotNull float value) {
        sharedPreferences.edit().putFloat(key, value).apply();
    }

    public float getFloat(@NotNull String key) {
        return sharedPreferences.getFloat(key, -1);
    }

    public void putLong(@NotNull String key, @NotNull long value) {
        sharedPreferences.edit().putLong(key, value).apply();
    }

    public long getLong(@NotNull String key) {
        return sharedPreferences.getLong(key, -1);
    }

    public void putStringSet(@NotNull String key, @NotNull Set<String> values) {
        sharedPreferences.edit().putStringSet(key, values).apply();
    }

    public Set<String> getStringSet(@NotNull String key) {
        return sharedPreferences.getStringSet(key, new HashSet<String>());
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
