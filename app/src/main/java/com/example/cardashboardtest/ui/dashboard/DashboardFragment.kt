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
import com.example.cardashboardtest.ui.views.GaugeView
import com.example.cardashboardtest.ui.views.ProgressGaugeView
import com.example.cardashboardtest.ui.views.CorvetteSpeedBarView
import com.example.cardashboardtest.ui.views.CorvetteTachometerCurveView
import com.example.cardashboardtest.ui.views.CorvetteFuelBarView
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
            
            currentTheme = theme
            themePreferences.setTheme(theme)
            
            handler.post {
                applyTheme(theme)
                android.util.Log.d("DashboardFragment", "Theme applied: $theme")
            }
        }
        dialog.show(parentFragmentManager, "theme_selection")
    }

    private fun applyCurrentTheme() {
        currentTheme = themePreferences.getTheme()
        android.util.Log.d("DashboardFragment", "Applying current theme from prefs: $currentTheme")
        currentTheme?.let { applyTheme(it) }
    }

    private fun applyTheme(theme: DashboardTheme) {
        try {
            android.util.Log.d("DashboardFragment", "Starting theme application: $theme")
            currentTheme = theme
            
            val dashboardContent = binding.root.findViewById<ViewGroup>(R.id.dashboard_content)
            
            if (dashboardContent == null) {
                android.util.Log.e("DashboardFragment", "Dashboard content container not found")
                return
            }
            
            dashboardContent.removeAllViews()
            
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
            
            layoutInflater.inflate(layoutRes, dashboardContent, true)
            
            viewModel.speed.removeObservers(viewLifecycleOwner)
            viewModel.rpm.removeObservers(viewLifecycleOwner)
            viewModel.fuelLevel.removeObservers(viewLifecycleOwner)
            viewModel.engineTemp.removeObservers(viewLifecycleOwner)
            viewModel.oilPressure.removeObservers(viewLifecycleOwner)
            viewModel.voltage.removeObservers(viewLifecycleOwner)
            viewModel.engineRunning.removeObservers(viewLifecycleOwner)
            viewModel.gear.removeObservers(viewLifecycleOwner)

            when (theme) {
                DashboardTheme.MODERN -> setupModernDashboardObserversAndListeners()
                DashboardTheme.CORVETTE_1985 -> setupCorvetteDashboardObserversAndListeners()
            }
            
            handler.post {
                updateDisplayedValues()
                android.util.Log.d("DashboardFragment", "Display values updated for theme: $theme")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("DashboardFragment", "Error applying theme: ${e.message}", e)
        }
    }

    private fun updateDisplayedValues() {
        android.util.Log.d("DashboardFragment", "Updating displayed values")
        
        viewModel.engineRunning.value?.let { running ->
            if (running) viewModel.startEngine() else viewModel.stopEngine()
        }
        viewModel.speed.value?.let { viewModel.setSpeed(it) }
        viewModel.gear.value?.let { viewModel.setGear(it) }
        viewModel.simulateDataChanges() 
    }

    private fun setupCorvetteDashboardObserversAndListeners() {
        val dashboardContent = binding.root.findViewById<ViewGroup>(R.id.dashboard_content) ?: return
        android.util.Log.d("DashboardFragment", "Setting up Corvette Observers")

        viewModel.speed.observe(viewLifecycleOwner) { speed ->
            dashboardContent.findViewById<CorvetteSpeedBarView>(R.id.speed_bar_graph_placeholder_corvette)?.setSpeed(speed)
            dashboardContent.findViewById<TextView>(R.id.digital_speed_value_corvette)?.text = speed?.toString() ?: "0"
        }

        viewModel.rpm.observe(viewLifecycleOwner) { rpm ->
            val displayRpm = rpm?.let { it / 100 } ?: 0
            dashboardContent.findViewById<CorvetteTachometerCurveView>(R.id.rpm_bar_graph_placeholder_corvette)?.setRpm(rpm)
            dashboardContent.findViewById<TextView>(R.id.digital_rpm_value_corvette)?.text = displayRpm.toString()
        }

        viewModel.fuelLevel.observe(viewLifecycleOwner) { fuel ->
            dashboardContent.findViewById<CorvetteFuelBarView>(R.id.fuel_bar_graph_placeholder_corvette)?.setFuelLevel(fuel)
            val reserveIndicator = dashboardContent.findViewById<TextView>(R.id.reserve_indicator_corvette)
            reserveIndicator?.visibility = if (fuel != null && fuel < 15) View.VISIBLE else View.GONE
        }

        viewModel.engineTemp.observe(viewLifecycleOwner) { temp ->
            dashboardContent.findViewById<TextView>(R.id.coolant_temp_value_corvette)?.text = temp?.toString() ?: "0"
        }
        
        viewModel.oilTemp.observe(viewLifecycleOwner) { oilTemp ->
            dashboardContent.findViewById<TextView>(R.id.oil_temp_value_corvette)?.text = oilTemp?.toString() ?: "0"
        }

        viewModel.oilPressure.observe(viewLifecycleOwner) { pressure ->
            dashboardContent.findViewById<TextView>(R.id.oil_pressure_value_corvette)?.text = pressure?.toString() ?: "0"
        }

        viewModel.voltage.observe(viewLifecycleOwner) { voltage ->
            dashboardContent.findViewById<TextView>(R.id.volts_value_corvette)?.text = String.format(Locale.US, "%.1f", voltage ?: 0.0f)
        }

        viewModel.checkGauges.observe(viewLifecycleOwner) { milOn ->
            dashboardContent.findViewById<TextView>(R.id.upshift_indicator_corvette)?.visibility = 
                if (milOn == true) View.VISIBLE else View.GONE
        }
        
        setupGeneralListeners(dashboardContent)
        setupGaugeCardClickListenersCorvette(dashboardContent)
    }

    private fun setupModernDashboardObserversAndListeners() {
        val dashboardContent = binding.root.findViewById<ViewGroup>(R.id.dashboard_content) ?: return
        android.util.Log.d("DashboardFragment", "Setting up Modern Observers")

        viewModel.speed.observe(viewLifecycleOwner) { speed ->
            dashboardContent.findViewById<GaugeView>(R.id.speedGauge)?.speedTo(speed.toFloat(), 500)
            dashboardContent.findViewById<TextView>(R.id.speedValue)?.text = speed.toString()
        }

        viewModel.rpm.observe(viewLifecycleOwner) { rpm ->
            dashboardContent.findViewById<GaugeView>(R.id.rpmGauge)?.speedTo((rpm / 1000f), 500)
            dashboardContent.findViewById<TextView>(R.id.rpmValue)?.text = (rpm / 1000f).toString()
        }

        viewModel.fuelLevel.observe(viewLifecycleOwner) { fuel ->
            dashboardContent.findViewById<ProgressGaugeView>(R.id.fuelGauge)?.setProgress(fuel.toFloat())
            dashboardContent.findViewById<TextView>(R.id.fuelValue)?.text = "$fuel%"
        }

        viewModel.engineTemp.observe(viewLifecycleOwner) { temp ->
            dashboardContent.findViewById<ProgressGaugeView>(R.id.tempGauge)?.setProgress(temp.toFloat() - 50)
            val tempFahrenheit = celsiusToFahrenheit(temp ?: 0)
            dashboardContent.findViewById<TextView>(R.id.tempValue)?.text = "$tempFahrenheit°F"
        }
        
        viewModel.engineRunning.observe(viewLifecycleOwner) { running ->
            dashboardContent.findViewById<CompoundButton>(R.id.engineSwitch)?.isChecked = running ?: false
        }

        viewModel.gear.observe(viewLifecycleOwner) { gear ->
            dashboardContent.findViewById<TextView>(R.id.gearValue)?.text = gear ?: "P"
        }
        setupGeneralListeners(dashboardContent)
        setupGaugeCardClickListenersModern(dashboardContent)
    }

    private fun setupGeneralListeners(dashboardContent: ViewGroup) {
        dashboardContent.findViewById<CompoundButton>(R.id.engineSwitch)?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.startEngine() else viewModel.stopEngine()
        }

        dashboardContent.findViewById<android.widget.SeekBar>(R.id.speedSeekBar)?.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && viewModel.engineRunning.value == true) {
                    viewModel.setSpeed(progress)
                    updateGear(progress)
                } else if (viewModel.engineRunning.value == false) {
                     seekBar?.progress = 0
                     updateGear(0)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun setupObservers() {
        viewModel.connectionState.observe(viewLifecycleOwner) { state ->
            updateConnectionState(state)
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
        // This method is deprecated. Listeners are now set up in 
        // setupModernDashboardObserversAndListeners or setupCorvetteDashboardObserversAndListeners
        // after the respective layout is inflated.
    }

    private fun startSimulation() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {
                    if (viewModel.connectionState.value !is ObdBluetoothService.ConnectionState.Connected) {
                         viewModel.simulateDataChanges()
                    }
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
                        android.util.Log.w("DashboardFragment", "timeValue TextView not found in current layout.")
                    }
                }
            }
        }
        handler.post(timeRunnable)
    }

    private fun setupGaugeCardClickListenersModern(dashboardContent: ViewGroup) {
        dashboardContent.findViewById<CardView>(R.id.speedCard)?.setOnClickListener {
            toggleCardExpansion(it as CardView)
        }
        dashboardContent.findViewById<CardView>(R.id.rpmCard)?.setOnClickListener {
            toggleCardExpansion(it as CardView)
        }
        dashboardContent.findViewById<CardView>(R.id.fuelCard)?.setOnClickListener {
            toggleCardExpansion(it as CardView)
        }
        dashboardContent.findViewById<CardView>(R.id.tempCard)?.setOnClickListener {
            toggleCardExpansion(it as CardView)
        }
    }
    
    private fun setupGaugeCardClickListenersCorvette(dashboardContent: ViewGroup) {
        android.util.Log.d("DashboardFragment", "Corvette click listeners setup: Card expansion is not applicable to this theme.")
        // No individual cards to click and expand in the Corvette theme as designed.
        // If specific clickable areas are needed for Corvette, they'd need different handling.
    }

    private fun toggleCardExpansion(cardToExpand: CardView) {
        if (currentTheme != DashboardTheme.MODERN) {
            android.util.Log.d("DashboardFragment", "Card expansion skipped for non-Modern theme.")
            return
        }

        val isExpanded = cardToExpand.tag as? Boolean ?: false
        val dashboardContent = binding.root.findViewById<ViewGroup>(R.id.dashboard_content)
        
        val allCards = listOfNotNull(
            dashboardContent?.findViewById<CardView>(R.id.speedCard),
            dashboardContent?.findViewById<CardView>(R.id.rpmCard),
            dashboardContent?.findViewById<CardView>(R.id.fuelCard),
            dashboardContent?.findViewById<CardView>(R.id.tempCard)
        )

        if (!isExpanded) {
            cardToExpand.apply {
                elevation = resources.getDimension(R.dimen.expanded_card_elevation)
                translationZ = 8f 
                bringToFront() 
            }

            val centerX = (dashboardContent?.width ?: 0) / 2f
            val centerY = (dashboardContent?.height ?: 0) / 2f
            
            val cardCenterX = cardToExpand.x + cardToExpand.width / 2
            val cardCenterY = cardToExpand.y + cardToExpand.height / 2
            
            val translateX = centerX - cardCenterX
            val translateY = centerY - cardCenterY
            
            allCards.forEach { card ->
                if (card != cardToExpand) {
                    card.animate().alpha(0.3f).setDuration(300).start()
                    card.tag = false
                }
            }
            
            cardToExpand.animate()
                .translationX(translateX)
                .translationY(translateY)
                .scaleX(1.5f)
                .scaleY(1.5f)
                .setDuration(300)
                .start()
            
            cardToExpand.tag = true
            increaseGaugeSize(cardToExpand)
        } else {
            cardToExpand.apply {
                animate().translationZ(0f).setDuration(300).start()
                elevation = resources.getDimension(R.dimen.default_card_elevation)
            }

            allCards.forEach { card ->
                card.animate().alpha(1f).setDuration(300).start()
            }
            
            cardToExpand.animate()
                .translationX(0f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start()
            
            cardToExpand.tag = false
            resetGaugeSize(cardToExpand)
        }
    }

    private fun increaseGaugeSize(card: CardView) {
        if (currentTheme != DashboardTheme.MODERN) return

        when (card.id) {
            R.id.speedCard, R.id.rpmCard -> {
                val gaugeView = if (card.id == R.id.speedCard) 
                    card.findViewById<GaugeView>(R.id.speedGauge) 
                else 
                    card.findViewById<GaugeView>(R.id.rpmGauge)
                
                gaugeView?.animate()?.scaleX(1.5f)?.scaleY(1.5f)?.setDuration(300)?.start()
                
                val valueText = if (card.id == R.id.speedCard)
                    card.findViewById<TextView>(R.id.speedValue)
                else
                    card.findViewById<TextView>(R.id.rpmValue)
                
                valueText?.animate()?.scaleX(1.5f)?.scaleY(1.5f)?.setDuration(300)?.start()
            }
            
            R.id.fuelCard, R.id.tempCard -> {
                val gaugeView = if (card.id == R.id.fuelCard) 
                    card.findViewById<ProgressGaugeView>(R.id.fuelGauge) 
                else 
                    card.findViewById<ProgressGaugeView>(R.id.tempGauge)
                
                gaugeView?.animate()?.scaleX(1.5f)?.scaleY(1.5f)?.setDuration(300)?.start()
                
                val valueText = if (card.id == R.id.fuelCard)
                    card.findViewById<TextView>(R.id.fuelValue)
                else
                    card.findViewById<TextView>(R.id.tempValue)
                
                valueText?.animate()?.scaleX(1.5f)?.scaleY(1.5f)?.setDuration(300)?.start()
            }
        }
    }

    private fun resetGaugeSize(card: CardView) {
        if (currentTheme != DashboardTheme.MODERN) return

        when (card.id) {
            R.id.speedCard, R.id.rpmCard -> {
                val gaugeView = if (card.id == R.id.speedCard) 
                    card.findViewById<GaugeView>(R.id.speedGauge) 
                else 
                    card.findViewById<GaugeView>(R.id.rpmGauge)
                
                gaugeView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(300)?.start()
                
                val valueText = if (card.id == R.id.speedCard)
                    card.findViewById<TextView>(R.id.speedValue)
                else
                    card.findViewById<TextView>(R.id.rpmValue)
                
                valueText?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(300)?.start()
            }
            
            R.id.fuelCard, R.id.tempCard -> {
                val gaugeView = if (card.id == R.id.fuelCard) 
                    card.findViewById<ProgressGaugeView>(R.id.fuelGauge) 
                else 
                    card.findViewById<ProgressGaugeView>(R.id.tempGauge)
                
                gaugeView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(300)?.start()
                
                val valueText = if (card.id == R.id.fuelCard)
                    card.findViewById<TextView>(R.id.fuelValue)
                else
                    card.findViewById<TextView>(R.id.tempValue)
                
                valueText?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(300)?.start()
            }
        }
    }

    private fun checkAndRequestBluetoothPermissions() {
        val missingPermissions = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        when {
            missingPermissions.isEmpty() -> {
                proceedWithBluetoothConnection()
            }
            else -> {
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
        if(isAdded) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun updateConnectionState(state: ObdBluetoothService.ConnectionState) {
        activity?.invalidateOptionsMenu()

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
        if(isAdded) {
             Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
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
        timer?.cancel()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
