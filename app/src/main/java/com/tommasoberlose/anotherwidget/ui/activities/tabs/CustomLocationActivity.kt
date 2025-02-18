package com.tommasoberlose.anotherwidget.ui.activities.tabs

import android.app.Activity
import android.location.Address
import android.location.Geocoder
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivityCustomLocationBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.viewmodels.tabs.CustomLocationViewModel
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CustomLocationActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: CustomLocationViewModel
    private lateinit var binding: ActivityCustomLocationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[CustomLocationViewModel::class.java]
        binding = ActivityCustomLocationBinding.inflate(layoutInflater)


        binding.listView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        binding.listView.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<String>(R.layout.custom_location_item) { _, injector ->
                injector.text(R.id.text, getString(R.string.custom_location_gps))
                injector.clicked(R.id.text) {
                    Preferences.bulk {
                        remove(Preferences::customLocationLat)
                        remove(Preferences::customLocationLon)
                        remove(Preferences::customLocationAdd)
                    }
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
            .register<Address>(R.layout.custom_location_item) { item, injector ->
                injector.text(R.id.text, item.getAddressLine(0) ?: "")
                injector.clicked(R.id.item) {
                    Preferences.bulk {
                        customLocationLat = item.latitude.toString()
                        customLocationLon = item.longitude.toString()
                        customLocationAdd = item.getAddressLine(0) ?: ""
                    }
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
            .attachTo(binding.listView)


        viewModel.addresses.observe(this, Observer {
            adapter.updateData(listOf("Default") + it)
        })

        setupListener()
        subscribeUi(binding, viewModel)

        binding.location.requestFocus()

        setContentView(binding.root)
    }

    private var searchJob: Job? = null

    private fun subscribeUi(binding: ActivityCustomLocationBinding, viewModel: CustomLocationViewModel) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.addresses.observe(this, Observer {
            adapter.updateData(listOf("Default") + it)
            binding.loader.visibility = View.INVISIBLE
        })

        viewModel.locationInput.observe(this, Observer { location ->
            binding.loader.visibility = View.VISIBLE
            searchJob?.cancel()
            searchJob = lifecycleScope.launch(Dispatchers.IO) {
                delay(200)
                var list = viewModel.addresses.value!!
                if (location != null && location != "")
                {
                    val coder = Geocoder(this@CustomLocationActivity)
                    try {
                        if (SDK_INT >= TIRAMISU) {
                            list = suspendCoroutine { continuation ->
                                coder.getFromLocationName(location, 10, Geocoder.GeocodeListener { addresses ->
                                    continuation.resume(addresses)
                                })
                            }
                        }
                        else {
                            @Suppress("DEPRECATION")
                            list = coder.getFromLocationName(location, 10) as ArrayList<Address>
                        }
                    } catch (ignored: Exception) {
                        emptyList<Address>()
                    }
                }
                withContext(Dispatchers.Main) {
                    viewModel.addresses.value = list
                    binding.loader.visibility = View.INVISIBLE
                }

            }
            binding.clearSearch.isVisible = location.isNotBlank()
        })
    }

    private fun setupListener() {
        binding.actionBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.clearSearch.setOnClickListener {
            viewModel.locationInput.value = ""
        }
    }
}
