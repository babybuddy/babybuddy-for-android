package eu.pkgsoftware.babybuddywidgets;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import eu.pkgsoftware.babybuddywidgets.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private CredStore credStore = null;
    private BabyBuddyClient client = null;

    public BabyBuddyClient.Timer selectedTimer = null;
    public BabyBuddyClient.Child[] children = new BabyBuddyClient.Child[0];

    public CredStore getCredStore() {
        if (credStore == null) {
            credStore = new CredStore(getApplicationContext());
        }
        return credStore;
    }

    public BabyBuddyClient getClient() {
        if (client == null) {
            client = new BabyBuddyClient(getMainLooper(), getCredStore());
        }
        return client;
    }

    public void setTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        binding.toolbar.setNavigationOnClickListener(view -> {});
        binding.toolbar.setNavigationIcon(null);
    }
}