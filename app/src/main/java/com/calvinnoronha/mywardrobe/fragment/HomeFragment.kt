package com.calvinnoronha.mywardrobe.fragment

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import com.calvinnoronha.mywardrobe.R
import com.calvinnoronha.mywardrobe.adapter.ImagePagerAdapter
import com.calvinnoronha.mywardrobe.adapter.PageChangedListener
import com.calvinnoronha.mywardrobe.data_layer.DataRepo
import com.calvinnoronha.mywardrobe.events.BottomAddedEvent
import com.calvinnoronha.mywardrobe.events.TopAddedEvent
import com.calvinnoronha.mywardrobe.events.WardrobeLoadedEvent
import com.calvinnoronha.mywardrobe.model.BottomElement
import com.calvinnoronha.mywardrobe.model.FavoriteModel
import com.calvinnoronha.mywardrobe.model.TopElement
import com.calvinnoronha.mywardrobe.model.WardrobeType
import com.calvinnoronha.mywardrobe.util.Events
import com.calvinnoronha.mywardrobe.util.getRandomId
import com.calvinnoronha.mywardrobe.util.getRandomInt
import com.calvinnoronha.mywardrobe.util.greaterThan
import kotlinx.android.synthetic.main.fragment_home.*
import org.greenrobot.eventbus.Subscribe

/**
 * A fragment to display Home.
 */
class HomeFragment : BasePickerFragment() {

    override val fragmentTag = TAG
    override val layout = R.layout.fragment_home

    companion object {
        const val TAG = "HomeFragment"

        private const val SAVE_FAVORITE = "save_favorite"
        private const val SAVE_CURRENT_TOP = "save_current_top"
        private const val SAVE_CURRENT_BOTTOM = "save_current_bottom"
        private const val SAVE_CURRENT_TOP_POS = "save_current_top_pos"
        private const val SAVE_CURRENT_BOTTOM_POS = "save_current_bottom_pos"

        // Allow more pages to be cached off-screen to allow smooth scrolling
        private const val VIEWPAGER_OFFSCREEN_CACHE = 3
    }

    private val topPagerAdapter = ImagePagerAdapter<TopElement>()
    private val bottomPagerAdapter = ImagePagerAdapter<BottomElement>()

    private var currentTop: String = ""
    private var currentBottom: String = ""
    private var isFavorite = false

    private var currentTopPosition = 0
    private var currentBottomPosition = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) restoreFromState(savedInstanceState)
        setupListeners()
        initialiseViews()
    }

    override fun onStart() {
        super.onStart()
        Events.subscribe(this)
    }

    override fun onStop() {
        Events.unsubscribe(this)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVE_FAVORITE, isFavorite)
        outState.putString(SAVE_CURRENT_TOP, currentTop)
        outState.putString(SAVE_CURRENT_BOTTOM, currentBottom)
        outState.putInt(SAVE_CURRENT_TOP_POS, currentTopPosition)
        outState.putInt(SAVE_CURRENT_BOTTOM_POS, currentBottomPosition)
    }

    private fun restoreFromState(inState: Bundle) {
        isFavorite = inState.getBoolean(SAVE_FAVORITE)
        currentTop = inState.getString(SAVE_CURRENT_TOP)
        currentBottom = inState.getString(SAVE_CURRENT_BOTTOM)
        currentTopPosition = inState.getInt(SAVE_CURRENT_TOP_POS)
        currentBottomPosition = inState.getInt(SAVE_CURRENT_BOTTOM_POS)
    }

    private fun setupListeners() {
        add_top_element.setOnClickListener { showImageSourceDialog(WardrobeType.TOP) }
        add_bottom_element.setOnClickListener { showImageSourceDialog(WardrobeType.BOTTOM) }
        wardrobe_shuffle.setOnClickListener { shuffleItems() }
        favorite_element.setOnClickListener { toggleFavorite() }
    }

    private fun shuffleItems() {
        if (topPagerAdapter.count greaterThan 1)
            wardrobe_top_viewpager.setCurrentItem(getRandomInt(topPagerAdapter.count), true)

        if (bottomPagerAdapter.count greaterThan 1)
            wardrobe_bottom_viewpager.setCurrentItem(getRandomInt(bottomPagerAdapter.count), true)
    }

    private fun setupViewPagers() {
        topPagerAdapter.setItems(DataRepo.getTops())
        bottomPagerAdapter.setItems(DataRepo.getBottoms())

        wardrobe_top_viewpager.apply {
            offscreenPageLimit = VIEWPAGER_OFFSCREEN_CACHE
            addOnPageChangeListener(object : PageChangedListener() {

                override fun onPageSelected(position: Int) {
                    provideTopPosition(position)
                }
            })
            adapter = topPagerAdapter
        }

        wardrobe_bottom_viewpager.apply {
            offscreenPageLimit = VIEWPAGER_OFFSCREEN_CACHE
            addOnPageChangeListener(object : PageChangedListener() {

                override fun onPageSelected(position: Int) {
                    provideBottomPosition(position)
                }
            })
            adapter = bottomPagerAdapter
        }
    }

    private fun toggleFavoriteIcon() {
        isFavorite = DataRepo.isFavorite(currentTop, currentBottom)
        val imageRes = if (isFavorite) R.drawable.favorite_active_icon else R.drawable.favorite_inactive_icon
        favorite_element.setImageResource(imageRes)
    }

    private fun provideTopPosition(position: Int) {
        currentTopPosition = position
        currentTop = topPagerAdapter.getItems()[position].id
        toggleFavoriteIcon()
    }

    private fun provideBottomPosition(position: Int) {
        currentBottomPosition = position
        currentBottom = bottomPagerAdapter.getItems()[position].id
        toggleFavoriteIcon()
    }

    private fun toggleFavorite() {
        if (currentTop.isBlank() || currentBottom.isBlank()) {
            AlertDialog.Builder(context!!)
                    .setTitle(R.string.favorite_error_title)
                    .setMessage(R.string.favorite_error_message)
                    .show()
            return
        }
        if (isFavorite) DataRepo.removeFavorite(currentTop, currentBottom)
        else DataRepo.addFavorite(FavoriteModel(getRandomId(), currentTop, currentBottom))
        toggleFavoriteIcon()
    }

    private fun initialiseCurrents() {
        if (currentTop.isBlank() && topPagerAdapter.getItems().isNotEmpty()) {
            currentTop = topPagerAdapter.getItems()[0].id
        }
        if (currentBottom.isBlank() && bottomPagerAdapter.getItems().isNotEmpty()) {
            currentBottom = bottomPagerAdapter.getItems()[0].id
        }
    }

    private fun initialiseViews() {
        setupViewPagers()
        initialiseCurrents()
        toggleFavoriteIcon()
    }

    @Subscribe(sticky = true)
    fun onWardrobeLoaded(loadedEvent: WardrobeLoadedEvent) {
        Events.removeSticky(loadedEvent)
        initialiseViews()
    }

    @Subscribe(sticky = true)
    fun onTopAdded(topAddedEvent: TopAddedEvent) {
        Events.removeSticky(topAddedEvent)
        topPagerAdapter.setItems(DataRepo.getTops())
        wardrobe_top_viewpager.setCurrentItem(currentTopPosition, false)

        if (topPagerAdapter.count == 1) provideTopPosition(0)
    }

    @Subscribe(sticky = true)
    fun onBottomAdded(bottomAddedEvent: BottomAddedEvent) {
        Events.removeSticky(bottomAddedEvent)
        bottomPagerAdapter.setItems(DataRepo.getBottoms())
        wardrobe_bottom_viewpager.setCurrentItem(currentBottomPosition, false)

        if (bottomPagerAdapter.count == 1) provideBottomPosition(0)
    }
}
