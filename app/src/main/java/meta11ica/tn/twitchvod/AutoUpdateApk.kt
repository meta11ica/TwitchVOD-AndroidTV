//
//	Copyright (c) 2012 lenik terenin
//
//	Licensed under the Apache License, Version 2.0 (the "License");
//	you may not use this file except in compliance with the License.
//	You may obtain a copy of the License at
//
//		http://www.apache.org/licenses/LICENSE-2.0
//
//	Unless required by applicable law or agreed to in writing, software
//	distributed under the License is distributed on an "AS IS" BASIS,
//	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//	See the License for the specific language governing permissions and
//	limitations under the License.
package meta11ica.tn.twitchvod

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Parcelable
import android.provider.Settings.Secure
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import khttp.get
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Calendar
import java.util.LinkedList
import java.util.Observable
import java.util.zip.CRC32
import java.util.zip.Checksum
import khttp.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class AutoUpdateApk : Observable {
    /**
     * This class is supposed to be instantiated in any of your activities or,
     * better yet, in Application subclass. Something along the lines of:
     *
     * <pre>
     * private AutoUpdateApk aua;	<-- you need to add this line of code
     *
     * public void onCreate(Bundle savedInstanceState) {
     * super.onCreate(savedInstanceState);
     * setContentView(R.layout.main);
     *
     * aua = new AutoUpdateApk(getApplicationContext());	<-- and add this line too
    </pre> *
     *
     * @param ctx
     * parent activity context
     * @param server
     * server name and port (eg. myserver.domain.com:8123 ). Should
     * be null when using absolutes apiPath.
     */
    constructor(ctx: Context, apiPath: String, server: String?) {
        setupVariables(ctx)
        this.server = server
        this.apiPath = apiPath
    }

    constructor(ctx: Context, apiURL: String) {
        setupVariables(ctx)
        server = null
        apiPath = apiURL
    }

    /**
     * set update interval (in milliseconds).
     *
     * there are nice constants in this file: MINUTES, HOURS, DAYS you may use
     * them to specify update interval like: 5 * DAYS
     *
     * please, don't specify update interval below 1 hour, this might be
     * considered annoying behaviour and result in service suspension
     */
    fun setUpdateInterval(interval: Long) {
        updateInterval = interval

    }

    fun checkUpdatesManually() {
        checkUpdates(false) // force update check
    }

    fun clearSchedule() {
        schedule.clear()
    }

    fun addSchedule(start: Int, end: Int) {
        schedule.add(ScheduleEntry(start, end))
    }

    protected val server: String?
    protected val apiPath: String

    // 3-4 hours in dev.mode, 1-2 days for stable releases
    private var updateInterval = 3 * HOURS // how often to check
    var result = arrayOfNulls<String>(3)

    private inner class ScheduleEntry(var start: Int, var end: Int)

    private val periodicUpdate: Runnable = object : Runnable {
        override fun run() {
            checkUpdates(false)
            updateHandler.removeCallbacks(this) // remove whatever
            // others may have
            // posted
            updateHandler.postDelayed(this, WAKEUP_INTERVAL)
        }
    }
    private val connectivity_receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val currentNetworkInfo = intent
                .getParcelableExtra<Parcelable>(ConnectivityManager.EXTRA_NETWORK_INFO) as NetworkInfo?

            // do application-specific task(s) based on the current network
            // state, such
            // as enabling queuing of HTTP requests when currentNetworkInfo is
            // connected etc.

            if (currentNetworkInfo != null) {
                if (currentNetworkInfo.isConnected
                ) {
                    checkUpdates(true)
                    updateHandler.postDelayed(periodicUpdate, updateInterval)
                } else {
                    updateHandler.removeCallbacks(periodicUpdate) // no network
                    // anyway
                }
            }
        }
    }

    private fun setupVariables(ctx: Context) {
        context = ctx
        packageName = context!!.packageName
        preferences = context!!.getSharedPreferences(
            packageName + "_" + TAG,
            Context.MODE_PRIVATE
        )

        device_id = crc32(
            Secure.getString(
                context!!.contentResolver,
                Secure.ANDROID_ID
            )
        )
        last_update = preferences.getLong("last_update", 0)
        NOTIFICATION_ID += crc32(packageName)

        val appinfo = context!!.applicationInfo
        if (appinfo.icon != 0) {
            appIcon = appinfo.icon
        } else {
            Log_w(TAG, "unable to find application icon")
        }

        if (appinfo.labelRes != 0) {
            appName = context!!.getString(appinfo.labelRes)
        } else {
            Log_w(TAG, "unable to find application label")
        }

        if (File(appinfo.sourceDir).lastModified() > preferences.getLong(
                MD5_TIME, 0
            )
        ) {
            preferences.edit().putString(MD5_KEY, MD5Hex(appinfo.sourceDir))
                .commit()
            preferences.edit().putLong(MD5_TIME, System.currentTimeMillis())
                .commit()
            val update_file = preferences.getString(UPDATE_FILE, "")
            if (update_file!!.length > 0) {
                if (File(
                        context!!.filesDir.absolutePath + "/"
                                + update_file
                    ).delete()
                ) {
                    preferences.edit().remove(UPDATE_FILE)
                        .remove(SILENT_FAILED).commit()
                }
            }
        }

        raise_notification()
        if (haveInternetPermissions()) {
            context!!.registerReceiver(
                connectivity_receiver, IntentFilter(
                    ConnectivityManager.CONNECTIVITY_ACTION
                )
            )
        }

    }

    private fun checkSchedule(): Boolean {
        if (schedule.size == 0) return true // empty schedule always fits
        val now = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        for (e: ScheduleEntry in schedule) {
            if (now >= e.start && now < e.end) return true
        }
        return false
    }

    private inner class CheckUpdateTask() :
        AsyncTask<Void?, Void?, Array<String?>?>() {
        private var get: String? = null
        private val retrieved: List<String> = LinkedList()

        init {
            Log_v(TAG, apiPath)
            if (server != null) {
                get = get(server + apiPath).text
            } else {
                get = get(apiPath).text
            }
        }

        @SuppressLint("ResourceType")
        override fun doInBackground(vararg v: Void?): Array<String?>? {
            val start = System.currentTimeMillis()
            try {
                val response: String
                if (server != null) {
                    response = get(server + apiPath).text
                } else {
                    response = get(apiPath).text
                }
                Log.v(TAG, "got a reply from update server")
                val lastversion = JSONObject(response).getJSONArray("elements").getJSONObject(0)
                    .getInt("versionCode")
                if (lastversion > context!!.resources.getInteger(R.integer.app_version_code)) {
                    result[0] = "have update"
                    result[1] = ("$lastversion.apk").toString()
                    val fileName = "app-release.apk"
                    val outputFile = File(
                        context!!.filesDir,
                        result[1]
                    )
                    val savePath = "app-release.apk"
                    val link =
                        "https://raw.githubusercontent.com/meta11ica/TwitchVOD-AndroidTV/master/app/release/app-release.apk"
                    result[2] = "app-release.apk"
                    val file = File(context!!.filesDir, result[1])
                    if(file.exists()) {
                        val url = URL(link)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.connect()
                        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                            Log.e(
                                TAG,
                                "Server returned HTTP " + connection.responseCode + " " + connection.responseMessage
                            )
                        }
                        val inputStream = connection.inputStream
                        val outputStream = FileOutputStream(outputFile)
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        outputStream.close()
                        inputStream.close()
                        Log.v(TAG, "File downloaded successfully.")
                        setChanged()
                        notifyObservers(AUTOUPDATE_GOT_UPDATE)
                    }
                    return result
                } else {
                    setChanged()
                    notifyObservers(AUTOUPDATE_NO_UPDATE)
                    Log.v(TAG, "no update available")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val elapsed = System.currentTimeMillis() - start
                Log.v(TAG, "update check finished in " + elapsed + "ms")
            }

            result[0] = "no update"
            result[1] = "no file"
            result[2] = "no file"
            return result
        }


        override fun onPreExecute() {
            // show progress bar or something
            Log.v(TAG, "checking if there's update on the server")
        }

        override fun onPostExecute(result: Array<String?>?) {
            // kill progress bar here

            if (result != null) {
                val editor = preferences?.edit()
                if (result[0].equals("have update", ignoreCase = true)) {
                    editor?.putString(
                        UPDATE_FILE,
                        result[1]
                    )
                    val update_file_path = context!!.filesDir
                        .absolutePath + "/" + result[1]
                    editor
                        ?.putString(MD5_KEY, MD5Hex(update_file_path))
                    editor
                        ?.putLong(MD5_TIME, System.currentTimeMillis())
                }
editor?.commit()
                raise_notification()
            } else {
                Log.v(TAG, "no reply from update server")
            }
        }
    }

    private fun checkUpdates(forced: Boolean) {
        val now = System.currentTimeMillis()
        if (forced || (last_update + updateInterval) < now && checkSchedule()) {
            CheckUpdateTask().execute()
            last_update = System.currentTimeMillis()
            preferences?.edit()?.putLong(LAST_UPDATE_KEY, last_update)?.commit()
            setChanged()
            this.notifyObservers(AUTOUPDATE_CHECKING)
        }
    }

    protected fun raise_notification() {
        val ns = Context.NOTIFICATION_SERVICE
        val nm = context
            ?.getSystemService(ns) as NotificationManager
        nm.cancelAll()
        val update_file = preferences?.getString(UPDATE_FILE, "")
        if (update_file!!.length > 0) {
            setChanged()
            notifyObservers(AUTOUPDATE_HAVE_UPDATE)
            val file = File(context!!.filesDir, update_file)
            val contentTitle: CharSequence = appName + " update available"
            val contentText: CharSequence = "Select to install"
            val contentUri = FileProvider.getUriForFile(
                (context)!!,
                BuildConfig.APPLICATION_ID + ".provider", file
            )
            val notificationIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            notificationIntent.setDataAndType(contentUri, ANDROID_PACKAGE)
            notificationIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            notificationIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
            notificationIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            // raise notification
            var notification = Notification(
                appIcon, (appName
                        + " update"), System.currentTimeMillis()
            )
            notification.flags = notification.flags or NOTIFICATION_FLAGS
            val pendingIntent = PendingIntent.getActivity(
                context, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE
            )
            createNotificationChannel()
            val builder = NotificationCompat.Builder(
                (context)!!, context!!.getString(R.string.update_channel_name)
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            notification = builder.build()
            nm.notify(NOTIFICATION_ID, notification)
        } else {
            nm.cancel(NOTIFICATION_ID)
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = context!!.getString(R.string.update_channel_name)
            val description = context!!.getString(R.string.update_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(name as String, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this.
            ContextCompat.getSystemService(
                (context)!!,
                NotificationManager::class.java
            )!!.createNotificationChannel(channel)
        }
    }

    private fun MD5Hex(filename: String): String {
        val BUFFER_SIZE = 8192
        val buf = ByteArray(BUFFER_SIZE)
        var length: Int
        try {
            val fis = FileInputStream(filename)
            val bis = BufferedInputStream(fis)
            val md = MessageDigest.getInstance("MD5")
            while ((bis.read(buf).also { length = it }) != -1) {
                md.update(buf, 0, length)
            }
            bis.close()
            val array = md.digest()
            val sb = StringBuffer()
            for (i in array.indices) {
                sb.append(
                    Integer.toHexString((array[i].toInt() and 0xFF) or 0x100)
                        .substring(1, 3)
                )
            }
            Log.v(TAG, "md5sum: $sb")
            return sb.toString()
        } catch (e: Exception) {
            e.message?.let { Log.e(TAG, it) }
        }
        return "md5bad"
    }

    private fun haveInternetPermissions(): Boolean {
        val required_perms: MutableSet<String> = HashSet()
        required_perms.add("android.permission.INTERNET")
        required_perms.add("android.permission.ACCESS_NETWORK_STATE")
        val pm = context!!.packageManager
        val packageName = context!!.packageName
        val flags = PackageManager.GET_PERMISSIONS
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = pm.getPackageInfo(packageName, flags)
            versionCode = packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.message?.let { Log.e(TAG, it) }
        }
        if (packageInfo!!.requestedPermissions != null) {
            for (p: String in packageInfo.requestedPermissions) {
                // Log_v(TAG, "permission: " + p.toString());
                required_perms.remove(p)
            }
            if (required_perms.size == 0) {
                return true // permissions are in order
            }
            // something is missing
            for (p: String in required_perms) {
                Log.e(
                    TAG,
                    "required permission missing: $p"
                )
            }
        }
        Log.e(
            TAG,
            "INTERNET/WIFI access required, but no permissions are found in Manifest.xml"
        )
        return false
    }

    // logging facilities to enable easy overriding. thanks, Dan!
    //
    protected fun Log_v(tag: String?, message: String?, e: Throwable? = null) {
        log("v", tag, message, e)
    }

    protected fun Log_d(tag: String?, message: String?, e: Throwable? = null) {
        log("d", tag, message, e)
    }

    protected fun Log_i(tag: String?, message: String?) {
        Log_d(tag, message, null)
    }

    protected fun Log_i(tag: String?, message: String?, e: Throwable?) {
        log("i", tag, message, e)
    }

    protected fun Log_w(tag: String?, message: String?, e: Throwable? = null) {
        log("w", tag, message, e)
    }

    protected fun Log_e(tag: String?, message: String?, e: Throwable? = null) {
        log("e", tag, message, e)
    }

    protected fun log(level: String, tag: String?, message: String?, e: Throwable?) {
        if (message == null) {
            return
        }
        if (level.equals("v", ignoreCase = true)) {
            if (e == null) Log.v(tag, message) else Log.v(tag, message, e)
        } else if (level.equals("d", ignoreCase = true)) {
            if (e == null) Log.d(tag, message) else Log.d(tag, message, e)
        } else if (level.equals("i", ignoreCase = true)) {
            if (e == null) Log.i(tag, message) else Log.i(tag, message, e)
        } else if (level.equals("w", ignoreCase = true)) {
            if (e == null) Log.w(tag, message) else Log.w(tag, message, e)
        } else {
            if (e == null) Log.e(tag, message) else Log.e(tag, message, e)
        }
    }

    companion object {
        // set icon for notification popup (default = application icon)
        //
        fun setIcon(icon: Int) {
            appIcon = icon
        }

        // set name to display in notification popup (default = application label)
        //
        fun setName(name: String?) {
            appName = name
        }

        // set Notification flags (default = Notification.FLAG_AUTO_CANCEL |
        // Notification.FLAG_NO_CLEAR)
        //
        fun setNotificationFlags(flags: Int) {
            NOTIFICATION_FLAGS = flags
        }

        // software updates will use WiFi/Ethernet only (default mode)
        //
        fun disableMobileUpdates() {
            mobile_updates = false
        }

        // software updates will use any internet connection, including mobile
        // might be a good idea to have 'unlimited' plan on your 3.75G connection
        //
        fun enableMobileUpdates() {
            mobile_updates = true
        }

        val AUTOUPDATE_CHECKING = "autoupdate_checking"
        val AUTOUPDATE_NO_UPDATE = "autoupdate_no_update"
        val AUTOUPDATE_GOT_UPDATE = "autoupdate_got_update"
        val AUTOUPDATE_HAVE_UPDATE = "autoupdate_have_update"
        val PUBLIC_API_URL = "http://www.auto-update-apk.com/check"

        //
        // ---------- everything below this line is private and does not belong to
        // the public API ----------
        //
        protected val TAG = "AutoUpdateApk"
        private val ANDROID_PACKAGE = "application/vnd.android.package-archive"
        protected var context: Context? = null
        lateinit var preferences: SharedPreferences
        private val LAST_UPDATE_KEY = "last_update"
        private var last_update: Long = 0
        private var appIcon = android.R.drawable.ic_popup_reminder
        private var versionCode = 0 // as low as it gets
        private var packageName: String? = null
        private var appName: String? = null
        private var device_id = 0
        val MINUTES = (60 * 1000).toLong()
        val HOURS = 60 * MINUTES
        val DAYS = 24 * HOURS
        private var mobile_updates = false // download updates over wifi

        // only
        private val updateHandler = Handler()
        protected val UPDATE_FILE = "update_file"
        protected val SILENT_FAILED = "silent_failed"
        private val MD5_TIME = "md5_time"
        private val MD5_KEY = "md5"
        private var NOTIFICATION_ID = -0x21524111
        private var NOTIFICATION_FLAGS = (Notification.FLAG_AUTO_CANCEL
                or Notification.FLAG_NO_CLEAR)
        private val WAKEUP_INTERVAL: Long = 500
        private val schedule: MutableList<ScheduleEntry> = ArrayList()
        private fun crc32(str: String?): Int {
            val bytes = str!!.toByteArray()
            val checksum: Checksum = CRC32()
            checksum.update(bytes, 0, bytes.size)
            return checksum.value.toInt()
        }
    }
}