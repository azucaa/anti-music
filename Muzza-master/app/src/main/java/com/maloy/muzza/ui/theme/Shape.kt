package com.maloy.muzza.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Shape definitions from Spotify + YouTube Music design system
val CardKecil = RoundedCornerShape(8.dp)
val CardBesar = RoundedCornerShape(12.dp)
val PillChip = RoundedCornerShape(50.dp) // Fully rounded
val BottomSheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

val Shapes = Shapes(
    small = CardKecil,
    medium = CardBesar,
    large = CardBesar,
    extraLarge = BottomSheetShape
)
