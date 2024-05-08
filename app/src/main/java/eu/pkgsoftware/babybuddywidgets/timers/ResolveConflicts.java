package eu.pkgsoftware.babybuddywidgets.timers;

import com.squareup.phrase.Phrase;

import org.jetbrains.annotations.NotNull;

import eu.pkgsoftware.babybuddywidgets.BaseFragment;
import eu.pkgsoftware.babybuddywidgets.R;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.RequestCodeFailure;
import eu.pkgsoftware.babybuddywidgets.utils.Promise;

public abstract class ResolveConflicts {
    private final @NotNull BaseFragment baseFragment;
    private final @NotNull BabyBuddyClient.Timer timer;
    private final @NotNull TimerControlInterface timerControl;

    public ResolveConflicts(
        @NotNull BaseFragment baseFragment,
        @NotNull BabyBuddyClient.Timer timer,
        @NotNull TimerControlInterface timerControl
    ) {
        this.baseFragment = baseFragment;
        this.timer = timer;
        this.timerControl = timerControl;
    }

    public void tryResolveStoreError(@NotNull Exception error) {
        String message = "" + error.getMessage();
        if (error instanceof RequestCodeFailure) {
            final RequestCodeFailure rcf = (RequestCodeFailure) error;
            if (rcf.hasJSONMessage()) {
                message = Phrase.from(baseFragment.getResources(), R.string.activity_store_failure_server_error)
                    .put("message", baseFragment.getString(R.string.activity_store_failure_server_error_general))
                    .put("server_message", String.join(", ", rcf.jsonErrorMessages()))
                    .format().toString();
            }
        }

        baseFragment.showQuestion(
            true,
            baseFragment.getString(R.string.activity_store_failure_message),
            message,
            baseFragment.getString(R.string.activity_store_failure_cancel),
            baseFragment.getString(R.string.activity_store_failure_stop_timer),
            b -> {
                if (!b) {
                    timerControl.stopTimer(timer, new Promise<>() {
                        @Override
                        public void succeeded(Object o) {
                            updateTimerActiveState();
                            finished();
                        }

                        @Override
                        public void failed(TranslatedException s) {
                            baseFragment.showError(
                                true,
                                R.string.activity_store_failure_failed_to_stop_title,
                                R.string.activity_store_failure_failed_to_stop_message
                            );
                            updateTimerActiveState();
                            finished();
                        }
                    });
                } else {
                    updateTimerActiveState();
                    finished();
                }
            }
        );

        updateTimerActiveState();
    }

    protected abstract void updateTimerActiveState();
    protected abstract void finished();
}
