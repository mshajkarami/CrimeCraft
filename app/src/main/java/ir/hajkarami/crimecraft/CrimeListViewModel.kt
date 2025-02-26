package ir.hajkarami.crimecraft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hajkarami.crimecraft.database.Crime
import ir.hajkarami.crimecraft.database.CrimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class CrimeListViewModel : ViewModel() {

    private val crimeRepository  = CrimeRepository.get()
    private val _crimes: MutableStateFlow<List<Crime>> = MutableStateFlow(emptyList())
    val crimes: StateFlow<List<Crime>> get() = _crimes.asStateFlow()

    init {
        viewModelScope.launch {
            crimeRepository.getCrimes().collect{
                _crimes.value = it
            }
        }

    }
    suspend fun addCrime(crime: Crime){
        crimeRepository.addCrime(crime)
    }

}