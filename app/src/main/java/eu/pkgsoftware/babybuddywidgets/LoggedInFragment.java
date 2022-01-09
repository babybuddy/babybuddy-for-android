package eu.pkgsoftware.babybuddywidgets;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.databinding.LoggedInFragmentBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding;

public class LoggedInFragment extends Fragment {
    private LoggedInFragmentBinding binding;

    private BabyBuddyClient client = null;
    private CredStore credStore = null;

    private class TimerListViewHolder extends RecyclerView.ViewHolder {
        public QuickTimerEntryBinding binding;
        public BabyBuddyClient.Timer timer = null;

        public TimerListViewHolder(View itemView, QuickTimerEntryBinding binding) {
            super(itemView);
            this.binding = binding;
            binding.appTimerDefaultType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    credStore.setTimerDefaultSelection(timer.id, (int) l);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
        }

        public void assignTimer(BabyBuddyClient.Timer timer) {
            this.timer = timer;
            binding.timerName.setText(timer.readableName());
            binding.appStartTimerButton.setVisibility(timer.active ? View.GONE : View.VISIBLE);
            binding.appStopTimerButton.setVisibility(!timer.active ? View.GONE : View.VISIBLE);
            Integer defaultSelection = credStore.getTimerDefaultSelections().get(timer.id);
            if (defaultSelection == null) {
                defaultSelection = 0;
            }
            binding.appTimerDefaultType.setSelection(defaultSelection);

        }
    }

    private class TimerListProvider extends RecyclerView.Adapter<TimerListViewHolder> {
        private BabyBuddyClient.Timer[] timers;

        public TimerListProvider(BabyBuddyClient.Timer[] timers) {
            this.timers = timers;
        }

        @Override
        public TimerListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            QuickTimerEntryBinding entryBinding = QuickTimerEntryBinding.inflate(LayoutInflater.from(parent.getContext()));
            return new TimerListViewHolder(entryBinding.getRoot(), entryBinding);
        }

        @Override
        public void onBindViewHolder(LoggedInFragment.TimerListViewHolder holder, int position) {
            holder.assignTimer(timers[position]);
        }

        @Override
        public int getItemCount() {
            return timers.length;
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = LoggedInFragmentBinding.inflate(inflater, container, false);

        credStore = new CredStore(getContext());
        client = new BabyBuddyClient(getActivity().getMainLooper(), credStore);

        client.listTimers(new BabyBuddyClient.RequestCallback<BabyBuddyClient.Timer[]>() {
            @Override
            public void error(Exception error) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Login failed")
                        .setMessage("Error occurred while obtaining timers: " + error.getMessage())
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .show();
            }

            @Override
            public void response(BabyBuddyClient.Timer[] response) {
                System.out.println(response.length);
                binding.timersList.setAdapter(new TimerListProvider(response));
            }
        });

        GridLayoutManager l = new GridLayoutManager(binding.timersList.getContext(), 1);
        binding.timersList.setLayoutManager(l);

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
}
