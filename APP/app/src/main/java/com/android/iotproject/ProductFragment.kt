package com.android.iotproject

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.iotproject.adapter.DiscoveredBluetoothDevice
import com.android.iotproject.adapter.ProductData
import com.android.iotproject.data.ResquestInstance
import com.android.iotproject.data.model.VolleyMultipartRequest
import com.android.iotproject.databinding.FragmentProductBinding
import com.android.iotproject.utils.Utils
import com.android.iotproject.viewmodels.ProductViewModel
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.ble.observer.ConnectionObserver
import org.json.JSONObject
import java.io.UnsupportedEncodingException


class ProductFragment(val device: DiscoveredBluetoothDevice) : Fragment() {
    private lateinit var viewModel: ProductViewModel
    private lateinit var binding: FragmentProductBinding
    private lateinit var sendMessage: SendMessage

    interface SendMessage {
        fun send(product: ProductData)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[ProductViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getImage().observe(viewLifecycleOwner, {
            binding.ivImageCanvas.setImageBitmap(it)
        })
        viewModel.getPercent().observe(viewLifecycleOwner, {
            binding.btnGetImage.isEnabled = it == 0 || it >= 100
            binding.btnGetData.isEnabled = it == 0 || it >= 100
            binding.pbDownloadProgress.progress = it
        })
        binding.apply {
            btnGetImage.setOnClickListener { viewModel.getPicture() }
            btnGetData.setOnClickListener { button ->
                button.isEnabled = false
                val url = getString(R.string.url_upload_item)
                val multipartRequest: VolleyMultipartRequest = object : VolleyMultipartRequest(
                    Method.POST, url,
                    Response.Listener { response ->
                        val charset = HttpHeaderParser.parseCharset(response.headers)
                        val parsed: String = try {
                            String(response.data, charset = charset(charset))
                        } catch (e: UnsupportedEncodingException) {
                            String(response.data)
                        }
                        val res = JSONObject(parsed)
                        binding.tvProductName.text = res.getString("name")
                        binding.tvProductPrice.text = res.getString("price")
                        binding.tvProductID.text = res.getString("id")
                        button.isEnabled = true
                    },
                    Response.ErrorListener { error ->
                        error.printStackTrace()
                        button.isEnabled = true
                    }) {
                    override fun getParams(): Map<String, String> {
                        val params: MutableMap<String, String> = HashMap()
                        params["token"] = ""

                        return params
                    }

                    override fun getByteData(): Map<String, DataPart> {
                        val params: MutableMap<String, DataPart> = HashMap()
                        params["file"] = DataPart(
                            "item.jpg",
                            Utils.getFileDataFromDrawable(binding.ivImageCanvas.drawable),
                            "image/jpeg"
                        )
                        return params
                    }
                }
                ResquestInstance.getInstance(requireContext()).addToRequestQueue(multipartRequest)
            }
            btnAddProduct.setOnClickListener {
                val image = ivImageCanvas.drawable.toBitmap()
                try {
                    val icon = Bitmap.createScaledBitmap(image, 50, 50, false)
                    val quantity = etQuantity.text.toString().toInt()
                    val price = tvProductPrice.text.toString().toFloat()
                    val name = tvProductName.text.toString()
                    val id = tvProductID.text.toString().toInt()
                    sendMessage.send(
                        ProductData(
                            name = name,
                            id = id,
                            quantitaty = quantity,
                            price = price,
                            icon = icon
                        )
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "Invalid data", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Configure the view model.
        viewModel.connect(device)
        viewModel.connectionState.observe(viewLifecycleOwner, { state: ConnectionState ->
            when (state.state!!) {
                ConnectionState.State.CONNECTING -> {
                    binding.progressContainer.visibility = View.VISIBLE
                    binding.infoNotSupported.container.visibility = View.GONE
                    binding.infoTimeout.container.visibility = View.GONE
                    binding.connectionState.setText(R.string.state_connecting)
                }
                ConnectionState.State.INITIALIZING -> binding.connectionState.setText(R.string.state_initializing)
                ConnectionState.State.READY -> {
                    binding.progressContainer.visibility = View.GONE
                    binding.layoutMainFragment.visibility = View.VISIBLE
                }
                ConnectionState.State.DISCONNECTED -> {
                    if (state is ConnectionState.Disconnected) {
                        binding.layoutMainFragment.visibility = View.GONE
                        binding.progressContainer.visibility = View.GONE
                        if (state.reason == ConnectionObserver.REASON_NOT_SUPPORTED) {
                            binding.infoNotSupported.container.visibility = View.VISIBLE
                        } else {
                            binding.infoTimeout.container.visibility = View.VISIBLE
                        }
                    }
                }
                ConnectionState.State.DISCONNECTING -> {
                }
            }
        })


        // Set up views.
        binding.apply {
            infoNotSupported.actionRetry.setOnClickListener { viewModel.reconnect() }
            infoTimeout.actionRetry.setOnClickListener { viewModel.reconnect() }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sendMessage = activity as SendMessage
    }
}