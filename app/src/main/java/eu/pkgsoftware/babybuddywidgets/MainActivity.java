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

    public BabyBuddyClient.Timer selectedTimer = null;

    public BabyBuddyClient getClient() {
        CredStore credStore = new CredStore(getApplicationContext());
        BabyBuddyClient client = new BabyBuddyClient(getMainLooper(), credStore);
        return client;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        binding.toolbar.setNavigationOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                }
            }
        );

        binding.toolbar.setNavigationIcon(null);
    }
}