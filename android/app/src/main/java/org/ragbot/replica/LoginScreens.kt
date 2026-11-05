package org.ragbot.replica

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

internal object LoginDestinations {
    const val INTRO = "S-048"
    const val WELCOME = "S-049"
    const val LOGIN = "S-027"
    val implemented = setOf(INTRO, WELCOME, LOGIN)
}

internal object LoginRules {
    const val WRONG = "نام کاربری یا رمز عبور صحیح نیست."

    fun error(username: String, password: String) =
        MockData.current.customer.let {
            if (username.trim() == it.username && password == it.password) null else WRONG
        }
}

/** Login presentation before Home, for the demo user only. Designed for mock. */
@Composable
internal fun LoginPage(
    page: String,
    onOpen: (String) -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
) {
    val title =
        when (page) {
            LoginDestinations.INTRO -> "معرفی برنامه"
            LoginDestinations.WELCOME -> "خوش آمدید"
            else -> "ورود"
        }
    PageFrame(page, title, onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (page) {
                LoginDestinations.INTRO -> {
                    Text("همه خدمات بانکی در یک برنامه", style = MaterialTheme.typography.titleLarge)
                    Text("حساب، کارت، قبض و تسهیلات را از همین‌جا مدیریت کنید.")
                    Button(
                        { onOpen(LoginDestinations.WELCOME) },
                        Modifier.fillMaxWidth().testTag("intro-next"),
                    ) {
                        Text("ادامه")
                    }
                }
                LoginDestinations.WELCOME -> {
                    Text(
                        "${MockData.current.customer.name} خوش آمدید",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text("آخرین ورود: ${MockData.current.customer.lastLogin}")
                    Button(
                        { onOpen(LoginDestinations.LOGIN) },
                        Modifier.fillMaxWidth().testTag("welcome-login"),
                    ) {
                        Text("ورود به حساب کاربری")
                    }
                }
                else -> LoginForm(onLogin)
            }
        }
    }
}

@Composable
private fun LoginForm(onLogin: () -> Unit) {
    // The password stays in memory only and never reaches saved state.
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Text("کاربر نمایشی: ${MockData.current.customer.username}")
    Text("رمز نمایشی در customer.json است؛ رمز واقعی وارد نکنید.")
    OutlinedTextField(
        username,
        { username = it },
        Modifier.fillMaxWidth().testTag("login-username"),
        label = { Text("نام کاربری") },
        singleLine = true,
    )
    OutlinedTextField(
        password,
        { password = it },
        Modifier.fillMaxWidth().testTag("login-password"),
        label = { Text("رمز عبور") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
    error?.let {
        Text(it, Modifier.testTag("login-error"), color = MaterialTheme.colorScheme.error)
    }
    Button(
        {
            error = LoginRules.error(username, password)
            if (error == null) onLogin()
        },
        Modifier.fillMaxWidth().testTag("login-submit"),
    ) {
        Text("ورود")
    }
}
