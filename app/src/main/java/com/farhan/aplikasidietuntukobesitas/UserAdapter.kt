package com.farhan.aplikasidietuntukobesitas

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(private val userList: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvEmail: TextView = itemView.findViewById(R.id.tv_email)
        val tvWeight: TextView = itemView.findViewById(R.id.tv_weight)
        val tvHeight: TextView = itemView.findViewById(R.id.tv_height)
        val tvAge: TextView = itemView.findViewById(R.id.tv_age)
        val tvGender: TextView = itemView.findViewById(R.id.tv_gender)
        val tvBmi: TextView = itemView.findViewById(R.id.tv_bmi)
        val tvBmiStatus: TextView = itemView.findViewById(R.id.tv_bmi_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        // Set user data with null safety
        holder.tvName.text = if (user.name.isNotEmpty()) user.name else "User ${position + 1}"
        holder.tvEmail.text = user.email

        // Format weight and height
        holder.tvWeight.text = if (user.weight > 0) "${user.weight} kg" else "N/A"
        holder.tvHeight.text = if (user.height > 0) "${user.height} cm" else "N/A"
        holder.tvAge.text = if (user.age > 0) "${user.age} tahun" else "N/A"
        holder.tvGender.text = if (user.gender.isNotEmpty()) user.gender else "N/A"

        // Calculate and display BMI
        val bmiValue = if (user.bmi > 0) user.bmi else {
            if (user.weight > 0 && user.height > 0) {
                val heightInMeters = user.height / 100
                user.weight / (heightInMeters * heightInMeters)
            } else 0.0
        }

        holder.tvBmi.text = if (bmiValue > 0) String.format("%.1f", bmiValue) else "N/A"

        // Determine BMI status and color
        if (bmiValue > 0) {
            val (bmiStatus, statusColor, bgColor) = when {
                bmiValue < 18.5 -> Triple("Kurus", Color.parseColor("#2196F3"), Color.parseColor("#E3F2FD"))
                bmiValue < 25.0 -> Triple("Normal", Color.parseColor("#4CAF50"), Color.parseColor("#E8F5E8"))
                bmiValue < 30.0 -> Triple("Gemuk", Color.parseColor("#FF9800"), Color.parseColor("#FFF3E0"))
                else -> Triple("Obesitas", Color.parseColor("#F44336"), Color.parseColor("#FFEBEE"))
            }

            holder.tvBmiStatus.text = bmiStatus
            holder.tvBmiStatus.setTextColor(statusColor)
            holder.tvBmiStatus.setBackgroundColor(bgColor)
        } else {
            holder.tvBmiStatus.text = "N/A"
            holder.tvBmiStatus.setTextColor(Color.GRAY)
            holder.tvBmiStatus.setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        // Add padding to status
        val padding = (8 * holder.itemView.context.resources.displayMetrics.density).toInt()
        holder.tvBmiStatus.setPadding(padding, padding/2, padding, padding/2)
    }

    override fun getItemCount(): Int = userList.size
}