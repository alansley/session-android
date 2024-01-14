package org.session.libsession.utilities.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

public abstract class ProgressDialogAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
  @Override
  protected Result doInBackground(Params... params) {

    Log.d("[ACL]", "[ProgressDialogAsyncTask] Progress is: " + this.progress.getProgress());

    return null;
  }

  private final WeakReference<Context> contextReference;
  private       ProgressDialog         progress;
  private final String                 title;
  private final String                 message;

  public ProgressDialogAsyncTask(@NonNull Context context, @NonNull String title, @NonNull String message) {
    super();
    this.contextReference = new WeakReference<>(context);
    this.title            = title;
    this.message          = message;
  }

  public ProgressDialogAsyncTask(@NonNull Context context, int title, int message) {
    this(context, context.getString(title), context.getString(message));
  }

  @Override
  protected void onPreExecute() {

    Log.d("[ACL]", "Hit ProgressDialogAsyncTask.onPreExecute");

    final Context context = contextReference.get();
    if (context != null) progress = ProgressDialog.show(context, title, message, true);
  }

  @Override
  protected void onPostExecute(Result result) {

    Log.d("[ACL]", "Hit ProgressDialogAsyncTask onPostExecute");

    if (progress != null) progress.dismiss();
  }

  protected @NonNull Context getContext() {
    return contextReference.get();
  }
}

