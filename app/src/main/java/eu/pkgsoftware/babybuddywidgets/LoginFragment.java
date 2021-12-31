package eu.pkgsoftware.babybuddywidgets;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import eu.pkgsoftware.babybuddywidgets.databinding.LoginFragmentBinding;

public class LoginFragment extends Fragment {

    private LoginFragmentBinding binding;

    private Button loginButton;
    private EditText addressEdit, loginNameEdit, loginPasswordEdit;

    private void updateLoginButton() {
        loginButton.setEnabled(
                (addressEdit.getText().length() > 0) &&
                        (loginNameEdit.getText().length() > 0) &&
                        (loginPasswordEdit.getText().length() > 0));
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = LoginFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        loginButton = view.findViewById(R.id.loginButton);
        addressEdit = view.findViewById(R.id.serverAddressEdit);
        loginNameEdit = view.findViewById(R.id.loginNameEdit);
        loginPasswordEdit = view.findViewById(R.id.passwordEdit);

        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                updateLoginButton();
            }
        };

        addressEdit.addTextChangedListener(tw);
        loginNameEdit.addTextChangedListener(tw);
        loginPasswordEdit.addTextChangedListener(tw);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performLogin();
            }
        });

        updateLoginButton();

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void performLogin() {
        System.out.println("Logging in");
        NavController controller = Navigation.findNavController(getView());
        controller.navigate(R.id.action_LoginFragment_to_loggedInFragment2);
    }
}