package com.userstar.phonekeyblelockdemokotlin

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcV
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.userstar.phonekeyblelockdemokotlin.timber.ReleaseTree
import com.userstar.phonekeyblelockdemokotlin.timber.ThreadIncludedDebugTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (BuildConfig.DEBUG) {
            Timber.plant(ThreadIncludedDebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        runUpdate()
    }

    override fun onResume() {
        super.onResume()
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(
                this,
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    0
                ),
                arrayOf(
                    IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
                ),
                null
            )
        } else {
            Timber.w("Can't no get NFC adapter")
            Toast.makeText(this, "This lock does not support NFC", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val nfcVTag = NfcV.get(intent.getParcelableExtra<Parcelable>(NfcAdapter.EXTRA_TAG) as Tag)
        EventBus.getDefault().post(nfcVTag)
    }

    private fun runUpdate() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + "OQRScanner" + ".apk")
        Updater(this).apply {
            auto(file, object : ProgressListener {
                lateinit var dialog: AlertDialog
                override fun onDownloading(percentage: Long) {
                    GlobalScope.launch(Dispatchers.Main) {
                        dialog = AlertDialog.Builder(this@MainActivity)
                            .setView(R.layout.progress_dialog)
                            .setCancelable(false)
                            .show()
                    }
                }

                override fun onDone() {

                }

                override fun onFinished(file: File) {
                    dialog.dismiss()
                    startInstallIntent(this@MainActivity, file)
                }
            })
        }
    }
}