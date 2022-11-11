package org.thoughtcrime.securesms.conversation.v2.dialogs

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import dagger.hilt.android.AndroidEntryPoint
import network.loki.messenger.R
import network.loki.messenger.databinding.DialogDownloadBinding
import org.session.libsession.messaging.contacts.Contact
import org.session.libsession.messaging.jobs.JobQueue
import org.session.libsession.messaging.sending_receiving.attachments.DatabaseAttachment
import org.session.libsession.utilities.recipients.Recipient
import org.thoughtcrime.securesms.conversation.v2.utilities.BaseDialog
import org.thoughtcrime.securesms.database.SessionContactDatabase
import org.thoughtcrime.securesms.util.createAndStartAttachmentDownload
import org.thoughtcrime.securesms.util.displaySize
import javax.inject.Inject

/** Shown when receiving media from a contact for the first time, to confirm that
 * they are to be trusted and files sent by them are to be downloaded. */
@AndroidEntryPoint
class DownloadDialog(private val recipient: Recipient,
                     private val databaseAttachment: DatabaseAttachment
) : BaseDialog() {

    @Inject lateinit var contactDB: SessionContactDatabase

    override fun setContentView(builder: AlertDialog.Builder) {
        val binding = DialogDownloadBinding.inflate(LayoutInflater.from(requireContext()))
        val sessionID = recipient.address.toString()
        val contact = contactDB.getContactWithSessionID(sessionID)
        val name = contact?.displayName(Contact.ContactContext.REGULAR) ?: sessionID
        val title = resources.getString(R.string.dialog_download_title, name)
        binding.downloadTitleTextView.text = title
        val displaySize = databaseAttachment.displaySize()
        val explanation = resources.getString(R.string.dialog_download_explanation, "$name ($displaySize)")
        val spannable = SpannableStringBuilder(explanation)
        val startIndex = explanation.indexOf(name)
        spannable.setSpan(StyleSpan(Typeface.BOLD), startIndex, startIndex + name.count(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.downloadExplanationTextView.text = spannable
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.downloadButton.setOnClickListener { download() }
        builder.setView(binding.root)
    }

    private fun download() {
        // TODO: add attachment download job trigger with attachmentID and databaseMessageID
        JobQueue.shared.createAndStartAttachmentDownload(databaseAttachment)
        dismiss()
    }
}