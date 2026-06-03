package com.gwani.keyboard

// -----------------------------------------------------------
// KEY DATA
// This file only holds data. No drawing. No logic. Just data.
// -----------------------------------------------------------

// A Key describes one single key on the keyboard.
data class Key(
    val label: String,   // Text shown on the key (example: "q", "⌫", "space")
    val output: String,  // What gets typed when tapped (example: "q", "delete", " ")
    val width: Float = 1f // Relative width. 1f = normal. 4f = four times wider (spacebar).
)

// BaseLayer holds the full QWERTY keyboard layout.
// It is an object, meaning there is only ever one copy of it in memory.
object BaseLayer {

    // rows is a list of rows. Each row is a list of keys.
    val rows: List<List<Key>> = listOf(
        listOf(
            Key("1", "1"), Key("2", "2"), Key("3", "3"), Key("4", "4"), Key("5", "5"), 
            Key("6", "6"), Key("7", "7"), Key("8", "8"), Key("9", "9"), Key("0", "0")
        ), 

        // Row 1 — top letter row
        listOf(
            Key("q", "q"), Key("w", "w"), Key("e", "e"), Key("r", "r"), Key("t", "t"),
            Key("y", "y"), Key("u", "u"), Key("i", "i"), Key("o", "o"), Key("p", "p")
        ),

        // Row 2 — middle letter row
        listOf(
            Key("a", "a"), Key("s", "s"), Key("d", "d"), Key("f", "f"), Key("g", "g"),
            Key("h", "h"), Key("j", "j"), Key("k", "k"), Key("l", "l")
        ),

        // Row 3 — shift, letters, delete
        listOf(
            Key("⇧", "shift", 1.5f),
            Key("z", "z"), Key("x", "x"), Key("c", "c"),
            Key("v", "v"), Key("b", "b"), Key("n", "n"), Key("m", "m"),
            Key("⌫", "delete", 1.5f)
        ),

        // Row 4 — symbol row (always visible)
        listOf(
            Key("₦", "₦"), Key("@", "@"), Key("&", "&"),
            Key("%", "%"), Key("/", "/"), Key("(", "("),
            Key(")", ")"), Key("?", "?"), Key("$", "$")
        ),

        // Row 5 — bottom row: numbers, comma, space, period, enter
        listOf(
            Key("?123", "numbers", 1.2f),
            Key(",", ",", 0.8f),
            Key("", "space", 3.5f),
            Key(".", ".", 0.8f),
            Key("↩", "enter", 1.2f)
        )
    )
}

