package com.aniplex.app.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniplex.app.domain.model.HomeData
import com.aniplex.app.domain.model.Result
import com.aniplex.app.domain.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val homeData: HomeData) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AnimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        getHomePage(forceRefresh = false)
    }

    fun getHomePage(forceRefresh: Boolean) {
        viewModelScope.launch {
            repository.getHomePage(forceRefresh).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.value = HomeUiState.Loading
                    }
                    is Result.Success -> {
                        _uiState.value = HomeUiState.Success(result.data)
                    }
                    is Result.Error -> {
                        _uiState.value = HomeUiState.Error(result.message)
                    }
                }
            }
        }
    }

    fun refresh() {
        getHomePage(forceRefresh = true)
    }
}
