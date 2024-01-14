package org.thoughtcrime.securesms.conversation.v2.messages

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.Log
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import network.loki.messenger.R
import network.loki.messenger.databinding.ViewDocumentBinding
import org.thoughtcrime.securesms.database.model.MmsMessageRecord

class DocumentView : LinearLayout {
    private val binding: ViewDocumentBinding by lazy { ViewDocumentBinding.bind(this) }
    // region Lifecycle
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    // endregion

    // region Updating
    fun bind(message: MmsMessageRecord, @ColorInt textColor: Int) {

        Log.d("[ACL]", "[DocumentView] Hit bind!")

        val document = message.slideDeck.documentSlide!!
        binding.documentTitleTextView.text = document.fileName.or("Untitled File")
        binding.documentTitleTextView.setTextColor(textColor)
        binding.documentViewIconImageView.imageTintList = ColorStateList.valueOf(textColor)

        // Make the icon for the file appear as sending if attachment download is not yet complete
        // TODO: Change this to be a spinner?
        // TODO: Need to update the icon back to being a file icon when the attachment download DOES complete!
        // TODO: Currently it stays as the pending icon forever, unless you exit the chat then go back in and then it displays as a file icon (once downloaded)
        if (message.isMediaPending) {

            //val rotatingImageView = binding.documentViewIconImageView
            //rotatingImageView.setImageResource(R.drawable.timer60)

            var rotateAnimation = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
            rotateAnimation.interpolator = LinearInterpolator()
            rotateAnimation.repeatCount = Animation.INFINITE
            rotateAnimation.duration = 1000 // Duration is in milliseconds, so 1000ms is 1 second

            //rotatingImageView.startAnimation(rotateAnimation)


            Log.d("[ACL]", "[DocumentView] Setting `documentViewIconImageView` to look like status pending!")
            binding.documentViewIconImageView.setImageResource(R.drawable.ic_delivery_status_sending)
            binding.documentViewIconImageView.startAnimation(rotateAnimation)
        }
    }
    // endregion
}