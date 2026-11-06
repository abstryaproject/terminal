package com.lailaba.linux

import android.os.Bundle
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val terminal by lazy { NativePty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        File(filesDir, "home").mkdirs()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF080B08),
                    surface = Color(0xFF111711),
                    primary = Color(0xFF39D353)
                )
            ) {
                TerminalScreen(terminal = terminal)
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) terminal.close()
        super.onDestroy()
    }
}

@Composable
private fun TerminalScreen(terminal: NativePty) {
    val outputLines = remember { mutableStateListOf<String>() }
    var partialLine by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var started by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(terminal) {
        val result = withContext(Dispatchers.IO) { terminal.start() }
        started = result == 0
        if (!started) {
            error = "Could not start the PTY (native error $result)."
            return@LaunchedEffect
        }

        while (true) {
            val chunk = withContext(Dispatchers.IO) { terminal.read() }
            if (chunk.isNotEmpty()) {
                val combined = partialLine + chunk
                val segments = combined.split('\n')
                if (segments.size > 1) {
                    outputLines.addAll(segments.dropLast(1))
                    partialLine = segments.last()
                } else {
                    partialLine = combined
                }
                while (outputLines.size > 1500) outputLines.removeAt(0)
            }
            delay(35)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080B08))
            .padding(12.dp)
    ) {
        Text("LINUX TERMINAL", color = Color(0xFF39D353), fontSize = 13.sp)
        Text(
            if (started) "PTY connected • Android host shell" else "Starting terminal…",
            color = Color.Gray,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(outputLines) { line ->
                Text(
                    line,
                    color = Color(0xFFE0E8E0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
            item {
                Text(
                    partialLine,
                    color = Color(0xFFE0E8E0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        error?.let { Text(it, color = Color.Red, fontSize = 12.sp) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("›", color = Color(0xFF39D353), fontSize = 20.sp)
            BasicTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.weight(1f).padding(top = 7.dp),
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (command.isNotBlank() && started) {
                            terminal.write(command + "\n")
                            command = ""
                        }
                    }
                ),
                decorationBox = { inner ->
                    Box {
                        if (command.isEmpty()) Text("Enter command", color = Color.Gray)
                        inner()
                    }
                }
            )
            Button(
                enabled = started,
                onClick = {
                    if (command.isNotBlank()) {
                        terminal.write(command + "\n")
                        command = ""
                    }
                }
            ) { Text("Run") }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Milestone 1 • PTY transport only • Debian/PRoot not installed",
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}
