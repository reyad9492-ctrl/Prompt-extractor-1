package com.example

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ui.theme.CutieBackground
import com.example.ui.theme.CutieBorder
import com.example.ui.theme.CutieBorderPink
import com.example.ui.theme.CutieGold
import com.example.ui.theme.CutieLavender
import com.example.ui.theme.CutiePink
import com.example.ui.theme.CutiePinkDark
import com.example.ui.theme.CutiePinkLight
import com.example.ui.theme.CutieSurface
import com.example.ui.theme.CutieSurfaceCard
import com.example.ui.theme.CutieSurfaceElevated
import com.example.ui.theme.CutieViolet
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// Data model for history items
data class HistoryPromptItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val prompt: String,
    val engine: String,
    val vibe: String,
    val aspectRatio: String,
    val previewBitmap: Bitmap? = null,
    val timestamp: Long = System.currentTimeMillis(),
    var isFavorite: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppHost()
            }
        }
    }
}

@Composable
fun MainAppHost() {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("Studio") }

    // Shared Application State
    var profileName by remember { mutableStateOf("Elma the cutie") }
    var profileBio by remember { mutableStateOf("AI Prompt Muse & Fashionista ✨ Crafting dreamy cinema looks 💕") }
    var profileAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var customApiKey by remember { mutableStateOf("") }
    var selectedEngine by remember { mutableStateOf("ChatGPT") }
    var selectedStyleVibe by remember { mutableStateOf("🌸 Soft Glam Pastel") }
    var selectedAspectRatio by remember { mutableStateOf("9:16") }

    // Saved History
    val historyList = remember { mutableStateListOf<HistoryPromptItem>() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CutieBackground),
        bottomBar = {
            CutieBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CutieBackground)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "Studio" -> {
                    StudioScreen(
                        profileName = profileName,
                        customApiKey = customApiKey,
                        selectedEngine = selectedEngine,
                        onEngineChange = { selectedEngine = it },
                        selectedStyleVibe = selectedStyleVibe,
                        onStyleVibeChange = { selectedStyleVibe = it },
                        selectedAspectRatio = selectedAspectRatio,
                        onAspectRatioChange = { selectedAspectRatio = it },
                        onNavigateToProfile = { currentTab = "Profile" },
                        onPromptGenerated = { item ->
                            historyList.add(0, item)
                        }
                    )
                }
                "History" -> {
                    HistoryVaultScreen(
                        historyList = historyList,
                        onNavigateToStudio = { currentTab = "Studio" }
                    )
                }
                "Profile" -> {
                    CutieProfileScreen(
                        profileName = profileName,
                        onProfileNameChange = { profileName = it },
                        profileBio = profileBio,
                        onProfileBioChange = { profileBio = it },
                        profileAvatarUri = profileAvatarUri,
                        onProfileAvatarChange = { profileAvatarUri = it },
                        customApiKey = customApiKey,
                        onCustomApiKeyChange = { customApiKey = it },
                        selectedEngine = selectedEngine,
                        onEngineChange = { selectedEngine = it },
                        selectedAspectRatio = selectedAspectRatio,
                        onAspectRatioChange = { selectedAspectRatio = it },
                        historyCount = historyList.size,
                        onApplyVibeToStudio = { vibe ->
                            selectedStyleVibe = vibe
                            currentTab = "Studio"
                        }
                    )
                }
            }
        }
    }
}

/**
 * Ultra Cutie & Premium Bottom Navigation Bar
 */
@Composable
fun CutieBottomNavigationBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        color = CutieSurface,
        border = BorderStroke(1.dp, CutieBorderPink.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Studio Tab
            CutieNavItem(
                icon = Icons.Default.AutoAwesome,
                label = "Studio",
                isSelected = currentTab == "Studio",
                onClick = { onTabSelected("Studio") },
                testTag = "nav_studio_tab"
            )

            // History Tab
            CutieNavItem(
                icon = Icons.Default.Favorite,
                label = "Cutie Vault",
                isSelected = currentTab == "History",
                onClick = { onTabSelected("History") },
                testTag = "nav_history_tab"
            )

            // Profile Tab
            CutieNavItem(
                icon = Icons.Default.Face,
                label = "Elma VIP",
                isSelected = currentTab == "Profile",
                onClick = { onTabSelected("Profile") },
                testTag = "nav_profile_tab"
            )
        }
    }
}

@Composable
fun CutieNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 42.dp else 36.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        Brush.linearGradient(
                            listOf(CutiePink, CutieViolet)
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.Transparent)
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else CutieLavender.copy(alpha = 0.65f),
                modifier = Modifier.size(if (isSelected) 22.dp else 20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) CutiePinkLight else CutieLavender.copy(alpha = 0.7f)
        )
    }
}

/**
 * Custom Dashed Border Modifier for Cute Upload Areas
 */
fun Modifier.dashedBorder(
    color: Color,
    strokeWidth: androidx.compose.ui.unit.Dp,
    dashLength: androidx.compose.ui.unit.Dp = 12.dp,
    gapLength: androidx.compose.ui.unit.Dp = 8.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 18.dp
): Modifier = this.drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength.toPx(), gapLength.toPx()), 0f)
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx())
    )
}

/**
 * -------------------------------------------------------------
 * 1. STUDIO SCREEN (CREATE PROMPTS & AI VISUAL SYNTHESIS)
 * NOTE: Profile details strictly removed from here into Profile Screen.
 * -------------------------------------------------------------
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    profileName: String,
    customApiKey: String,
    selectedEngine: String,
    onEngineChange: (String) -> Unit,
    selectedStyleVibe: String,
    onStyleVibeChange: (String) -> Unit,
    selectedAspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onPromptGenerated: (HistoryPromptItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var instagramUrl by remember { mutableStateOf("") }
    var userPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var stylePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var customPrefix by remember { mutableStateOf("") }

    var isGenerating by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var generationResult by remember { mutableStateOf<PromptGenerationResult?>(null) }
    var promptCopied by remember { mutableStateOf(false) }

    // Visual Preview states
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingPreview by remember { mutableStateOf(false) }
    var previewErrorMessage by remember { mutableStateOf<String?>(null) }
    var isFullScreenPreviewVisible by remember { mutableStateOf(false) }
    var activeReportTab by remember { mutableIntStateOf(0) }

    val engines = listOf(
        "ChatGPT" to "✨",
        "Gemini" to "🌸"
    )

    val vibes = listOf(
        "🌸 Soft Glam Pastel",
        "👑 Royal Cutie",
        "✨ Dreamy Anime",
        "🎀 Cyber Kawaii",
        "🍓 Sweet Strawberry",
        "💎 Luxury Chic",
        "🦄 Fantasy Glow",
        "🧸 Vintage Doll"
    )

    val aspectRatios = listOf(
        "9:16" to "Story / Reel",
        "1:1" to "Square Feed",
        "16:9" to "Cinema Wide",
        "4:5" to "Portrait Post",
        "3:4" to "Editorial"
    )

    val singlePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) userPhotoUri = uri
    }

    val stylePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) stylePhotoUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Studio Glam Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(CutiePinkDark, Color(0xFF4C1D95))
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .border(BorderStroke(1.dp, CutiePink.copy(alpha = 0.6f)), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "✨ CUTIE PROMPT STUDIO ✨",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CutiePinkLight,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Vision Prompt Blender",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Created with 💕 by Riyad Bhai for $profileName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CutieLavender
                    )
                }
            }

            // Quick Jump to Profile Button
            IconButton(
                onClick = onNavigateToProfile,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(CutiePink, CutieViolet)
                        )
                    )
                    .border(BorderStroke(1.dp, CutieGold), CircleShape)
                    .testTag("studio_quick_profile_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Cutie Profile",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 1. Instagram Video Context Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("instagram_source_card"),
            colors = CardDefaults.cardColors(containerColor = CutieSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CutieBorderPink.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF8B5CF6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Instagram",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INSTAGRAM REEL / POST",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CutiePinkLight
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF3B1D54), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OPTIONAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CutieLavender
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = instagramUrl,
                    onValueChange = { instagramUrl = it },
                    placeholder = {
                        Text(
                            text = "Paste Instagram Reel or Post URL...",
                            fontSize = 12.sp,
                            color = CutieLavender.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("instagram_url_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CutiePink,
                        unfocusedBorderColor = CutieBorder,
                        focusedContainerColor = CutieSurfaceElevated,
                        unfocusedContainerColor = CutieSurfaceElevated
                    ),
                    trailingIcon = {
                        if (instagramUrl.isNotEmpty()) {
                            IconButton(onClick = { instagramUrl = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = CutieLavender,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            IconButton(onClick = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    instagramUrl = clipText
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = CutiePink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Multimodal Photos Upload
        Text(
            text = "VISUAL ASSETS & PERSONA",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CutiePinkLight,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        // Upload Muse Face Photo
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    singlePhotoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                .testTag("upload_user_photo_card"),
            colors = CardDefaults.cardColors(containerColor = CutieSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, if (userPhotoUri != null) CutiePink else CutieBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (userPhotoUri == null) {
                            Modifier.dashedBorder(
                                color = CutiePink.copy(alpha = 0.6f),
                                strokeWidth = 1.5.dp
                            )
                        } else Modifier
                    )
                    .padding(14.dp)
            ) {
                if (userPhotoUri == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(CutiePinkDark, CutieViolet)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Add Face Portrait",
                                tint = CutiePinkLight,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Muse Portrait Likeness",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "👑", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Upload face photo to preserve facial structure & eyes",
                                fontSize = 11.sp,
                                color = CutieLavender.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = userPhotoUri,
                            contentDescription = "Selected Portrait",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(BorderStroke(2.dp, CutiePink), RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Persona: $profileName",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "✓ Facial likeness ready for cinematic synthesis",
                                fontSize = 11.sp,
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { userPhotoUri = null },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CutieSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove photo",
                                tint = CutiePinkLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Upload Style Reference Screenshot
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    stylePhotoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                .testTag("upload_style_photo_card"),
            colors = CardDefaults.cardColors(containerColor = CutieSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, if (stylePhotoUri != null) CutieViolet else CutieBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (stylePhotoUri == null) {
                            Modifier.dashedBorder(
                                color = CutieViolet.copy(alpha = 0.6f),
                                strokeWidth = 1.5.dp
                            )
                        } else Modifier
                    )
                    .padding(14.dp)
            ) {
                if (stylePhotoUri == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF3B1D54), CutieSurfaceElevated)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Add Style Screenshot",
                                tint = CutieLavender,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Style Reference Screenshot",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Screenshot from Reel for lighting & atmosphere (Optional)",
                                fontSize = 11.sp,
                                color = CutieLavender.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = stylePhotoUri,
                            contentDescription = "Selected Style",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(BorderStroke(2.dp, CutieViolet), RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Atmosphere Reference Added",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "✓ Color grade, lighting & depth enabled",
                                fontSize = 11.sp,
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { stylePhotoUri = null },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CutieSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove style photo",
                                tint = CutieLavender,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Target Engine Selector
        Text(
            text = "TARGET AI ENGINE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CutiePinkLight,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val engineOptions = listOf(
                Triple("ChatGPT", "✨", "OpenAI / DALL-E 3"),
                Triple("Gemini", "🌸", "Google Imagen 3")
            )
            engineOptions.forEach { (engine, icon, subtitle) ->
                val isSelected = selectedEngine == engine
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) CutiePinkDark else CutieSurfaceCard,
                    border = BorderStroke(
                        1.5.dp,
                        if (isSelected) CutiePink else CutieBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEngineChange(engine) }
                        .testTag("engine_chip_${engine.replace(" ", "_")}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = icon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = engine,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else CutieLavender
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            color = if (isSelected) CutiePinkLight else CutieLavender.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Cutie Aesthetic Vibes
        Text(
            text = "CUTIE AESTHETIC VIBE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CutiePinkLight,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            vibes.forEach { vibe ->
                val isSelected = selectedStyleVibe == vibe
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) Color(0xFF4C1D95) else CutieSurfaceCard,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) CutieViolet else CutieBorder
                    ),
                    modifier = Modifier
                        .clickable { onStyleVibeChange(vibe) }
                        .testTag("vibe_chip_${vibe.take(6).trim()}")
                ) {
                    Text(
                        text = vibe,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else CutieLavender,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Aspect Ratio Selector
        Text(
            text = "ASPECT RATIO",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CutiePinkLight,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            aspectRatios.forEach { (ratio, desc) ->
                val isSelected = selectedAspectRatio == ratio
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) CutiePinkDark else CutieSurfaceCard,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) CutiePink else CutieBorder
                    ),
                    modifier = Modifier
                        .clickable { onAspectRatioChange(ratio) }
                        .testTag("aspect_ratio_${ratio.replace(":", "_")}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = ratio,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else CutiePinkLight
                        )
                        Text(
                            text = desc,
                            fontSize = 9.sp,
                            color = if (isSelected) CutieLavender else CutieLavender.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Custom Directives / Prefix
        Text(
            text = "CUSTOM DIRECTIVES / PROMPT PREFIX",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CutiePinkLight,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        OutlinedTextField(
            value = customPrefix,
            onValueChange = { customPrefix = it },
            placeholder = {
                Text(
                    text = "e.g. In a glowing neon cherry blossom garden, ethereal sunset lighting...",
                    fontSize = 12.sp,
                    color = CutieLavender.copy(alpha = 0.5f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_prefix_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = CutiePink,
                unfocusedBorderColor = CutieBorder,
                focusedContainerColor = CutieSurfaceCard,
                unfocusedContainerColor = CutieSurfaceCard
            ),
            minLines = 2,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 7. Sparkle Action Button: "GENERATE CUTIE PROMPT"
        Button(
            onClick = {
                val finalApiKey = customApiKey.trim().ifBlank { BuildConfig.GEMINI_API_KEY.trim() }
                if (finalApiKey.isBlank()) {
                    Toast.makeText(context, "Please set Gemini API Key in Profile Settings", Toast.LENGTH_LONG).show()
                    onNavigateToProfile()
                    return@Button
                }

                if (userPhotoUri == null && instagramUrl.isBlank() && stylePhotoUri == null) {
                    Toast.makeText(context, "Please add a photo or Instagram URL to blend", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isGenerating = true
                showResults = false
                promptCopied = false
                previewBitmap = null
                previewErrorMessage = null

                coroutineScope.launch {
                    val result = GeminiClient.generatePrompt(
                        context = context,
                        apiKey = finalApiKey,
                        instagramUrl = instagramUrl,
                        userPhotoUri = userPhotoUri,
                        stylePhotoUri = stylePhotoUri,
                        targetEngine = selectedEngine,
                        creativeVibe = selectedStyleVibe,
                        aspectRatio = selectedAspectRatio,
                        customPrefix = customPrefix,
                        profileName = profileName
                    )

                    generationResult = result
                    isGenerating = false
                    showResults = true
                    scrollState.animateScrollTo(scrollState.maxValue)

                    if (result.isSuccess && result.generatedPrompt.isNotBlank()) {
                        isGeneratingPreview = true
                        val previewRes = GeminiClient.generateImagePreview(
                            context = context,
                            apiKey = finalApiKey,
                            prompt = result.generatedPrompt,
                            userPhotoUri = userPhotoUri,
                            aspectRatio = selectedAspectRatio
                        )
                        if (previewRes.isSuccess && previewRes.bitmap != null) {
                            previewBitmap = previewRes.bitmap
                        } else {
                            previewErrorMessage = previewRes.errorMessage
                        }
                        isGeneratingPreview = false

                        // Add to history
                        onPromptGenerated(
                            HistoryPromptItem(
                                prompt = result.generatedPrompt,
                                engine = selectedEngine,
                                vibe = selectedStyleVibe,
                                aspectRatio = selectedAspectRatio,
                                previewBitmap = previewBitmap
                            )
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("generate_prompt_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(),
            shape = RoundedCornerShape(16.dp),
            enabled = !isGenerating
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(CutiePink, CutieViolet, Color(0xFFEC4899))
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .border(BorderStroke(1.5.dp, CutieGold), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Synthesizing Cutie Vision...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Generate Prompt",
                            tint = CutieGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✨ GENERATE CUTIE PROMPT ✨",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 8. Results Card
        if (showResults && generationResult != null) {
            val res = generationResult!!
            if (!res.isSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFF87171))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Generation Notice",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = res.errorMessage ?: "An error occurred during prompt synthesis.",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // AI Output Image Preview Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("output_image_preview_card"),
                        colors = CardDefaults.cardColors(containerColor = CutieSurfaceCard),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, CutieBorderPink.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "AI Image Preview",
                                        tint = CutiePinkLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI OUTPUT IMAGE PREVIEW",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(CutiePinkDark, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$selectedAspectRatio PREVIEW",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CutiePinkLight
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isGeneratingPreview) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(CutieSurfaceElevated)
                                        .border(BorderStroke(1.dp, CutieBorder), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(36.dp),
                                            color = CutiePink,
                                            strokeWidth = 3.dp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Rendering AI Visual Concept Preview...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CutiePinkLight
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Simulating prompt output with Gemini Flash Image",
                                            fontSize = 10.sp,
                                            color = CutieLavender.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else if (previewBitmap != null) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { isFullScreenPreviewVisible = true }
                                    ) {
                                        Image(
                                            bitmap = previewBitmap!!.asImageBitmap(),
                                            contentDescription = "Generated Prompt Visual Preview",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 200.dp, max = 320.dp)
                                                .clip(RoundedCornerShape(14.dp)),
                                            contentScale = ContentScale.Crop
                                        )

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                                .background(Color(0xCC130E1D), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.ZoomIn,
                                                    contentDescription = "Zoom",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Tap to enlarge",
                                                    fontSize = 10.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Facial likeness & style concept preview",
                                            fontSize = 10.sp,
                                            color = CutieLavender,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(
                                                onClick = {
                                                    val finalApiKey = customApiKey.trim().ifBlank { BuildConfig.GEMINI_API_KEY.trim() }
                                                    isGeneratingPreview = true
                                                    previewErrorMessage = null
                                                    coroutineScope.launch {
                                                        val previewRes = GeminiClient.generateImagePreview(
                                                            context = context,
                                                            apiKey = finalApiKey,
                                                            prompt = res.generatedPrompt,
                                                            userPhotoUri = userPhotoUri,
                                                            aspectRatio = selectedAspectRatio
                                                        )
                                                        if (previewRes.isSuccess && previewRes.bitmap != null) {
                                                            previewBitmap = previewRes.bitmap
                                                        } else {
                                                            previewErrorMessage = previewRes.errorMessage
                                                        }
                                                        isGeneratingPreview = false
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(CutieSurfaceElevated)
                                                    .testTag("regenerate_preview_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Regenerate Preview",
                                                    tint = CutiePinkLight,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    shareBitmap(context, previewBitmap!!)
                                                },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(CutieSurfaceElevated)
                                                    .testTag("share_preview_image_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Share,
                                                    contentDescription = "Share Preview Image",
                                                    tint = CutiePinkLight,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Monospace Prompt Terminal Container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF0F0A18))
                            .border(BorderStroke(1.dp, CutiePink.copy(alpha = 0.4f)), RoundedCornerShape(18.dp))
                            .testTag("generated_prompt_terminal")
                    ) {
                        // Terminal Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CutieSurfaceElevated)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CutieGold))
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "$selectedEngine // OUTPUT",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CutiePinkLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Copy Prompt Button
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(res.generatedPrompt))
                                    promptCopied = true
                                    Toast.makeText(context, "Prompt copied to clipboard! ✨", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (promptCopied) Color(0xFF059669) else CutiePink,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp).testTag("copy_prompt_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (promptCopied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy",
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (promptCopied) "COPIED" else "COPY PROMPT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Monospace Code Text Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = res.generatedPrompt,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFF3E8FF),
                                lineHeight = 18.sp,
                                modifier = Modifier.testTag("prompt_text_content")
                            )
                        }

                        // Terminal Footer Watermark
                        val wordCount = res.generatedPrompt.split("\\s+".toRegex()).size
                        val charCount = res.generatedPrompt.length
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CutieSurfaceElevated)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CREATED BY RIYAD BHAI",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CutiePinkLight,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "WORDS: $wordCount  //  FOR ${profileName.uppercase()}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CutieLavender
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Multimodal Breakdown Tabs
                    TabRow(
                        selectedTabIndex = activeReportTab,
                        containerColor = CutieSurfaceCard,
                        contentColor = CutiePink,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeReportTab]),
                                color = CutiePink
                            )
                        },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = activeReportTab == 0,
                            onClick = { activeReportTab = 0 },
                            text = { Text("Face Likeness", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeReportTab == 1,
                            onClick = { activeReportTab = 1 },
                            text = { Text("Atmosphere & Style", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CutieSurfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CutieBorder)
                    ) {
                        Box(modifier = Modifier.padding(14.dp)) {
                            if (activeReportTab == 0) {
                                Text(
                                    text = res.analyzedFace.ifBlank { "Analyzed facial symmetry, jawline contours, eye color, and hairstyle preserved accurately." },
                                    fontSize = 11.sp,
                                    color = Color(0xFFF3E8FF),
                                    lineHeight = 16.sp
                                )
                            } else {
                                Text(
                                    text = res.analyzedStyle.ifBlank { "Cinematic color grading, atmospheric haze, lighting highlights, and depth of field harmonized." },
                                    fontSize = 11.sp,
                                    color = Color(0xFFF3E8FF),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // Full Screen Preview Dialog
    if (isFullScreenPreviewVisible && previewBitmap != null) {
        Dialog(
            onDismissRequest = { isFullScreenPreviewVisible = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AI Visual Prompt Preview",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Created by Riyad Bhai • Specially for $profileName",
                                color = CutiePinkLight,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = { isFullScreenPreviewVisible = false },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CutieSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Full Screen Preview",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Full visual preview image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { shareBitmap(context, previewBitmap!!) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CutiePink,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share Image",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SHARE PREVIEW IMAGE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * -------------------------------------------------------------
 * 2. CUTIE PROFILE SCREEN (ELMA THE CUTIE VIP LOUNGE & SETTINGS)
 * Full rich profile details, dedication, stats, and aesthetic preferences.
 * -------------------------------------------------------------
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CutieProfileScreen(
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    profileBio: String,
    onProfileBioChange: (String) -> Unit,
    profileAvatarUri: Uri?,
    onProfileAvatarChange: (Uri?) -> Unit,
    customApiKey: String,
    onCustomApiKeyChange: (String) -> Unit,
    selectedEngine: String,
    onEngineChange: (String) -> Unit,
    selectedAspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    historyCount: Int,
    onApplyVibeToStudio: (String) -> Unit
) {
    val context = LocalContext.current
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(profileName) }
    var tempBio by remember { mutableStateOf(profileBio) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    val avatarPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) onProfileAvatarChange(uri)
    }

    val favoriteVibes = listOf(
        "🌸 Soft Glam Pastel",
        "👑 Royal Cutie",
        "✨ Dreamy Anime",
        "🎀 Cyber Kawaii",
        "🍓 Sweet Strawberry",
        "💎 Luxury Chic"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Profile Hero Header Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("cutie_profile_hero_card"),
            colors = CardDefaults.cardColors(containerColor = CutieSurfaceCard),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, CutieBorderPink.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar with Glowing Crown Ring
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(CutiePink, CutieViolet, CutieGold, CutiePink)
                                )
                            )
                            .clickable {
                                avatarPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileAvatarUri != null) {
                            AsyncImage(
                                model = profileAvatarUri,
                                contentDescription = "Profile Avatar",
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(CutiePinkDark, CutieViolet)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Cutie Avatar",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }

                    // Crown Icon Badge on Top
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(CutieGold)
                            .border(BorderStroke(2.dp, CutieSurfaceCard), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👑", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profileName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.testTag("profile_name_heading")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "🌸", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(listOf(CutiePinkDark, Color(0xFF581C87))),
                                RoundedCornerShape(6.dp)
                            )
                            .border(BorderStroke(1.dp, CutiePink.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "👑 ROYAL CUTIE VIP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CutiePinkLight
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "✨ AI MUSE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CutieGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = profileBio,
                    fontSize = 12.sp,
                    color = CutieLavender,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Edit Profile Button
                OutlinedButton(
                    onClick = {
                        tempName = profileName
                        tempBio = profileBio
                        showEditProfileDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CutiePink),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CutiePinkLight),
                    modifier = Modifier.height(36.dp).testTag("edit_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Edit Profile Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cutie Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CutieStatBadge(
                title = "Cutie Factor",
                value = "100% ✨",
                gradient = listOf(CutiePinkDark, Color(0xFF831843)),
                modifier = Modifier.weight(1f)
            )
            CutieStatBadge(
                title = "Creations",
                value = "$historyCount 🌸",
                gradient = listOf(Color(0xFF4C1D95), Color(0xFF2E1065)),
                modifier = Modifier.weight(1f)
            )
            CutieStatBadge(
                title = "Neural Tier",
                value = "VIP 👑",
                gradient = listOf(Color(0xFF713F12), Color(0xFF451A03)),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Creator Watermark & Dedication Card (By Riyad Bhai)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("creator_dedication_card"),
            colors = CardDefaults.cardColors(containerColor = CutieSurfaceCard),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, CutieGold.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(CutieGold, Color(0xFFF59E0B))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Heart",
                                tint = CutieSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Crafted by Riyad Bhai",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CutieGold
                            )
                            Text(
                                text = "Dedicated with love for $profileName 🌸",
                                fontSize = 11.sp,
                                color = CutiePinkLight
                            )
                        }
                    }

                    Text(text = "💕", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "This AI Vision Prompt Blender was specially engineered and designed by Riyad Bhai for Elma the cutie. Powered by Gemini 2.5 multimodal intelligence to transform real portrait aesthetics into breathtaking cinematic masterworks.",
                    fontSize = 12.sp,
                    color = Color(0xFFF3E8FF),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CutieSurfaceElevated, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👑 Creator: Riyad Bhai",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CutieGold
                    )
                    Text(
                        text = "🌸 Muse: $profileName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CutiePinkLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Favorite Cutie Aesthetics & Presets
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CutieSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CutieBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "FAVORITE CUTIE AESTHETICS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CutiePinkLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap any vibe to load it directly into the Studio",
                    fontSize = 11.sp,
                    color = CutieLavender.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    favoriteVibes.forEach { vibe ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CutieSurfaceElevated,
                            border = BorderStroke(1.dp, CutieBorderPink.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                onApplyVibeToStudio(vibe)
                                Toast.makeText(context, "Applied '$vibe' to Studio! ✨", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = vibe,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Apply",
                                    tint = CutiePink,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings & API Key Management Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_settings_card"),
            colors = CardDefaults.cardColors(containerColor = CutieSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CutieBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = CutiePinkLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APP & GEMINI API SETTINGS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CutiePinkLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Gemini API Key (Optional override)",
                    fontSize = 11.sp,
                    color = CutieLavender
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = customApiKey,
                    onValueChange = onCustomApiKeyChange,
                    placeholder = {
                        Text(
                            text = if (BuildConfig.GEMINI_API_KEY.isNotBlank()) "Default Environment Key Active" else "Paste AI Studio Gemini Key...",
                            fontSize = 11.sp,
                            color = CutieLavender.copy(alpha = 0.5f)
                        )
                    },
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Key Visibility",
                                tint = CutieLavender,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_api_key_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CutiePink,
                        unfocusedBorderColor = CutieBorder,
                        focusedContainerColor = CutieSurfaceElevated,
                        unfocusedContainerColor = CutieSurfaceElevated
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "• Version: 2.5 Cutie Edition\n• Engine: Gemini 2.5 Multimodal Vision\n• Designed & Built by Riyad Bhai",
                    fontSize = 10.sp,
                    color = CutieLavender.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    // Edit Profile Modal Dialog
    if (showEditProfileDialog) {
        Dialog(onDismissRequest = { showEditProfileDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("edit_profile_dialog"),
                colors = CardDefaults.cardColors(containerColor = CutieSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, CutiePink)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(CutiePink, CutieViolet)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Cutie Persona",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Edit Profile Persona",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Crafted with 💕 by Riyad Bhai",
                        fontSize = 11.sp,
                        color = CutiePinkLight
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Profile Name", color = CutiePinkLight) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CutiePink,
                            unfocusedBorderColor = CutieBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text("Persona Tagline / Bio", color = CutiePinkLight) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("profile_bio_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CutiePink,
                            unfocusedBorderColor = CutieBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditProfileDialog = false },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CutieLavender),
                            border = BorderStroke(1.dp, CutieBorder)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (tempName.isNotBlank()) onProfileNameChange(tempName.trim())
                                if (tempBio.isNotBlank()) onProfileBioChange(tempBio.trim())
                                showEditProfileDialog = false
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CutiePink,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CutieStatBadge(
    title: String,
    value: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CutieSurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CutieBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CutieGold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                color = CutieLavender,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * -------------------------------------------------------------
 * 3. HISTORY & CUTIE VAULT SCREEN
 * -------------------------------------------------------------
 */
@Composable
fun HistoryVaultScreen(
    historyList: List<HistoryPromptItem>,
    onNavigateToStudio: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    if (historyList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CutieSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Empty History",
                        tint = CutiePink,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Cutie Vault is Empty 🌸",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "All your synthesized prompts, style combinations, and visual concept renders will be safely stored here.",
                    fontSize = 12.sp,
                    color = CutieLavender,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.widthIn(max = 280.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onNavigateToStudio,
                    colors = ButtonDefaults.buttonColors(containerColor = CutiePink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Create",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Create First Cutie Prompt",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cutie Vault & Creations",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "${historyList.size} Saved prompt creations",
                        fontSize = 11.sp,
                        color = CutiePinkLight
                    )
                }
            }

            historyList.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("history_item_${item.id}"),
                    colors = CardDefaults.cardColors(containerColor = CutieSurfaceCard),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CutieBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(CutiePinkDark, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.engine,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CutiePinkLight
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.vibe,
                                    fontSize = 11.sp,
                                    color = CutieLavender
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(item.prompt))
                                        Toast.makeText(context, "Prompt copied! ✨", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = CutiePinkLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if (item.previewBitmap != null) {
                                    IconButton(
                                        onClick = { shareBitmap(context, item.previewBitmap) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
                                            contentDescription = "Share",
                                            tint = CutiePinkLight,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (item.previewBitmap != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Image(
                                bitmap = item.previewBitmap.asImageBitmap(),
                                contentDescription = "Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = item.prompt,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFF3E8FF),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

/**
 * Helper to cache and share generated preview bitmap via FileProvider
 */
fun shareBitmap(context: android.content.Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "ai_prompt_preview.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share AI Visual Preview"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
