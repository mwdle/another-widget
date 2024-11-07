package com.tommasoberlose.anotherwidget.ui.activities.tabs

import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivityAppNotificationsFilterBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.ActiveNotificationsHelper
import com.tommasoberlose.anotherwidget.ui.viewmodels.tabs.AppNotificationsViewModel
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter


class AppNotificationsFilterActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: AppNotificationsViewModel
    private lateinit var binding: ActivityAppNotificationsFilterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[AppNotificationsViewModel::class.java]
        binding = ActivityAppNotificationsFilterBinding.inflate(layoutInflater)

        binding.listView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        binding.listView.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<ResolveInfo>(R.layout.application_info_layout) { item, injector ->
                injector
                    .text(R.id.text, item.loadLabel(viewModel.pm))
                    .with<ImageView>(R.id.icon) {
                        Glide
                            .with(this)
                            .load(item.loadIcon(viewModel.pm))
                            .centerCrop()
                            .into(it)
                    }
                    .visible(R.id.checkBox)
                    .clicked(R.id.item) {
                        toggleApp(item)
                        adapter.notifyItemRangeChanged(0, adapter.data.size)
                    }
                    .clicked(R.id.checkBox) {
                        toggleApp(item)
                        adapter.notifyItemRangeChanged(0, adapter.data.size)
                    }
                    .checked(R.id.checkBox, ActiveNotificationsHelper.isAppAccepted(item.activityInfo.packageName))
            }
            .attachTo(binding.listView)

        setupListener()
        subscribeUi(binding, viewModel)

        binding.search.requestFocus()

        setContentView(binding.root)
    }

    private var filterJob: Job? = null

    private fun subscribeUi(binding: ActivityAppNotificationsFilterBinding, viewModel: AppNotificationsViewModel) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.appList.observe(this, Observer {
            updateList(list = it)
            binding.loader.visibility = View.INVISIBLE
        })

        viewModel.searchInput.observe(this, Observer { search ->
            updateList(search = search)
            binding.clearSearch.isVisible = search.isNotBlank()
        })

        viewModel.appNotificationsFilter.observe(this, {
            updateList()
            binding.clearSelection.isVisible = Preferences.appNotificationsFilter != ""
        })
    }

    private fun updateList(
        list: List<ResolveInfo>? = viewModel.appList.value,
        search: String? = viewModel.searchInput.value
    ) {
        binding.loader.visibility = View.VISIBLE
        filterJob?.cancel()
        filterJob = lifecycleScope.launch(Dispatchers.IO) {
            if (!list.isNullOrEmpty()) {
                delay(200)
                val filteredList: List<ResolveInfo> = if (search == null || search == "") {
                    list
                } else {
                    list.filter {
                        it.loadLabel(viewModel.pm).contains(search, true)
                    }
                }.sortedWith { app1, app2 ->
                    when {
                        ActiveNotificationsHelper.isAppAccepted(app1.activityInfo.packageName) -> {
                            -1
                        }

                        ActiveNotificationsHelper.isAppAccepted(app2.activityInfo.packageName) -> {
                            1
                        }

                        else -> {
                            app1.loadLabel(viewModel.pm).toString()
                                .compareTo(app2.loadLabel(viewModel.pm).toString(), ignoreCase = true)
                        }
                    }
                }


                withContext(Dispatchers.Main) {
                    adapter.updateData(filteredList)
                    binding.loader.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun setupListener() {
        binding.actionBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.clearSearch.setOnClickListener {
            viewModel.searchInput.value = ""
        }

        binding.clearSelection.setOnClickListener {
            Preferences.appNotificationsFilter = ""
        }
    }

    private fun toggleApp(app: ResolveInfo) {
        ActiveNotificationsHelper.toggleAppFilter(app.activityInfo.packageName)
    }
}
