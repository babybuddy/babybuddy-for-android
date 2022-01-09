package eu.pkgsoftware.babybuddywidgets;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

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

    public CredStore getCredStore() {
        return credStore;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        credStore = new CredStore(getApplicationContext());
        if (credStore.getAppToken() != null) {
            BabyBuddyClient client = new BabyBuddyClient(getMainLooper(), credStore);
            client.listChildren(new BabyBuddyClient.RequestCallback<BabyBuddyClient.Child[]>() {
                @Override
                public void error(Exception error) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Login failed")
                            .setMessage("[Login failed?]")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .show();
                }

                @Override
                public void response(BabyBuddyClient.Child[] response) {
                    for (BabyBuddyClient.Child c : response) {
                        System.out.println("Child: " + c.slug);
                    }
                    navController.navigate(R.id.action_LoginFragment_to_loggedInFragment2);
                }
            });
        }
    }

}