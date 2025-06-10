package com.farhan.aplikasidietuntukobesitas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TipsAdapter(
    private val tips: List<PelatihActivity.TipData>,
    private val onItemAction: (PelatihActivity.TipData, String) -> Unit
) : RecyclerView.Adapter<TipsAdapter.TipViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tip, parent, false)
        return TipViewHolder(view)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        val tip = tips[position]
        holder.bind(tip)
    }

    override fun getItemCount(): Int = tips.size

    inner class TipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTipText: TextView = itemView.findViewById(R.id.tv_tip_text)
        private val tvCreatedBy: TextView = itemView.findViewById(R.id.tv_created_by)
        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tv_created_at)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(tip: PelatihActivity.TipData) {
            tvTipText.text = tip.text
            tvCreatedBy.text = "Oleh: ${tip.createdByName}"

            // Format tanggal dengan locale Indonesia
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            tvCreatedAt.text = dateFormat.format(tip.createdAt)

            // Status dengan penanganan resource yang lebih baik
            if (tip.isActive) {
                tvStatus.text = "Aktif"

                // Set background dengan fallback
                try {
                    tvStatus.setBackgroundResource(R.drawable.status_active_bg)
                } catch (e: Exception) {
                    // Fallback background untuk status aktif
                    tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_light))
                    tvStatus.setPadding(8, 4, 8, 4)
                }

                // Set text color dengan fallback
                try {
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_active))
                } catch (e: Exception) {
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
                }
            } else {
                tvStatus.text = "Nonaktif"

                // Set background dengan fallback
                try {
                    tvStatus.setBackgroundResource(R.drawable.status_inactive_bg)
                } catch (e: Exception) {
                    // Fallback background untuk status nonaktif
                    tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                    tvStatus.setPadding(8, 4, 8, 4)
                }

                // Set text color dengan fallback
                try {
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_inactive))
                } catch (e: Exception) {
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                }
            }

            // Button actions
            btnEdit.setOnClickListener {
                onItemAction(tip, "edit")
            }

            btnDelete.setOnClickListener {
                onItemAction(tip, "delete")
            }

            // Button state berdasarkan status tip
            val isEnabled = tip.isActive
            btnEdit.isEnabled = isEnabled
            btnDelete.isEnabled = isEnabled
            btnEdit.alpha = if (isEnabled) 1.0f else 0.5f
            btnDelete.alpha = if (isEnabled) 1.0f else 0.5f

            // Set content description untuk accessibility
            btnEdit.contentDescription = if (isEnabled) "Edit tip" else "Tip nonaktif, tidak dapat diedit"
            btnDelete.contentDescription = if (isEnabled) "Hapus tip" else "Tip nonaktif, tidak dapat dihapus"
        }
    }
}