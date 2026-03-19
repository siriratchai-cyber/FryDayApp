package com.example.frydayapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

@Serializable
data class MenuUpdate(
    val name: String,
    @SerialName("Details") val details: String,
    val price: Double
)

@Serializable
data class PromotionUpdate(
    @SerialName("pro_name") val name: String,
    val price: Double
)

@Serializable
data class AddonInsert(
    @SerialName("addon_id") val addonId: String,
    val name: String,
    val price: Double,
    val available: Boolean
)

@Serializable
data class AddonUpdate(
    val name: String,
    val price: Double
)

object SupabaseHelper {

    private val tag = "SupabaseHelper"

    /**
     * อัปเดตข้อมูลเมนู - ใช้ Retrofit
     */
    suspend fun updateMenu(
        menuId: String,
        name: String,
        details: String,
        price: Double
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "=== UPDATE MENU VIA RETROFIT ===")
                Log.d(tag, "menuId: $menuId")
                Log.d(tag, "name: $name")
                Log.d(tag, "details: $details")
                Log.d(tag, "price: $price")

                val filters = mapOf("menu_id" to "eq.$menuId")
                val updates = MenuUpdateRequest(
                    name = name,
                    Details = details,
                    price = price
                )

                val response = RetrofitClient.instance.updateMenu(
                    filters = filters,
                    updates = updates
                ).execute()

                if (response.isSuccessful) {
                    Log.d(tag, "✅ Update successful via Retrofit")
                    true
                } else {
                    Log.e(tag, "❌ Update failed: ${response.code()} - ${response.errorBody()?.string()}")
                    false
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error updating menu: ${e.message}")
                false
            }
        }
    }

    /**
     * โหลด options ทั้งหมด (Global)
     */
    suspend fun loadAllOptions(): List<Option> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Loading all options from addons table")

                val response = SupabaseClientProvider.client
                    .from("addons")
                    .select()
                    .decodeList<AddonModel>()

                response.map { addon ->
                    Option(
                        id = addon.addon_id,
                        name = addon.name,
                        price = addon.price ?: 0.0,
                        isSelected = false
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading options: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * เพิ่ม option ใหม่ - ใช้ Retrofit
     */
    suspend fun addOption(option: Option): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "=== ADD OPTION VIA RETROFIT ===")
                Log.d(tag, "optionId: ${option.id}")
                Log.d(tag, "name: ${option.name}")
                Log.d(tag, "price: ${option.price}")

                val newOption = OptionAddRequest(
                    addon_id = option.id,
                    name = option.name,
                    price = option.price,
                    available = true
                )

                Log.d(tag, "New Option: $newOption")

                val response = RetrofitClient.instance.addOption(newOption).execute()

                if (response.isSuccessful) {
                    Log.d(tag, "✅ Option added via Retrofit: ${option.name}")
                    true
                } else {
                    Log.e(tag, "❌ Add failed: ${response.code()} - ${response.errorBody()?.string()}")
                    false
                }

            } catch (e: Exception) {
                Log.e(tag, "❌ Error adding option: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * อัปเดต option - ใช้ Retrofit
     */
    suspend fun updateOption(option: Option): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "=== UPDATE OPTION VIA RETROFIT ===")
                Log.d(tag, "optionId: ${option.id}")
                Log.d(tag, "name: ${option.name}")
                Log.d(tag, "price: ${option.price}")

                val filters = mapOf("addon_id" to "eq.${option.id}")
                val updates = OptionUpdateRequest(
                    name = option.name,
                    price = option.price
                )

                Log.d(tag, "Filters: $filters")
                Log.d(tag, "Updates: $updates")

                val response = RetrofitClient.instance.updateOption(
                    filters = filters,
                    updates = updates
                ).execute()

                if (response.isSuccessful) {
                    Log.d(tag, "✅ Option updated via Retrofit: ${option.name}")

                    val responseBody = response.body()
                    if (responseBody != null && responseBody.isNotEmpty()) {
                        val updatedOption = responseBody[0]
                        Log.d(tag, "After update - name: ${updatedOption["name"]}, price: ${updatedOption["price"]}")
                    }

                    true
                } else {
                    Log.e(tag, "❌ Update failed: ${response.code()} - ${response.errorBody()?.string()}")
                    false
                }

            } catch (e: Exception) {
                Log.e(tag, "❌ Error updating option: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * ดึง addon ตาม ID - ใช้ Retrofit
     */
    suspend fun getAddonById(addonId: String): AddonModel? {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getAddonById(addonId).execute()
                if (response.isSuccessful) {
                    response.body()?.firstOrNull()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * ลบ option - ใช้ Retrofit
     */
    /**
     * ลบ option - ใช้ Retrofit
     */
    suspend fun deleteOption(optionId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "=== DELETE OPTION VIA RETROFIT ===")
                Log.d(tag, "optionId: $optionId")


                val filters = mapOf("addon_id" to "eq.$optionId")

                val response = RetrofitClient.instance.deleteOption(filters).execute()

                if (response.isSuccessful) {
                    Log.d(tag, "✅ Option deleted: $optionId")
                    true
                } else {
                    Log.e(
                        tag,
                        "❌ Delete failed: ${response.code()} - ${response.errorBody()?.string()}"
                    )
                    false
                }

            } catch (e: Exception) {
                Log.e(tag, "❌ Error deleting option: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }}