package com.example.frydayapp

import retrofit2.Call
import retrofit2.http.*

// Data Class สำหรับอัปเดตเมนู
data class MenuUpdateRequest(
    val name: String,
    val Details: String,
    val price: Double
)

// Data Class สำหรับอัปเดต option
data class OptionUpdateRequest(
    val name: String,
    val price: Double
)

// Data Class สำหรับเพิ่ม option ใหม่ (ต้องมี addon_id)
data class OptionAddRequest(
    val addon_id: String,
    val name: String,
    val price: Double,
    val available: Boolean = true
)

interface SupabaseService {
    @GET("menu")
    @Headers("Accept: application/json")
    fun getMenus(
        @Query("select") select: String = "menu_id,name,price,image_url,category,cate_id,Details,is_popular"
    ): Call<List<MenuItemModel>>

    @GET("menu")
    @Headers("Accept: application/json")
    fun getMenuById(
        @Query("menu_id") menuId: String,
        @Query("select") select: String = "*"
    ): Call<List<MenuItemModel>>

    @PATCH("menu")
    @Headers("Content-Type: application/json", "Prefer: return=representation")
    fun updateMenu(
        @QueryMap filters: Map<String, String>,
        @Body updates: MenuUpdateRequest
    ): Call<List<MenuItemModel>>

    // ========== OPTIONS (ADDONS) ==========

    @GET("addons")
    @Headers("Accept: application/json")
    fun getAllOptions(
        @Query("select") select: String = "*"
    ): Call<List<AddonModel>>

    @GET("addons")
    @Headers("Accept: application/json")
    fun getAddonById(
        @Query("addon_id") addonId: String,
        @Query("select") select: String = "*"
    ): Call<List<AddonModel>>

    @POST("addons")
    @Headers("Content-Type: application/json", "Prefer: return=representation")
    fun addOption(
        @Body newOption: OptionAddRequest
    ): Call<List<Map<String, Any>>>

    @PATCH("addons")
    @Headers("Content-Type: application/json", "Prefer: return=representation")
    fun updateOption(
        @QueryMap filters: Map<String, String>,  // ✅ ใช้ QueryMap
        @Body updates: OptionUpdateRequest
    ): Call<List<Map<String, Any>>>

    @DELETE("addons")
    @Headers("Content-Type: application/json")
    fun deleteOption(
        @QueryMap filters: Map<String, String>  // ✅ ต้องเป็น Map
    ): Call<Void>
}