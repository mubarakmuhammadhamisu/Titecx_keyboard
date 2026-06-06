package com.gwani.keyboard

// -----------------------------------------------------------
// KEY DATA
// This file only holds data. No drawing. No logic. Just data.
// -----------------------------------------------------------

data class Key(
    val label: String,      // Text shown on the key
    val output: String,     // What gets typed or action triggered
    val width: Float = 1f,  // Relative width. 1f = normal size.
    val swipeUp: String = "" // What swipe-up types. Empty = no gesture.
)

object BaseLayer {

    val rows: List<List<Key>> = listOf(

        // Row 0 — number row (thin, always visible)
        // These keys have no swipe gesture — numbers are already one tap
        listOf(
            Key("1","1"), Key("2","2"), Key("3","3"), Key("4","4"), Key("5","5"),
            Key("6","6"), Key("7","7"), Key("8","8"), Key("9","9"), Key("0","0")
        ),

        // Row 1 — top letter row
        // swipeUp = symbols from reference keyboard layout
        listOf(
            Key("q","q", swipeUp="%"),
            Key("w","w", swipeUp="^"),
            Key("e","e", swipeUp="~"),
            Key("r","r", swipeUp="|"),
            Key("t","t", swipeUp="["),
            Key("y","y", swipeUp="ƴ"),  // Hausa hooked Y
            Key("u","u", swipeUp="{"),
            Key("i","i", swipeUp="}"),
            Key("o","o", swipeUp="<"),
            Key("p","p", swipeUp=">")
        ),

        // Row 2 — middle letter row
        listOf(
            Key("a","a", swipeUp="@"),
            Key("s","s", swipeUp="#"),
            Key("d","d", swipeUp="ɗ"),  // Hausa hooked D
            Key("f","f", swipeUp="*"),
            Key("g","g", swipeUp="-"),
            Key("h","h", swipeUp="+"),
            Key("j","j", swipeUp="("),
            Key("k","k", swipeUp="ƙ"),  // Hausa hooked K
            Key("l","l", swipeUp=")")
        ),

        // Row 3 — shift, letters, delete
        listOf(
            Key("⇧","shift", 1.5f),
            Key("z","z", swipeUp="!"),
            Key("x","x", swipeUp="_"),
            Key("c","c", swipeUp="\""),
            Key("v","v", swipeUp="'"),
            Key("b","b", swipeUp="ɓ"),  // Hausa hooked B
            Key("n","n", swipeUp=";"),
            Key("m","m", swipeUp="/"),
            Key("⌫","delete", 1.5f)
        ),

        // Row 4 — bottom row
        // space has no swipeUp — its gestures (left/right) are handled separately
        listOf(
            Key("?123","numbers", 1.2f),
            Key("😊","emoji",     1.0f),
            Key(",",",",          0.8f),
            Key("","space",       4.5f),
            Key(".",".",          0.8f),
            Key("↩","enter",      1.2f)
        )
    )
}
