package com.everybuddy.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.local.TokenManager
import com.everybuddy.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val tokenManager   : TokenManager,
    private val userRepository : UserRepository,
) : ViewModel() {

    fun markAttendanceIfNeeded() {
        viewModelScope.launch {
            val today    = LocalDate.now().toString()
            val lastDate = tokenManager.lastAttendanceDate.firstOrNull()
            if (lastDate == today) return@launch
            val res = userRepository.markAttendance()
            if (res is ApiResult.Success) {
                tokenManager.saveLastAttendanceDate(today)
            }
        }
    }
}
