package com.paradox.app.feature.quickball

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.paradox.app.MainActivity
import com.paradox.app.R
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.ui.theme.ParadoxTheme
import com.paradox.app.core.ui.theme.ThemePalette
import com.paradox.app.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import javax.inject.Inject

@AndroidEntryPoint
class QuickBallOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    @Inject
    lateinit var sessionDataStore: SessionDataStore

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    // Reactive State for Window & Position
    private val isExpandedFlow = MutableStateFlow(false)
    private val isIdleFlow = MutableStateFlow(false)
    private val currentXFlow = MutableStateFlow(0)
    private val currentYFlow = MutableStateFlow(500)
    private val isDockedLeftFlow = MutableStateFlow(false)

    private var inactivityJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (overlayView == null) {
            showOverlay()
        }
        resetInactivityTimer()
        return START_STICKY
    }

    private fun resetInactivityTimer() {
        isIdleFlow.value = false
        inactivityJob?.cancel()
        if (!isExpandedFlow.value) {
            inactivityJob = scope.launch {
                delay(3000)
                if (!isExpandedFlow.value) {
                    isIdleFlow.value = true
                    snapWindowToEdge(tuck = true)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val ballSizePx = (54 * displayMetrics.density).toInt()

        val initX = screenWidth - ballSizePx - 8
        val initY = (displayMetrics.heightPixels * 0.45f).toInt()
        currentXFlow.value = initX
        currentYFlow.value = initY
        isDockedLeftFlow.value = false

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            ballSizePx,
            ballSizePx,
            initX,
            initY,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(this@QuickBallOverlayService)
            setViewTreeSavedStateRegistryOwner(this@QuickBallOverlayService)
            setViewTreeViewModelStoreOwner(this@QuickBallOverlayService)

            setContent {
                val themeMode by sessionDataStore.themeMode.collectAsState(initial = "SYSTEM")
                val themePalette by sessionDataStore.themePalette.collectAsState(initial = "SLATE")
                val isExpanded by isExpandedFlow.collectAsState()
                val isIdle by isIdleFlow.collectAsState()
                val ballX by currentXFlow.collectAsState()
                val ballY by currentYFlow.collectAsState()
                val isDockedLeft by isDockedLeftFlow.collectAsState()
                val palette = ThemePalette.fromName(themePalette)

                ParadoxTheme(
                    darkTheme = themeMode != "LIGHT",
                    palette = palette
                ) {
                    RadialQuickBallContent(
                        isExpanded = isExpanded,
                        isIdle = isIdle,
                        ballX = ballX,
                        ballY = ballY,
                        isDockedLeft = isDockedLeft,
                        onDismiss = {
                            toggleWindowExpansion(false)
                        },
                        onQuickAdd = { launchApp(Screen.Capture.createRoute("QUICK_ADD")) },
                        onVoiceEntry = { launchApp(Screen.Capture.createRoute("VOICE")) },
                        onScanReceipt = { launchApp(Screen.Capture.createRoute("OCR")) },
                        onAskParadox = { launchApp(Screen.AskParadox.route) }
                    )
                }
            }
        }

        // Native Touch Listener for 100% Responsive Drag & Instant Tap Handling
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        overlayView?.setOnTouchListener { _, event ->
            if (isExpandedFlow.value) {
                // When expanded, let Compose handle taps on the radial items & dismiss scrim
                return@setOnTouchListener false
            }

            val params = windowParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    resetInactivityTimer()
                    if (isIdleFlow.value) {
                        isIdleFlow.value = false
                        snapWindowToEdge(tuck = false)
                    }
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (abs(dx) > 10f || abs(dy) > 10f) {
                        isDragging = true
                        val newX = (initialX + dx).toInt().coerceIn(0, displayMetrics.widthPixels - ballSizePx)
                        val newY = (initialY + dy).toInt().coerceIn(80, displayMetrics.heightPixels - ballSizePx - 120)
                        params.x = newX
                        params.y = newY
                        currentXFlow.value = newX
                        currentYFlow.value = newY
                        try {
                            windowManager?.updateViewLayout(overlayView, params)
                        } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Instant 1-tap open
                        toggleWindowExpansion(true)
                    } else {
                        // Magnetically snap to left or right edge
                        snapWindowToEdge(tuck = false)
                        resetInactivityTimer()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(overlayView, windowParams)
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun snapWindowToEdge(tuck: Boolean = false) {
        if (isExpandedFlow.value) return
        val params = windowParams ?: return
        val displayMetrics = resources.displayMetrics
        val ballSize = (54 * displayMetrics.density).toInt()
        val screenWidth = displayMetrics.widthPixels

        val isLeft = currentXFlow.value < screenWidth / 2
        val targetX = when {
            tuck && isLeft -> -(ballSize * 0.45f).toInt()
            tuck && !isLeft -> screenWidth - (ballSize * 0.55f).toInt()
            isLeft -> 8
            else -> screenWidth - ballSize - 8
        }
        isDockedLeftFlow.value = isLeft
        currentXFlow.value = targetX
        params.x = targetX
        try {
            windowManager?.updateViewLayout(overlayView, params)
        } catch (_: Exception) {}
    }

    private fun toggleWindowExpansion(expand: Boolean) {
        isExpandedFlow.value = expand
        if (expand) {
            inactivityJob?.cancel()
            isIdleFlow.value = false
        } else {
            resetInactivityTimer()
        }

        val params = windowParams ?: return
        val displayMetrics = resources.displayMetrics
        val ballSizePx = (54 * displayMetrics.density).toInt()

        if (expand) {
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            params.x = 0
            params.y = 0
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            params.width = ballSizePx
            params.height = ballSizePx
            params.x = currentXFlow.value
            params.y = currentYFlow.value
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }

        try {
            windowManager?.updateViewLayout(overlayView, params)
        } catch (_: Exception) {}
    }

    private fun launchApp(route: String) {
        toggleWindowExpansion(false)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, route)
        }

        try {
            val pendingIntent = PendingIntent.getActivity(
                this,
                route.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic().apply {
                    setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                }
                pendingIntent.send(options.toBundle())
            } else {
                pendingIntent.send()
            }
        } catch (_: Exception) {
            try {
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        inactivityJob?.cancel()
        job.cancel()

        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_ROUTE = "com.paradox.app.EXTRA_QUICK_BALL_ROUTE"

        fun start(context: Context) {
            val intent = Intent(context, QuickBallOverlayService::class.java)
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            val intent = Intent(context, QuickBallOverlayService::class.java)
            try {
                context.stopService(intent)
            } catch (_: Exception) {}
        }
    }
}

@Composable
private fun RadialQuickBallContent(
    isExpanded: Boolean,
    isIdle: Boolean,
    ballX: Int,
    ballY: Int,
    isDockedLeft: Boolean,
    onDismiss: () -> Unit,
    onQuickAdd: () -> Unit,
    onVoiceEntry: () -> Unit,
    onScanReceipt: () -> Unit,
    onAskParadox: () -> Unit
) {
    val expansionProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "radialExpansion"
    )

    val ballAlpha by animateFloatAsState(
        targetValue = if (isIdle && !isExpanded) 0.38f else 0.95f,
        label = "ballAlpha"
    )

    if (isExpanded || expansionProgress > 0.01f) {
        // Ultra-minimal transparent dismiss scrim (does NOT darken user's wallpaper/apps)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.05f * expansionProgress))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            // Tight C-Curve offsets fanning around the Quick Ball with Minimal Light Pastel sRGB
            // 1. Quick Add (Top Arc ~ -55 deg) - Minimal Light Mint
            RadialCurvedItem(
                icon = Icons.Default.Edit,
                label = "Quick Add",
                surfaceColor = Color(0xFFF0FDF4),
                borderColor = Color(0xFF86EFAC),
                textColor = Color(0xFF14532D),
                badgeBg = Color(0xFFDCFCE7),
                iconTint = Color(0xFF16A34A),
                ballX = ballX,
                ballY = ballY,
                targetDx = if (isDockedLeft) 56.dp else (-142).dp,
                targetDy = (-76).dp,
                progress = expansionProgress,
                isDockedLeft = isDockedLeft,
                onClick = onQuickAdd
            )

            // 2. Voice Entry (Upper Middle Arc ~ -18 deg) - Minimal Light Sky Cyan
            RadialCurvedItem(
                icon = Icons.Default.Mic,
                label = "Voice Entry",
                surfaceColor = Color(0xFFF0F9FF),
                borderColor = Color(0xFF7DD3FC),
                textColor = Color(0xFF0C4A6E),
                badgeBg = Color(0xFFE0F2FE),
                iconTint = Color(0xFF0284C7),
                ballX = ballX,
                ballY = ballY,
                targetDx = if (isDockedLeft) 72.dp else (-162).dp,
                targetDy = (-26).dp,
                progress = expansionProgress,
                isDockedLeft = isDockedLeft,
                onClick = onVoiceEntry
            )

            // 3. Scan Receipt (Lower Middle Arc ~ +18 deg) - Minimal Light Periwinkle Blue
            RadialCurvedItem(
                icon = Icons.Default.CameraAlt,
                label = "Scan Receipt",
                surfaceColor = Color(0xFFEEF2FF),
                borderColor = Color(0xFF93C5FD),
                textColor = Color(0xFF1E3A8A),
                badgeBg = Color(0xFFDBEAFE),
                iconTint = Color(0xFF2563EB),
                ballX = ballX,
                ballY = ballY,
                targetDx = if (isDockedLeft) 72.dp else (-162).dp,
                targetDy = 26.dp,
                progress = expansionProgress,
                isDockedLeft = isDockedLeft,
                onClick = onScanReceipt
            )

            // 4. Ask Paradox AI (Bottom Arc ~ +55 deg) - Minimal Light Lavender Lilac
            RadialCurvedItem(
                icon = Icons.Default.AutoAwesome,
                label = "Ask Paradox",
                surfaceColor = Color(0xFFFAF5FF),
                borderColor = Color(0xFFC4B5FD),
                textColor = Color(0xFF581C87),
                badgeBg = Color(0xFFF3E8FF),
                iconTint = Color(0xFF7C3AED),
                ballX = ballX,
                ballY = ballY,
                targetDx = if (isDockedLeft) 56.dp else (-142).dp,
                targetDy = 76.dp,
                progress = expansionProgress,
                isDockedLeft = isDockedLeft,
                onClick = onAskParadox
            )

            // Central Active Orb (Anchored at exact ball coordinates)
            Box(
                modifier = Modifier
                    .offset { IntOffset(ballX, ballY) }
                    .size(54.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(1.5.dp, Color(0xFFCBD5E1), CircleShape)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.paradox_logo),
                    contentDescription = "Paradox Quick Ball",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                )

                // Overlay subtle close indicator
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f * expansionProgress)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Menu",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    } else {
        // Collapsed Floating Orb with 3s Idle Dimming & Auto-Tuck
        Box(
            modifier = Modifier
                .size(54.dp)
                .alpha(ballAlpha)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.Black)
                .border(1.5.dp, Color(0xFFCBD5E1).copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.paradox_logo),
                contentDescription = "Paradox Quick Ball",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun RadialCurvedItem(
    icon: ImageVector,
    label: String,
    surfaceColor: Color,
    borderColor: Color,
    textColor: Color,
    badgeBg: Color,
    iconTint: Color,
    ballX: Int,
    ballY: Int,
    targetDx: androidx.compose.ui.unit.Dp,
    targetDy: androidx.compose.ui.unit.Dp,
    progress: Float,
    isDockedLeft: Boolean,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val targetDxPx = with(density) { targetDx.toPx() }
    val targetDyPx = with(density) { targetDy.toPx() }

    val currentDx = targetDxPx * progress
    val currentDy = targetDyPx * progress
    val scale = (0.3f + 0.7f * progress).coerceIn(0f, 1f)
    val alpha = progress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .offset {
                val posX = (ballX + currentDx).toInt()
                val posY = (ballY + currentDy).toInt()
                IntOffset(posX, posY)
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
            shadowElevation = 8.dp,
            modifier = Modifier.wrapContentSize()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isDockedLeft) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = textColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(badgeBg)
                        .border(1.dp, borderColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(17.dp)
                    )
                }

                if (isDockedLeft) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = textColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
