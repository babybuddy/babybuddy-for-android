package eu.pkgsoftware.babybuddywidgets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import eu.pkgsoftware.babybuddywidgets.compat.BabyBuddyV2TimerAdapter;
import eu.pkgsoftware.babybuddywidgets.databinding.FeedingFragmentBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.NotesEditorBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.timers.utils.AmountValuesGenerator;
import eu.pkgsoftware.babybuddywidgets.widgets.HorizontalNumberPicker;

public class FeedingFragment extends BaseFragment {
    public interface ButtonListCallback {
        void onSelectionChanged(int i);
    }

    private FeedingFragmentBinding binding = null;
    private Double amount = -1.0;
    private NotesEditorBinding notesEditor = null;
    private AmountValuesGenerator amountValuesGenerator = new AmountValuesGenerator();
    private BabyBuddyClient.Timer selectedTimer = null;
    private BabyBuddyClient.Timer virtTimer = null;
    private boolean restoredPreviousState = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FeedingFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.submitButton.setOnClickListener(view1 -> submitFeeding());
        binding.feedingTypeSpinner.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    setupFeedingMethodButtons(Constants.FeedingTypeEnumValues.get((int) l));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            }
        );

        binding.amountNumberPicker.setValueGenerator(amountValuesGenerator);
        binding.amountNumberPicker.setValueUpdatedListener(
            new HorizontalNumberPicker.ValueUpdatedListener() {
                @Override
                public void valueChangeChanging(long valueIndex, float relativeOffset) {
                    amount = amountValuesGenerator.getRawValue(valueIndex, relativeOffset);
                    updateAmount();
                }

                @Override
                public void valueChangeFinished(long valueIndex, float relativeOffset) {
                    valueChangeChanging(valueIndex, relativeOffset);
                }
            }
        );

        notesEditor = NotesEditorBinding.inflate(mainActivity().getLayoutInflater());
        binding.notes.addView(notesEditor.getRoot());

        String notes = "";
        final Bundle args = getArguments();
        if (args != null) {
            notes = args.getString("notes", "");
        }
        notesEditor.noteEditor.setText(notes);

        resetVisibilityState();

        return view;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        restoredPreviousState = savedInstanceState != null;
        if (restoredPreviousState) {
            final String timerIdString = savedInstanceState.getString("timerId", null);
            if (timerIdString != null) {
                try {
                    selectedTimer = BabyBuddyClient.Timer.fromJSON(new JSONObject(timerIdString));
                    virtTimer = timerControl().timerToVirtualTimer(
                        selectedTimer
                    );
                } catch (ParseException | JSONException e) {
                    selectedTimer = null;
                    virtTimer = null;
                    System.err.println("Could not decode selected timer data");
                }
            }
            amount = savedInstanceState.getDouble("amount", -1.0);
            final String notes = savedInstanceState.getString("notes");
            notesEditor.noteEditor.setText(notes);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!restoredPreviousState) {
            Double lastUsedAmount = mainActivity().getCredStore().getLastUsedAmount();
            if (lastUsedAmount != null) {
                amount = lastUsedAmount;
            }
        }

        if (amount == null) {
            amount = -1.0;
        }
        final double newAmountValue = amount;
        binding.amountNumberPicker.setValueIndex(amountValuesGenerator.getValueIndex(newAmountValue));
        binding.amountNumberPicker.setRelativeValueIndexOffset(amountValuesGenerator.getValueOffset(newAmountValue));

        resetVisibilityState();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!restoredPreviousState) {
            if (mainActivity().selectedTimer != null) {
                selectedTimer = mainActivity().selectedTimer;
                virtTimer = timerControl().timerToVirtualTimer(
                    selectedTimer
                );
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (selectedTimer != null) {
            CredStore.Notes notes = timerControl().getNotes(virtTimer);
            notes.note = notesEditor.noteEditor.getText().toString();
            timerControl().setNotes(virtTimer, notes);
            mainActivity().getCredStore().storePrefs();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (selectedTimer == null) {
            outState.putString("timerId", null);
        } else {
            outState.putString("timerId", selectedTimer.toJSON().toString());
        }
        if (amount == null) {
            outState.putDouble("amount", -1.0);
        } else {
            outState.putDouble("amount", amount);
        }
        outState.putString("notes", notesEditor.noteEditor.getText().toString());
    }

    private void resetVisibilityState() {
        populateButtonList(
            getResources().getTextArray(R.array.feedingTypes),
            binding.feedingTypeButtons,
            binding.feedingTypeSpinner,
            i -> setupFeedingMethodButtons(Constants.FeedingTypeEnumValues.get(i))
        );
        binding.feedingMethodSpinner.setVisibility(View.GONE);
        binding.feedingMethodButtons.setVisibility(View.GONE);
        updateAmount();
    }

    private void updateAmount() {
        String text = "(None)";
        if (amount != null) {
            text = AmountValuesGenerator.FORMAT_VALUE.format(amount);
        }
        binding.amountText.setText("Amount: " + text);
    }

    private static class ButtonListOnClickListener implements View.OnClickListener {
        private int i;
        private ButtonListCallback cb;

        public ButtonListOnClickListener(ButtonListCallback cb, int i) {
            this.i = i;
            this.cb = cb;
        }

        public void onClick(View view) {
            cb.onSelectionChanged(i);
        }
    }

    private void populateButtonList(CharSequence[] textArray, LinearLayout buttons, Spinner spinner, ButtonListCallback callback) {
        spinner.setVisibility(View.GONE);
        buttons.setVisibility(View.VISIBLE);

        buttons.removeAllViewsInLayout();
        for (int i = 0; i < textArray.length; i++) {
            Button button = new Button(getContext());
            button.setOnClickListener(
                new ButtonListOnClickListener(
                    i0 -> {
                        spinner.setSelection(i0);
                        spinner.setVisibility(View.VISIBLE);
                        buttons.setVisibility(View.GONE);
                        callback.onSelectionChanged(i0);
                    }, i)
            );
            button.setText(textArray[i]);
            button.setLayoutParams(
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1));
            buttons.addView(button);
        }
    }

    private List<Constants.FeedingMethodEnum> assignedMethodButtons = null;

    private void setupFeedingMethodButtons(Constants.FeedingTypeEnum type) {
        binding.submitButton.setVisibility(View.GONE);
        assignedMethodButtons = new ArrayList<>(10);

        switch (type) {
            case BREAST_MILK:
                assignedMethodButtons.add(Constants.FeedingMethodEnum.LEFT_BREAST);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.RIGHT_BREAST);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.BOTH_BREASTS);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.BOTTLE);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.PARENT_FED);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.SELF_FED);
                break;
            default:
                assignedMethodButtons.add(Constants.FeedingMethodEnum.BOTTLE);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.PARENT_FED);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.SELF_FED);
        }

        CharSequence[] orgItems = getResources().getTextArray(R.array.feedingMethods);
        List<CharSequence> textItems = new ArrayList<>(10);
        for (int i = 0; i < assignedMethodButtons.size(); i++) {
            textItems.add(orgItems[assignedMethodButtons.get(i).value]);
        }

        binding.feedingMethodSpinner.setAdapter(
            new ArrayAdapter<CharSequence>(getContext(), android.R.layout.simple_spinner_dropdown_item, textItems)
        );

        populateButtonList(
            textItems.toArray(
                new CharSequence[0]),
            binding.feedingMethodButtons,
            binding.feedingMethodSpinner,
            i -> binding.submitButton.setVisibility(View.VISIBLE)
        );
    }

    private MainActivity mainActivity() {
        return (MainActivity) getActivity();
    }

    private BabyBuddyV2TimerAdapter timerControl() {
        return mainActivity().getChildTimerControl(selectedTimer.child_id);
    }

    private void submitFeeding() {
        long feedingTypeId = binding.feedingTypeSpinner.getSelectedItemId();
        Constants.FeedingTypeEnum feedingType = Constants.FeedingTypeEnumValues.get((int) feedingTypeId);
        long feedingMethodId = binding.feedingMethodSpinner.getSelectedItemId();
        Constants.FeedingMethodEnum feedingMethod = assignedMethodButtons.get((int) feedingMethodId);

        Float fAmount = amount == null ? null : (float) (amount * 1.0d);
        if (fAmount != null) {
            fAmount = Math.round(fAmount * 10.0f) / 10.0f;
        }
        final Float finalFloatAmount = fAmount;

        mainActivity().storeActivity(selectedTimer, new StoreFunction<Boolean>() {
            @Override
            public void error(@NonNull Exception error) {
                showError(
                    true,
                    "Failed storing feeding",
                    "Error: " + error.getMessage(),
                    b -> navUp()
                );
            }

            @Override
            public void response(Boolean response) {
                MainActivity ma = mainActivity();
                ma.getCredStore().storeLastUsedAmount(amount);
                notesEditor.noteEditor.setText("");

                timerControl().setNotes(virtTimer, CredStore.EMPTY_NOTES);
                navUp();
            }

            @Override
            public void cancel() {
            }

            @Override
            public void stopTimer(@NotNull BabyBuddyClient.Timer timer) {
                navUp();
            }

            @NonNull
            @Override
            public String name() {
                return BabyBuddyClient.ACTIVITIES.FEEDING;
            }

            @Override
            public void store(
                @NonNull BabyBuddyClient.Timer timer,
                @NonNull BabyBuddyClient.RequestCallback<Boolean> callback
            ) {
                mainActivity().getClient().createFeedingRecordFromTimer(
                    selectedTimer,
                    feedingType.post_name,
                    feedingMethod.post_name,
                    finalFloatAmount,
                    notesEditor.noteEditor.getText().toString().trim(),
                    callback
                );
            }
        });
    }

    private void navUp() {
        NavController nav = Navigation.findNavController(requireView());
        nav.navigateUp();
    }
}
