package com.mpt.gabut

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mpt.gabut.ui.theme.GabutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Firebase.firestore
        setContent {
            GabutTheme {
                val state = remember {
                    mutableStateListOf<Notification>()
                }
                val listStatus = remember {
                    mutableStateListOf<Status>()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    LaunchedEffect(key1 = true, block = {
                        db.collection("notification").orderBy("time", Query.Direction.DESCENDING)
                            .addSnapshotListener { snapshot, e ->
                                if (e != null) {
                                    Log.w(TAG, "Listen failed.", e)
                                    return@addSnapshotListener
                                }
                                if (snapshot != null) {
                                    Log.e("tag,", "hehe")
                                    state.clear()
                                    state.addAll(snapshot.map { item ->
                                        Notification(
                                            item.id,
                                            item.data["title"].toString(),
                                            item.data["body"].toString()
                                        )
                                    })
                                }
                            }

                        db.collection("status").document("first").addSnapshotListener { value, _ ->
                            listStatus.clear()
                            listStatus.add(Status("High Voltage Trip",
                                value?.data?.get("hvt").toString() == "1"))
                            listStatus.add(Status("Low Voltage Trip",
                                value?.data?.get("lvt").toString() == "1"))
                            listStatus.add(Status("Anomali",
                                value?.data?.get("anomali").toString() == "1"))
                        }
                    })


                    Column {
                        Text(text = "Status",
                            modifier = Modifier
                                .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp))
                        LazyRow {
                            items(listStatus.size) {
                                CardStatus(listStatus[it])
                            }
                        }

                        Text(
                            text = "Notification",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        if (state.size > 0) {
                            LazyColumn {
                                items(state.size) {
                                    CardNotification(state[it])
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "Tidak ada data")
                            }
                        }
                    }

                }
            }
        }
        askNotificationPermission()
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun CardNotification(notification: Notification) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Text(
            text = notification.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
        )
        Text(
            text = notification.body,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun CardStatus(status: Status) {
    val color = if (status.isError) Color.Red else Color.Cyan
    val textColor = if (status.isError) Color.White else Color.Black

    Card(
        modifier = Modifier
            .padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Text(text = status.name, Modifier.padding(8.dp), color = textColor, fontSize = 16.sp)
    }
}

data class Status(val name: String, val isError: Boolean )
data class Notification(val id: String, val title: String, val body: String)
