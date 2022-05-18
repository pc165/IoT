package com.android.iotproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.android.iotproject.adapter.DiscoveredBluetoothDevice
import com.android.iotproject.adapter.ProductData
import com.android.iotproject.databinding.ActivityFragmentBinding
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.tabs.TabLayoutMediator


class FragmentActivity : AppCompatActivity(), ProductFragment.SendMessage {
    private lateinit var binding: ActivityFragmentBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val device = intent.getParcelableExtra<DiscoveredBluetoothDevice>(EXTRA_DEVICE)!!
        val deviceName = device.name
        val deviceAddress = device.address
        val toolbar = binding.toolbar
        toolbar.subtitle = "${deviceName ?: getString(R.string.unknown_device)} $deviceAddress"
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.pager.offscreenPageLimit = 3
        binding.pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> {
                        ProductFragment(device)
                    }
                    1 -> {
                        BasketFragment()
                    }
                    else -> ProductFragment(device)
                }
            }
        }

        val tabLayoutMediator =
            TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
                when (position + 1) {
                    1 -> {
                        tab.text = getString(R.string.product)
                    }
                    2 -> {
                        tab.text = getString(R.string.basket)
                        val badge: BadgeDrawable = tab.orCreateBadge
                        badge.backgroundColor =
                            ContextCompat.getColor(applicationContext, R.color.colorPrimary)
                        badge.number = 10
                        badge.isVisible = true
                    }
                }
            }
        tabLayoutMediator.attach()
    }

    companion object {
        const val EXTRA_DEVICE = "com.android.iotproject.EXTRA_DEVICE"
    }

    override fun send(product: ProductData) {
        val f = supportFragmentManager.findFragmentByTag("f1") as BasketFragment
        f.addProduct(product)
    }
}