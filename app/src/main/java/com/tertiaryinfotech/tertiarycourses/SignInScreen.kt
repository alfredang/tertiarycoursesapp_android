package com.tertiaryinfotech.tertiarycourses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private enum class Method(val label: String) { CODE("Email Code"), PASSWORD("Password") }

/** Sign in with an emailed one-time code, or with the website password. */
@Composable
fun SignInScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var method by rememberSaveable { mutableStateOf(Method.CODE) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var codeSent by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    val passwordFocus = remember { FocusRequester() }
    val codeFocus = remember { FocusRequester() }

    val e = email.trim()
    val emailValid = e.contains("@") && e.contains(".") && e.length >= 5

    fun run(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        scope.launch {
            try { block() } catch (ex: Exception) { error = ex.message } finally { busy = false }
        }
    }
    fun sendCode() {
        if (!emailValid) return
        run {
            info = vm.sendCode(email)
            codeSent = true; code = ""
            try { codeFocus.requestFocus() } catch (_: Exception) { }
        }
    }
    fun signInWithPassword() {
        if (!emailValid || password.isEmpty()) return
        focus.clearFocus()
        run { vm.signIn(email, password) }
    }
    fun verify() {
        focus.clearFocus()
        run { vm.verifyCode(email, code) }
    }

    ScreenScaffold { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sign in", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Learners and trainers: use your ${Site.websiteLabel} email to see your classes, materials, attendance, certificates and billing.",
                    fontSize = 15.sp, color = Theme.secondaryText, lineHeight = 21.sp,
                )
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Method.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = method == m,
                        onClick = { method = m; error = null; info = null },
                        shape = SegmentedButtonDefaults.itemShape(i, Method.entries.size),
                        icon = {},
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Theme.card, activeContentColor = Theme.accent, activeBorderColor = Theme.cardBorder,
                            inactiveContainerColor = Theme.field, inactiveContentColor = Theme.ink, inactiveBorderColor = Theme.cardBorder,
                        ),
                    ) { Text(m.label, fontWeight = if (method == m) FontWeight.SemiBold else FontWeight.Normal) }
                }
            }

            InfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FieldLabel("Email")
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; codeSent = false },
                        placeholder = { Text("you@example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email, autoCorrectEnabled = false,
                            imeAction = if (method == Method.PASSWORD) ImeAction.Next else ImeAction.Send,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocus.requestFocus() },
                            onSend = { sendCode() },
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("email"),
                    )

                    when {
                        method == Method.PASSWORD -> {
                            FieldLabel("Password")
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text("Your website password") },
                                singleLine = true,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (showPassword) "Hide password" else "Show password",
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = { signInWithPassword() }),
                                shape = RoundedCornerShape(10.dp),
                                colors = fieldColors(),
                                modifier = Modifier.fillMaxWidth().focusRequester(passwordFocus).testTag("password"),
                            )
                            PrimaryButton("Sign In", enabled = emailValid && password.isNotEmpty(), busy = busy, modifier = Modifier.testTag("signIn")) {
                                signInWithPassword()
                            }
                            TextButton(onClick = { openUrl(context, Site.forgotPasswordUrl) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                Text("Forgot your password?", fontSize = 14.sp)
                            }
                        }
                        codeSent -> {
                            FieldLabel("6-digit code")
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it.filter(Char::isDigit).take(6) },
                                placeholder = { Text("123456") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { if (code.length == 6) verify() }),
                                shape = RoundedCornerShape(10.dp),
                                colors = fieldColors(),
                                modifier = Modifier.fillMaxWidth().focusRequester(codeFocus),
                            )
                            PrimaryButton("Verify & Sign In", enabled = code.length == 6, busy = busy) { verify() }
                            TextButton(onClick = { sendCode() }, enabled = !busy, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                Text("Send a new code", fontSize = 14.sp)
                            }
                        }
                        else -> PrimaryButton("Email Me a Code", enabled = emailValid, busy = busy) { sendCode() }
                    }
                }
            }

            info?.let { IconLine(Icons.Filled.MarkEmailUnread, it, color = Theme.accent, iconTint = Theme.accent, fontSize = 13) }
            error?.let { IconLine(Icons.Filled.Warning, it, color = Theme.danger, iconTint = Theme.danger, fontSize = 13) }

            InfoCard {
                IconLine(Icons.Filled.AutoAwesome, "New to ${Site.siteName}?", color = Theme.accent, iconTint = Theme.accent, fontSize = 15, weight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your learner account is created when you register for a course on ${Site.websiteLabel}. Browse the Courses tab to find one.",
                    fontSize = 13.sp, color = Theme.secondaryText, lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.secondaryText, modifier = Modifier.padding(bottom = 0.dp))
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Theme.accent,
    unfocusedBorderColor = Theme.cardBorder,
    focusedContainerColor = Theme.page,
    unfocusedContainerColor = Theme.page,
)
