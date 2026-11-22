package com.tertiaryinfotech.tertiarycourses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private enum class Method { CODE, PASSWORD }

/**
 * Sign in with an emailed one-time code (the default) or the website password.
 * Layout follows the TIA house sign-in: app mark, name, one card. Admin sign-in
 * with Google is a quiet link at the foot of the page.
 */
@Composable
fun SignInScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var method by rememberSaveable { mutableStateOf(Method.CODE) }
    var email by rememberSaveable { mutableStateOf(vm.rememberedEmail) }
    var remember by rememberSaveable { mutableStateOf(vm.rememberEmail) }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by rememberSaveable { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    val passwordFocus = remember { FocusRequester() }
    val codeFocus = remember { FocusRequester() }

    val e = email.trim()
    val emailValid = e.contains("@") && e.contains(".") && e.length >= 5
    // Busy also while a Google sign-in that came back from the browser is being exchanged.
    val busy = working || vm.googleBusy
    val error = vm.signInError
    val primaryDisabled = busy || !emailValid || when (method) {
        Method.PASSWORD -> password.isEmpty()
        Method.CODE -> codeSent && code.length != 6
    }

    fun run(block: suspend () -> Unit) {
        if (busy) return
        working = true; vm.signInError = null
        scope.launch {
            try { block() } catch (ex: Exception) { vm.signInError = ex.message } finally { working = false }
        }
    }
    fun sendCode() {
        if (!emailValid) return
        run {
            info = vm.sendCode(email)
            vm.rememberSignInEmail(email)
            codeSent = true; code = ""
            try { codeFocus.requestFocus() } catch (_: Exception) { }
        }
    }
    fun signInWithPassword() {
        if (!emailValid || password.isEmpty()) return
        focus.clearFocus()
        run {
            vm.signIn(email, password)
            vm.rememberSignInEmail(email)
        }
    }
    fun verify() {
        if (code.length != 6) return
        focus.clearFocus()
        run { vm.verifyCode(email, code) }
    }
    /** Admin sign-in: Google in the browser, for the allowed admin account only. The site returns to the app. */
    fun signInAsAdmin() {
        if (busy) return
        info = null
        focus.clearFocus()
        openUrl(context, vm.startGoogleSignIn())
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Theme.page, Theme.accentSoft.compositeOver(Theme.page)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // App mark and name
            Column(
                Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val markShape = RoundedCornerShape(22.dp)
                Column(
                    Modifier
                        .size(92.dp)
                        .shadow(12.dp, markShape, ambientColor = Color(0xFF0B3D8E), spotColor = Color(0xFF0B3D8E))
                        .background(Brush.verticalGradient(listOf(Color(0xFF0B3D8E), Color(0xFF0A2D6F))), markShape)
                        .clearAndSetSemantics { },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("TC", color = Color.White, fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold)
                    Text(Site.code, color = Color.White, fontSize = 17.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold)
                }
                Text(Site.appName, color = Theme.ink, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold)
                Text(Site.siteName, color = Theme.secondaryText, fontSize = 15.sp, lineHeight = 20.sp)
            }

            // The sign-in card
            val cardShape = RoundedCornerShape(22.dp)
            Column(
                Modifier.fillMaxWidth().background(Theme.card, cardShape).border(1.dp, Theme.cardBorder, cardShape).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sign in", color = Theme.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                    Text(
                        if (codeSent && method == Method.CODE) "Enter the 6-digit code we emailed you."
                        else "Enter the email you registered with to continue.",
                        fontSize = 15.sp, color = Theme.secondaryText, lineHeight = 20.sp,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val roles = listOf("learner" to "Learner", "trainer" to "Trainer")
                    SingleChoiceSegmentedButtonRow(
                        Modifier.fillMaxWidth().testTag("loginRole").semantics { contentDescription = "Sign in as" }
                    ) {
                        roles.forEachIndexed { i, (key, label) ->
                            SegmentedButton(
                                selected = vm.loginRole == key,
                                onClick = { vm.chooseLoginRole(key) },
                                shape = SegmentedButtonDefaults.itemShape(i, roles.size),
                                icon = {},
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = Theme.card, activeContentColor = Theme.accent, activeBorderColor = Theme.cardBorder,
                                    inactiveContainerColor = Theme.field, inactiveContentColor = Theme.ink, inactiveBorderColor = Theme.cardBorder,
                                ),
                                modifier = Modifier.testTag("loginRole-$key"),
                            ) { Text(label, fontWeight = if (vm.loginRole == key) FontWeight.SemiBold else FontWeight.Normal) }
                        }
                    }
                    Text(
                        if (vm.loginRole == "trainer") "See the classes you teach, attendance, payout and training hours."
                        else "See the courses you are taking. Trainers attending a course sign in as a learner.",
                        fontSize = 13.sp, color = Theme.secondaryText, lineHeight = 18.sp,
                    )
                }

                InputField(
                    value = email,
                    onValueChange = { email = it; codeSent = false },
                    icon = Icons.Filled.Email,
                    placeholder = "Email",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email, autoCorrectEnabled = false,
                        imeAction = if (method == Method.PASSWORD) ImeAction.Next else ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocus.requestFocus() },
                        onGo = { sendCode() },
                    ),
                    modifier = Modifier.testTag("email"),
                )

                if (method == Method.PASSWORD) {
                    InputField(
                        value = password,
                        onValueChange = { password = it },
                        icon = Icons.Filled.Lock,
                        placeholder = "Website password",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { signInWithPassword() }),
                        modifier = Modifier.focusRequester(passwordFocus).testTag("password"),
                    )
                } else if (codeSent) {
                    InputField(
                        value = code,
                        onValueChange = { code = it.filter(Char::isDigit).take(6) },
                        icon = Icons.Filled.Tag,
                        placeholder = "6-digit code",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { verify() }),
                        modifier = Modifier.focusRequester(codeFocus).testTag("code"),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Remember my email", color = Theme.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = remember,
                        onCheckedChange = { on ->
                            remember = on
                            // Switching it off also forgets the email kept so far.
                            vm.rememberEmail = on
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White, checkedTrackColor = Theme.brand, checkedBorderColor = Theme.brand,
                        ),
                        modifier = Modifier.testTag("rememberEmail").semantics { contentDescription = "Remember my email" },
                    )
                }

                Button(
                    onClick = {
                        when {
                            method == Method.PASSWORD -> signInWithPassword()
                            codeSent -> verify()
                            else -> sendCode()
                        }
                    },
                    enabled = !primaryDisabled,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Theme.brand, contentColor = Color.White,
                        disabledContainerColor = Theme.brand.copy(alpha = 0.45f), disabledContentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(vertical = 15.dp),
                    modifier = Modifier.fillMaxWidth().testTag(if (method == Method.PASSWORD) "signIn" else "continue"),
                ) {
                    if (busy) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (method == Method.PASSWORD) "Sign In" else if (codeSent) "Verify & Sign In" else "Continue",
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    )
                }

                info?.let { IconLine(Icons.Filled.MarkEmailUnread, it, color = Theme.accent, iconTint = Theme.accent, fontSize = 13) }
                error?.let { IconLine(Icons.Filled.Warning, it, color = Theme.danger, iconTint = Theme.danger, fontSize = 13) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (method == Method.CODE && codeSent) {
                        FooterLink("Send a new code", enabled = !busy) { sendCode() }
                    } else if (method == Method.PASSWORD) {
                        FooterLink("Forgot your password?") { openUrl(context, Site.forgotPasswordUrl) }
                    }
                    Spacer(Modifier.weight(1f))
                    FooterLink(
                        if (method == Method.CODE) "Use password instead" else "Use email code instead",
                        modifier = Modifier.testTag("switchMethod"),
                    ) {
                        method = if (method == Method.CODE) Method.PASSWORD else Method.CODE
                        vm.signInError = null
                        info = null
                    }
                }
            }

            Column(
                Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Learners: your account is created when you register for a course on ${Site.websiteLabel}.",
                    fontSize = 13.sp, color = Theme.secondaryText, lineHeight = 18.sp, textAlign = TextAlign.Center,
                )
                // Deliberately quiet: staff know it is here.
                TextButton(
                    onClick = { signInAsAdmin() },
                    enabled = !busy,
                    colors = ButtonDefaults.textButtonColors(contentColor = Theme.secondaryText, disabledContentColor = Theme.tertiaryText),
                    modifier = Modifier.testTag("adminSignIn"),
                ) { Text("Admin sign-in", fontSize = 13.sp, fontWeight = FontWeight.Normal) }
            }
        }
    }
}

/** A rounded field with a leading icon and its name as the placeholder. */
@Composable
private fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = { Icon(icon, contentDescription = null, tint = Theme.secondaryText, modifier = Modifier.size(22.dp)) },
        placeholder = { Text(placeholder, color = Theme.tertiaryText) },
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Theme.accent,
            unfocusedBorderColor = Theme.cardBorder,
            focusedContainerColor = Theme.page,
            unfocusedContainerColor = Theme.page,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** A text button in the card's footer row. */
@Composable
private fun FooterLink(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(
        onClick = onClick, enabled = enabled, modifier = modifier.height(36.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = Theme.accent),
    ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
}
