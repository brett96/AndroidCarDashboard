package com.example.cardashboardtest.ui.dashboard

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.cardashboardtest.R
import com.example.cardashboardtest.bluetooth.BluetoothDeviceDialog
import com.example.cardashboardtest.bluetooth.ObdBluetoothService
import com.example.cardashboardtest.databinding.FragmentDashboardBinding
import com.example.cardashboardtest.model.CarData
import com.example.cardashboardtest.ui.theme.DashboardTheme
import com.example.cardashboardtest.ui.theme.ThemeSelectionDialog
import com.example.cardashboardtest.ui.views.DigitalRPMView
import com.example.cardashboardtest.ui.views.DigitalSpeedView
import com.example.cardashboardtest.ui.views.GaugeView
import com.example.cardashboardtest.ui.views.ProgressGaugeView
import com.example.cardashboardtest.utils.ThemePreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.Timer
import java.util.TimerTask
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")
    private lateinit var viewModel: DashboardViewModel
    private val handler = Handler(Looper.getMainLooper())
    private var timer: Timer? = null
    private lateinit var themePreferences: ThemePreferences
    private var currentTheme: DashboardTheme? = null

    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // All permissions granted, proceed with Bluetooth connection
            proceedWithBluetoothConnection()
        } else {
            showError(
                "Permission Required",
                "Bluetooth permissions are required to connect to OBD device"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        themePreferences = ThemePreferences(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupPersistentErrorObserver()
        setupListeners()
        startSimulation()
        updateTime()
        applyCurrentTheme()
        setupGaugeCardClickListeners()
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.dashboard_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val connectItem = menu.findItem(R.id.action_bluetooth_connect)
        val disconnectItem = menu.findItem(R.id.action_bluetooth_disconnect)

        when (viewModel.connectionState.value) {
            is ObdBluetoothService.ConnectionState.Connected -> {
                connectItem?.isVisible = false
                disconnectItem?.isVisible = true
            }
            is ObdBluetoothService.ConnectionState.Disconnected -> {
                connectItem?.isVisible = true
                disconnectItem?.isVisible = false
            }
            is ObdBluetoothService.ConnectionState.Error -> {
                connectItem?.isVisible = true
                disconnectItem?.isVisible = false
            }
            is ObdBluetoothService.ConnectionState.Connecting -> {
                connectItem?.isVisible = false
                disconnectItem?.isVisible = false
            }
            else -> {
                connectItem?.isVisible = true
                disconnectItem?.isVisible = false
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_change_theme -> {
                showThemeSelectionDialog()
                true
            }
            R.id.action_bluetooth_connect -> {
                showBluetoothDeviceDialog()
                true
            }
            R.id.action_bluetooth_disconnect -> {
                viewModel.disconnectDevice()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showThemeSelectionDialog() {
        val dialog = ThemeSelectionDialog.newInstance()
        dialog.setThemeSelectedListener { theme ->
            android.util.Log.d("DashboardFragment", "Theme selection callback received: $theme")
            
            // Update current theme reference before applying
            currentTheme = theme
            
            // Apply the theme immediately
            handler.post {
                applyTheme(theme)
                android.util.Log.d("DashboardFragment", "Theme applied: $theme")
            }
        }
        dialog.show(parentFragmentManager, "theme_selection")
    }

    private fun applyCurrentTheme() {
        val currentThemeFromPrefs = themePreferences.getTheme()
        applyTheme(currentThemeFromPrefs)
    }

    private fun applyTheme(theme: DashboardTheme) {
        try {
            android.util.Log.d("DashboardFragment", "Starting theme application: $theme")
            
            // Get dashboard content container
            val dashboardContent = binding.root.findViewById<ViewGroup>(R.id.dashboard_content)
            
            if (dashboardContent == null) {
                android.util.Log.e("DashboardFragment", "Dashboard content container not found")
                return
            }
            
            // Remove existing views
            dashboardContent.removeAllViews()
            
            // Inflate new layout based on theme
            val layoutRes = when (theme) {
                DashboardTheme.MODERN -> {
                    android.util.Log.d("DashboardFragment", "Inflating modern dashboard layout")
                    R.layout.layout_modern_dashboard
                }
                DashboardTheme.CORVETTE_1985 -> {
                    android.util.Log.d("DashboardFragment", "Inflating Corvette dashboard layout")
                    R.layout.layout_corvette_dashboard
                }
            }
            
            // Inflate the new layout
            layoutInflater.inflate(layoutRes, dashboardContent, true)
            
            // Setup appropriate observers
            when (theme) {
                DashboardTheme.MODERN -> setupObservers()
                DashboardTheme.CORVETTE_1985 -> setupCorvetteObservers()
            }
            
            // Setup listeners after inflating new layout
            setupListeners()
            
            // Force update all displayed values
            handler.post {
                updateDisplayedValues()
                android.util.Log.d("DashboardFragment", "Display values updated for theme: $theme")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("DashboardFragment", "Error applying theme: ${e.message}", e)
        }
    }

    private fun updateDisplayedValues() {
        // Update all values to ensure they're displayed in the new layout
        android.util.Log.d("DashboardFragment", "Updating displayed values")
        
        // First handle engine state as it affects other values
        if (viewModel.engineRunning.value == true) {
            viewModel.startEngine()
        } else {
            viewModel.stopEngine()
        }
        
        // Then update speed (which also updates RPM)
        viewModel.speed.value?.let { 
            viewModel.setSpeed(it)
            android.util.Log.d("DashboardFragment", "Setting speed to $it")
        }
        
        // Update gear
        viewModel.gear.value?.let { 
            viewModel.setGear(it)
            android.util.Log.d("DashboardFragment", "Setting gear to $it")
        }

        // Update other values
        viewModel.simulateDataChanges() // This will update temp, oil, voltage, fuel and warning lights
    }

    private fun setupCorvetteObservers() {
        val dashboardContent = binding.root.findViewById<ViewGroup>(R.id.dashboard_content)

        // Observe speed changes for digital display
        viewModel.speed.observe(viewLifecycleOwner) { speed ->
            dashboardContent?.findViewById<DigitalSpeedView>(R.id.digitalSpeedView)?.setSpeed(speed)
        }

        // Observe RPM changes for bar graph
        viewModel.rpm.observe(viewLifecycleOwner) { rpm ->
            dashboardContent?.findViewById<DigitalRPMView>(R.id.digitalRpmView)?.setRPM(rpm / 1000f)
        }

        // Observe fuel level
        viewModel.fuelLevel.observe(viewLifecycleOwner) { fuel ->
            dashboardContent?.findViewById<ProgressGaugeView>(R.id.fuelGauge)?.let { gauge ->
                gauge.setProgress(fuel.toFloat())
                android.util.Log.d("DashboardFragment", "Updating Corvette fuel gauge to: $fuel")
            }
        }

        // Observe temperature
        viewModel.engineTemp.observe(viewLifecycleOwner) { temp ->
            dashboardContent?.findViewById<ProgressGaugeView>(R.id.tempGauge)?.setProgress(temp.toFloat() - 50)
        }

        // Observe oil pressure
        viewModel.oilPressure.observe(viewLifecycleOwner) { pressure ->
            dashboardContent?.findViewById<TextView>(R.id.oilPressureValue)?.text = pressure.toString()
        }

        // Observe voltage
        viewModel.voltage.observe(viewLifecycleOwner) { voltage ->
            dashboardContent?.findViewById<TextView>(R.id.voltageValue)?.text = String.format("%.1f", voltage)
        }

        // Observe warning lights
        viewModel.checkGauges.observe(viewLifecycleOwner) { check ->
            dashboardContent?.findViewById<TextView>(R.id.checkGaugesWarning)?.visibility = 
                if (check) View.VISIBLE else View.GONE
        }

        viewModel.lowOil.observe(viewLifecycleOwner) { low ->
            dashboardContent?.findViewById<TextView>(R.id.lowOilWarning)?.visibility = 
                if (low) View.VISIBLE else View.GONE
        }

        // Observe engine state
        viewModel.engineRunning.observe(viewLifecycleOwner) { running ->
            dashboardContent?.findViewById<CompoundButton>(R.id.engineSwitch)?.isChecked = running
        }

        // Observe gear
        viewModel.gear.observe(viewLifecycleOwner) { gear ->
            dashboardContent?.findViewById<TextView>(R.id.gearValue)?.text = gear
        }
    }

    private fun setupObservers() {
        val dashboardContent = binding.root.findViewById<ViewGroup>(R.id.dashboard_content)

        // Observe connection state
        viewModel.connectionState.observe(viewLifecycleOwner) { state ->
            updateConnectionState(state)
        }

        // Observe speed changes
        viewModel.speed.observe(viewLifecycleOwner) { speed ->
            dashboardContent?.findViewById<com.example.cardashboardtest.ui.views.GaugeView>(R.id.speedGauge)?.speedTo(speed.toFloat(), 500)
            dashboardContent?.findViewById<TextView>(R.id.speedValue)?.text = speed.toString()
        }

        // Observe RPM changes
        viewModel.rpm.observe(viewLifecycleOwner) { rpm ->
            dashboardContent?.findViewById<com.example.cardashboardtest.ui.views.GaugeView>(R.id.rpmGauge)?.speedTo((rpm / 1000f), 500)
            dashboardContent?.findViewById<TextView>(R.id.rpmValue)?.text = (rpm / 1000f).toString()
        }

        // Observe fuel level changes
        viewModel.fuelLevel.observe(viewLifecycleOwner) { fuel ->
            dashboardContent?.findViewById<ProgressGaugeView>(R.id.fuelGauge)?.let { gauge ->
                gauge.setProgress(fuel.toFloat())
                android.util.Log.d("DashboardFragment", "Updating Modern fuel gauge to: $fuel")
            }
            dashboardContent?.findViewById<TextView>(R.id.fuelValue)?.text = "$fuel%"
        }

        // Observe temperature changes
        viewModel.engineTemp.observe(viewLifecycleOwner) { temp ->
            dashboardContent?.findViewById<ProgressGaugeView>(R.id.tempGauge)?.setProgress(temp.toFloat() - 50)
            val tempFahrenheit = celsiusToFahrenheit(temp)
            dashboardContent?.findViewById<TextView>(R.id.tempValue)?.text = "$tempFahrenheit°F"
        }

        // Observe engine state
        viewModel.engineRunning.observe(viewLifecycleOwner) { running ->
            dashboardContent?.findViewById<CompoundButton>(R.id.engineSwitch)?.isChecked = running
        }

        // Observe gear
        viewModel.gear.observe(viewLifecycleOwner) { gear ->
            dashboardContent?.findViewById<TextView>(R.id.gearValue)?.text = gear
        }
    }

    private fun celsiusToFahrenheit(celsius: Int): Int {
        return (celsius * 9 / 5) + 32
    }
    
    private fun updateGear(speed: Int) {
        if (speed > 0) {
            viewModel.setGear("D")
        } else {
            viewModel.setGear("P")
        }
    }

    private fun setupListeners() {
        val dashboardContent = binding.root.findViewById<ViewGroup>(R.id.dashboard_content)

        // Engine start/stop switch
        dashboardContent?.findViewById<CompoundButton>(R.id.engineSwitch)?.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            if (isChecked) {
                viewModel.startEngine()
            } else {
                viewModel.stopEngine()
            }
        }

        // Speed control
        dashboardContent?.findViewById<android.widget.SeekBar>(R.id.speedSeekBar)?.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && viewModel.engineRunning.value == true) {
                    viewModel.setSpeed(progress)
                    // Update gear based on speed
                    updateGear(progress)
                } else {
                    updateGear(0)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun startSimulation() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {
                    viewModel.simulateDataChanges()
                }
            }
        }, 1000, 1000)
    }

    private fun updateTime() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeRunnable = object : Runnable {
            override fun run() {
                if (_binding != null && isAdded) {
                    try {
                        binding.root.findViewById<TextView>(R.id.timeValue)?.text = timeFormat.format(Date())
                        handler.postDelayed(this, 1000)
                    } catch (e: Exception) {
                        // Handle any exceptions that might occur
                    }
                }
            }
        }
        handler.post(timeRunnable)
    }

    private fun setupGaugeCardClickListeners() {
        val dashboardContent = binding.root.findViewById<ViewGroup>(R.id.dashboard_content)
        
        // For Modern theme
        dashboardContent?.findViewById<CardView>(R.id.speedCard)?.setOnClickListener {
            toggleCardExpansion(it as CardView)
        }
        dashboardContent?.findViewById<CardView>(R.id.rpmCard)?.setOnClickListener {
            toggleCardExpansion(it as CardView)
        }
        dashboardContent?.findViewById<CardView>(R.id.fuelCard)?.setOnClickListener {
            toggleCardExpansion(it as CardView)
        }
        dashboardContent?.findViewById<CardView>(R.id.tempCard)?.setOnClickListener {
            toggleCardExpansion(it as CardView)
        }
        
        // For Corvette theme
        dashboardContent?.findViewById<CardView>(R.id.digitalSpeedCard)?.setOnClickListener {
            toggleCardExpansion(it as CardView)
        }
        dashboardContent?.findViewById<CardView>(R.id.digitalRpmCard)?.setOnClickListener {
            toggleCardExpansion(it as CardView)
        }
    }

    private fun toggleCardExpansion(cardToExpand: CardView) {
        val isExpanded = cardToExpand.tag as? Boolean ?: false
        val dashboardContent = binding.root.findViewById<ViewGroup>(R.id.dashboard_content)
        
        // Find all cards in the current theme
        val allCards = when (currentTheme) {
            DashboardTheme.MODERN -> listOf(
                dashboardContent?.findViewById<CardView>(R.id.speedCard),
                dashboardContent?.findViewById<CardView>(R.id.rpmCard),
                dashboardContent?.findViewById<CardView>(R.id.fuelCard),
                dashboardContent?.findViewById<CardView>(R.id.tempCard)
            )
            DashboardTheme.CORVETTE_1985 -> listOf(
                dashboardContent?.findViewById<CardView>(R.id.digitalSpeedCard),
                dashboardContent?.findViewById<CardView>(R.id.digitalRpmCard)
            )
            else -> listOf()
        }

        if (!isExpanded) {
            // Prepare the card for expansion
            cardToExpand.apply {
                // Bring card to front before animation
                elevation = resources.getDimension(R.dimen.expanded_card_elevation)
                translationZ = 8f // Additional z-translation for expanded state
                bringToFront() // Ensure it's at the top of the view hierarchy
            }

            // Calculate center position
            val centerX = (dashboardContent?.width ?: 0) / 2f
            val centerY = (dashboardContent?.height ?: 0) / 2f
            
            val cardCenterX = cardToExpand.x + cardToExpand.width / 2
            val cardCenterY = cardToExpand.y + cardToExpand.height / 2
            
            val translateX = centerX - cardCenterX
            val translateY = centerY - cardCenterY
            
            // Animate other cards
            allCards.forEach { card ->
                if (card != null && card != cardToExpand) {
                    card.animate()
                        .alpha(0.3f) // Fade but don't completely hide
                        .setDuration(300)
                        .start()
                    card.tag = false
                }
            }
            
            // Animate the expanding card
            cardToExpand.animate()
                .translationX(translateX)
                .translationY(translateY)
                .scaleX(1.5f)
                .scaleY(1.5f)
                .setDuration(300)
                .start()
            
            cardToExpand.tag = true
            
            // Increase gauge size
            increaseGaugeSize(cardToExpand)
        } else {
            // Reset z-ordering and elevation
            cardToExpand.apply {
                animate()
                    .translationZ(0f)
                    .setDuration(300)
                    .start()
                elevation = resources.getDimension(R.dimen.default_card_elevation)
            }

            // Animate all cards simultaneously
            allCards.forEach { card ->
                card?.animate()
                    ?.alpha(1f)
                    ?.setDuration(300)
                    ?.start()
            }
            
            // Reset the expanded card
            cardToExpand.animate()
                .translationX(0f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start()
            
            cardToExpand.tag = false
            
            // Reset gauge size
            resetGaugeSize(cardToExpand)
        }
    }

    private fun increaseGaugeSize(card: CardView) {
        when (currentTheme) {
            DashboardTheme.MODERN -> {
                when (card.id) {
                    R.id.speedCard, R.id.rpmCard -> {
                        val gaugeView = if (card.id == R.id.speedCard) 
                            card.findViewById<GaugeView>(R.id.speedGauge) 
                        else 
                            card.findViewById<GaugeView>(R.id.rpmGauge)
                        
                        gaugeView?.animate()
                            ?.scaleX(1.5f)
                            ?.scaleY(1.5f)
                            ?.setDuration(300)
                            ?.start()
                        
                        // Increase text size
                        val valueText = if (card.id == R.id.speedCard)
                            card.findViewById<TextView>(R.id.speedValue)
                        else
                            card.findViewById<TextView>(R.id.rpmValue)
                        
                        valueText?.animate()
                            ?.scaleX(1.5f)
                            ?.scaleY(1.5f)
                            ?.setDuration(300)
                            ?.start()
                    }
                    
                    R.id.fuelCard, R.id.tempCard -> {
                        val gaugeView = if (card.id == R.id.fuelCard) 
                            card.findViewById<ProgressGaugeView>(R.id.fuelGauge) 
                        else 
                            card.findViewById<ProgressGaugeView>(R.id.tempGauge)
                        
                        gaugeView?.animate()
                            ?.scaleX(1.5f)
                            ?.scaleY(1.5f)
                            ?.setDuration(300)
                            ?.start()
                        
                        // Increase text size
                        val valueText = if (card.id == R.id.fuelCard)
                            card.findViewById<TextView>(R.id.fuelValue)
                        else
                            card.findViewById<TextView>(R.id.tempValue)
                        
                        valueText?.animate()
                            ?.scaleX(1.5f)
                            ?.scaleY(1.5f)
                            ?.setDuration(300)
                            ?.start()
                    }
                }
            }
            DashboardTheme.CORVETTE_1985 -> {
                when (card.id) {
                    R.id.digitalSpeedCard -> {
                        val speedView = card.findViewById<DigitalSpeedView>(R.id.digitalSpeedView)
                        speedView?.animate()
                            ?.scaleX(1.5f)
                            ?.scaleY(1.5f)
                            ?.setDuration(300)
                            ?.start()
                    }
                    R.id.digitalRpmCard -> {
                        val rpmView = card.findViewById<DigitalRPMView>(R.id.digitalRpmView)
                        rpmView?.animate()
                            ?.scaleX(1.5f)
                            ?.scaleY(1.5f)
                            ?.setDuration(300)
                            ?.start()
                    }
                }
            }
            else -> {}
        }
    }

    private fun resetGaugeSize(card: CardView) {
        when (currentTheme) {
            DashboardTheme.MODERN -> {
                when (card.id) {
                    R.id.speedCard, R.id.rpmCard -> {
                        val gaugeView = if (card.id == R.id.speedCard) 
                            card.findViewById<GaugeView>(R.id.speedGauge) 
                        else 
                            card.findViewById<GaugeView>(R.id.rpmGauge)
                        
                        gaugeView?.animate()
                            ?.scaleX(1f)
                            ?.scaleY(1f)
                            ?.setDuration(300)
                            ?.start()
                        
                        // Reset text size
                        val valueText = if (card.id == R.id.speedCard)
                            card.findViewById<TextView>(R.id.speedValue)
                        else
                            card.findViewById<TextView>(R.id.rpmValue)
                        
                        valueText?.animate()
                            ?.scaleX(1f)
                            ?.scaleY(1f)
                            ?.setDuration(300)
                            ?.start()
                    }
                    
                    R.id.fuelCard, R.id.tempCard -> {
                        val gaugeView = if (card.id == R.id.fuelCard) 
                            card.findViewById<ProgressGaugeView>(R.id.fuelGauge) 
                        else 
                            card.findViewById<ProgressGaugeView>(R.id.tempGauge)
                        
                        gaugeView?.animate()
                            ?.scaleX(1f)
                            ?.scaleY(1f)
                            ?.setDuration(300)
                            ?.start()
                        
                        // Reset text size
                        val valueText = if (card.id == R.id.fuelCard)
                            card.findViewById<TextView>(R.id.fuelValue)
                        else
                            card.findViewById<TextView>(R.id.tempValue)
                        
                        valueText?.animate()
                            ?.scaleX(1f)
                            ?.scaleY(1f)
                            ?.setDuration(300)
                            ?.start()
                    }
                }
            }
            DashboardTheme.CORVETTE_1985 -> {
                when (card.id) {
                    R.id.digitalSpeedCard -> {
                        val speedView = card.findViewById<DigitalSpeedView>(R.id.digitalSpeedView)
                        speedView?.animate()
                            ?.scaleX(1f)
                            ?.scaleY(1f)
                            ?.setDuration(300)
                            ?.start()
                    }
                    R.id.digitalRpmCard -> {
                        val rpmView = card.findViewById<DigitalRPMView>(R.id.digitalRpmView)
                        rpmView?.animate()
                            ?.scaleX(1f)
                            ?.scaleY(1f)
                            ?.setDuration(300)
                            ?.start()
                    }
                }
            }
            else -> {}
        }
    }

    private fun checkAndRequestBluetoothPermissions() {
        val missingPermissions = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        when {
            missingPermissions.isEmpty() -> {
                // All permissions are granted, proceed with Bluetooth connection
                proceedWithBluetoothConnection()
            }
            else -> {
                // Request the missing permissions
                requestPermissionLauncher.launch(missingPermissions.toTypedArray())
            }
        }
    }

    private fun proceedWithBluetoothConnection() {
        val devices = viewModel.scanForDevices()
        if (devices.isEmpty()) {
            showError("No paired OBD devices found", "Please pair your OBD device in Bluetooth settings first.")
            return
        }

        val dialog = BluetoothDeviceDialog.newInstance()
        dialog.setDevices(devices)
        dialog.setDeviceSelectedListener { device ->
            viewModel.connectToDevice(device)
        }
        dialog.show(parentFragmentManager, "bluetooth_device_selection")
    }

    private fun showBluetoothDeviceDialog() {
        checkAndRequestBluetoothPermissions()
    }

    private fun showError(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updateConnectionState(state: ObdBluetoothService.ConnectionState) {
        activity?.invalidateOptionsMenu() // This will trigger onPrepareOptionsMenu

        when (state) {
            is ObdBluetoothService.ConnectionState.Connected -> {
                showSnackbar("Connected to ${state.deviceName}")
            }
            is ObdBluetoothService.ConnectionState.Error -> {
                showError("Connection Error", state.message)
            }
            is ObdBluetoothService.ConnectionState.Connecting -> {
                showSnackbar("Connecting to OBD device...")
            }
            else -> {}
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun setupPersistentErrorObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showPersistentError.collectLatest { showError ->
                if (showError) {
                    showPersistentConnectionErrorDialog()
                }
            }
        }
    }

    private fun showPersistentConnectionErrorDialog() {
        if (isAdded) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Persistent Connection Error")
                .setMessage("The connection to the OBD device was lost and could not be re-established. Please check the device and reconnect manually.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel all pending handler callbacks to prevent memory leaks
        timer?.cancel()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
