package org.thoughtcrime.securesms.conversation.v2.input_bar

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_input_bar.view.*
import network.loki.messenger.R
import org.thoughtcrime.securesms.conversation.v2.messages.QuoteView
import org.thoughtcrime.securesms.conversation.v2.messages.QuoteViewDelegate
import org.thoughtcrime.securesms.database.model.MessageRecord
import org.thoughtcrime.securesms.database.model.MmsMessageRecord
import org.thoughtcrime.securesms.loki.utilities.toDp
import org.thoughtcrime.securesms.loki.utilities.toPx
import org.thoughtcrime.securesms.mms.SlideDeck
import kotlin.math.max

class InputBar : RelativeLayout, InputBarEditTextDelegate, QuoteViewDelegate {
    private val vMargin by lazy { toDp(4, resources) }
    var delegate: InputBarDelegate? = null
    var additionalContentHeight = 0

    private val attachmentsButton by lazy { InputBarButton(context, R.drawable.ic_plus_24) }
    private val microphoneButton by lazy { InputBarButton(context, R.drawable.ic_microphone) }
    private val sendButton by lazy { InputBarButton(context, R.drawable.ic_arrow_up, true) }

    // region Lifecycle
    constructor(context: Context) : super(context) { initialize() }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { initialize() }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initialize() }

    private fun initialize() {
        LayoutInflater.from(context).inflate(R.layout.view_input_bar, this)
        // Attachments button
        attachmentsButtonContainer.addView(attachmentsButton)
        attachmentsButton.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        attachmentsButton.onPress = { toggleAttachmentOptions() }
        // Microphone button
        microphoneOrSendButtonContainer.addView(microphoneButton)
        microphoneButton.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        microphoneButton.onLongPress = { showVoiceMessageUI() }
        microphoneButton.onMove = { delegate?.onMicrophoneButtonMove(it) }
        microphoneButton.onCancel = { delegate?.onMicrophoneButtonCancel(it) }
        microphoneButton.onUp = { delegate?.onMicrophoneButtonUp(it) }
        // Send button
        microphoneOrSendButtonContainer.addView(sendButton)
        sendButton.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        sendButton.isVisible = false
        // Edit text
        inputBarEditText.imeOptions = inputBarEditText.imeOptions or 16777216 // Always use incognito keyboard
        inputBarEditText.delegate = this
    }
    // endregion

    // region General
    private fun setHeight(newHeight: Int) {
        val layoutParams = inputBarLinearLayout.layoutParams as LayoutParams
        layoutParams.height = newHeight
        inputBarLinearLayout.layoutParams = layoutParams
        delegate?.inputBarHeightChanged(newHeight)
    }
    // endregion

    // region Updating
    override fun inputBarEditTextContentChanged(text: CharSequence) {
        sendButton.isVisible = text.isNotEmpty()
        microphoneButton.isVisible = text.isEmpty()
        delegate?.inputBarEditTextContentChanged(text)
    }

    override fun inputBarEditTextHeightChanged(newValue: Int) {
        val newHeight = max(newValue + 2 * vMargin, toPx(56, resources)) + inputBarAdditionalContentContainer.height
        setHeight(newHeight)
    }

    private fun toggleAttachmentOptions() {
        delegate?.toggleAttachmentOptions()
    }

    private fun showVoiceMessageUI() {
        delegate?.showVoiceMessageUI()
    }

    fun draftQuote(message: MessageRecord) {
        inputBarAdditionalContentContainer.removeAllViews()
        val quoteView = QuoteView(context, QuoteView.Mode.Draft)
        quoteView.delegate = this
        inputBarAdditionalContentContainer.addView(quoteView)
        val attachments = (message as? MmsMessageRecord)?.slideDeck
        quoteView.bind(message.individualRecipient.address.toString(), message.body, attachments, message.recipient, true)
        val quoteViewIntrinsicHeight = quoteView.getIntrinsicHeight()
        val newHeight = max(inputBarEditText.height + 2 * vMargin, toPx(56, resources)) + quoteViewIntrinsicHeight
        additionalContentHeight = quoteViewIntrinsicHeight
        setHeight(newHeight)
    }

    override fun cancelQuoteDraft() {
        inputBarAdditionalContentContainer.removeAllViews()
        val newHeight = max(inputBarEditText.height + 2 * vMargin, toPx(56, resources))
        additionalContentHeight = 0
        setHeight(newHeight)
    }
    // endregion
}

interface InputBarDelegate {

    fun inputBarHeightChanged(newValue: Int)
    fun inputBarEditTextContentChanged(newContent: CharSequence)
    fun toggleAttachmentOptions()
    fun showVoiceMessageUI()
    fun onMicrophoneButtonMove(event: MotionEvent)
    fun onMicrophoneButtonCancel(event: MotionEvent)
    fun onMicrophoneButtonUp(event: MotionEvent)
}