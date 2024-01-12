package org.thoughtcrime.securesms.conversation.v2.messages

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.Log
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

        message.

        // Make the icon for the file a spinner if download is not yet complete
        if (message.isMediaPending) {
            Log.d("[ACL]", "[DocumentView] Setting `documentViewIconImageView` to look like status pending!")
            binding.documentViewIconImageView.setImageResource(R.drawable.ic_delivery_status_sending)
        }

    }
    // endregion
}