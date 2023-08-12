package eu.pkgsoftware.babybuddywidgets.timers

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class EmptyTimerListProvider : RecyclerView.Adapter<TimerListViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerListViewHolder {
        throw NotImplementedError()
    }

    override fun onBindViewHolder(holder: TimerListViewHolder, position: Int) {}

    override fun getItemCount(): Int {
        return 0
    }
}
