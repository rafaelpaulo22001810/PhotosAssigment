package com.example.marsphotos.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marsphotos.model.PicsumPhoto
import com.example.marsphotos.network.PicsumApi
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface PicsumUiState {
    data class Success(val photos: String, val randomPhoto: PicsumPhoto, val refresh: () -> Unit) :
        PicsumUiState

    object Error : PicsumUiState
    object Loading : PicsumUiState
}

class PicsumViewModel : ViewModel() {
    var picsumUiState: PicsumUiState by mutableStateOf(PicsumUiState.Loading)
        private set

    init {
        getPicsumPhotos()
    }

    fun getPicsumPhotos() {
        viewModelScope.launch {
            picsumUiState = PicsumUiState.Loading
            picsumUiState = try {
                val listResult = PicsumApi.retrofitService.getPhotos()
                PicsumUiState.Success(
                    "Success: ${listResult.size} Picsum photos retrieved",
                    listResult.random(), ::refresh
                )
            } catch (e: Exception) {
                PicsumUiState.Error
            } catch (e: HttpException) {
                PicsumUiState.Error
            }
        }
    }

    fun refresh() {
        getPicsumPhotos()
    }
}

