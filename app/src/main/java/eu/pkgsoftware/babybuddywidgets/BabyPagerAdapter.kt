package eu.pkgsoftware.babybuddywidgets

import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker

// I need to extract this one!!!
internal class BabyPagerAdapter : RecyclerView.Adapter<BabyLayoutHolder?>() {
    private val holders: MutableList<BabyLayoutHolder> = ArrayList<BabyLayoutHolder>()
    var active: BabyLayoutHolder? = null
        private set

    private var fragment: BaseFragment? = null
    private var children: Array<BabyBuddyClient.Child?>? = null
    private var stateTracker: ChildrenStateTracker? = null

    fun postInit(
        fragment: BaseFragment?,
        children: Array<BabyBuddyClient.Child?>?,
        stateTracker: ChildrenStateTracker?
    ) {
        this.fragment = fragment
        this.stateTracker = stateTracker
        updateChildren(children)
    }

    fun updateChildren(children: Array<BabyBuddyClient.Child?>?) {
        this.children = children
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BabyLayoutHolder {
        val babyBinding = BabyManagerBinding.inflate(
            fragment!!.getLayoutInflater(), null, false
        )
        val v: View = babyBinding.getRoot()
        v.setLayoutParams(
            RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val holder = BabyLayoutHolder(fragment!!, babyBinding)
        holders.add(holder)
        return holder
    }

    override fun onBindViewHolder(holder: BabyLayoutHolder, position: Int) {
        holder.updateChild(children!![position], stateTracker!!)

        val childIndex = LoggedInFragment.childIndexBySlug(
            children,
            fragment!!.mainActivity.credStore.getSelectedChild()
        )
        if (childIndex >= 0) {
            if (children!![position] == children!![childIndex]) {
                activeViewChanged(children!![position])
            }
        }
    }

    override fun onViewRecycled(holder: BabyLayoutHolder) {
        holder.clear()
    }

    override fun getItemCount(): Int {
        if (children == null) {
            return 0
        }
        return children!!.size
    }

    fun activeViewChanged(c: BabyBuddyClient.Child?) {
        this.active = null
        for (h in holders) {
            if (c == h.child) {
                h.updateChild(c, stateTracker!!)
                this.active = h
            } else {
                h.onViewDeselected()
            }
        }
    }

    fun close() {
        for (h in holders) {
            h.close()
        }
    }
}
