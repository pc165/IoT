package com.android.iotproject

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.iotproject.adapter.ProductData
import com.android.iotproject.adapter.ProductListAdapter
import com.android.iotproject.data.LoginRepository
import com.android.iotproject.data.ResquestInstance
import com.android.iotproject.databinding.FragmentBasketBinding
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

class BasketFragment : Fragment() {
    private val TAG: String = javaClass.simpleName
    private lateinit var binding: FragmentBasketBinding
    private lateinit var productListAdapter: ProductListAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentBasketBinding.inflate(inflater, container, false)
        productListAdapter = ProductListAdapter(mutableListOf())
        binding.rvProductList.adapter = productListAdapter
        binding.rvProductList.layoutManager = LinearLayoutManager(activity)
        productListAdapter.price.observe(viewLifecycleOwner) {
            binding.tvTotal.text = "Total ${it} €"
        }
        binding.btnClear.setOnClickListener {
            productListAdapter.clear()
        }
        binding.btnCheckout.setOnClickListener {
            binding.btnCheckout.isEnabled = false
            val url = getString(R.string.upload_basket)
            val response = Response.Listener<JSONObject> {
                Log.i(TAG, it.toString())
                binding.btnCheckout.isEnabled = true
                productListAdapter.clear()
            }
            val error = Response.ErrorListener {
                Log.i(TAG, it.toString())
                binding.btnCheckout.isEnabled = true
                Toast.makeText(context, "Error while checking out", Toast.LENGTH_SHORT).show()
            }
            val req: JsonObjectRequest =
                object : JsonObjectRequest(url, productListAdapter.getAsJSON(), response, error) {
                    override fun getHeaders(): MutableMap<String, String> {
                        super.getHeaders()
                        val params: MutableMap<String, String> = HashMap()
                        val token =
                            if (LoginRepository.user != null) LoginRepository.user!!.access_token else ""
                        params["Authorization"] = "Bearer $token"
                        return params
                    }
                }
            ResquestInstance.getInstance(requireContext()).addToRequestQueue(req)
        }

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    fun addProduct(productData: ProductData) {
        productListAdapter.addProduct(productData)
    }
}