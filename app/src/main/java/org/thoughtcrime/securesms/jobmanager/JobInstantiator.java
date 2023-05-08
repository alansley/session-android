package org.thoughtcrime.securesms.jobmanager;

import androidx.annotation.NonNull;

import org.session.libsession.messaging.jobs.Job;
import org.session.libsession.messaging.utilities.Data;

import java.util.HashMap;
import java.util.Map;

class JobInstantiator {

  private final Map<String, Job.Factory> jobFactories;

  JobInstantiator(@NonNull Map<String, Job.Factory> jobFactories) {
    this.jobFactories = new HashMap<>(jobFactories);
  }

  public @NonNull Job instantiate(@NonNull String jobFactoryKey, @NonNull Data data) {
    if (jobFactories.containsKey(jobFactoryKey)) {
      return jobFactories.get(jobFactoryKey).create(data);
    } else {
      throw new IllegalStateException("Tried to instantiate a job with key '" + jobFactoryKey + "', but no matching factory was found.");
    }
  }
}
