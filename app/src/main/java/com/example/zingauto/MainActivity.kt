package com.example.zingauto

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.zingauto.ui.theme.ZingAutoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        credentialManager = CredentialManager(this)

        setContent {
            ZingAutoTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.MenuList) }
                var selectedCredentials by remember { mutableStateOf<List<CredentialManager.Credentials>>(emptyList()) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        if (currentScreen == Screen.MenuList) {
                            FloatingActionButton(onClick = { currentScreen = Screen.AddCredential }) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            is Screen.MenuList -> {
                                CredentialListScreen(
                                    credentialManager = credentialManager,
                                    onStartAutomation = {
                                        selectedCredentials = it
                                        currentScreen = Screen.Automation
                                    }
                                )
                            }
                            is Screen.AddCredential -> {
                                AddCredentialScreen(
                                    onSave = { company, employee, pass ->
                                        credentialManager.addCredential(
                                            CredentialManager.Credentials(
                                                companyCode = company,
                                                employeeCode = employee,
                                                password = pass
                                            )
                                        )
                                        currentScreen = Screen.MenuList
                                    },
                                    onCancel = { currentScreen = Screen.MenuList }
                                )
                            }
                            is Screen.Automation -> {
                                AutomationScreen(
                                    credentialsList = selectedCredentials,
                                    onFinished = { currentScreen = Screen.MenuList }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    sealed class Screen {
        object MenuList : Screen()
        object AddCredential : Screen()
        object Automation : Screen()
    }
}

@Composable
fun CredentialListScreen(
    credentialManager: CredentialManager,
    onStartAutomation: (List<CredentialManager.Credentials>) -> Unit
) {
    var list by remember { mutableStateOf(credentialManager.getCredentialList()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("ZingAuto - Accounts", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (list.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No accounts saved. Tap + to add one.")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(list) { cred ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Company: ${cred.companyCode}", style = MaterialTheme.typography.titleMedium)
                                Text("Employee: ${cred.employeeCode}", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = {
                                credentialManager.removeCredential(cred.id)
                                list = credentialManager.getCredentialList()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { onStartAutomation(list) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            enabled = list.isNotEmpty()
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start Automation Run")
        }
    }
}

@Composable
fun AddCredentialScreen(
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var companyCode by remember { mutableStateOf("") }
    var employeeCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add ZingHR Account", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = companyCode, onValueChange = { companyCode = it }, label = { Text("Company Code") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = employeeCode, onValueChange = { employeeCode = it }, label = { Text("Employee Code") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it }, 
            label = { Text("Password") }, 
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { if(companyCode.isNotBlank() && employeeCode.isNotBlank() && password.isNotBlank()) onSave(companyCode, employeeCode, password) },
                modifier = Modifier.weight(1f)
            ) { Text("Save") }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AutomationScreen(
    credentialsList: List<CredentialManager.Credentials>,
    onFinished: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentCred = credentialsList.getOrNull(currentIndex)
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf("Initializing...") }
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Runtime location permission request — required for geofenced Punch In/Out
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                          permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        statusText = if (locationGranted) "Location granted. Starting automation..."
                     else "Location denied — Punch Out may fail geofence check."
    }

    // Ask for location as soon as this screen opens
    LaunchedEffect(Unit) {
        if (!locationGranted) {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    if (currentCred == null) {
        LaunchedEffect(Unit) { onFinished() }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / credentialsList.size },
            modifier = Modifier.fillMaxWidth()
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = "Processing ${currentIndex + 1} of ${credentialsList.size}\nAccount: ${currentCred.employeeCode}\nStatus: $statusText",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        
        var webViewInstance by remember { mutableStateOf<WebView?>(null) }
        var retryCount by remember { mutableIntStateOf(0) }

        // Retry polling: when fields aren't found, increment retryCount to try again
        LaunchedEffect(retryCount) {
            if (retryCount > 0) {
                delay(3000)
                val view = webViewInstance ?: return@LaunchedEffect
                statusText = "Retrying... (attempt $retryCount)"
                val checkJs = """
                    (function() {
                        var c = document.getElementById('txtCompanyCode') || document.getElementsByName('txtCompanyCode')[0];
                        var u = document.getElementById('txtEmpCode') || document.getElementsByName('txtEmpCode')[0];
                        var p = document.getElementById('txtPassword') || document.getElementsByName('txtPassword')[0];
                        var b = document.getElementById('btnLogin') || document.getElementsByName('btnLogin')[0];
                        if(c && u && p && b) return 'found';
                        // Fallback: check for visible text inputs + password + button
                        var inputs = document.querySelectorAll('input[type="text"], input[type="password"]');
                        var btn = document.querySelector('button[type="submit"], input[type="submit"], button');
                        if(inputs.length >= 2 && btn) return 'alt_found';
                        return 'not_found:' + document.title + ':inputs=' + document.querySelectorAll('input').length;
                    })();
                """.trimIndent()
                view.evaluateJavascript(checkJs) { result ->
                    val r = result.trim('"')
                    when {
                        r == "found" -> {
                            scope.launch {
                                isProcessing = true
                                performAutomation(view, credentialsList[currentIndex], scope,
                                    onStatus = { statusText = it },
                                    onDone = {
                                        CookieManager.getInstance().removeAllCookies {
                                            scope.launch {
                                                if (currentIndex < credentialsList.size - 1) {
                                                    currentIndex++
                                                    isProcessing = false
                                                    retryCount = 0
                                                    statusText = "Moving to next account..."
                                                    view.loadUrl("https://portal.zinghr.com")
                                                } else {
                                                    statusText = "Automation Complete!"
                                                    delay(2000)
                                                    onFinished()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        r.startsWith("alt_found") -> {
                            statusText = "Found alternate form layout, trying..."
                            scope.launch {
                                isProcessing = true
                                performAutomationAlt(view, credentialsList[currentIndex], scope,
                                    onStatus = { statusText = it },
                                    onDone = {
                                        CookieManager.getInstance().removeAllCookies {
                                            scope.launch {
                                                if (currentIndex < credentialsList.size - 1) {
                                                    currentIndex++
                                                    isProcessing = false
                                                    retryCount = 0
                                                    statusText = "Moving to next account..."
                                                    view.loadUrl("https://portal.zinghr.com")
                                                } else {
                                                    statusText = "Automation Complete!"
                                                    delay(2000)
                                                    onFinished()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        else -> {
                            statusText = "Form not ready ($r). Retrying..."
                            if (retryCount < 10) {
                                retryCount++
                            } else {
                                statusText = "Could not find login form after 10 attempts. Reloading..."
                                retryCount = 0
                                isProcessing = false
                                view.loadUrl("https://portal.zinghr.com")
                            }
                        }
                    }
                }
            }
        }

        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                        allowContentAccess = true
                        allowFileAccess = true
                        setGeolocationEnabled(true)
                        setGeolocationDatabasePath(context.filesDir.absolutePath)
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        javaScriptCanOpenWindowsAutomatically = true
                        mediaPlaybackRequiresUserGesture = false
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                    }
                    setBackgroundColor(android.graphics.Color.WHITE)
                    isScrollbarFadingEnabled = true
                    scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY

                    // Enable cookies
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            if (newProgress < 100) {
                                statusText = "Loading page... $newProgress%"
                            }
                        }

                        // Auto-grant location to the ZingHR portal (required for geofenced Punch In/Out)
                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: GeolocationPermissions.Callback?
                        ) {
                            callback?.invoke(origin, true, false)
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                            return false // Let the WebView handle all URLs
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            statusText = "Page loaded. Scanning for login form..."
                            if (!isProcessing) {
                                retryCount = 1 // Trigger the retry LaunchedEffect to find fields
                            }
                        }

                        override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                            statusText = "Error loading page: ${error?.description}"
                        }

                        override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                            handler?.proceed() // Accept SSL certs for portal
                        }
                    }
                    webViewInstance = this
                    loadUrl("https://portal.zinghr.com")
                }
            }
        )
    }
}

/**
 * Primary automation: targets ZingHR's known field IDs directly.
 * Fills each field one at a time with a 1-second pause so the user can
 * watch the automation happen in real-time.
 */
suspend fun performAutomation(
    view: WebView,
    cred: CredentialManager.Credentials,
    scope: kotlinx.coroutines.CoroutineScope,
    onStatus: (String) -> Unit,
    onDone: () -> Unit
) {
    // Step 1: Focus + fill company code
    onStatus("Entering company code...")
    view.evaluateJavascript("""
        (function() {
            var c = document.getElementById('txtCompanyCode') || document.getElementsByName('txtCompanyCode')[0];
            if (c) {
                c.focus();
                c.value = '${cred.companyCode}';
                c.dispatchEvent(new Event('input', {bubbles: true}));
                c.dispatchEvent(new Event('change', {bubbles: true}));
            }
        })();
    """.trimIndent(), null)
    kotlinx.coroutines.delay(1000)

    // Step 2: Focus + fill employee code
    onStatus("Entering employee code...")
    view.evaluateJavascript("""
        (function() {
            var u = document.getElementById('txtEmpCode') || document.getElementsByName('txtEmpCode')[0];
            if (u) {
                u.focus();
                u.value = '${cred.employeeCode}';
                u.dispatchEvent(new Event('input', {bubbles: true}));
                u.dispatchEvent(new Event('change', {bubbles: true}));
            }
        })();
    """.trimIndent(), null)
    kotlinx.coroutines.delay(1000)

    // Step 3: Focus + fill password
    onStatus("Entering password...")
    view.evaluateJavascript("""
        (function() {
            var p = document.getElementById('txtPassword') || document.getElementsByName('txtPassword')[0];
            if (p) {
                p.focus();
                p.value = '${cred.password}';
                p.dispatchEvent(new Event('input', {bubbles: true}));
                p.dispatchEvent(new Event('change', {bubbles: true}));
            }
        })();
    """.trimIndent(), null)
    kotlinx.coroutines.delay(1000)

    // Step 4: Click login button
    onStatus("Clicking login button...")
    view.evaluateJavascript("""
        (function() {
            var b = document.getElementById('btnLogin') || document.getElementsByName('btnLogin')[0];
            if (b) {
                b.focus();
                b.click();
            }
        })();
    """.trimIndent(), null)

    onStatus("Login submitted — waiting for dashboard...")

    // Poll every 2s (up to 20s) for the URL to leave the login/auth page
    var onDashboard = false
    repeat(10) {
        kotlinx.coroutines.delay(2000)
        val currentUrl = view.url ?: ""
        if (!currentUrl.contains("login", ignoreCase = true) &&
            !currentUrl.contains("authentication", ignoreCase = true) &&
            currentUrl.contains("zinghr.com")
        ) {
            onDashboard = true
        }
        if (onDashboard) return@repeat
    }

    if (onDashboard) {
        // Wait for dashboard widgets to render
        onStatus("Dashboard loaded — looking for Punch Out...")
        kotlinx.coroutines.delay(3000)
        performPunchOut(view, onStatus)
    } else {
        onStatus("Timed out waiting for dashboard. Skipping Punch Out.")
        kotlinx.coroutines.delay(1000)
    }

    onDone()
}

/**
 * Fallback automation: uses generic CSS selectors when the ZingHR portal
 * renders its form via a JS framework that changes element IDs.
 * Targets the first two text inputs and first password input it finds.
 */
suspend fun performAutomationAlt(
    view: WebView,
    cred: CredentialManager.Credentials,
    scope: kotlinx.coroutines.CoroutineScope,
    onStatus: (String) -> Unit,
    onDone: () -> Unit
) {
    // Step 1: Fill first text input (company code)
    onStatus("[Alt] Entering company code...")
    view.evaluateJavascript("""
        (function() {
            var inputs = document.querySelectorAll('input[type="text"]');
            if (inputs.length > 0) {
                inputs[0].focus();
                inputs[0].value = '${cred.companyCode}';
                inputs[0].dispatchEvent(new Event('input', {bubbles: true}));
                inputs[0].dispatchEvent(new Event('change', {bubbles: true}));
            }
        })();
    """.trimIndent(), null)
    kotlinx.coroutines.delay(1000)

    // Step 2: Fill second text input (employee code)
    onStatus("[Alt] Entering employee code...")
    view.evaluateJavascript("""
        (function() {
            var inputs = document.querySelectorAll('input[type="text"]');
            if (inputs.length > 1) {
                inputs[1].focus();
                inputs[1].value = '${cred.employeeCode}';
                inputs[1].dispatchEvent(new Event('input', {bubbles: true}));
                inputs[1].dispatchEvent(new Event('change', {bubbles: true}));
            }
        })();
    """.trimIndent(), null)
    kotlinx.coroutines.delay(1000)

    // Step 3: Fill password input
    onStatus("[Alt] Entering password...")
    view.evaluateJavascript("""
        (function() {
            var p = document.querySelector('input[type="password"]');
            if (p) {
                p.focus();
                p.value = '${cred.password}';
                p.dispatchEvent(new Event('input', {bubbles: true}));
                p.dispatchEvent(new Event('change', {bubbles: true}));
            }
        })();
    """.trimIndent(), null)
    kotlinx.coroutines.delay(1000)

    // Step 4: Click first submit/button
    onStatus("[Alt] Clicking login button...")
    view.evaluateJavascript("""
        (function() {
            var b = document.querySelector('button[type="submit"]')
                 || document.querySelector('input[type="submit"]')
                 || document.querySelector('button');
            if (b) {
                b.focus();
                b.click();
            }
        })();
    """.trimIndent(), null)

    onStatus("[Alt] Login submitted — waiting for dashboard...")

    // Poll every 2s (up to 20s) for the URL to leave the login/auth page
    var onDashboard = false
    repeat(10) {
        kotlinx.coroutines.delay(2000)
        val currentUrl = view.url ?: ""
        if (!currentUrl.contains("login", ignoreCase = true) &&
            !currentUrl.contains("authentication", ignoreCase = true) &&
            currentUrl.contains("zinghr.com")
        ) {
            onDashboard = true
        }
        if (onDashboard) return@repeat
    }

    if (onDashboard) {
        onStatus("[Alt] Dashboard loaded — looking for Punch Out...")
        kotlinx.coroutines.delay(3000)
        performPunchOut(view, onStatus)
    } else {
        onStatus("[Alt] Timed out waiting for dashboard. Skipping Punch Out.")
        kotlinx.coroutines.delay(1000)
    }

    onDone()
}

/**
 * Finds and clicks the "Punch Out" button on the ZingHR dashboard.
 * Searches by button text content since ZingHR uses dynamic class names.
 * Also tries scrolling down first since the card may be below the fold.
 */
suspend fun performPunchOut(view: WebView, onStatus: (String) -> Unit) {
    // First scroll down to ensure the Punch Out widget is in view
    view.evaluateJavascript("""
        window.scrollTo({ top: 400, behavior: 'smooth' });
    """.trimIndent(), null)
    kotlinx.coroutines.delay(1500)

    // Click Punch Out — search all clickable elements by their visible text
    var punchResult = ""
    view.evaluateJavascript("""
        (function() {
            // Search buttons, anchors, and any element with role=button
            var candidates = document.querySelectorAll('button, a, input[type="button"], input[type="submit"], [role="button"]');
            for (var i = 0; i < candidates.length; i++) {
                var txt = (candidates[i].innerText || candidates[i].value || '').trim();
                if (txt === 'Punch Out' || txt.startsWith('Punch Out')) {
                    candidates[i].scrollIntoView({ behavior: 'smooth', block: 'center' });
                    candidates[i].click();
                    return 'punched_out:button';
                }
            }
            // Fallback: walk all leaf-node elements for the text
            var all = document.querySelectorAll('*');
            for (var i = 0; i < all.length; i++) {
                if (all[i].children.length === 0) {
                    var txt = (all[i].innerText || '').trim();
                    if (txt === 'Punch Out') {
                        all[i].scrollIntoView({ behavior: 'smooth', block: 'center' });
                        all[i].click();
                        return 'punched_out:leaf';
                    }
                }
            }
            return 'not_found';
        })();
    """.trimIndent()) { result ->
        punchResult = result.trim('"')
    }

    kotlinx.coroutines.delay(500) // give callback time to set punchResult

    if (punchResult.startsWith("punched_out")) {
        onStatus("Punch Out clicked ($punchResult) — waiting to register...")
        kotlinx.coroutines.delay(4000) // wait for backend to record the punch
        onStatus("Punch Out complete! Clearing session...")
    } else {
        onStatus("Punch Out button not found ($punchResult). Clearing session anyway...")
        kotlinx.coroutines.delay(1000)
    }
}
