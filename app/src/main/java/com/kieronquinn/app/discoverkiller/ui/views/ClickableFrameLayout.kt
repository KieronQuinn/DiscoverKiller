package com.kieronquinn.app.discoverkiller.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class ClickableFrameLayout : FrameLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var mOnClickListener: OnClickListener? = null
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        mOnClickListener = l
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return mOnClickListener != null
    }

}