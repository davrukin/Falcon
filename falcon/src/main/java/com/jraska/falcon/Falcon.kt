package com.jraska.falcon

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager.LayoutParams
import android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
import java.io.*
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

/**
 * Utility class to take screenshots of activity screen
 */
object Falcon {
  //region Constants

  private const val TAG = "Falcon"

  //endregion

  //region Public API

  /**
   * Takes screenshot of provided activity and saves it to provided file.
   * File content will be overwritten if there is already some content.
   *
   * @param activity Activity of which the screenshot will be taken.
   * @param toFile   File where the screenshot will be saved.
   * If there is some content it will be overwritten
   * @throws UnableToTakeScreenshotException When there is unexpected error during taking screenshot
   */
  @JvmStatic fun takeScreenshot(activity: Activity?, toFile: File?, type: Bitmap.CompressFormat) {
    if (activity == null) {
      throw IllegalArgumentException("Parameter activity cannot be null.")
    }

    if (toFile == null) {
      throw IllegalArgumentException("Parameter toFile cannot be null.")
    }

    var bitmap: Bitmap? = null
    try {
      bitmap = takeBitmapUnchecked(activity)

      when(type) {
        Bitmap.CompressFormat.JPEG -> writeBitmapJPEG(bitmap, toFile)
        Bitmap.CompressFormat.PNG -> writeBitmapPNG(bitmap, toFile)
        Bitmap.CompressFormat.WEBP -> writeBitmapWEBP(bitmap, toFile)
      }
    } catch (e: Exception) {
      val message = ("Unable to take screenshot to file " + toFile.absolutePath
        + " of activity " + activity.javaClass.name)

      Log.e(TAG, message, e)
      throw UnableToTakeScreenshotException(message, e)
    } finally {
      bitmap?.recycle()
    }

    Log.d(TAG, "Screenshot captured to " + toFile.absolutePath)
  }

  /**
   * Takes screenshot of provided activity and puts it into bitmap.
   *
   * @param activity Activity of which the screenshot will be taken.
   * @return Bitmap of what is displayed in activity.
   * @throws UnableToTakeScreenshotException When there is unexpected error during taking screenshot
   */
  @JvmStatic fun takeScreenshotBitmap(activity: Activity?): Bitmap {
    if (activity == null) {
      throw IllegalArgumentException("Parameter activity cannot be null.")
    }

    try {
      return takeBitmapUnchecked(activity)
    } catch (e: Exception) {
      val message = "Unable to take screenshot to bitmap of activity " + activity.javaClass.getName()

      Log.e(TAG, message, e)
      throw UnableToTakeScreenshotException(message, e)
    }

  }

  //endregion

  //region Methods

  @Throws(InterruptedException::class)
  private fun takeBitmapUnchecked(activity: Activity): Bitmap {
    val viewRoots = getRootViews(activity)
    if (viewRoots.isEmpty()) {
      throw UnableToTakeScreenshotException("Unable to capture any view data in $activity")
    }

    var maxWidth = Integer.MIN_VALUE
    var maxHeight = Integer.MIN_VALUE

    for (viewRoot in viewRoots) {
      if (viewRoot._winFrame.right > maxWidth) {
        maxWidth = viewRoot._winFrame.right
      }

      if (viewRoot._winFrame.bottom > maxHeight) {
        maxHeight = viewRoot._winFrame.bottom
      }
    }

    val bitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.RGB_565)

    // We need to do it in main thread
    if (Looper.myLooper() == Looper.getMainLooper()) {
      drawRootsToBitmap(viewRoots, bitmap)
    } else {
      drawRootsToBitmapOtherThread(activity, viewRoots, bitmap)
    }

    return bitmap
  }

  @Throws(InterruptedException::class)
  private fun drawRootsToBitmapOtherThread(activity: Activity, viewRoots: List<ViewRootData>,
                                           bitmap: Bitmap) {
    val errorInMainThread = AtomicReference<Exception>()
    val latch = CountDownLatch(1)
    activity.runOnUiThread {
      try {
        drawRootsToBitmap(viewRoots, bitmap)
      } catch (ex: Exception) {
        errorInMainThread.set(ex)
      } finally {
        latch.countDown()
      }
    }

    latch.await()

    val exception = errorInMainThread.get()
    if (exception != null) {
      throw UnableToTakeScreenshotException(exception)
    }
  }

  private fun drawRootsToBitmap(viewRoots: List<ViewRootData>, bitmap: Bitmap) {
    for (rootData in viewRoots) {
      drawRootToBitmap(rootData, bitmap)
    }
  }

  private fun drawRootToBitmap(config: ViewRootData, bitmap: Bitmap) {
    // now only dim supported
    if (config._layoutParams.flags and FLAG_DIM_BEHIND == FLAG_DIM_BEHIND) {
      val dimCanvas = Canvas(bitmap)

      val alpha = (255 * config._layoutParams.dimAmount).toInt()
      dimCanvas.drawARGB(alpha, 0, 0, 0)
    }

    val canvas = Canvas(bitmap)
    canvas.translate(config._winFrame.left.toFloat(), config._winFrame.top.toFloat())
    config._view.draw(canvas)
  }

  @Throws(IOException::class)
  private fun writeBitmapPNG(bitmap: Bitmap, toFile: File) {
    var outputStream: OutputStream? = null
    try {
      outputStream = BufferedOutputStream(FileOutputStream(toFile))
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    } finally {
      closeQuietly(outputStream)
    }
  }

  @Throws(IOException::class)
  private fun writeBitmapJPEG(bitmap: Bitmap, toFile: File) {
    var outputStream: OutputStream? = null
    try {
      outputStream = BufferedOutputStream(FileOutputStream(toFile))
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    } finally {
      closeQuietly(outputStream)
    }
  }

  @Throws(IOException::class)
  private fun writeBitmapWEBP(bitmap: Bitmap, toFile: File) {
    var outputStream: OutputStream? = null
    try {
      outputStream = BufferedOutputStream(FileOutputStream(toFile))
      bitmap.compress(Bitmap.CompressFormat.WEBP, 100, outputStream)
    } finally {
      closeQuietly(outputStream)
    }
  }

  private fun closeQuietly(closable: Closeable?) {
    if (closable != null) {
      try {
        closable.close()
      } catch (e: IOException) {
        // Do nothing
      }

    }
  }

  private fun getRootViews(activity: Activity): List<ViewRootData> {
    val globalWindowManager: Any = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
      getFieldValue("mWindowManager", activity.windowManager)
    } else {
      getFieldValue("mGlobal", activity.windowManager)
    }
    val rootObjects = getFieldValue("mRoots", globalWindowManager)
    val paramsObject = getFieldValue("mParams", globalWindowManager)

    val roots: Array<Any>?
    val params: Array<LayoutParams>

    //  There was a change to ArrayList implementation in 4.4
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      roots = (rootObjects as List<Any>).toTypedArray()

      val paramsList = paramsObject as List<LayoutParams>
      params = paramsList.toTypedArray<LayoutParams>()
    } else {
      roots = rootObjects as Array<Any>
      params = paramsObject as Array<LayoutParams>
    }

    val rootViews = viewRootData(roots, params)
    if (rootViews.isEmpty()) {
      return emptyList()
    }

    offsetRootsTopLeft(rootViews)
    ensureDialogsAreAfterItsParentActivities(rootViews)

    return rootViews
  }

  private fun viewRootData(roots: Array<Any>, params: Array<LayoutParams>): MutableList<ViewRootData> {
    val rootViews = ArrayList<ViewRootData>()
    for (i in roots.indices) {
      val root = roots[i]

      val rootView = getFieldValue("mView", root) as View

      // fixes https://github.com/jraska/Falcon/issues/10
      if (rootView == null) {
        Log.e(TAG, "null View stored as root in Global window manager, skipping")
        continue
      }

      if (!rootView.isShown) {
        continue
      }

      val location = IntArray(2)
      rootView.getLocationOnScreen(location)

      val left = location[0]
      val top = location[1]
      val area = Rect(left, top, left + rootView.width, top + rootView.height)

      rootViews.add(ViewRootData(rootView, area, params[i]))
    }

    return rootViews
  }

  private fun offsetRootsTopLeft(rootViews: List<ViewRootData>) {
    var minTop = Integer.MAX_VALUE
    var minLeft = Integer.MAX_VALUE
    for (rootView in rootViews) {
      if (rootView._winFrame.top < minTop) {
        minTop = rootView._winFrame.top
      }

      if (rootView._winFrame.left < minLeft) {
        minLeft = rootView._winFrame.left
      }
    }

    for (rootView in rootViews) {
      rootView._winFrame.offset(-minLeft, -minTop)
    }
  }

  // This fixes issue #11. It is not perfect solution and maybe there is another case
  // of different type of view, but it works for most common case of dialogs.
  private fun ensureDialogsAreAfterItsParentActivities(viewRoots: MutableList<ViewRootData>) {
    if (viewRoots.size <= 1) {
      return
    }

    for (dialogIndex in 0 until viewRoots.size - 1) {
      val viewRoot = viewRoots[dialogIndex]
      if (!viewRoot.isDialogType) {
        continue
      }

      if (viewRoot.windowToken == null) {
        // make sure we will never compare null == null
        return
      }

      for (parentIndex in dialogIndex + 1 until viewRoots.size) {
        val possibleParent = viewRoots[parentIndex]
        if (possibleParent.isActivityType && possibleParent.windowToken === viewRoot.windowToken) {
          viewRoots.remove(possibleParent)
          viewRoots.add(dialogIndex, possibleParent)

          break
        }
      }
    }
  }

  private fun getFieldValue(fieldName: String, target: Any): Any {
    try {
      return getFieldValueUnchecked(fieldName, target)
    } catch (e: Exception) {
      throw UnableToTakeScreenshotException(e)
    }

  }

  @Throws(NoSuchFieldException::class, IllegalAccessException::class)
  private fun getFieldValueUnchecked(fieldName: String, target: Any): Any {
    val field = findField(fieldName, target.javaClass)

    field.isAccessible = true
    return field.get(target)
  }

  @Throws(NoSuchFieldException::class)
  internal fun findField(name: String, klass: Class<*>): Field {
    var currentClass: Class<*>? = klass
    while (currentClass != Any::class.java) {
      for (field in currentClass!!.declaredFields) {
        if (name == field.name) {
          return field
        }
      }

      currentClass = currentClass.superclass
    }

    throw NoSuchFieldException("Field $name not found for class $klass")
  }

  //endregion

  //region Nested classes

  /**
   * Custom exception thrown if there is some exception thrown during
   * screenshot capturing to enable better client code exception handling.
   */
  class UnableToTakeScreenshotException : RuntimeException {

    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
    constructor(detailMessage: String, exception: Exception) : super(detailMessage, exception)

    /**
     * Method to avoid multiple wrapping. If there is already our exception,
     * just wrap the cause again
     */
    private fun extractException(e: Exception): Throwable {
      return if (e is UnableToTakeScreenshotException) {
        e.cause!!
      } else e
    }


  }

  internal class ViewRootData(val _view: View, internal val _winFrame: Rect, internal val _layoutParams: LayoutParams) {

    val isDialogType: Boolean
      get() = _layoutParams.type == LayoutParams.TYPE_APPLICATION

    val isActivityType: Boolean
      get() = _layoutParams.type == LayoutParams.TYPE_BASE_APPLICATION

    val windowToken: IBinder?
      get() = _layoutParams.token

    fun context(): Context {
      return _view.context
    }
  }

  //endregion
}//endregion
//region Constructors
// No instances
