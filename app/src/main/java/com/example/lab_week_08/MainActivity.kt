package com.example.lab_week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    private val workManager = WorkManager.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ✅ Meminta izin notifikasi (khusus Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // ✅ Membuat constraint: hanya jalan jika ada koneksi internet
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        // ✅ Membuat WorkRequest untuk FirstWorker
        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        // ✅ Membuat WorkRequest untuk SecondWorker
        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        // ✅ Membuat WorkRequest untuk ThirdWorker
        val thirdRequest = OneTimeWorkRequest.Builder(ThirdWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(ThirdWorker.INPUT_DATA_ID, id))
            .build()

        // ✅ Urutan eksekusi:
        // FirstWorker → SecondWorker → NotificationService → ThirdWorker → SecondNotificationService
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .then(thirdRequest)
            .enqueue()

        // ✅ Observasi FirstWorker selesai
        workManager.getWorkInfoByIdLiveData(firstRequest.id).observe(this) { info ->
            if (info.state.isFinished) {
                showResult("First process is done")
            }
        }

        // ✅ Observasi SecondWorker selesai → panggil NotificationService
        workManager.getWorkInfoByIdLiveData(secondRequest.id).observe(this) { info ->
            if (info.state.isFinished) {
                showResult("Second process is done")
                launchNotificationService()
            }
        }

        // ✅ Observasi ThirdWorker selesai → panggil SecondNotificationService
        workManager.getWorkInfoByIdLiveData(thirdRequest.id).observe(this) { info ->
            if (info.state.isFinished) {
                showResult("Third process is done")
                launchSecondNotificationService()
            }
        }
    }

    // 🔧 Fungsi untuk membangun data input worker
    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    // 🔧 Menampilkan toast hasil proses
    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // 🚀 Meluncurkan NotificationService (service pertama)
    private fun launchNotificationService() {
        NotificationService.trackingCompletion.observe(this) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
        }

        val serviceIntent = Intent(this, NotificationService::class.java).apply {
            putExtra(EXTRA_ID, "001")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    // 🚀 Meluncurkan SecondNotificationService (service kedua)
    private fun launchSecondNotificationService() {
        SecondNotificationService.trackingCompletion.observe(this) { Id ->
            showResult("Second Notification Service for Channel ID $Id is done!")
        }

        val serviceIntent = Intent(this, SecondNotificationService::class.java).apply {
            putExtra(EXTRA_ID, "002")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    companion object {
        const val EXTRA_ID = "Id"
    }
}
