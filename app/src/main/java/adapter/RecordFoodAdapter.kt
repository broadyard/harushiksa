package com.example.harushiksa.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.harushiksa.R

class RecordFoodAdapter(
    private val context: Context,
    private val foodList: MutableList<String>,  // 음식 이름 목록
    private val imageMap: Map<String, Uri>,     // 음식 이름 -> 이미지 URI 매핑
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<RecordFoodAdapter.FoodViewHolder>() {

    // 각 아이템의 뷰들을 보관
    inner class FoodViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageViewFood: ImageView = view.findViewById(R.id.imageViewFood)
        val textViewFoodName: TextView = view.findViewById(R.id.textViewFoodName)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    // 뷰 홀더 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record_food, parent, false)
        return FoodViewHolder(view)
    }

    // 각 항목에 데이터 바인딩
    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val foodName = foodList[position]
        holder.textViewFoodName.text = foodName

        val imageUri = imageMap[foodName]
        if (imageUri != null) {
            when {
                // 기본 이미지 리소스
                imageUri.toString().startsWith("default:") -> {
                    val drawableName = imageUri.toString().substringAfter("default:")
                    val resId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
                    holder.imageViewFood.setImageResource(resId)
                }
                // drawable 리소스를 정수 ID로 처리한 경우
                imageUri.toString().startsWith("drawable:") -> {
                    try {
                        val resId = imageUri.toString().substringAfter("drawable:").toInt()
                        holder.imageViewFood.setImageResource(resId)
                    } catch (e: Exception) {
                        holder.imageViewFood.setImageResource(R.drawable.placeholder_image)
                    }
                }
                // 내부 저장소에 저장된 이미지 처리
                else -> {
                    try {
                        val inputStream = context.contentResolver.openInputStream(imageUri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        holder.imageViewFood.setImageBitmap(bitmap)
                        inputStream?.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        holder.imageViewFood.setImageResource(R.drawable.placeholder_image)
                    }
                }
            }
        } else {
            // 이미지 URI가 없을 경우 기본 이미지 표시
            holder.imageViewFood.setImageResource(R.drawable.placeholder_image)
        }

        // 삭제 버튼 클릭 시 리스트에서 제거하고 콜백 호출
        holder.btnDelete.setOnClickListener {
            val deletedItem = foodList[position]
            foodList.removeAt(position)
            notifyItemRemoved(position)
            onDeleteClick(deletedItem)
        }
    }
    // 항목 수 반환
    override fun getItemCount(): Int = foodList.size
}
