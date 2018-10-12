package com.jraska.falcon;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.jraska.falcon.sample.SampleActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(AndroidJUnit4.class)
public class FalconThreadingTest {
  @Rule
  public ActivityTestRule<SampleActivity> activityRule = new ActivityTestRule<>(SampleActivity.class);

  @Test
  public void screenshotFromOtherThreadWorks() throws InterruptedException {
    SampleActivity activity = activityRule.getActivity();

    CountDownLatch countDownLatch = new CountDownLatch(1);
    TakeScreenShotRunnable takeScreenShotRunnable = new TakeScreenShotRunnable(activity, countDownLatch);
    new Thread(takeScreenShotRunnable).run();

    boolean await = countDownLatch.await(10L, TimeUnit.SECONDS);
    assertThat(await).isTrue();
    assertThat(takeScreenShotRunnable.bitmap).isNotNull();
  }

  /*@Test(expected = Falcon.UnableToTakeScreenshotException.class)
  public void crashesWithUnableToTakeScreenshotExceptionWhenWrongView() throws Exception {
    SampleActivity activity = activityRule.getActivity();

    View view = Falcon.getRootViews(activity).get(0).get_view().findViewById(android.R.id.content);

    Field childrenField = Falcon.INSTANCE.findField("mChildren", ViewGroup.class);
    childrenField.setAccessible(true);
    Object children = childrenField.get(view);
    childrenField.set(view, null);

    try {
      Falcon.INSTANCE.takeScreenshotBitmap(activity);
    } finally {
      childrenField.set(view, children);
    }
  }*/

  static class TakeScreenShotRunnable implements Runnable {
    private final Activity activity;
    private final CountDownLatch latch;

    Bitmap bitmap;

    TakeScreenShotRunnable(Activity activity, CountDownLatch latch) {
      this.activity = activity;
      this.latch = latch;
    }

    @Override
    public void run() {
      try {
        bitmap = Falcon.takeScreenshotBitmap(activity);
      } finally {
        latch.countDown();
      }
    }
  }
}
