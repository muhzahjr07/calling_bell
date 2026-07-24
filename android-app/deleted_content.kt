import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
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
data class RingIntention(val id: String, val label: String, val emoji: String, val color: Color)
val INTENTIONS_LIST = listOf(
    RingIntention("breakfast", "Breakfast", "🍳", Color(0xFFF59E0B)),
    RingIntention("lunch", "Lunch", "🍲", Color(0xFF10B981)),
    RingIntention("dinner", "Dinner", "🍽️", Color(0xFF3B82F6)),
    RingIntention("tea", "Tea Time", "🍵", Color(0xFF8B5CF6)),
    RingIntention("ready", "Ready", "✅", Color(0xFF14B8A6)),
    RingIntention("general", "Ring", "🔔", Color(0xFF6366F1)),
    RingIntention("coffee", "Coffee", "☕", Color(0xFFA16207)),
    RingIntention("urgent", "Urgent", "🚨", Color(0xFFEF4444))
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
    
    var currentTheme by remember { 
        mutableStateOf(
            try { AppTheme.valueOf(prefs.getSettings().themeId) } 
            catch(e: Exception) { AppTheme.MIDNIGHT }
        ) 
    }
    var incomingAlertText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        RingEventBus.events.collect { event ->
            incomingAlertText = "${event.emoji} ${event.senderName}: ${event.intentLabel}!"
            delay(4000)
            incomingAlertText = null
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
                        DashboardScreen(prefs, currentTheme, navController)
                    }
                    composable("rooms") {
                        RoomsScreen(prefs, currentTheme, navController)
                    }
                    composable("room/{roomId}") { backStackEntry ->
                        val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                        RoomScreen(roomId, prefs, currentTheme, navController)
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

        // Global Alert Overlay
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
fun DashboardScreen(prefs: SharedPrefsManager, theme: AppTheme, navController: NavHostController) {
    val context = LocalContext.current
    var rooms by remember { mutableStateOf(prefs.getRooms()) }
    var activeRoomIndex by remember { mutableStateOf(0) }
    val activeRoom = rooms.getOrNull(activeRoomIndex)
    var expanded by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    var newRoomCode by remember { mutableStateOf("") }

    val quickChimes = listOf(
        Triple("breakfast", "Breakfast 🍳", Color(0xFFF97316)),
        Triple("lunch", "Lunch 🍲", Color(0xFFEAB308)),
        Triple("dinner", "Dinner 🍽️", Color(0xFFFF4A8D)),
        Triple("tea", "Tea Time 🍵", Color(0xFF00F0FF)),
        Triple("ready", "Ready ✅", Color(0xFF00FF9D)),
        Triple("general", "Ring 🔔", Color(0xFF3B82F6))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (rooms.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, theme.cardBorderColor, RoundedCornerShape(14.dp))
                    .clickable { navController.navigate("rooms") },
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("0 Rooms Found", fontSize = 20.sp, color = theme.textColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Create or join a room to start ringing.", color = theme.subtextColor, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate("rooms") },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.cardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.5.dp, theme.accentColor, RoundedCornerShape(12.dp))
                    ) {
                        Text("Go to Rooms", color = theme.textColor)
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
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        val activeCount = activeRoom?.members?.size ?: 0
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Member List Item Card
                    val membersList = activeRoom?.members ?: emptyList()

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        membersList.take(3).forEachIndexed { idx, member ->
                            // Simulate online detection: if it's the current user, or random
                            val isOnline = true 
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (idx == 0) Color(0x33FFFFFF) else theme.bgColor)
                                    .border(1.dp, if (idx == 0) theme.accentColor else theme.cardBorderColor, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(theme.cardBg)
                                                .border(1.dp, theme.cardBorderColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(member.name.take(1), color = theme.textColor, fontWeight = FontWeight.Bold)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (isOnline) Color(0xFF00FF9D) else Color.Red)
                                                .border(1.dp, theme.bgColor, CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(member.name, color = theme.textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text(if (isOnline) "Active in room" else "Offline", color = theme.subtextColor, fontSize = 12.sp)
                                    }
                                }

                                IconButton(onClick = {
                                    Toast.makeText(context, "Ringing ${member.name}...", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text("🔔", fontSize = 18.sp)
                                }
                            }
                        }
                        if (membersList.isEmpty()) {
                            Text("No members in this room yet.", color = theme.subtextColor, fontSize = 14.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Quick Chime Grid
        Text(
            "GIVE A RING",
            color = theme.accentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(quickChimes) { (id, label, borderClr) ->
                Card(
                    modifier = Modifier
                        .height(85.dp)
                        .border(1.dp, theme.cardBorderColor, RoundedCornerShape(12.dp))
                        .clickable {
                            playToneAndHaptics(context, id, prefs.getSettings().defaultChimeId, prefs.getSettings())
                            Toast.makeText(context, "Chime Sent: $label", Toast.LENGTH_SHORT).show()
                        },
                    colors = CardDefaults.cardColors(containerColor = theme.cardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(label, color = theme.textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Developed by Risk Technologies © 2026 All Rights Reserved",
            color = theme.textColor.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun RoomsScreen(prefs: SharedPrefsManager, theme: AppTheme, navController: NavHostController) {
    val context = LocalContext.current
    var rooms by remember { mutableStateOf(prefs.getRooms()) }
    var showAddRoom by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    var newRoomCode by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Rooms", fontSize = 22.sp, color = theme.textColor, fontWeight = FontWeight.Bold)
            Button(
                onClick = { showAddRoom = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300F0FF)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.border(1.5.dp, Brush.linearGradient(listOf(Color(0xFF00F0FF), Color(0xFFFF007F))), RoundedCornerShape(10.dp))
            ) {
                Text("+ Add Room", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        // Sign out moved to top app bar
    }

    if (showAddRoom) {
        AlertDialog(
            containerColor = theme.bgColor,
            titleContentColor = theme.textColor,
            textContentColor = theme.textColor,
            onDismissRequest = { showAddRoom = false },
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
                    OutlinedTextField(
                        value = newRoomCode, 
                        onValueChange = { newRoomCode = it.uppercase() }, 
                        label = { Text("Room Code", color = theme.subtextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accentColor,
                            unfocusedBorderColor = theme.cardBorderColor,
                            focusedTextColor = theme.textColor,
                            unfocusedTextColor = theme.textColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRoomName.isNotBlank() && newRoomCode.isNotBlank()) {
                            val newRoom = Room(UUID.randomUUID().toString(), newRoomName, newRoomCode, emptyList())
                            prefs.addRoom(newRoom)
                            rooms = prefs.getRooms()
                            showAddRoom = false
                            newRoomName = ""
                            newRoomCode = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.cardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.5.dp, theme.accentColor, RoundedCornerShape(10.dp))
                ) { Text("Add", color = theme.textColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddRoom = false },
                    modifier = Modifier.border(1.dp, theme.cardBorderColor, RoundedCornerShape(10.dp))
                ) { Text("Cancel", color = theme.textColor) }
            }
        )
    }
}

@Composable
fun RoomScreen(roomId: String, prefs: SharedPrefsManager, theme: AppTheme, navController: NavHostController) {
    val context = LocalContext.current
    var room by remember { mutableStateOf(prefs.getRooms().find { it.id == roomId }) }
    var selectedIntention by remember { mutableStateOf(INTENTIONS_LIST[0]) }
    var showAddMember by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }
    
    val onlineMembers = remember { mutableStateMapOf<String, Long>() }

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
                
                // Auto-add new members
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) { Text("⬅️", fontSize = 24.sp) }
                Spacer(modifier = Modifier.width(8.dp))
                Text(room!!.name, fontSize = 22.sp, color = theme.textColor, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { navController.navigate("settings") }) { Text("⚙️", fontSize = 24.sp) }
        }
        Text("Code: ${room!!.code}", color = theme.textColor.copy(alpha = 0.7f), modifier = Modifier.padding(start = 48.dp))
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Intentions Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(INTENTIONS_LIST) { intention ->
                val isSelected = selectedIntention.id == intention.id
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) intention.color else Color.DarkGray)
                        .clickable { selectedIntention = intention }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("${intention.emoji} ${intention.label}", color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Tap a member to ring them", color = theme.textColor, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(room!!.members) { member ->
                val isOnline = onlineMembers.containsKey(member.name)
                Card(
                    modifier = Modifier.height(100.dp).clickable {
                        // Ring logic
                        val payload = JSONObject().apply {
                            put("roomCode", room!!.code)
                            put("senderName", prefs.userName)
                            put("targetUserName", member.name)
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
                        // sender feedback
                        playToneAndHaptics(context, selectedIntention.id, prefs.getSettings().defaultChimeId, prefs.getSettings().copy(volume = 0f))
                        Toast.makeText(context, "Ringing ${member.name}...", Toast.LENGTH_SHORT).show()
                    },
                    colors = CardDefaults.cardColors(containerColor = theme.accentColor.copy(alpha = if (isOnline) 0.8f else 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (isOnline) Color.Green else Color.Red))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(member.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .height(80.dp)
                        .border(1.dp, theme.cardBorderColor, RoundedCornerShape(12.dp))
                        .clickable { showAddMember = true },
                    colors = CardDefaults.cardColors(containerColor = theme.bgColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("➕ Add", color = theme.accentColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                prefs.deleteRoom(room!!.id)
                navController.popBackStack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = theme.cardBg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(14.dp))
        ) {
            Text("Leave Room", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
        }
    }

    if (showAddMember) {
        AlertDialog(
            containerColor = theme.bgColor,
            titleContentColor = theme.textColor,
            textContentColor = theme.textColor,
            onDismissRequest = { showAddMember = false },
            title = { Text("Add Member") },
            text = {
                OutlinedTextField(
                    value = newMemberName, 
                    onValueChange = { newMemberName = it }, 
                    label = { Text("Member Name", color = theme.subtextColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.cardBorderColor,
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMemberName.isNotBlank() && room!!.members.none { it.name == newMemberName }) {
                            val updatedMembers = room!!.members.toMutableList()
                            updatedMembers.add(Member(UUID.randomUUID().toString(), newMemberName))
                            val updatedRoom = room!!.copy(members = updatedMembers)
                            prefs.updateRoom(updatedRoom)
                            room = updatedRoom
                            showAddMember = false
                            newMemberName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.cardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.5.dp, theme.accentColor, RoundedCornerShape(10.dp))
                ) { Text("Add", color = theme.textColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddMember = false },
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Chime Settings", fontSize = 22.sp, color = theme.accentColor, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))

        // Custom Audio & Haptics Bento
        Text("Custom Audio & Haptics", color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            modifier = Modifier.fillMaxWidth().border(1.5.dp, theme.cardBorderColor, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
    val context = LocalContext.current
        Text("Permissions & PRO", fontSize = 22.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
        Text("Manage system alerts and unlock premium features.", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
        // Permissions Bento
        Text("Permissions", color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            modifier = Modifier.fillMaxWidth().border(1.5.dp, theme.cardBorderColor, RoundedCornerShape(12.dp)),
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
                    Text("Disable Battery Restrictions", color = theme.textColor, fontWeight = FontWeight.Bold)
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
                    Text("Enable Alerts & Notifications", color = theme.textColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

                listOf("♾️ Unlimited Rooms & Members", "⏱️ Scheduled Chimes", "🤖 AI Assistant Integration").forEach { feature ->
