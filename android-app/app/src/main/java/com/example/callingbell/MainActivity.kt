package com.example.callingbell

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.callingbell.data.Member
import com.example.callingbell.data.Room
import com.example.callingbell.data.SharedPrefsManager
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.util.UUID
import com.google.firebase.messaging.FirebaseMessaging

// --- DATA ---
data class RingIntention(val id: String, val label: String, val emoji: String, val color: Color, val isCustom: Boolean = false)

val DEFAULT_INTENTIONS_LIST = listOf(
    RingIntention("breakfast", "Breakfast", "🍳", Color(0xFFF59E0B)),
    RingIntention("lunch", "Lunch", "🍲", Color(0xFF10B981)),
    RingIntention("dinner", "Dinner", "🍽️", Color(0xFF3B82F6)),
    RingIntention("tea", "Tea Time", "🍵", Color(0xFF8B5CF6)),
    RingIntention("ready", "Ready", "✅", Color(0xFF14B8A6)),
    RingIntention("general", "Ring", "🔔", Color(0xFF6366F1)),
    RingIntention("coffee", "Coffee", "☕", Color(0xFFA16207)),
    RingIntention("urgent", "Urgent", "🚨", Color(0xFFEF4444))
)

val EMOJI_OPTIONS_LIST = listOf("☕", "🍽️", "🚨", "❤️", "🔔", "💬", "🎮", "🚪", "🍕", "📢")

data class DeliveryAckState(
    val isDelivered: Boolean,
    val title: String,
    val message: String
)

enum class AppTheme(
    val label: String,
    val bgColor: Color,
    val cardBg: Color,
    val accentColor: Color,
    val textColor: Color,
    val subtextColor: Color,
    val cardBorderColor: Color
) {
    MIDNIGHT("MIDNIGHT CELESTIAL", Color(0xFF051833), Color(0xFF0A2246), Color(0xFFFFC19E), Color.White, Color(0xFF8DA4C4), Color(0xFF183B6B))
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val context = LocalContext.current
    val prefs = remember { SharedPrefsManager(context) }
    val navController = rememberNavController()
    
    // Request Notification Permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = {}
        )
        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    val activeRoomCode = remember(prefs.getRooms()) { prefs.getRooms().firstOrNull()?.code ?: "" }

    DisposableEffect(activeRoomCode) {
        if (activeRoomCode.isNotEmpty()) {
            val intent = Intent(context, RingListenerService::class.java).apply {
                putExtra("ROOM_CODE", activeRoomCode)
            }
            FirebaseMessaging.getInstance().subscribeToTopic(activeRoomCode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        onDispose { }
    }

    var currentTheme by remember { 
        mutableStateOf(
            try { AppTheme.valueOf(prefs.getSettings().themeId) } 
            catch(e: Exception) { AppTheme.MIDNIGHT }
        ) 
    }
    var incomingAlertText by remember { mutableStateOf<String?>(null) }
    var globalDeliveryAck by remember { mutableStateOf<DeliveryAckState?>(null) }

    LaunchedEffect(Unit) {
        RingEventBus.events.collect { event ->
            incomingAlertText = "${event.emoji} ${event.senderName}: ${event.intentLabel}!"
            delay(4000)
            incomingAlertText = null
        }
    }

    // Auto-dismiss delivery acknowledgment after 4s
    LaunchedEffect(globalDeliveryAck) {
        if (globalDeliveryAck != null) {
            delay(4000)
            globalDeliveryAck = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.bgColor)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: "splash"
        val showNav = currentRoute in listOf("dashboard", "rooms", "settings", "pro")

        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar Header for main screens
            if (showNav) {
                Surface(
                    color = currentTheme.bgColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = currentTheme.cardBorderColor,
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔔", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Calling Bell",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.textColor
                            )
                        }
                        IconButton(onClick = { 
                            prefs.userName = ""
                            navController.navigate("login") { popUpTo(0) }
                        }) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33EF4444))
                                    .border(1.5.dp, Color(0xFFEF4444), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🚪", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen(navController, prefs, currentTheme)
                    }
                    composable("login") {
                        LoginScreen(prefs, currentTheme, onLoginSuccess = { navController.navigate("dashboard") { popUpTo("login") { inclusive = true } } })
                    }
                    composable("dashboard") {
                        DashboardScreen(prefs, currentTheme, navController, onShowAck = { globalDeliveryAck = it })
                    }
                    composable("rooms") {
                        RoomsScreen(prefs, currentTheme, navController)
                    }
                    composable("room/{roomId}") { backStackEntry ->
                        val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                        RoomScreen(roomId, prefs, currentTheme, navController, onShowAck = { globalDeliveryAck = it })
                    }
                    composable("settings") {
                        SettingsScreen(prefs, currentTheme, navController)
                    }
                    composable("pro") {
                        ProScreen(prefs, currentTheme, navController)
                    }
                    composable("about") {
                        AboutScreen(currentTheme, navController)
                    }
                }
            }

            // Bottom Navigation Bar
            if (showNav) {
                AppBottomNavigation(currentRoute, navController, currentTheme)
            }
        }

        // Global Incoming Alert Overlay
        AnimatedVisibility(
            visible = incomingAlertText != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
        ) {
            incomingAlertText?.let { alert ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEC4899)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = alert,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // Delivered / Not Delivered Pop-Up Confirmation Banner Modal
        AnimatedVisibility(
            visible = globalDeliveryAck != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 70.dp, start = 16.dp, end = 16.dp)
        ) {
            globalDeliveryAck?.let { ack ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (ack.isDelivered) Color(0xFF064E3B) else Color(0xFF7F1D1D)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = if (ack.isDelivered) Color(0xFF10B981) else Color(0xFFEF4444),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                if (ack.isDelivered) "✅" else "❌",
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    ack.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    ack.message,
                                    color = Color(0xFFD1D5DB),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        IconButton(onClick = { globalDeliveryAck = null }) {
                            Text("✖️", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigation(currentRoute: String, navController: NavHostController, theme: AppTheme) {
    Surface(
        color = theme.bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = theme.cardBorderColor,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isHome = currentRoute == "dashboard"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    if (!isHome) navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = true } }
                }
            ) {
                Text(if (isHome) "🔔" else "🏠", fontSize = 20.sp)
                Text(
                    "Home",
                    fontSize = 11.sp,
                    fontWeight = if (isHome) FontWeight.Bold else FontWeight.Normal,
                    color = if (isHome) theme.accentColor else theme.subtextColor
                )
            }

            val isRooms = currentRoute == "rooms"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    if (!isRooms) navController.navigate("rooms")
                }
            ) {
                Text("🚪", fontSize = 20.sp)
                Text(
                    "Rooms",
                    fontSize = 11.sp,
                    fontWeight = if (isRooms) FontWeight.Bold else FontWeight.Normal,
                    color = if (isRooms) theme.accentColor else theme.subtextColor
                )
            }

            val isSettings = currentRoute == "settings"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    if (!isSettings) navController.navigate("settings")
                }
            ) {
                Text("⚙️", fontSize = 20.sp)
                Text(
                    "Settings",
                    fontSize = 11.sp,
                    fontWeight = if (isSettings) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSettings) theme.accentColor else theme.subtextColor
                )
            }

            val isPro = currentRoute == "pro"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    if (!isPro) navController.navigate("pro")
                }
            ) {
                Text("⭐", fontSize = 20.sp)
                Text(
                    "PRO",
                    fontSize = 11.sp,
                    fontWeight = if (isPro) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPro) theme.accentColor else theme.subtextColor
                )
            }
        }
    }
}

@Composable
fun LoginScreen(prefs: SharedPrefsManager, theme: AppTheme, onLoginSuccess: () -> Unit) {
    var name by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize().background(theme.bgColor), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text("Calling Bell", fontSize = 28.sp, color = theme.textColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Instant Chimes. One Tap Away.", fontSize = 15.sp, color = theme.accentColor, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Enter your name", color = theme.textColor.copy(alpha = 0.7f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accentColor,
                    unfocusedBorderColor = theme.cardBorderColor,
                    focusedTextColor = theme.textColor,
                    unfocusedTextColor = theme.textColor
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        prefs.userName = name
                        onLoginSuccess()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.cardBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, theme.accentColor, RoundedCornerShape(14.dp))
            ) {
                Text("Get Started", color = theme.textColor, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            "Developed by Risk Technologies © 2026 All Rights Reserved",
            color = theme.textColor.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

@Composable
fun DashboardScreen(
    prefs: SharedPrefsManager,
    theme: AppTheme,
    navController: NavHostController,
    onShowAck: (DeliveryAckState) -> Unit
) {
    val context = LocalContext.current
    var rooms by remember { mutableStateOf(prefs.getRooms()) }
    var activeRoomIndex by remember { mutableStateOf(0) }
    val activeRoom = rooms.getOrNull(activeRoomIndex)
    var expanded by remember { mutableStateOf(false) }

    val onlineMembers = remember { mutableStateMapOf<String, Long>() }
    var selectedTargetMember by remember { mutableStateOf<Member?>(null) }
    var isScanningMembers by remember { mutableStateOf(false) }

    // Custom intentions state for PRO list
    var customIntentionsList by remember { mutableStateOf(prefs.getCustomIntentions()) }
    var selectedIntention by remember { mutableStateOf(DEFAULT_INTENTIONS_LIST[0]) }
    var showCustomMsgDialog by remember { mutableStateOf(false) }
    var newCustomText by remember { mutableStateOf("") }
    var selectedCustomEmoji by remember { mutableStateOf("💬") }

    val allIntentions = remember(customIntentionsList) {
        DEFAULT_INTENTIONS_LIST + customIntentionsList.map { (label, emoji) ->
            RingIntention("custom_${label}", label, emoji, Color(0xFFA855F7), isCustom = true)
        }
    }

    // Listen for heartbeats
    LaunchedEffect(activeRoom?.code) {
        HeartbeatEventBus.events.collect { event ->
            if (event.roomCode == activeRoom?.code) {
                onlineMembers[event.userName] = event.timestamp
                
                if (activeRoom != null && activeRoom.members.none { it.name == event.userName }) {
                    val updatedMembers = activeRoom.members.toMutableList()
                    updatedMembers.add(Member(UUID.randomUUID().toString(), event.userName))
                    val updatedRoom = activeRoom.copy(members = updatedMembers)
                    prefs.updateRoom(updatedRoom)
                    rooms = prefs.getRooms()
                }
            }
        }
    }

    // Periodically remove stale offline members (>10s without heartbeat)
    LaunchedEffect(Unit) {
        while(true) {
            delay(2000)
            val now = System.currentTimeMillis()
            val offlineKeys = onlineMembers.filter { now - it.value > 10000 }.keys
            offlineKeys.forEach { onlineMembers.remove(it) }
        }
    }

    // Trigger ring action function
    val triggerRingAction = {
        if (activeRoom != null) {
            val isTargeted = selectedTargetMember != null
            val targetName = selectedTargetMember?.name ?: ""
            val activeCount = activeRoom.members.count { (onlineMembers.containsKey(it.name) || it.name == prefs.userName) && it.name != prefs.userName }

            val payload = JSONObject().apply {
                put("roomCode", activeRoom.code)
                put("senderName", prefs.userName)
                put("targetUserName", targetName)
                put("sound", prefs.getSettings().defaultChimeId)
                val intentObj = JSONObject().apply {
                    put("id", selectedIntention.id)
                    put("label", selectedIntention.label)
                    put("emoji", selectedIntention.emoji)
                }
                put("intention", intentObj)
                put("timestamp", System.currentTimeMillis().toString())
            }
            sendUdpBroadcast(payload)
            playToneAndHaptics(context, selectedIntention.id, prefs.getSettings().defaultChimeId, prefs.getSettings().copy(volume = 0f))

            // Delivered vs Not Delivered Pop-Up Confirmation logic
            if (isTargeted) {
                val isOnline = onlineMembers.containsKey(targetName)
                if (isOnline) {
                    onShowAck(DeliveryAckState(true, "Ring Delivered!", "Successfully delivered ring to ${targetName}."))
                } else {
                    onShowAck(DeliveryAckState(false, "Ring Not Delivered", "${targetName} is not online in this room."))
                }
            } else {
                if (activeCount > 0) {
                    onShowAck(DeliveryAckState(true, "Ring Delivered!", "Delivered ring to ${activeCount} active member(s)."))
                } else {
                    onShowAck(DeliveryAckState(false, "Ring Not Delivered", "No other active members in room to receive ring."))
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (rooms.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, theme.cardBorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(theme.bgColor)
                            .border(1.5.dp, theme.accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚪", fontSize = 30.sp)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "0 Rooms Found",
                        fontSize = 20.sp,
                        color = theme.textColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Create or join a room to start sending instant chimes.",
                        color = theme.subtextColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { navController.navigate("rooms") },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.bgColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.5.dp, theme.accentColor, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🚪", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Go to Rooms", color = theme.textColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Active Room Card
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.White, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { expanded = true }
                            ) {
                                Text(
                                    activeRoom?.name ?: "",
                                    fontSize = 22.sp,
                                    color = theme.textColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("▼", fontSize = 16.sp, color = theme.accentColor)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(theme.cardBg)
                            ) {
                                rooms.forEachIndexed { index, room ->
                                    DropdownMenuItem(
                                        text = { Text(room.name, color = theme.textColor) },
                                        onClick = {
                                            activeRoomIndex = index
                                            selectedTargetMember = null
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        val activeCount = (activeRoom?.members?.count { onlineMembers.containsKey(it.name) || it.name == prefs.userName } ?: 0)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Scan Members Button
                            IconButton(
                                onClick = {
                                    isScanningMembers = true
                                    if (activeRoom != null) {
                                        val payload = JSONObject().apply {
                                            put("type", "heartbeat")
                                            put("roomCode", activeRoom.code)
                                            put("userName", prefs.userName)
                                        }
                                        sendUdpBroadcast(payload)
                                    }
                                    Toast.makeText(context, "Scanning active room members...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(theme.bgColor)
                                    .border(1.dp, theme.accentColor, CircleShape)
                            ) {
                                Text("📡", fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(theme.bgColor)
                                    .border(1.dp, theme.accentColor, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(theme.accentColor))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("$activeCount Active", fontSize = 12.sp, color = theme.accentColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Member Selection Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SELECT MEMBER TO TARGET",
                            color = theme.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (selectedTargetMember != null) "Target: ${selectedTargetMember!!.name}" else "Target: All Members",
                            color = Color(0xFF00FF9D),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Member List Item Card (Scrollable for > 3 members on home screen itself)
                    val membersList = activeRoom?.members ?: emptyList()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // "All Room Members" Option
                            item {
                                val isSelected = selectedTargetMember == null
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) theme.accentColor.copy(alpha = 0.25f) else theme.bgColor)
                                        .border(1.dp, if (isSelected) theme.accentColor else theme.cardBorderColor, RoundedCornerShape(10.dp))
                                        .clickable { selectedTargetMember = null }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📢", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("All Room Members (Broadcast)", color = theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (isSelected) {
                                        Text("✅", fontSize = 14.sp)
                                    }
                                }
                            }

                            items(membersList) { member ->
                                val isOnline = onlineMembers.containsKey(member.name) || member.name == prefs.userName
                                val isSelected = selectedTargetMember?.id == member.id

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) theme.accentColor.copy(alpha = 0.25f) else theme.bgColor)
                                        .border(1.dp, if (isSelected) theme.accentColor else theme.cardBorderColor, RoundedCornerShape(10.dp))
                                        .clickable {
                                            // Select member - DOES NOT RING immediately!
                                            selectedTargetMember = if (isSelected) null else member
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(contentAlignment = Alignment.BottomEnd) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(theme.cardBg)
                                                    .border(1.dp, theme.cardBorderColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(member.name.take(1).uppercase(), color = theme.textColor, fontWeight = FontWeight.Bold)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isOnline) Color(0xFF00FF9D) else Color(0xFFEF4444))
                                                    .border(1.dp, theme.bgColor, CircleShape)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(member.name + (if (member.name == prefs.userName) " (You)" else ""), color = theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text(if (isOnline) "Active in room" else "Offline", color = theme.subtextColor, fontSize = 11.sp)
                                        }
                                    }

                                    if (isSelected) {
                                        Text("✅ Targeted", color = theme.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Intention & PRO Custom Message Selector Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "RING REASON / INTENTION",
                color = theme.accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "+ Custom Message ${if (prefs.isPro) "✨" else "🔒"}",
                color = if (prefs.isPro) Color(0xFFFFD700) else theme.subtextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    if (prefs.isPro) {
                        showCustomMsgDialog = true
                    } else {
                        navController.navigate("pro")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(allIntentions) { intention ->
                val isSelected = selectedIntention.id == intention.id
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) intention.color else theme.cardBg)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color.White else theme.cardBorderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedIntention = intention }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        "${intention.emoji} ${intention.label}",
                        color = if (isSelected) Color.White else theme.textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                    if (intention.isCustom) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "✖",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                prefs.deleteCustomIntention(intention.label)
                                customIntentionsList = prefs.getCustomIntentions()
                                if (selectedIntention.id == intention.id) {
                                    selectedIntention = DEFAULT_INTENTIONS_LIST[0]
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Big Main Interactive Bell Icon Button (Rings selected member!)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .border(4.dp, selectedIntention.color, CircleShape)
                    .clickable { triggerRingAction() },
                colors = CardDefaults.cardColors(containerColor = theme.cardBg),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(listOf(selectedIntention.color.copy(alpha = 0.4f), theme.cardBg))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔔", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "TAP TO RING",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (selectedTargetMember != null) "Tap bell to ring ${selectedTargetMember!!.name}" else "Tap bell to ring all room devices.",
            color = theme.subtextColor,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Developed by Risk Technologies © 2026 All Rights Reserved",
            color = theme.textColor.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    // PRO Custom Message Dialog
    if (showCustomMsgDialog) {
        AlertDialog(
            containerColor = theme.bgColor,
            titleContentColor = theme.textColor,
            textContentColor = theme.textColor,
            onDismissRequest = { showCustomMsgDialog = false },
            title = { Text("✨ Add PRO Custom Message", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select Emoji", fontSize = 12.sp, color = theme.accentColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(EMOJI_OPTIONS_LIST) { em ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedCustomEmoji == em) theme.accentColor.copy(alpha = 0.3f) else theme.cardBg)
                                    .border(1.dp, if (selectedCustomEmoji == em) theme.accentColor else theme.cardBorderColor, RoundedCornerShape(8.dp))
                                    .clickable { selectedCustomEmoji = em },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(em, fontSize = 18.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newCustomText,
                        onValueChange = { newCustomText = it },
                        label = { Text("Custom Message / Reason", color = theme.subtextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accentColor,
                            unfocusedBorderColor = theme.cardBorderColor,
                            focusedTextColor = theme.textColor,
                            unfocusedTextColor = theme.textColor
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustomText.isNotBlank()) {
                            prefs.addCustomIntention(newCustomText.trim(), selectedCustomEmoji)
                            customIntentionsList = prefs.getCustomIntentions()
                            selectedIntention = RingIntention("custom_${newCustomText.trim()}", newCustomText.trim(), selectedCustomEmoji, Color(0xFFA855F7), isCustom = true)
                            showCustomMsgDialog = false
                            newCustomText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.cardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.5.dp, theme.accentColor, RoundedCornerShape(10.dp))
                ) { Text("Save & Select", color = theme.textColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCustomMsgDialog = false },
                    modifier = Modifier.border(1.dp, theme.cardBorderColor, RoundedCornerShape(10.dp))
                ) { Text("Cancel", color = theme.textColor) }
            }
        )
    }
}

@Composable
fun RoomsScreen(prefs: SharedPrefsManager, theme: AppTheme, navController: NavHostController) {
    val context = LocalContext.current
    var rooms by remember { mutableStateOf(prefs.getRooms()) }
    var showAddRoom by remember { mutableStateOf(false) }
    var showScanRoomsModal by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    var newRoomCode by remember { mutableStateOf("") }
    var codeErrorMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Rooms", fontSize = 22.sp, color = theme.textColor, fontWeight = FontWeight.Bold)
            
            Row {
                Button(
                    onClick = { showScanRoomsModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x336366F1)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.dp, Color(0xFF6366F1), RoundedCornerShape(10.dp))
                ) {
                    Text("🔍 Scan", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showAddRoom = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300F0FF)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.5.dp, Brush.linearGradient(listOf(Color(0xFF00F0FF), Color(0xFFFF007F))), RoundedCornerShape(10.dp))
                ) {
                    Text("+ Add Room", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rooms Grid (Scrollable for > 3 rooms)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(rooms) { room ->
                Card(
                    modifier = Modifier
                        .height(100.dp)
                        .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFF00F0FF), Color(0xFFFF007F))), RoundedCornerShape(16.dp))
                        .clickable { navController.navigate("room/${room.id}") },
                    colors = CardDefaults.cardColors(containerColor = theme.cardBg.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(room.name, color = theme.textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Code: ${room.code}", color = theme.textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Add Room Dialog with Duplicate Room Code Check
    if (showAddRoom) {
        AlertDialog(
            containerColor = theme.bgColor,
            titleContentColor = theme.textColor,
            textContentColor = theme.textColor,
            onDismissRequest = { 
                showAddRoom = false 
                codeErrorMessage = ""
            },
            title = { Text("Add Room", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newRoomName, 
                        onValueChange = { newRoomName = it }, 
                        label = { Text("Room Name", color = theme.subtextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accentColor,
                            unfocusedBorderColor = theme.cardBorderColor,
                            focusedTextColor = theme.textColor,
                            unfocusedTextColor = theme.textColor
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newRoomCode, 
                            onValueChange = { 
                                val upper = it.uppercase()
                                newRoomCode = upper
                                if (rooms.any { r -> r.code.equals(upper, ignoreCase = true) }) {
                                    codeErrorMessage = "Room code '${upper}' already exists! Choose another code."
                                } else {
                                    codeErrorMessage = ""
                                }
                            }, 
                            label = { Text("Room Code", color = theme.subtextColor) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (codeErrorMessage.isNotEmpty()) Color(0xFFEF4444) else theme.accentColor,
                                unfocusedBorderColor = if (codeErrorMessage.isNotEmpty()) Color(0xFFEF4444) else theme.cardBorderColor,
                                focusedTextColor = theme.textColor,
                                unfocusedTextColor = theme.textColor
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {
                            val gen = (1000..9999).filter { g -> rooms.none { r -> r.code == g.toString() } }.random().toString()
                            newRoomCode = gen
                            codeErrorMessage = ""
                        }) {
                            Text("✨", fontSize = 18.sp)
                        }
                    }

                    if (codeErrorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(codeErrorMessage, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanCode = newRoomCode.trim().uppercase()
                        if (rooms.any { it.code.equals(cleanCode, ignoreCase = true) }) {
                            codeErrorMessage = "Room code '${cleanCode}' is already in use! Two rooms cannot share the same code."
                            return@Button
                        }
                        if (newRoomName.isNotBlank() && cleanCode.isNotBlank()) {
                            val newRoom = Room(UUID.randomUUID().toString(), newRoomName.trim(), cleanCode, emptyList())
                            prefs.addRoom(newRoom)
                            rooms = prefs.getRooms()
                            showAddRoom = false
                            newRoomName = ""
                            newRoomCode = ""
                            codeErrorMessage = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.cardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.5.dp, theme.accentColor, RoundedCornerShape(10.dp))
                ) { Text("Add", color = theme.textColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAddRoom = false
                        codeErrorMessage = ""
                    },
                    modifier = Modifier.border(1.dp, theme.cardBorderColor, RoundedCornerShape(10.dp))
                ) { Text("Cancel", color = theme.textColor) }
            }
        )
    }

    // Scan Available Rooms Dialog
    if (showScanRoomsModal) {
        AlertDialog(
            containerColor = theme.bgColor,
            titleContentColor = theme.textColor,
            textContentColor = theme.textColor,
            onDismissRequest = { showScanRoomsModal = false },
            title = { Text("📡 Scan Available Rooms", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Available Active Rooms on Network:", fontSize = 13.sp, color = theme.subtextColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (rooms.isEmpty()) {
                        Text("No saved rooms yet. Add a room code to get started!", color = theme.subtextColor, fontSize = 13.sp)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(rooms) { r ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(theme.cardBg)
                                        .border(1.dp, theme.cardBorderColor, RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(r.name, color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Code: ${r.code}", color = theme.accentColor, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            showScanRoomsModal = false
                                            navController.navigate("room/${r.id}")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = theme.bgColor),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.border(1.dp, theme.accentColor, RoundedCornerShape(8.dp))
                                    ) {
                                        Text("Join", color = theme.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScanRoomsModal = false }) {
                    Text("Close", color = theme.textColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun RoomScreen(
    roomId: String,
    prefs: SharedPrefsManager,
    theme: AppTheme,
    navController: NavHostController,
    onShowAck: (DeliveryAckState) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var room by remember { mutableStateOf(prefs.getRooms().find { it.id == roomId }) }
    
    // Custom intentions state
    var customIntentionsList by remember { mutableStateOf(prefs.getCustomIntentions()) }
    var selectedIntention by remember { mutableStateOf(DEFAULT_INTENTIONS_LIST[0]) }
    var showCustomMsgDialog by remember { mutableStateOf(false) }
    var newCustomText by remember { mutableStateOf("") }
    var selectedCustomEmoji by remember { mutableStateOf("💬") }

    val allIntentions = remember(customIntentionsList) {
        DEFAULT_INTENTIONS_LIST + customIntentionsList.map { (label, emoji) ->
            RingIntention("custom_${label}", label, emoji, Color(0xFFA855F7), isCustom = true)
        }
    }

    val onlineMembers = remember { mutableStateMapOf<String, Long>() }
    var selectedTargetMember by remember { mutableStateOf<Member?>(null) }
    var isScanningMembers by remember { mutableStateOf(false) }

    // Start listening for this room
    DisposableEffect(roomId) {
        val intent = Intent(context, RingListenerService::class.java).apply {
            putExtra("ROOM_CODE", room?.code ?: "")
        }
        room?.code?.let { if (it.isNotEmpty()) FirebaseMessaging.getInstance().subscribeToTopic(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        onDispose {
            context.stopService(intent)
        }
    }

    LaunchedEffect(Unit) {
        HeartbeatEventBus.events.collect { event ->
            if (event.roomCode == room?.code) {
                onlineMembers[event.userName] = event.timestamp
                
                if (room != null && room!!.members.none { it.name == event.userName }) {
                    val updatedMembers = room!!.members.toMutableList()
                    updatedMembers.add(Member(UUID.randomUUID().toString(), event.userName))
                    val updatedRoom = room!!.copy(members = updatedMembers)
                    prefs.updateRoom(updatedRoom)
                    room = updatedRoom
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while(true) {
            delay(2000)
            val now = System.currentTimeMillis()
            val offlineKeys = onlineMembers.filter { now - it.value > 10000 }.keys
            offlineKeys.forEach { onlineMembers.remove(it) }
        }
    }

    if (room == null) return

    val otherMembers = room!!.members.filter { it.name != prefs.userName }

    val triggerRingAction = {
        val targetName = selectedTargetMember?.name ?: ""
        val activeCount = otherMembers.count { onlineMembers.containsKey(it.name) }

        val payload = JSONObject().apply {
            put("roomCode", room!!.code)
            put("senderName", prefs.userName)
            put("targetUserName", targetName)
            put("sound", prefs.getSettings().defaultChimeId)
            val intentObj = JSONObject().apply {
                put("id", selectedIntention.id)
                put("label", selectedIntention.label)
                put("emoji", selectedIntention.emoji)
            }
            put("intention", intentObj)
            put("timestamp", System.currentTimeMillis().toString())
        }
        sendUdpBroadcast(payload)
        playToneAndHaptics(
            context,
            selectedIntention.id,
            prefs.getSettings().defaultChimeId,
            prefs.getSettings().copy(volume = 0f)
        )

        // Delivered / Not Delivered Pop-Up logic
        if (selectedTargetMember != null) {
            val isOnline = onlineMembers.containsKey(targetName)
            if (isOnline) {
                onShowAck(DeliveryAckState(true, "Ring Delivered!", "Successfully delivered ring to ${targetName}."))
            } else {
                onShowAck(DeliveryAckState(false, "Ring Not Delivered", "${targetName} is not online in this room."))
            }
        } else {
            if (activeCount > 0) {
                onShowAck(DeliveryAckState(true, "Ring Delivered!", "Delivered ring to ${activeCount} active member(s)."))
            } else {
                onShowAck(DeliveryAckState(false, "Ring Not Delivered", "No other active members in room to receive ring."))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top App Bar for Room
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Text("⬅️", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = room!!.name,
                        fontSize = 22.sp,
                        color = theme.textColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Code: ${room!!.code}",
                        color = theme.accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Scan Members Button
                IconButton(onClick = {
                    isScanningMembers = true
                    val payload = JSONObject().apply {
                        put("type", "heartbeat")
                        put("roomCode", room!!.code)
                        put("userName", prefs.userName)
                    }
                    sendUdpBroadcast(payload)
                    Toast.makeText(context, "Scanning members in room...", Toast.LENGTH_SHORT).show()
                }) {
                    Text("📡", fontSize = 20.sp)
                }

                IconButton(onClick = { navController.navigate("settings") }) {
                    Text("⚙️", fontSize = 22.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // Share Code & Waiting Banner Card
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, theme.cardBorderColor, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔑", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Share Room Code: ${room!!.code}",
                            color = theme.textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Give this code to others so they can join and ring.",
                        color = theme.subtextColor,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(room!!.code))
                        Toast.makeText(context, "Room code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.bgColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.dp, theme.accentColor, RoundedCornerShape(10.dp))
                ) {
                    Text("Copy 📋", color = theme.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Intentions Row & PRO Custom Message Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SELECT INTENTION",
                color = theme.accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "+ Custom Message ${if (prefs.isPro) "✨" else "🔒"}",
                color = if (prefs.isPro) Color(0xFFFFD700) else theme.subtextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    if (prefs.isPro) {
                        showCustomMsgDialog = true
                    } else {
                        navController.navigate("pro")
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(allIntentions) { intention ->
                val isSelected = selectedIntention.id == intention.id
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) intention.color else theme.cardBg)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color.White else theme.cardBorderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedIntention = intention }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        "${intention.emoji} ${intention.label}",
                        color = if (isSelected) Color.White else theme.textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                    if (intention.isCustom) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "✖",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                prefs.deleteCustomIntention(intention.label)
                                customIntentionsList = prefs.getCustomIntentions()
                                if (selectedIntention.id == intention.id) {
                                    selectedIntention = DEFAULT_INTENTIONS_LIST[0]
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SELECT MEMBER TO TARGET",
                color = theme.accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (selectedTargetMember != null) "Target: ${selectedTargetMember!!.name}" else "Target: All Members",
                color = Color(0xFF00FF9D),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Members List (Scrollable for > 3 members)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // "All Room Members" option
                item {
                    val isSelected = selectedTargetMember == null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.accentColor.copy(alpha = 0.25f) else theme.cardBg)
                            .border(1.dp, if (isSelected) theme.accentColor else theme.cardBorderColor, RoundedCornerShape(12.dp))
                            .clickable { selectedTargetMember = null }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📢", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("All Room Members (Broadcast)", color = theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        if (isSelected) {
                            Text("✅", fontSize = 14.sp)
                        }
                    }
                }

                items(otherMembers) { member ->
                    val isOnline = onlineMembers.containsKey(member.name)
                    val isSelected = selectedTargetMember?.id == member.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.accentColor.copy(alpha = 0.25f) else theme.cardBg)
                            .border(
                                1.5.dp,
                                if (isSelected) theme.accentColor else if (isOnline) Color(0xFF00FF9D).copy(alpha = 0.5f) else theme.cardBorderColor,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                // Select member - DOES NOT RING immediately!
                                selectedTargetMember = if (isSelected) null else member
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(theme.bgColor)
                                        .border(1.dp, theme.cardBorderColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        member.name.take(1).uppercase(),
                                        color = theme.textColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) Color(0xFF00FF9D) else Color(0xFFEF4444))
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    member.name,
                                    color = theme.textColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (isOnline) "Active in room" else "Offline",
                                    color = theme.subtextColor,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Text("✅ Targeted", color = theme.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Select 👤", color = theme.subtextColor, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Bell Button to trigger ring for selected target
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(4.dp, selectedIntention.color, CircleShape)
                    .clickable { triggerRingAction() },
                colors = CardDefaults.cardColors(containerColor = theme.cardBg),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(listOf(selectedIntention.color.copy(alpha = 0.4f), theme.cardBg))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔔", fontSize = 56.sp)
                        Text(
                            "RING BELL",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                prefs.deleteRoom(room!!.id)
                navController.popBackStack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = theme.bgColor),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(14.dp))
        ) {
            Text("🚪 Leave Room", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
        }
    }

    // PRO Custom Message Dialog
    if (showCustomMsgDialog) {
        AlertDialog(
            containerColor = theme.bgColor,
            titleContentColor = theme.textColor,
            textContentColor = theme.textColor,
            onDismissRequest = { showCustomMsgDialog = false },
            title = { Text("✨ Add PRO Custom Message", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select Emoji", fontSize = 12.sp, color = theme.accentColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(EMOJI_OPTIONS_LIST) { em ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedCustomEmoji == em) theme.accentColor.copy(alpha = 0.3f) else theme.cardBg)
                                    .border(1.dp, if (selectedCustomEmoji == em) theme.accentColor else theme.cardBorderColor, RoundedCornerShape(8.dp))
                                    .clickable { selectedCustomEmoji = em },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(em, fontSize = 18.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newCustomText,
                        onValueChange = { newCustomText = it },
                        label = { Text("Custom Message / Reason", color = theme.subtextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accentColor,
                            unfocusedBorderColor = theme.cardBorderColor,
                            focusedTextColor = theme.textColor,
                            unfocusedTextColor = theme.textColor
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustomText.isNotBlank()) {
                            prefs.addCustomIntention(newCustomText.trim(), selectedCustomEmoji)
                            customIntentionsList = prefs.getCustomIntentions()
                            selectedIntention = RingIntention("custom_${newCustomText.trim()}", newCustomText.trim(), selectedCustomEmoji, Color(0xFFA855F7), isCustom = true)
                            showCustomMsgDialog = false
                            newCustomText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.cardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.5.dp, theme.accentColor, RoundedCornerShape(10.dp))
                ) { Text("Save & Select", color = theme.textColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCustomMsgDialog = false },
                    modifier = Modifier.border(1.dp, theme.cardBorderColor, RoundedCornerShape(10.dp))
                ) { Text("Cancel", color = theme.textColor) }
            }
        )
    }
}

@Composable
fun SettingsScreen(prefs: SharedPrefsManager, theme: AppTheme, navController: NavHostController) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(prefs.getSettings()) }
    val textColor = theme.textColor

    var chimeMenuExpanded by remember { mutableStateOf(false) }
    val chimeOptions = listOf(
        "classic" to "Classic Bell 🔔",
        "hotel" to "Hotel Service Bell 🛎️",
        "beep" to "Beep Alert 📳",
        "chirp" to "Chirp Chime 🐥",
        "alarm" to "Emergency Alarm 🚨"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Chime Settings", fontSize = 22.sp, color = theme.accentColor, fontWeight = FontWeight.Bold)
        Text("Manage audio, haptics, permissions, and app preferences.", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Custom Audio Bento Card
        Text("Custom Audio", color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, theme.cardBorderColor, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Master Volume
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔊", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Master Volume", color = textColor, fontWeight = FontWeight.SemiBold)
                    }
                    Text("${(settings.volume * 100).toInt()}%", color = theme.accentColor, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = settings.volume,
                    onValueChange = { 
                        settings = settings.copy(volume = it)
                        prefs.saveSettings(settings)
                    },
                    onValueChangeFinished = { playToneAndHaptics(context, "test", settings.defaultChimeId, settings) },
                    valueRange = 0f..1f
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Default Chime Audio Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎵", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Default Chime Sound", color = textColor, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { chimeMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, theme.cardBorderColor)
                        ) {
                            Text(
                                chimeOptions.find { it.first == settings.defaultChimeId }?.second ?: "Classic Bell 🔔",
                                color = theme.textColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        DropdownMenu(
                            expanded = chimeMenuExpanded,
                            onDismissRequest = { chimeMenuExpanded = false },
                            modifier = Modifier.background(theme.cardBg)
                        ) {
                            chimeOptions.forEach { (id, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, color = theme.textColor) },
                                    onClick = {
                                        settings = settings.copy(defaultChimeId = id)
                                        prefs.saveSettings(settings)
                                        chimeMenuExpanded = false
                                        playToneAndHaptics(context, "test", id, settings)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { playToneAndHaptics(context, "test", settings.defaultChimeId, settings) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(theme.bgColor)
                            .border(1.dp, theme.accentColor, CircleShape)
                    ) {
                        Text("▶️", fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Messages Section (Below Custom Audio)
        var customMsgsList by remember { mutableStateOf(prefs.getCustomIntentions()) }
        var showAddCustomDialogSettings by remember { mutableStateOf(false) }
        var settingCustomText by remember { mutableStateOf("") }
        var settingCustomEmoji by remember { mutableStateOf("💬") }

        Text("Custom Messages", color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, theme.cardBorderColor, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PRO Custom Ring Messages", color = textColor, fontWeight = FontWeight.SemiBold)
                    }
                    if (prefs.isPro) {
                        Text("✨ PRO ACTIVE", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("🔒 PRO ONLY", color = theme.subtextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (customMsgsList.isEmpty()) {
                    Text(
                        "No custom ring messages created yet. Create personalized ring reasons for your household.",
                        color = theme.subtextColor,
                        fontSize = 12.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        customMsgsList.forEach { (label, emoji) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(theme.bgColor)
                                    .border(1.dp, theme.cardBorderColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(emoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(label, color = theme.textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = {
                                        prefs.deleteCustomIntention(label)
                                        customMsgsList = prefs.getCustomIntentions()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("🗑️", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (prefs.isPro) {
                            showAddCustomDialogSettings = true
                        } else {
                            navController.navigate("pro")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.bgColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (prefs.isPro) theme.accentColor else theme.cardBorderColor, RoundedCornerShape(10.dp))
                ) {
                    Text(
                        if (prefs.isPro) "+ Add Custom Message ✨" else "Unlock Custom Messages (PRO)",
                        color = if (prefs.isPro) theme.accentColor else theme.textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (showAddCustomDialogSettings) {
            AlertDialog(
                containerColor = theme.bgColor,
                titleContentColor = theme.textColor,
                textContentColor = theme.textColor,
                onDismissRequest = { showAddCustomDialogSettings = false },
                title = { Text("✨ Add Custom Ring Message", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Select Emoji", fontSize = 12.sp, color = theme.accentColor, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(EMOJI_OPTIONS_LIST) { em ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (settingCustomEmoji == em) theme.accentColor.copy(alpha = 0.3f) else theme.cardBg)
                                        .border(1.dp, if (settingCustomEmoji == em) theme.accentColor else theme.cardBorderColor, RoundedCornerShape(8.dp))
                                        .clickable { settingCustomEmoji = em },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(em, fontSize = 18.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = settingCustomText,
                            onValueChange = { settingCustomText = it },
                            label = { Text("Custom Message / Reason", color = theme.subtextColor) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = theme.accentColor,
                                unfocusedBorderColor = theme.cardBorderColor,
                                focusedTextColor = theme.textColor,
                                unfocusedTextColor = theme.textColor
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (settingCustomText.isNotBlank()) {
                                prefs.addCustomIntention(settingCustomText.trim(), settingCustomEmoji)
                                customMsgsList = prefs.getCustomIntentions()
                                showAddCustomDialogSettings = false
                                settingCustomText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.cardBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.border(1.5.dp, theme.accentColor, RoundedCornerShape(10.dp))
                    ) { Text("Save Message", color = theme.textColor, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddCustomDialogSettings = false },
                        modifier = Modifier.border(1.dp, theme.cardBorderColor, RoundedCornerShape(10.dp))
                    ) { Text("Cancel", color = theme.textColor) }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Haptics Bento Card
        Text("Haptics & Vibration", color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, theme.cardBorderColor, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📳", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable Vibration", color = textColor, fontWeight = FontWeight.SemiBold)
                    }
                    Switch(
                        checked = settings.isVibrationEnabled,
                        onCheckedChange = {
                            settings = settings.copy(isVibrationEnabled = it)
                            prefs.saveSettings(settings)
                            playToneAndHaptics(context, "test", settings.defaultChimeId, settings)
                        }
                    )
                }

                if (settings.isVibrationEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Haptic Intensity", color = textColor, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${(settings.hapticsIntensity * 100).toInt()}%", color = theme.accentColor, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = settings.hapticsIntensity,
                        onValueChange = {
                            settings = settings.copy(hapticsIntensity = it)
                            prefs.saveSettings(settings)
                        },
                        onValueChangeFinished = { playToneAndHaptics(context, "test", settings.defaultChimeId, settings) },
                        valueRange = 0.1f..1.0f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Permissions Section Card (Moved from ProScreen to Settings)
        Text("Permissions", color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, theme.cardBorderColor, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.bgColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, theme.accentColor, RoundedCornerShape(14.dp))
                ) {
                    Text("🔋 Disable Battery Restrictions", color = theme.textColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.bgColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, theme.accentColor, RoundedCornerShape(14.dp))
                ) {
                    Text("🔔 Enable Alerts & Notifications", color = theme.textColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. About Tab Card (Navigates to AboutScreen)
        Text("App Information", color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, theme.cardBorderColor, RoundedCornerShape(12.dp))
                .clickable { navController.navigate("about") },
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ℹ️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("About Calling Bell", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Version 1.0.0 • Developed by Risk Tech", color = theme.subtextColor, fontSize = 12.sp)
                    }
                }
                Text("➔", color = theme.accentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AboutScreen(theme: AppTheme, navController: NavHostController) {
    val textColor = theme.textColor
    Column(modifier = Modifier.fillMaxSize().background(theme.bgColor).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) { Text("⬅️", fontSize = 24.sp) }
            Spacer(modifier = Modifier.width(8.dp))
            Text("About", fontSize = 22.sp, color = textColor, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(30.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, theme.cardBorderColor, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(105.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Calling Bell", fontSize = 28.sp, color = textColor, fontWeight = FontWeight.Bold)
                Text("Version 1.0.0", fontSize = 14.sp, color = textColor.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(24.dp))
                Text("A seamless local Wi-Fi ringing solution for homes and offices.", color = textColor, modifier = Modifier.padding(horizontal = 16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Text("© 2026 Risk Technologies. All Rights Reserved", fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun ProScreen(prefs: SharedPrefsManager, theme: AppTheme, navController: NavHostController) {
    val textColor = theme.textColor

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("PRO Features", fontSize = 22.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
        Text("Unlock premium capabilities and power tools.", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Unlock PRO Bento Card
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFF8C00))), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock PRO", fontSize = 24.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Elevate your experience with Risk Technologies' premium communication suite.",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Feature Checklist with explicit Coming Soon Badges
                val features = listOf(
                    Triple("♾️ Unlimited Rooms & Members", false, ""),
                    Triple("🎵 Custom Audio & Chimes", false, ""),
                    Triple("💬 Custom Messages & Intentions", false, ""),
                    Triple("⏱️ Scheduled Chimes", true, "COMING SOON"),
                    Triple("🤖 AI Assistant Integration", true, "COMING SOON")
                )

                features.forEach { (featureName, isComingSoon, badgeText) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.cardBg.copy(alpha = 0.3f))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(featureName, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        if (isComingSoon) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    badgeText,
                                    color = Color(0xFFF59E0B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { prefs.isPro = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFD700)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFF8C00))), RoundedCornerShape(14.dp))
                ) {
                    Text(if (prefs.isPro) "PRO ACTIVE" else "UPGRADE NOW", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SplashScreen(navController: NavHostController, prefs: SharedPrefsManager, theme: AppTheme) {
    LaunchedEffect(Unit) {
        delay(2000)
        if (prefs.userName.isEmpty()) {
            navController.navigate("login") { popUpTo("splash") { inclusive = true } }
        } else {
            navController.navigate("dashboard") { popUpTo("splash") { inclusive = true } }
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(theme.bgColor), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(130.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text("Calling Bell", fontSize = 28.sp, color = theme.textColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Instant Chimes. One Tap Away.", fontSize = 15.sp, color = theme.accentColor, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(color = theme.accentColor)
        }
        Text(
            "Developed by Risk Technologies © 2026 All Rights Reserved",
            color = theme.textColor.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}
