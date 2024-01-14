package org.thoughtcrime.securesms.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.webkit.MimeTypeMap
import android.widget.Toast
import network.loki.messenger.R
import org.session.libsession.utilities.task.ProgressDialogAsyncTask
import org.session.libsignal.utilities.ExternalStorageUtil
import org.session.libsignal.utilities.Log
import org.thoughtcrime.securesms.mms.PartAuthority
import org.thoughtcrime.securesms.net.ChunkedDataFetcher
import org.thoughtcrime.securesms.showSessionDialog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Saves attachment files to an external storage using [MediaStore] API.
 * Requires [android.Manifest.permission.WRITE_EXTERNAL_STORAGE] on API 28 and below.
 *
 * Note: AsyncTasks are deprecated since Android 11 / API 30 - but migrating to Kotlin Coroutines
 * (preferred) or Java Executors (fallback) is going to be non-trivial and should likely be deferred
 * to a specific ticket related to replacing AsyncTasks across the entire codebase.
 * See: https://kotlinlang.org/docs/coroutines-overview.html
 * See: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executors.html
 */
class SaveAttachmentTask @JvmOverloads constructor(context: Context, count: Int = 1) :
    ProgressDialogAsyncTask<SaveAttachmentTask.Attachment, Void, Pair<Int, String?>>(
        context,
        context.resources.getQuantityString(R.plurals.ConversationFragment_saving_n_attachments, count, count),
        context.resources.getQuantityString(R.plurals.ConversationFragment_saving_n_attachments_to_sd_card, count, count)
    ) {

    companion object {
        @JvmStatic
        private val TAG = SaveAttachmentTask::class.simpleName

        private const val RESULT_SUCCESS = 0
        private const val RESULT_FAILURE = 1

        @JvmStatic
        @JvmOverloads
        fun showWarningDialog(context: Context, count: Int = 1, onAcceptListener: () -> Unit = {}) {
            context.showSessionDialog {
                title(R.string.ConversationFragment_save_to_sd_card)
                iconAttribute(R.attr.dialog_alert_icon)
                text(context.resources.getQuantityString(
                    R.plurals.ConversationFragment_saving_n_media_to_storage_warning,
                    count,
                    count))
                button(R.string.yes) { onAcceptListener() }
                button(R.string.no)
            }
        }
    }

    private val contextReference: WeakReference<Context>
    private val attachmentCount: Int = count

    init {
        this.contextReference = WeakReference(context)
    }

    override fun doInBackground(vararg attachments: Attachment?): Pair<Int, String?> {

        Log.d("[ACL]", "Hit SaveAttachmentTask.doInBackground")

        if (attachments.isEmpty()) {
            throw IllegalArgumentException("Must pass in at least one attachment")
        }

        var progressPercent: Int = 0
        this.publishProgress()

        try {
            val context = contextReference.get()
            var directory: String? = null

            if (context == null) {
                return Pair(RESULT_FAILURE, null)
            }

            for (attachment in attachments) {
                if (attachment != null) {
                    directory = saveAttachment(context, attachment)
                    if (directory == null) return Pair(RESULT_FAILURE, null)
                }
            }

            return if (attachments.size > 1)
                Pair(RESULT_SUCCESS, null)
            else
                Pair(RESULT_SUCCESS, directory)
        } catch (e: IOException) {
            Log.w(TAG, e)
            return Pair(RESULT_FAILURE, null)
        }
    }

    @Throws(IOException::class)
    private fun saveAttachment(context: Context, attachment: Attachment): String? {

        Log.d("[ACL]", "[SaveAttachmentTask] Asked to save attachment: $attachment")

        val contentType = Objects.requireNonNull(MediaUtil.getCorrectedMimeType(attachment.contentType))!!

        // Make sure we have a filename or generate one if needs be
        var fileName = attachment.fileName
        if (fileName == null) fileName = generateOutputFileName(contentType, attachment.date)
        fileName = sanitizeOutputFileName(fileName)

        val outputUri: Uri = getMediaStoreContentUriForType(contentType)
        val mediaUri = createOutputUri(outputUri, contentType, fileName)

        val updateValues = ContentValues()
        PartAuthority.getAttachmentStream(context, attachment.uri).use { inputStream ->
            if (inputStream == null) { return null }

            if (outputUri.scheme == ContentResolver.SCHEME_FILE) {

                Log.d("[ACL]", "Content resolver reckons this is a scheme file.")

                FileOutputStream(mediaUri!!.path).use { outputStream ->

                    Log.d("[ACL]", "Content resolver about to copy input->output (SCHEME file).")

                    StreamUtil.copy(inputStream, outputStream)


                    //MediaScannerConnection.scanFile(context, arrayOf(mediaUri.path), arrayOf(contentType), null)
                    //val attachmentDownloadedCallback = ChunkedDataFetcher.Callback(outputStream)
                    //attachmentDownloadedCallback.onSuccess(
                    //if (inputStream.)
                    //-> { Log.d("[ACL]", "Hit attachmentDownloadedCallback!") }
                    //MediaScannerConnection.scanFile(context, arrayOf(mediaUri.path), arrayOf(contentType), attachmentDownloadedCallback)
                }
            } else { // THIS TRIGGERS WITH A ZIP FILE

                Log.d("[ACL]", "Content resolver about to copy input->output (NON-scheme file).")

                context.contentResolver.openOutputStream(mediaUri!!, "w").use { outputStream ->
                    val total: Long = StreamUtil.copy(inputStream, outputStream)
                    if (total > 0) {
                        updateValues.put(MediaStore.MediaColumns.SIZE, total)
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT > 28) {
            updateValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        if (updateValues.size() > 0) {
            getContext().contentResolver.update(mediaUri!!, updateValues, null, null)
        }
        return outputUri.lastPathSegment
    }

    private fun getMediaStoreContentUriForType(contentType: String): Uri {
        return when {
            contentType.startsWith("video/") ->
                ExternalStorageUtil.getVideoUri()
            contentType.startsWith("audio/") ->
                ExternalStorageUtil.getAudioUri()
            contentType.startsWith("image/") ->
                ExternalStorageUtil.getImageUri()
            else ->
                ExternalStorageUtil.getDownloadUri()
        }
    }

    @Throws(IOException::class)
    private fun createOutputUri(outputUri: Uri, contentType: String, fileName: String): Uri? {
        val fileParts: Array<String> = getFileNameParts(fileName)
        val base = fileParts[0]
        val extension = fileParts[1]
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        contentValues.put(MediaStore.MediaColumns.DATE_ADDED, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()))
        contentValues.put(MediaStore.MediaColumns.DATE_MODIFIED, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()))
        if (Build.VERSION.SDK_INT > 28) {
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1)
        } else if (Objects.equals(outputUri.scheme, ContentResolver.SCHEME_FILE)) {
            val outputDirectory = File(outputUri.path)
            var outputFile = File(outputDirectory, "$base.$extension")
            var i = 0
            while (outputFile.exists()) {
                outputFile = File(outputDirectory, base + "-" + ++i + "." + extension)
            }
            if (outputFile.isHidden) {
                throw IOException("Specified name would not be visible")
            }
            return Uri.fromFile(outputFile)
        } else {
            var outputFileName = fileName
            var dataPath = String.format("%s/%s", getExternalPathToFileForType(contentType), outputFileName)
            var i = 0
            while (pathTaken(outputUri, dataPath)) {
                Log.d(TAG, "The content exists. Rename and check again.")
                outputFileName = base + "-" + ++i + "." + extension
                dataPath = String.format("%s/%s", getExternalPathToFileForType(contentType), outputFileName)
            }
            contentValues.put(MediaStore.MediaColumns.DATA, dataPath)
        }
        return context.contentResolver.insert(outputUri, contentValues)
    }

    private fun getFileNameParts(fileName: String): Array<String> {
        val tokens = fileName.split("\\.(?=[^\\.]+$)".toRegex()).toTypedArray()
        return arrayOf(tokens[0], if (tokens.size > 1) tokens[1] else "")
    }

    private fun getExternalPathToFileForType(contentType: String): String {
        val storage: File = when {
            contentType.startsWith("video/") ->
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
            contentType.startsWith("audio/") ->
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!
            contentType.startsWith("image/") ->
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            else ->
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!
        }
        return storage.absolutePath
    }

    @Throws(IOException::class)
    private fun pathTaken(outputUri: Uri, dataPath: String): Boolean {
        context.contentResolver.query(outputUri, arrayOf(MediaStore.MediaColumns.DATA),
                MediaStore.MediaColumns.DATA + " = ?", arrayOf(dataPath),
                null).use { cursor ->
            if (cursor == null) {
                throw IOException("Something is wrong with the filename to save")
            }
            return cursor.moveToFirst()
        }
    }

    private fun generateOutputFileName(contentType: String, timestamp: Long): String {
        val mimeTypeMap = MimeTypeMap.getSingleton()
        val extension = mimeTypeMap.getExtensionFromMimeType(contentType) ?: "attach"
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd-HHmmss")
        val base = "session-${dateFormatter.format(timestamp)}"

        return "${base}.${extension}";
    }

    private fun sanitizeOutputFileName(fileName: String): String {
        return File(fileName).name
    }

    // Note: When a task hits `onPostExecute` its status is still RUNNING (because this post-execute
    // element of the task is still considered part of the task!)
    @Deprecated("Deprecated in Java")
    override fun onPostExecute(result: Pair<Int, String?>) {
        super.onPostExecute(result)

        Log.d("[ACL]", "Hit onPostExecute with status: ${this.status.toString()}")

        val context = contextReference.get() ?: return
        Log.d("[ACL]", "Context is okay")

        /*
        when (this.status) {
            // TODO: Make this text multilingual!
            Status.PENDING, Status.RUNNING -> {
                Log.d("[ACL]", "Hit SaveAttachmentTask.onPostExectute and status is PENDING or RUNNING")
                Toast.makeText(this.context, "Please wait until download completes to save attachment.", Toast.LENGTH_SHORT)
                return
            }
            Status.FINISHED -> {
                Log.d("[ACL]", "Hit SaveAttachmentTask.onPostExectute and status is FINISHED")
                // Attachment download has completed and we are free to continue with the save operation as below
            }
        }
        */

        when (result.first) {
            RESULT_FAILURE -> {
                val message = context.resources.getQuantityText(R.plurals.ConversationFragment_error_while_saving_attachments_to_sd_card, attachmentCount)
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }

            RESULT_SUCCESS -> {
                val message = if (!TextUtils.isEmpty(result.second)) {
                    context.resources.getString(R.string.SaveAttachmentTask_saved_to, result.second)
                } else {
                    context.resources.getString(R.string.SaveAttachmentTask_saved)
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }

            else -> throw IllegalStateException("Unexpected result value: " + result.first)
        }
    }

    data class Attachment(val uri: Uri, val contentType: String, val date: Long, val fileName: String?)
}