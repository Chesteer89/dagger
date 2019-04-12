/*
 * Copyright (C) 2017 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dagger.android.support;

import static android.util.Log.DEBUG;
import static dagger.internal.Preconditions.checkNotNull;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import android.support.v4.app.Fragment;
import android.util.Log;
import dagger.android.AndroidInjector;
import dagger.android.HasViewInjector;
import dagger.internal.Beta;

/** Injects core Android types from support libraries. */
@Beta
public final class AndroidSupportInjection {
  private static final String TAG = "dagger.android.support";

  /**
   * Injects {@code fragment} if an associated {@link dagger.android.AndroidInjector} implementation
   * can be found, otherwise throws an {@link IllegalArgumentException}.
   *
   * <p>Uses the following algorithm to find the appropriate {@code AndroidInjector<Fragment>} to
   * use to inject {@code fragment}:
   *
   * <ol>
   *   <li>Walks the parent-fragment hierarchy to find the a fragment that implements {@link
   *       HasSupportFragmentInjector}, and if none do
   *   <li>Uses the {@code fragment}'s {@link Fragment#getActivity() activity} if it implements
   *       {@link HasSupportFragmentInjector}, and if not
   *   <li>Uses the {@link android.app.Application} if it implements {@link
   *       HasSupportFragmentInjector}.
   * </ol>
   *
   * If none of them implement {@link HasSupportFragmentInjector}, a {@link
   * IllegalArgumentException} is thrown.
   *
   * @throws IllegalArgumentException if no parent fragment, activity, or application implements
   *     {@link HasSupportFragmentInjector}.
   */
  public static void inject(Fragment fragment) {
    checkNotNull(fragment, "fragment");
    HasSupportFragmentInjector hasSupportFragmentInjector = findHasFragmentInjector(fragment);
    if (Log.isLoggable(TAG, DEBUG)) {
      Log.d(
          TAG,
          String.format(
              "An injector for %s was found in %s",
              fragment.getClass().getCanonicalName(),
              hasSupportFragmentInjector.getClass().getCanonicalName()));
    }

    AndroidInjector<Fragment> fragmentInjector =
        hasSupportFragmentInjector.supportFragmentInjector();
    checkNotNull(
        fragmentInjector,
        "%s.supportFragmentInjector() returned null",
        hasSupportFragmentInjector.getClass());

    fragmentInjector.inject(fragment);
  }

  private static HasSupportFragmentInjector findHasFragmentInjector(Fragment fragment) {
    Fragment parentFragment = fragment;
    while ((parentFragment = parentFragment.getParentFragment()) != null) {
      if (parentFragment instanceof HasSupportFragmentInjector) {
        return (HasSupportFragmentInjector) parentFragment;
      }
    }
    Activity activity = fragment.getActivity();
    if (activity instanceof HasSupportFragmentInjector) {
      return (HasSupportFragmentInjector) activity;
    }
    if (activity.getApplication() instanceof HasSupportFragmentInjector) {
      return (HasSupportFragmentInjector) activity.getApplication();
    }
    throw new IllegalArgumentException(
        String.format("No injector was found for %s", fragment.getClass().getCanonicalName()));
  }

  /**
   * Injects {@code view} if an associated {@link AndroidInjector} implementation can be found,
   * otherwise throws an {@link IllegalArgumentException}.
   *
   * @throws RuntimeException if the {@link View} doesn't implement {@link
   *     HasActivityInjector}.
   */
  public static void inject(View view) {
    checkNotNull(view, "view");
    Activity activity = getViewActivity(view);
    if (!(activity instanceof HasViewInjector)) {
      throw new RuntimeException(
              String.format(
                      "%s does not implement %s",
                      activity.getClass().getCanonicalName(),
                      HasViewInjector.class.getCanonicalName()));
    }

    AndroidInjector<View> viewInjector =
            ((HasViewInjector) activity).viewInjector();
    checkNotNull(viewInjector, "%s.viewInjector() returned null", activity.getClass());

    viewInjector.inject(view);
  }

  public static Activity getViewActivity(View view) {
    // https://android.googlesource.com/platform/frameworks/support/+/03e0f3daf3c97ee95cd78b2f07bc9c1be05d43db/v7/mediarouter/src/android/support/v7/app/MediaRouteButton.java#276
    Context context = view.getContext();
    while (context instanceof ContextWrapper) {
      if (context instanceof Activity) {
        return (Activity)context;
      }
      context = ((ContextWrapper) context).getBaseContext();
    }
    throw new IllegalStateException("Context does not stem from an activity: " + view.getContext());
  }

  private AndroidSupportInjection() {}
}
