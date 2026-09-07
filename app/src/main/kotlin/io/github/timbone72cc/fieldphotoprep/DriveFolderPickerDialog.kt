package io.github.timbone72cc.fieldphotoprep

import android.app.Activity
import android.app.AlertDialog
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.util.concurrent.ExecutorService

class DriveFolderPickerDialog(
    private val activity: Activity,
    private val driveApiClient: DriveApiClient,
    private val executor: ExecutorService,
    private val accessToken: String,
    private val onFolderSelected: (DriveFolder) -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    private val stack = mutableListOf(DriveFolder(id = "root", name = "My Drive"))
    private val folders = mutableListOf<DriveFolder>()
    private lateinit var dialog: AlertDialog
    private lateinit var pathText: TextView
    private lateinit var statusText: TextView
    private lateinit var listView: ListView
    private lateinit var upButton: Button
    private lateinit var useButton: Button
    private lateinit var adapter: ArrayAdapter<String>

    fun show() {
        pathText = TextView(activity).apply {
            textSize = 18f
        }
        statusText = TextView(activity)
        listView = ListView(activity)
        upButton = Button(activity).apply { text = "Up" }
        useButton = Button(activity).apply { text = "Use This Folder" }

        adapter = ArrayAdapter(
            activity,
            android.R.layout.simple_list_item_1,
            mutableListOf<String>(),
        )
        listView.adapter = adapter

        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(
                upButton,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
            addView(
                useButton,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(4))
            addView(
                pathText,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            addView(
                statusText,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            addView(
                listView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                ),
            )
            addView(buttonRow)
        }

        dialog = AlertDialog.Builder(activity)
            .setTitle("Choose Master Folder")
            .setView(content)
            .setNegativeButton("Cancel", null)
            .create()

        upButton.setOnClickListener {
            if (stack.size > 1) {
                stack.removeAt(stack.lastIndex)
                loadCurrentFolder()
            }
        }
        useButton.setOnClickListener {
            onFolderSelected(stack.last())
            dialog.dismiss()
        }
        listView.setOnItemClickListener { _, _, position, _ ->
            val selected = folders[position]
            stack += selected
            loadCurrentFolder()
        }

        dialog.setOnShowListener { loadCurrentFolder() }
        dialog.show()
    }

    private fun loadCurrentFolder() {
        val current = stack.last()
        pathText.text = stack.joinToString(" / ") { it.name }
        statusText.text = "Loading folders…"
        upButton.isEnabled = stack.size > 1
        useButton.isEnabled = false
        folders.clear()
        adapter.clear()

        executor.execute {
            try {
                val loaded = driveApiClient.listFolders(accessToken, current.id)
                activity.runOnUiThread {
                    if (!dialog.isShowing) return@runOnUiThread
                    folders += loaded
                    adapter.clear()
                    adapter.addAll(loaded.map { folderLabel(it) })
                    adapter.notifyDataSetChanged()
                    statusText.text = if (loaded.isEmpty()) {
                        "No folders inside this folder."
                    } else {
                        "Tap a folder to open it."
                    }
                    useButton.isEnabled = true
                }
            } catch (error: Throwable) {
                activity.runOnUiThread {
                    if (!dialog.isShowing) return@runOnUiThread
                    statusText.text = "Could not read this folder."
                    useButton.isEnabled = false
                    onError(error)
                }
            }
        }
    }

    private fun folderLabel(folder: DriveFolder): String {
        val shortId = if (folder.id.length <= 10) folder.id else "${folder.id.take(10)}…"
        return "${folder.name}\nID: $shortId"
    }

    private fun dp(value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
