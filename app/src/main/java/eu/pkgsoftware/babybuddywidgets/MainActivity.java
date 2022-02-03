package eu.pkgsoftware.babybuddywidgets;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import eu.pkgsoftware.babybuddywidgets.databinding.ActivityMainBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    private CredStore credStore = null;
    private BabyBuddyClient client = null;

    public BabyBuddyClient.Child[] children = new BabyBuddyClient.Child[0];
    public BabyBuddyClient.Timer selectedTimer = null;

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