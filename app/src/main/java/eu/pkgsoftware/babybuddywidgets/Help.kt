package eu.pkgsoftware.babybuddywidgets

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewpager2.widget.ViewPager2
import eu.pkgsoftware.babybuddywidgets.databinding.HelpDepthPagerBinding
import eu.pkgsoftware.babybuddywidgets.databinding.HelpFragmentBinding
import eu.pkgsoftware.babybuddywidgets.databinding.HelpPageBinding

class HelpDepthViewHolder(val imageView: HelpPageBinding) : ViewHolder(imageView.root) {
    init {
        imageView.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    fun setText(s: String) {
        imageView.helpText.setText(s)
    }

    fun setImage(d: Drawable) {
        imageView.helpImage.setImageDrawable(d)
    }
}

class HelpMainViewHolder(val binding: HelpDepthPagerBinding) : ViewHolder(binding.root) {
    var displayedIndex = -1

    init {
        binding.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
    }
}

class Help : BaseFragment() {
    fun constructName(i: Int, j: Int, type: String): String {
        return "help_item_${i}_${j}_${type}"
    }

    inner class HelpDepthAdapter(val baseIndex: Int) : Adapter<HelpDepthViewHolder>() {
        private var _count = 0

        init {
            while (hasResource(_count + 1)) {
                _count++
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpDepthViewHolder {
            val helpPage = HelpPageBinding.inflate(this@Help.mainActivity.layoutInflater)
            return HelpDepthViewHolder(helpPage)
        }

        private fun hasResource(i: Int): Boolean {
            val id = resources.getIdentifier(
                constructName(baseIndex, i, "text"), "string", mainActivity.packageName
            )
            return id != 0
        }

        private fun getString(name: String): String {
            val id = resources.getIdentifier(
                name, "string", mainActivity.packageName
            )
            return resources.getString(id);
        }

        private fun getDrawable(name: String): Drawable? {
            val drawableName = getString(name);
            val drawableId = resources.getIdentifier(
                drawableName, "drawable", mainActivity.packageName
            )
            try {
                return ResourcesCompat.getDrawableForDensity(
                    resources, drawableId, DisplayMetrics.DENSITY_XXXHIGH, null
                )
            } catch (e: Resources.NotFoundException) {
                return null
            }
        }

        override fun onBindViewHolder(holder: HelpDepthViewHolder, position: Int) {
            holder.setText(getString(constructName(baseIndex, position + 1, "text")))
            getDrawable(constructName(baseIndex, position + 1, "image"))?.let {
                holder.setImage(it)
            }
        }

        override fun getItemCount(): Int {
            return _count
        }
    }

    inner class HelpMainAdapter() : Adapter<HelpMainViewHolder>() {
        private var _count = 0

        val subHolders = mutableMapOf<Int, HelpMainViewHolder>()

        init {
            while (hasResource(_count + 1)) {
                _count++
            }
        }

        private fun hasResource(i: Int): Boolean {
            val id = resources.getIdentifier(
                constructName(i, 1,"text"), "string", mainActivity.packageName
            )
            return id != 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpMainViewHolder {
            return HelpMainViewHolder(HelpDepthPagerBinding.inflate(layoutInflater))
        }

        override fun onBindViewHolder(holder: HelpMainViewHolder, position: Int) {
            holder.binding.helpDepthPager.adapter = HelpDepthAdapter(position + 1)
            holder.displayedIndex = position
            subHolders.put(position, holder)
        }

        override fun getItemCount(): Int {
            return _count
        }
    }

    var binding: HelpFragmentBinding? = null
    var mainAdapter: HelpMainAdapter? = null
    var subAdapter: HelpDepthAdapter? = null

    inner class DepthPagerCallback : ViewPager2.OnPageChangeCallback() {
        var currentPager: ViewPager2? = null

        fun installOn(viewPager: ViewPager2?) {
            currentPager?.unregisterOnPageChangeCallback(this)
            currentPager = viewPager
            currentPager?.let {
                it.registerOnPageChangeCallback(this)
                onPageSelected(it.currentItem)
            }
        }

        override fun onPageSelected(position: Int) {
            currentPager?.let { pager ->
                subAdapter?.let { adapter ->
                    binding?.let {
                        it.upArrow.visibility = when(position) {
                            0 -> View.INVISIBLE
                            else -> View.VISIBLE
                        }
                        it.downArrow.visibility = when(position) {
                            adapter.itemCount - 1 -> View.INVISIBLE
                            else -> View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    val subPagerListener = DepthPagerCallback()

    private fun pageSelected(index: Int) {
        var count = 0
        subAdapter = null
        mainAdapter?.let {
            count = it.itemCount

            it.subHolders[index]?.binding?.helpDepthPager?.let { sv ->
                sv.setCurrentItem(0, false)
                subAdapter = sv.adapter as HelpDepthAdapter?
            }
        }
        subPagerListener.installOn(mainAdapter?.subHolders?.get(index)?.binding?.helpDepthPager)
        binding?.let {
            it.leftArrow.visibility = when(index) {
                0 -> View.INVISIBLE
                else -> View.VISIBLE
            }
            it.rightArrow.visibility = when(index) {
                count - 1 -> View.INVISIBLE
                else -> View.VISIBLE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainAdapter = HelpMainAdapter()

        val binding = HelpFragmentBinding.inflate(inflater)
        binding.helpPager.adapter = mainAdapter
        binding.helpPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                pageSelected(position)
            }
        })
        this.binding = binding
        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        mainActivity.setTitle("Help");
        mainActivity.enableBackNavigationButton(true)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            Help().apply {
                arguments = Bundle().apply {
                }
            }
    }
}
