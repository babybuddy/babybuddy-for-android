package eu.pkgsoftware.babybuddywidgets

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class EmptyBabyPagerAdapter : RecyclerView.Adapter<BabyLayoutHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BabyLayoutHolder {
        throw NotImplementedError()
    }

    override fun onBindViewHolder(holder: BabyLayoutHolder, position: Int) {}
    override fun getItemCount(): Int {
        return 0
    }
}