package ir.hajkarami.crimecraft

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ir.hajkarami.crimecraft.databinding.ListItemCrimeBinding
import android.text.format.DateFormat
import ir.hajkarami.crimecraft.database.Crime
import java.util.UUID

class CrimeHolder(
    private val binding: ListItemCrimeBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(crime: Crime, onCrimeClicked : (crimeId : UUID) -> Unit) {
        binding.crimeTitle.text = crime.title
        val formattedDate = DateFormat.format("EEEE, MMM dd, yyyy", crime.date).toString()
        binding.crimeDate.text = formattedDate
        binding.root.setOnClickListener {
            onCrimeClicked(crime.id)
        }

        binding.btnCrimeSolved.visibility = if (crime.isSolved)
            View.VISIBLE else View.GONE
    }

}

class CrimeListAdapter(
    private val crimes: List<Crime>,
    private val onCrimeClicked: (crimeId : UUID) -> Unit
) : RecyclerView.Adapter<CrimeHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemCrimeBinding.inflate(inflater, parent, false)
        return CrimeHolder(binding)
    }

    override fun getItemCount(): Int = crimes.size

    override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
        val item = crimes[position]
        holder.bind(item,onCrimeClicked)

    }

}
