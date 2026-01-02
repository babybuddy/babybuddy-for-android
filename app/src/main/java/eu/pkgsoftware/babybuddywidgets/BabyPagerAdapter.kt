package eu.pkgsoftware.babybuddywidgets

import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding
import eu.pkgsoftware.babybuddywidgets.logic.ChildrenStateTracker
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.Child
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.childIndexBySlug

class BabyPagerAdapter(val stateTracker: ChildrenStateTracker) : RecyclerView.Adapter<BabyLayoutHolder>() {
    var active: BabyLayoutHolder? = null

    private val holders: MutableList<BabyLayoutHolder> = ArrayList()
    private var fragment: BaseFragment? = null
    private var children = emptyArray<Child>()

    init {
        stateTracker.addChildListener({ newChildren ->
            children = newChildren
            notifyDataSetChanged()
        })
    }

    fun postInit(
        fragment: BaseFragment?
    ) {
        this.fragment = fragment
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
        holder.updateChild(children[position])

        val childIndex = childIndexBySlug(
            children,
            fragment!!.mainActivity.credStore.getSelectedChild() ?: ""
        )
        if (childIndex >= 0) {
            if (children[position] == children[childIndex]) {
                activeViewChanged(children[position])
            }
        }
    }

    override fun onViewRecycled(holder: BabyLayoutHolder) {
        holder.clear()
    }

    override fun getItemCount(): Int {
        return children.size
    }

    fun activeViewChanged(c: Child) {
        this.active = null
        for (h in holders) {
            if (c == h.child) {
                h.updateChild(c)
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
