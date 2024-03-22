package org.session.libsession.messaging.utilities

import android.content.Context
import com.squareup.phrase.Phrase
import org.session.libsession.R
import org.session.libsession.messaging.MessagingModuleConfiguration
import org.session.libsession.messaging.calls.CallMessageType
import org.session.libsession.messaging.calls.CallMessageType.CALL_FIRST_MISSED
import org.session.libsession.messaging.calls.CallMessageType.CALL_INCOMING
import org.session.libsession.messaging.calls.CallMessageType.CALL_MISSED
import org.session.libsession.messaging.calls.CallMessageType.CALL_OUTGOING
import org.session.libsession.messaging.contacts.Contact
import org.session.libsession.messaging.messages.ExpirationConfiguration.Companion.isNewConfigEnabled
import org.session.libsession.messaging.sending_receiving.data_extraction.DataExtractionNotificationInfoMessage
import org.session.libsession.utilities.ExpirationUtil
import org.session.libsession.utilities.getExpirationTypeDisplayValue
import org.session.libsession.utilities.recipients.Recipient
import org.session.libsession.utilities.truncateIdForDisplay

object UpdateMessageBuilder {

    // Phrase library substitution keys
    private val NAME        = "name"
    private val GROUP_NAME  = "groupname"
    private val MEMBERS     = "members"


    val storage = MessagingModuleConfiguration.shared.storage

    private fun getSenderName(senderId: String) = storage.getContactWithSessionID(senderId)
        ?.displayName(Contact.ContactContext.REGULAR)
        ?: truncateIdForDisplay(senderId)

    fun buildGroupUpdateMessage(
        context: Context,
        updateMessageData: UpdateMessageData,
        senderId: String? = null,
        isOutgoing: Boolean = false
    ): String {
        val updateData = updateMessageData.kind
        if (updateData == null || !isOutgoing && senderId == null) return ""
        val senderName: String = if (isOutgoing) context.getString(R.string.MessageRecord_you)
        else getSenderName(senderId!!)

        return when (updateData) {
            is UpdateMessageData.Kind.GroupCreation -> if (isOutgoing) {
                context.getString(R.string.groupCreated)
            } else {
                Phrase.from(context, R.string.groupAddedYou)
                    .put(NAME, senderName)
                    .format().toString()
            }

            is UpdateMessageData.Kind.GroupNameChange -> if (isOutgoing) {
                Phrase.from(context, R.string.groupYouRenamed)
                    .put(GROUP_NAME, updateData.name)
                    .format().toString()
            } else {
                Phrase.from(context, R.string.groupRenamedBy)
                    .put(NAME, senderName)
                    .put(GROUP_NAME, updateData.name)
                    .format().toString()
            }

            is UpdateMessageData.Kind.GroupMemberAdded -> {
                val members =
                    updateData.updatedMembers.joinToString(", ", transform = ::getSenderName)
                if (isOutgoing) {
                    Phrase.from(context, R.string.groupYouAdded)
                        .put(MEMBERS, members)
                        .format().toString()
                } else {
                    Phrase.from(context, R.string.groupMemberAdded)
                        .put(NAME, senderName)
                        .put(MEMBERS, members)
                        .format().toString()
                }
            }

            is UpdateMessageData.Kind.GroupMemberRemoved -> {
                val userPublicKey = storage.getUserPublicKey()!!
                // 1st case: you are part of the removed members
                return if (userPublicKey in updateData.updatedMembers) {
                    if (isOutgoing) {
                        context.getString(R.string.groupYouLeft)
                    } else {
                        context.getString(R.string.groupRemovedYou)
                    }
                } else {
                    // 2nd case: you are not part of the removed members
                    val members =
                        updateData.updatedMembers.joinToString(", ", transform = ::getSenderName)
                    if (isOutgoing) {
                        Phrase.from(context, R.string.groupYouRemoved)
                            .put(MEMBERS, members)
                            .format().toString()
                    } else {
                        Phrase.from(context, R.string.groupMemberRemoved)
                            .put(NAME, senderName)
                            .put(MEMBERS, members)
                            .format().toString()
                    }
                }
            }

            is UpdateMessageData.Kind.GroupMemberLeft -> if (isOutgoing) {
                context.getString(R.string.groupYouLeft)
            } else {
                Phrase.from(context, R.string.groupMemberLeft)
                    .put(NAME, senderName)
                    .format().toString()
            }

            else -> return ""
        }
    }

    fun buildExpirationTimerMessage(
        context: Context,
        duration: Long,
        recipient: Recipient,
        senderId: String? = null,
        isOutgoing: Boolean = false,
        timestamp: Long,
        expireStarted: Long
    ): String {
        if (!isOutgoing && senderId == null) return ""
        val senderName =
            if (isOutgoing) context.getString(R.string.MessageRecord_you) else getSenderName(
                senderId!!
            )
        return if (duration <= 0) {
            if (isOutgoing) {
                if (!isNewConfigEnabled) context.getString(R.string.disappearingMessagesDisabledYou)
                else context.getString(if (recipient.is1on1) R.string.disappearingMessagesTurnedOffOneOnOne else R.string.disappearingMessagesYouHaveTurnedOff)
            } else {
                if (!isNewConfigEnabled) context.getString(
                    R.string.MessageRecord_s_disabled_disappearing_messages,
                    senderName
                )
                else context.getString(
                    if (recipient.is1on1) R.string.MessageRecord_s_turned_off_disappearing_messages_1_on_1 else R.string.MessageRecord_s_turned_off_disappearing_messages,
                    senderName
                )
            }
        } else {
            val time = ExpirationUtil.getExpirationDisplayValue(context, duration.toInt())
            val action = context.getExpirationTypeDisplayValue(timestamp == expireStarted)
            if (isOutgoing) {
                if (!isNewConfigEnabled) context.getString(
                    R.string.MessageRecord_you_set_disappearing_message_time_to_s,
                    time
                )
                else context.getString(
                    if (recipient.is1on1) R.string.MessageRecord_you_set_messages_to_disappear_s_after_s_1_on_1 else R.string.MessageRecord_you_set_messages_to_disappear_s_after_s,
                    time,
                    action
                )
            } else {
                if (!isNewConfigEnabled) context.getString(
                    R.string.MessageRecord_s_set_disappearing_message_time_to_s,
                    senderName,
                    time
                )
                else context.getString(
                    if (recipient.is1on1) R.string.MessageRecord_s_set_messages_to_disappear_s_after_s_1_on_1 else R.string.MessageRecord_s_set_messages_to_disappear_s_after_s,
                    senderName,
                    time,
                    action
                )
            }
        }
    }

    fun buildDataExtractionMessage(
        context: Context,
        kind: DataExtractionNotificationInfoMessage.Kind,
        senderId: String? = null
    ): String {
        val senderName = getSenderName(senderId!!)
        return when (kind) {
            DataExtractionNotificationInfoMessage.Kind.SCREENSHOT ->
                context.getString(R.string.MessageRecord_s_took_a_screenshot, senderName)

            DataExtractionNotificationInfoMessage.Kind.MEDIA_SAVED ->
                context.getString(R.string.MessageRecord_media_saved_by_s, senderName)
        }
    }

    fun buildCallMessage(context: Context, callMessageType: CallMessageType, sender: String): String
    {
        val substitution = storage.getContactWithSessionID(sender)?.displayName(Contact.ContactContext.REGULAR) ?: sender
        return when (callMessageType) {
            CALL_INCOMING ->     { Phrase.from(context, R.string.callsCalledYou).put(NAME, substitution).format().toString()      }
            CALL_OUTGOING ->     { Phrase.from(context, R.string.callsYouCalled).put(NAME, substitution).format().toString()      }
            CALL_MISSED,
            CALL_FIRST_MISSED -> { Phrase.from(context, R.string.callsMissedCallFrom).put(NAME, substitution).format().toString() }
        }
    }
}
