package com.android.iotproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.iotproject.adapter.ProductListAdapter
import com.android.iotproject.databinding.ActivityProductListBinding

class ProductList : AppCompatActivity() {
    private lateinit var productListAdapter: ProductListAdapter
    private lateinit var binding: ActivityProductListBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productListAdapter = ProductListAdapter(mutableListOf())
        binding = ActivityProductListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }
}