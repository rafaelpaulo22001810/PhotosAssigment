/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.marsphotos.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.marsphotos.MainActivity
import com.example.marsphotos.R
import com.example.marsphotos.model.MarsPhoto
import com.example.marsphotos.model.PicsumPhoto
import com.example.marsphotos.ui.theme.MarsPhotosTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener

@Composable
fun HomeScreen(
    db: FirebaseDatabase,
    marsUiState: MarsUiState,
    picsumUiState: PicsumUiState,
    modifier: Modifier = Modifier
) {
    var marsPhoto by remember { mutableStateOf(MarsPhoto()) }
    var picsumPhoto by remember { mutableStateOf(PicsumPhoto()) }
    var rollCount by remember { mutableStateOf(0) }

    val rollRef = db.reference.child("roll")
    rollRef.get().addOnSuccessListener { dataSnapshot ->
        val rollValue = dataSnapshot.value
        if (rollValue != null) {
            if (rollValue is Long) {
                rollCount = rollValue.toInt()
            } else if (rollValue is Int) {
                rollCount = rollValue
            } else {
                println("O valor de 'roll' não é do tipo esperado (Long ou Int).")
            }
        } else {
            println("A chave 'roll' não existe no banco de dados.")
        }
    }.addOnFailureListener { exception ->
        println("Erro ao buscar o valor de 'roll': $exception")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (picsumUiState) {
            is PicsumUiState.Loading -> LoadingScreen(modifier = modifier.fillMaxSize())
            is PicsumUiState.Success -> {
                if (picsumPhoto.id == ""){
                    picsumPhoto = picsumUiState.randomPhoto
                }
                ResultScreenPicsum(
                    picsumUiState.photos, picsumPhoto, modifier = modifier.fillMaxWidth()
                )
            }

            is PicsumUiState.Error -> ErrorScreen(modifier = modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.height(32.dp))
        when (marsUiState) {
            is MarsUiState.Loading -> LoadingScreen(modifier = modifier.fillMaxSize())
            is MarsUiState.Success -> {
                if (marsPhoto.id == ""){
                    marsPhoto = marsUiState.randomPhoto
                }
                ResultScreen(
                    marsUiState.photos, marsPhoto, modifier = modifier.fillMaxWidth()
                )
            }

            is MarsUiState.Error -> ErrorScreen(modifier = modifier.fillMaxSize())
        }
        Button(
            modifier = Modifier
                .padding(16.dp)
                .size(100.dp, 50.dp),
            colors = ButtonDefaults.buttonColors(Color.Red),
            onClick = {
                if (marsUiState is MarsUiState.Success) {
                    marsPhoto = MarsPhoto()
                    marsUiState.refresh()
                }
                if (picsumUiState is PicsumUiState.Success) {
                    picsumPhoto = PicsumPhoto()
                    picsumUiState.refresh()
                }

                rollRef.setValue(rollCount + 1)

            }) {
            Text(text = stringResource(R.string.roll))
        }
        Text(text = "Roll: $rollCount")
        Row(verticalAlignment = Alignment.Bottom) {
            Button(modifier = Modifier
                .padding(16.dp)
                .size(100.dp, 50.dp),
                colors = ButtonDefaults.buttonColors(Color.Blue),
                onClick = {
                    if (marsUiState is MarsUiState.Success) {
                        val mars = db.getReference(MainActivity.MARS_CHILD)
                        mars.child(marsUiState.randomPhoto.id).setValue(marsUiState.randomPhoto)

                        val lastAdd = db.reference.child("lastAdd")
                        lastAdd.child("mars").setValue(marsUiState.randomPhoto.id)
                    }
                    if (picsumUiState is PicsumUiState.Success) {
                        val picsum = db.getReference(MainActivity.PICSUM_CHILD)
                        picsum.child(picsumUiState.randomPhoto.id)
                            .setValue(picsumUiState.randomPhoto)

                        val lastAdd = db.reference.child("lastAdd")
                        lastAdd.child("picsum").setValue(picsumUiState.randomPhoto.id)
                    }
                }) {
                Text(text = stringResource(R.string.save))
            }
            Button(modifier = Modifier
                .padding(16.dp)
                .size(100.dp, 50.dp),
                colors = ButtonDefaults.buttonColors(Color.Blue),
                onClick = {
                    val lastAdd = db.reference.child("lastAdd")
                    lastAdd.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val lastAddData = snapshot.getValue(object : GenericTypeIndicator<Map<String, String>>() {})

                            if (lastAddData != null){
                                val mars = lastAddData["mars"]
                                val picsum = lastAddData["picsum"]

                                if (mars != null){
                                    val marsReference = db.getReference(MainActivity.MARS_CHILD).child(mars)
                                    marsReference.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val marsData = snapshot.getValue(MarsPhoto::class.java)
                                            if (marsData != null){
                                                marsPhoto = marsData
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Log.w("TAG", "Failed to read value.", error.toException())
                                        }
                                    })
                                }

                                if (picsum != null){
                                    val picsumReference = db.getReference(MainActivity.PICSUM_CHILD).child(picsum)
                                    picsumReference.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val picsumData = snapshot.getValue(PicsumPhoto::class.java)
                                            if (picsumData != null){
                                                picsumPhoto = picsumData
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Log.w("TAG", "Failed to read value.", error.toException())
                                        }
                                    })
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.w("TAG", "Failed to read value.", error.toException())
                        }
                    })
                }) {
                Text(text = stringResource(R.string.load))
            }
        }
    }
}

/**
 * The home screen displaying the loading message.
 */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Image(
        modifier = modifier.size(250.dp),
        painter = painterResource(R.drawable.loading_img),
        contentDescription = stringResource(R.string.loading)
    )
}

/**
 * The home screen displaying error message with re-attempt button.
 */
@Composable
fun ErrorScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_connection_error), contentDescription = ""
        )
        Text(text = stringResource(R.string.loading_failed), modifier = Modifier.padding(16.dp))
    }
}

/**
 * ResultScreen displaying number of photos retrieved.
 */
@Composable
fun ResultScreenPicsum(photos: String, randomPhoto: PicsumPhoto, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = photos)
        AsyncImage(
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth(),
            model = ImageRequest.Builder(LocalContext.current)
                .data(randomPhoto.download_url)
                .crossfade(true)
                .build(),
            contentDescription = "A photo",
        )
    }
}

@Composable
fun ResultScreen(photos: String, randomPhoto: MarsPhoto, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = photos)
        AsyncImage(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth(),
            model = ImageRequest.Builder(LocalContext.current)
                .data(randomPhoto.imgSrc)
                .crossfade(true)
                .build(),
            contentDescription = "A photo",
        )
    }
}


@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    MarsPhotosTheme {
        LoadingScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorScreenPreview() {
    MarsPhotosTheme {
        ErrorScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun PhotosGridScreenPreview() {
    MarsPhotosTheme {
        //ResultScreen(stringResource(R.string.placeholder_success))
    }
}
