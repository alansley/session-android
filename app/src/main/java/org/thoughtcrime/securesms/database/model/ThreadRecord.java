/*
 * Copyright (C) 2012 Moxie Marlinspike
 * Copyright (C) 2013-2017 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.database.model;

import android.content.Context;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.session.libsession.utilities.ExpirationUtil;
import org.session.libsession.utilities.recipients.Recipient;
import org.thoughtcrime.securesms.database.MmsSmsColumns;
import org.thoughtcrime.securesms.database.SmsDatabase;

import network.loki.messenger.R;

/**
 * The message record model which represents thread heading messages.
 *
 * @author Moxie Marlinspike
 *
 */
public class ThreadRecord extends DisplayRecord {

  private @Nullable final Uri     snippetUri;
  private           final long    count;
  private           final int     unreadCount;
  private           final int     unreadMentionCount;
  private           final int     distributionType;
  private           final boolean archived;
  private           final long    expiresIn;
  private           final long    lastSeen;
  private           final boolean pinned;
  private           final int initialRecipientHash;

  public ThreadRecord(@NonNull String body, @Nullable Uri snippetUri,
                      @NonNull Recipient recipient, long date, long count, int unreadCount,
                      int unreadMentionCount, long threadId, int deliveryReceiptCount, int status,
                      long snippetType,  int distributionType, boolean archived, long expiresIn,
                      long lastSeen, int readReceiptCount, boolean pinned)
  {
    super(body, recipient, date, date, threadId, status, deliveryReceiptCount, snippetType, readReceiptCount);
    this.snippetUri         = snippetUri;
    this.count              = count;
    this.unreadCount        = unreadCount;
    this.unreadMentionCount = unreadMentionCount;
    this.distributionType   = distributionType;
    this.archived           = archived;
    this.expiresIn          = expiresIn;
    this.lastSeen           = lastSeen;
    this.pinned             = pinned;
    this.initialRecipientHash = recipient.hashCode();
  }

  public @Nullable Uri getSnippetUri() {
    return snippetUri;
  }

  @Override
  public SpannableString getDisplayBody(@NonNull Context context) {
    if (isGroupUpdateMessage()) {
      return emphasisAdded(context.getString(R.string.ThreadRecord_group_updated));
    } else if (isOpenGroupInvitation()) {
      return emphasisAdded(context.getString(R.string.ThreadRecord_open_group_invitation));
    } else if (MmsSmsColumns.Types.isLegacyType(type)) {
      return emphasisAdded(context.getString(R.string.MessageRecord_message_encrypted_with_a_legacy_protocol_version_that_is_no_longer_supported));
    } else if (MmsSmsColumns.Types.isDraftMessageType(type)) {
      String draftText = context.getString(R.string.ThreadRecord_draft);
      return emphasisAdded(draftText + " " + getBody(), 0, draftText.length());
    } else if (SmsDatabase.Types.isOutgoingCall(type)) {
      return emphasisAdded(context.getString(network.loki.messenger.R.string.ThreadRecord_called));
    } else if (SmsDatabase.Types.isIncomingCall(type)) {
      return emphasisAdded(context.getString(network.loki.messenger.R.string.ThreadRecord_called_you));
    } else if (SmsDatabase.Types.isMissedCall(type)) {
      return emphasisAdded(context.getString(network.loki.messenger.R.string.ThreadRecord_missed_call));
    } else if (SmsDatabase.Types.isExpirationTimerUpdate(type)) {
      int seconds = (int) (getExpiresIn() / 1000);
      if (seconds <= 0) {
        return emphasisAdded(context.getString(R.string.ThreadRecord_disappearing_messages_disabled));
      }
      String time = ExpirationUtil.getExpirationDisplayValue(context, seconds);
      return emphasisAdded(context.getString(R.string.ThreadRecord_disappearing_message_time_updated_to_s, time));
    } else if (MmsSmsColumns.Types.isMediaSavedExtraction(type)) {
      return emphasisAdded(context.getString(R.string.ThreadRecord_media_saved_by_s, getRecipient().toShortString()));
    } else if (MmsSmsColumns.Types.isScreenshotExtraction(type)) {
      return emphasisAdded(context.getString(R.string.ThreadRecord_s_took_a_screenshot, getRecipient().toShortString()));
    } else if (MmsSmsColumns.Types.isMessageRequestResponse(type)) {
      return emphasisAdded(context.getString(R.string.message_requests_accepted));
    } else if (getCount() == 0) {
      return new SpannableString(context.getString(R.string.ThreadRecord_empty_message));
    } else {
      if (TextUtils.isEmpty(getBody())) {
        return new SpannableString(emphasisAdded(context.getString(R.string.ThreadRecord_media_message)));
      } else {
        return new SpannableString(getBody());
      }
    }
  }

  private SpannableString emphasisAdded(String sequence) {
    return emphasisAdded(sequence, 0, sequence.length());
  }

  private SpannableString emphasisAdded(String sequence, int start, int end) {
    SpannableString spannable = new SpannableString(sequence);
    spannable.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC),
                      start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    return spannable;
  }

  public long getCount() {
    return count;
  }

  public int getUnreadCount() {
    return unreadCount;
  }

  public int getUnreadMentionCount() {
    return unreadMentionCount;
  }

  public long getDate() {
    return getDateReceived();
  }

  public boolean isArchived() {
    return archived;
  }

  public int getDistributionType() {
    return distributionType;
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public long getLastSeen() {
    return lastSeen;
  }

  public boolean isPinned() {
    return pinned;
  }

  public int getInitialRecipientHash() {
    return initialRecipientHash;
  }
}
