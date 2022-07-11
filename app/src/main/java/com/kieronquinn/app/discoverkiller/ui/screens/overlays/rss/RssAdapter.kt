package com.kieronquinn.app.discoverkiller.ui.screens.overlays.rss

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.databinding.ItemRssBinding
import com.kieronquinn.app.discoverkiller.model.rss.RssItem
import com.kieronquinn.app.discoverkiller.utils.picasso.OgImageHandler
import com.kieronquinn.app.discoverkiller.utils.extensions.isDarkMode
import com.kieronquinn.monetcompat.core.MonetCompat
import com.squareup.picasso.Picasso

class RssAdapter(
    context: Context,
    private val onItemClicked: (item: RssItem) -> Unit
) : ArrayAdapter<RssItem>(context, R.layout.item_rss) {

    private val layoutInflater = LayoutInflater.from(context)
    private val picasso = Picasso.Builder(context)
        .addRequestHandler(OgImageHandler())
        .build()

    private val monet by lazy {
        MonetCompat.getInstance()
    }

    private val cardBackground by lazy {
        monet.getBackgroundColor(context)
    }

    private val cardTextColor by lazy {
        if (context.isDarkMode) Color.WHITE else Color.BLACK
    }

    private val cardContentColor by lazy {
        ColorUtils.setAlphaComponent(cardTextColor, 191)
    }

    private val placeholderImage by lazy {
        ContextCompat.getDrawable(context, R.drawable.ic_rss_placeholder)!!.apply {
            setTint(cardTextColor)
        }
    }

    private val imageSize by lazy {
        context.resources.getDimension(R.dimen.rss_image_size).toInt()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = (convertView ?: layoutInflater.inflate(
            R.layout.item_rss, parent, false
        )).let {
            ItemRssBinding.bind(it)
        }
        val item = getItem(position) ?: return view.root
        view.itemRssItem.backgroundTintList = ColorStateList.valueOf(cardBackground)
        view.itemRssTitle.text = item.title
        view.itemRssTitle.setTextColor(cardTextColor)
        view.itemRssContent.isVisible = !item.description.isNullOrBlank()
        view.itemRssContent.text = item.description
        view.itemRssContent.setTextColor(cardContentColor)
        view.itemRssImage.setImageDrawable(placeholderImage)
        view.itemRssImage.clipToOutline = true
        val imageUrl = item.imageUrl?.let { Uri.parse(it) } ?: OgImageHandler.createUri(item.url)
        picasso.load(imageUrl).error(placeholderImage)
            .resize(imageSize, imageSize).centerCrop().into(view.itemRssImage)
        view.itemRssItem.setOnClickListener {
            onItemClicked(item)
        }
        return view.root
    }

    fun setItems(items: List<RssItem>) {
        clear()
        addAll(items)
        notifyDataSetChanged()
    }

}