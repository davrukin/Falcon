package com.jraska.falcon.sample

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.ListPopupWindow
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnLongClick
import com.jraska.falcon.Falcon

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

open class SampleActivity : AppCompatActivity() {

    //region Fields

    @BindView(R.id.toolbar)
    internal var toolbar: Toolbar? = null
    @BindView(R.id.countdown)
    internal var countdownText: TextView? = null

    private var _remainingSeconds: Int = 0
    private val _executorService = Executors.newSingleThreadScheduledExecutor()

    val screenshotFile: File
        get() {
            val screenshotDirectory: File
            try {
                screenshotDirectory = getScreenshotsDirectory(applicationContext)
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS", Locale.getDefault())

            val screenshotName = dateFormat.format(Date()) + ".png"
            return File(screenshotDirectory, screenshotName)
        }

    //endregion

    //region Activity overrides

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sample)
        ButterKnife.bind(this)

        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_sample, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_floating) {
            FloatingViewActivity.start(this)
            return true
        }

        return super.onOptionsItemSelected(item)
    }


    //endregion

    //region Methods

    @OnClick(R.id.show_snack)
    fun showSnackBar(view: View) {
        Snackbar.make(view, "Snackbar", Snackbar.LENGTH_LONG)
                .setAction("Screenshot") { takeScreenshot(Bitmap.CompressFormat.JPEG) }.show()
    }

    @OnClick(R.id.show_toast)
    fun showToast() {
        Toast.makeText(this, "Toast", Toast.LENGTH_LONG).show()
    }

    @OnClick(R.id.show_dialog)
    fun showDialog(v: View) {

        //show all to have possibility see it in dialog screenshot
        showToast()
        showSnackBar(v)

        val builder = AlertDialog.Builder(this@SampleActivity)
        builder.setTitle("Falcon")
        builder.setMessage("Click button below to take screenshot with dialog")
        builder.setPositiveButton("Screenshot") { dialog, _ -> takeScreenshot(Bitmap.CompressFormat.JPEG) }
        builder.show()
    }

    @OnClick(R.id.fab_screenshot)
    fun startScreenshotCountDown() {
        if (_remainingSeconds > 0) {
            return
        }

        _remainingSeconds = 3
        updateRemainingSeconds()

        val counterCommand = object : Runnable {
            override fun run() {
                if (Looper.getMainLooper() != Looper.myLooper()) {
                    runOnUiThread(this)
                    return
                }

                _remainingSeconds--
                updateRemainingSeconds()

                if (_remainingSeconds > 0) {
                    scheduleInSecond(this)
                } else {
                    // post to see update of screen before screenshot freezes screen
                    countdownText!!.post { takeScreenshot(Bitmap.CompressFormat.JPEG) }
                }
            }
        }

        scheduleInSecond(counterCommand)
    }

    @OnLongClick(R.id.toolbar)
    fun showExtraActivity(): Boolean {
        startActivity(Intent(this, SampleActivity::class.java))
        return true
    }

    private fun scheduleInSecond(command: Runnable) {
        _executorService.schedule(command, 1, TimeUnit.SECONDS)
    }

    private fun updateRemainingSeconds() {
        if (_remainingSeconds <= 0) {
            countdownText!!.visibility = View.GONE
        } else {
            countdownText!!.visibility = View.VISIBLE
            val countDownText = getString(R.string.screenshot_in, _remainingSeconds)
            countdownText!!.text = countDownText
        }
    }

    fun takeScreenshot(type: Bitmap.CompressFormat) {
        val screenshotFile = screenshotFile

        Falcon.takeScreenshot(this, screenshotFile, type)

        val message = "Screenshot captured to " + screenshotFile.absolutePath
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        val uri = Uri.fromFile(screenshotFile)
        val scanFileIntent = Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
        sendBroadcast(scanFileIntent)
    }

    @OnClick(R.id.show_popup)
    fun showPopup() {
        val listPopupWindow = ListPopupWindow(this, null)

        val data = arrayOf("Item 1", "Item 2", "Item 3")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, data)
        listPopupWindow.setAdapter(adapter)

        listPopupWindow.anchorView = findViewById(R.id.show_popup)
        listPopupWindow.show()
    }

    @Throws(IllegalAccessException::class)
    private fun getScreenshotsDirectory(context: Context): File {
        val dirName = "screenshots_" + context.packageName

        val rootDir: File?

        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            rootDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        } else {
            rootDir = context.getDir("screens", Context.MODE_PRIVATE)
        }

        val directory = File(rootDir, dirName)

        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw IllegalAccessException("Unable to create screenshot directory " + directory.absolutePath)
            }
        }

        return directory
    }

    //endregion
}
