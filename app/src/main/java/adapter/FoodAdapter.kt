package com.example.harushiksa.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.harushiksa.R

class FoodAdapter(
    private val context: Context,
    private val foodList: MutableList<String>,          // 음식 이름 목록
    private val excludedFoods: MutableSet<String>,      // 제외할 음식 목록 (체크박스로 제어)
    private val imageMap: Map<String, Uri>,             // 음식 이름 → 이미지 URI 매핑
    private val onDeleteClick: (String) -> Unit         // 삭제 콜백 함수
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    // 각 아이템의 뷰들을 보관
    inner class FoodViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFood: ImageView = view.findViewById(R.id.imgFood)
        val tvFoodName: TextView = view.findViewById(R.id.tvFoodName)
        val checkBox: CheckBox = view.findViewById(R.id.checkbox)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    // 뷰 홀더 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_food, parent, false)
        return FoodViewHolder(view)
    }

    // 뷰 홀더에 데이터 바인딩
    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foodList[position]
        holder.tvFoodName.text = food
        holder.checkBox.isChecked = !excludedFoods.contains(food)

        // 체크박스 상태 변경
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) excludedFoods.add(food) else excludedFoods.remove(food)
        }

        // 이미지 설정
        val imageUri = imageMap[food]
        if (imageUri != null) {
            if (imageUri.toString().startsWith("default:")) {
                // 기본 이미지 리소스를 사용하는 경우
                val drawableName = imageUri.toString().substringAfter("default:")
                val resId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
                holder.imgFood.setImageResource(resId)
            } else {
                // 사용자가 추가한 이미지 처리
                try {
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    holder.imgFood.setImageBitmap(bitmap)
                    inputStream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    holder.imgFood.setImageResource(R.drawable.placeholder_image)
                }
            }
        } else {
            // 이미지가 없을 경우 기본 이미지 표시
            holder.imgFood.setImageResource(R.drawable.placeholder_image)
        }

        // 삭제 버튼 클릭
        holder.btnDelete.setOnClickListener {
            val deletedItem = foodList[position]
            foodList.removeAt(position)
            notifyItemRemoved(position)
            onDeleteClick(deletedItem)
        }
    }

    // 전체 아이템 수 반환
    override fun getItemCount(): Int = foodList.size
}
