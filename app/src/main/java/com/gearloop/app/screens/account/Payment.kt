package com.gearloop.app.screens.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.gearloop.app.ui.components.GearDialog
import com.gearloop.app.ui.components.GearTextField
import com.gearloop.app.ui.components.VGap
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.theme.Text
import com.gearloop.app.ui.vm.MoneyViewModel

/**
 * Add a payment card. The number is checked with the Luhn rule and the expiry
 * against today; only the brand and the last four digits are kept.
 */
@Composable
fun AddCardDialog(vm: MoneyViewModel, onSaved: () -> Unit = {}, onDismiss: () -> Unit) {
    var number by rememberSaveable { mutableStateOf("") }
    var expiry by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    GearDialog(
        title = "Add a card",
        onDismiss = onDismiss,
        confirmLabel = "Save card",
        onConfirm = { vm.saveCard(number, expiry) { e -> if (e == null) { onSaved(); onDismiss() } else error = e } },
        confirmEnabled = number.filter(Char::isDigit).length >= 13 && expiry.filter(Char::isDigit).length == 4,
    ) {
        Text("Only the last four digits are kept on this phone. Nothing is charged until you book.", style = GearType.bodyS, color = GearText.secondary)
        VGap(Space.s)
        GearTextField(
            number,
            label = "Card number",
            placeholder = "1234 5678 9012 3456",
            onValueChange = { number = it.filter(Char::isDigit).take(19); error = null },
            keyboardType = KeyboardType.Number,
            visualTransformation = com.gearloop.app.ui.components.CardNumberTransformation,
        )
        VGap(Space.s)
        GearTextField(
            expiry,
            label = "Expiry",
            placeholder = "MM/YY",
            onValueChange = { expiry = it.filter(Char::isDigit).take(4); error = null },
            keyboardType = KeyboardType.Number,
            error = error,
            visualTransformation = com.gearloop.app.ui.components.ExpiryTransformation,
        )
    }
}
