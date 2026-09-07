package io.github.timbone72cc.fieldphotoprep

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.ApiException
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val driveApiClient = DriveApiClient()

    private lateinit var authorizationManager: DriveAuthorizationManager
    private lateinit var masterFolderStore: MasterFolderStore
    private lateinit var statusText: TextView
    private lateinit var masterText: TextView
    private lateinit var connectButton: Button
    private lateinit var chooseMasterButton: Button
    private lateinit var refreshButton: Button
    private lateinit var addressAdapter: ArrayAdapter<String>

    private var accessToken: String? = null

    private val authorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { activityResult ->
        if (activityResult.resultCode != Activity.RESULT_OK || activityResult.data == null) {
            setStatus("Google Drive connection was cancelled.")
            return@registerForActivityResult
        }
        try {
            handleAuthorizationResult(
                authorizationManager.resultFromIntent(activityResult.data!!),
            )
        } catch (error: ApiException) {
            setStatus("Google Drive authorization failed: ${error.statusCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authorizationManager = DriveAuthorizationManager(this)
        masterFolderStore = MasterFolderStore.from(this)
        buildUi()
        renderMasterFolder()
        updateControls()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi() {
        val title = TextView(this).apply {
            text = "Field Photo Prep"
            textSize = 26f
        }
        val phase = TextView(this).apply {
            text = "Phase 1 · Drive folders only"
            textSize = 16f
        }
        statusText = TextView(this).apply {
            text = "Connect Google Drive to begin."
        }
        connectButton = Button(this).apply {
            text = "Connect Google Drive"
            setOnClickListener { connectGoogleDrive() }
        }
        masterText = TextView(this)
        chooseMasterButton = Button(this).apply {
            text = "Choose Master Folder"
            setOnClickListener { chooseMasterFolder() }
        }
        refreshButton = Button(this).apply {
            text = "Refresh Addresses"
            setOnClickListener { refreshAddresses() }
        }

        addressAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf<String>(),
        )
        val addressList = ListView(this).apply {
            adapter = addressAdapter
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            addView(title)
            addView(phase)
            addView(statusText)
            addView(connectButton)
            addView(masterText)
            addView(chooseMasterButton)
            addView(refreshButton)
            addView(
                addressList,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                ),
            )
        }
        setContentView(layout)
    }

    private fun connectGoogleDrive() {
        connectButton.isEnabled = false
        setStatus("Opening Google account authorization…")
        authorizationManager.authorize(
            onResult = { result ->
                if (result.hasResolution()) {
                    val pendingIntent = result.pendingIntent
                    if (pendingIntent == null) {
                        connectButton.isEnabled = true
                        setStatus("Google Drive authorization did not return a usable prompt.")
                        return@authorize
                    }
                    val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    authorizationLauncher.launch(request)
                } else {
                    handleAuthorizationResult(result)
                }
            },
            onFailure = { error ->
                connectButton.isEnabled = true
                setStatus("Could not connect Google Drive: ${error.message ?: error.javaClass.simpleName}")
            },
        )
    }

    private fun handleAuthorizationResult(result: AuthorizationResult) {
        val token = result.accessToken?.takeIf { it.isNotBlank() }
        if (token == null) {
            accessToken = null
            setStatus("Google Drive did not return an access token.")
        } else {
            accessToken = token
            setStatus(
                if (masterFolderStore.load() == null) {
                    "Drive connected. Choose your master folder."
                } else {
                    "Drive connected. Refresh addresses when ready."
                },
            )
        }
        updateControls()
    }

    private fun chooseMasterFolder() {
        val token = accessToken ?: run {
            setStatus("Connect Google Drive first.")
            return
        }

        DriveFolderPickerDialog(
            activity = this,
            driveApiClient = driveApiClient,
            executor = executor,
            accessToken = token,
            onFolderSelected = { folder ->
                masterFolderStore.save(MasterFolder(folder.id, folder.name))
                renderMasterFolder()
                updateControls()
                refreshAddresses()
            },
            onError = { error -> handleDriveError(error) },
        ).show()
    }

    private fun refreshAddresses() {
        val token = accessToken ?: run {
            setStatus("Connect Google Drive first.")
            return
        }
        val masterFolder = masterFolderStore.load() ?: run {
            setStatus("Choose a master folder first.")
            return
        }

        refreshButton.isEnabled = false
        setStatus("Reading address folders from Drive…")
        executor.execute {
            try {
                val folders = driveApiClient.listFolders(token, masterFolder.id)
                runOnUiThread {
                    addressAdapter.clear()
                    addressAdapter.addAll(folders.map { it.name })
                    addressAdapter.notifyDataSetChanged()
                    setStatus(
                        if (folders.isEmpty()) {
                            "No address folders found under ${masterFolder.name}."
                        } else {
                            "Found ${folders.size} address folder${if (folders.size == 1) "" else "s"}."
                        },
                    )
                    updateControls()
                }
            } catch (error: Throwable) {
                runOnUiThread {
                    handleDriveError(error)
                    updateControls()
                }
            }
        }
    }

    private fun handleDriveError(error: Throwable) {
        if (error is DriveApiException && error.statusCode == 401) {
            accessToken = null
            setStatus("Drive authorization expired. Reconnect; your saved master folder is unchanged.")
        } else {
            setStatus("Drive read failed: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun renderMasterFolder() {
        val folder = masterFolderStore.load()
        masterText.text = if (folder == null) {
            "Master folder: Not selected"
        } else {
            "Master folder: ${folder.name}\nDrive ID: ${folder.id}"
        }
    }

    private fun updateControls() {
        val connected = !accessToken.isNullOrBlank()
        connectButton.isEnabled = true
        connectButton.text = if (connected) "Choose Google Account Again" else "Connect Google Drive"
        chooseMasterButton.isEnabled = connected
        refreshButton.isEnabled = connected && masterFolderStore.load() != null
    }

    private fun setStatus(message: String) {
        statusText.text = message
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
