package com.amadeusk.liftlog.ui.screens

// Compose layouts + scrolling
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Keyboard options for numeric inputs
import androidx.compose.foundation.text.KeyboardOptions

// Material UI
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Math for Navy method logs
import kotlin.math.log10

// Sub-tabs within the Tools screen
enum class InfoSubTab {
    TDEE,
    ONE_RM,
    PROTEIN,
    BODY_FAT
}

// Main Tools screen with sub-tabs (TDEE / 1RM / Protein / Body Fat)
@Composable
fun ToolsScreen() {
    // Which tool tab is selected
    var currentSubTab by remember { mutableStateOf(InfoSubTab.TDEE) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Sub-tabs row across the top
        TabRow(
            selectedTabIndex = currentSubTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = currentSubTab == InfoSubTab.TDEE,
                onClick = { currentSubTab = InfoSubTab.TDEE },
                text = { Text("TDEE") }
            )
            Tab(
                selected = currentSubTab == InfoSubTab.ONE_RM,
                onClick = { currentSubTab = InfoSubTab.ONE_RM },
                text = { Text("1RM") }
            )
            Tab(
                selected = currentSubTab == InfoSubTab.PROTEIN,
                onClick = { currentSubTab = InfoSubTab.PROTEIN },
                text = { Text("Protein") }
            )
            Tab(
                selected = currentSubTab == InfoSubTab.BODY_FAT,
                onClick = { currentSubTab = InfoSubTab.BODY_FAT },
                text = { Text("Body Fat %") }
            )
        }

        // Show the selected calculator
        when (currentSubTab) {
            InfoSubTab.TDEE -> TdeeCalculator()
            InfoSubTab.ONE_RM -> OneRmCalculator()
            InfoSubTab.PROTEIN -> ProteinNeedsCalculator()
            InfoSubTab.BODY_FAT -> BodyFatCalculator()
        }
    }
}

// TDEE calculator using Mifflin–St Jeor equation
@Composable
fun TdeeCalculator() {
    val scrollState = rememberScrollState()

    // Unit selection (metric or imperial)
    var useMetric by remember { mutableStateOf(true) }

    // User input fields
    var weightText by remember { mutableStateOf("") } // kg or lb
    var heightCmText by remember { mutableStateOf("") } // cm (metric)
    var heightFeetText by remember { mutableStateOf("") } // ft (imperial)
    var heightInchesText by remember { mutableStateOf("") } // in (imperial)
    var ageText by remember { mutableStateOf("") }

    // Sex selection
    var isMale by remember { mutableStateOf(true) }

    // Activity level selection index (0..4)
    var activityIndex by remember { mutableStateOf(1) }

    // Result / error state
    var showResult by remember { mutableStateOf(false) }
    var savedBmr by remember { mutableStateOf<Double?>(null) }
    var savedTdee by remember { mutableStateOf<Double?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Labels shown in the UI
    val activityLabels = listOf(
        "Sedentary (x1.2)",
        "Light (x1.375)",
        "Moderate (x1.55)",
        "Heavy (x1.725)",
        "Athlete (x1.9)"
    )

    // Multipliers used for TDEE calculation
    val activityMultipliers = listOf(1.2, 1.375, 1.55, 1.725, 1.9)

    // Convert text inputs to numbers (used when user taps Calculate)
    fun currentWeightKg(): Double? {
        val rawWeight = weightText.toDoubleOrNull() ?: return null
        return if (useMetric) rawWeight else rawWeight * 0.45359237
    }

    fun currentHeightCm(): Double? {
        return if (useMetric) {
            heightCmText.toDoubleOrNull()
        } else {
            val ft = heightFeetText.toDoubleOrNull() ?: return null
            val inch = heightInchesText.toDoubleOrNull() ?: 0.0
            ((ft * 12.0) + inch) * 2.54
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!showResult) {
            Text(
                text = "TDEE (Total Daily Energy Expenditure)",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "This estimates how many calories you burn per day based on your stats and activity. " +
                        "It uses the Mifflin–St Jeor equation. You can enter metric (kg / cm) or imperial (lb / ft + in).",
                style = MaterialTheme.typography.bodySmall
            )

            // Units toggle
            Text("Units", style = MaterialTheme.typography.labelMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = useMetric, onClick = { useMetric = true })
                    Text("Metric (kg / cm)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !useMetric, onClick = { useMetric = false })
                    Text("Imperial (lb / ft + in)")
                }
            }

            // Weight input
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text(if (useMetric) "Body weight (kg)" else "Body weight (lb)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Height input (metric: single field in cm, imperial: ft + in)
            if (useMetric) {
                OutlinedTextField(
                    value = heightCmText,
                    onValueChange = { heightCmText = it },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("Height", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = heightFeetText,
                        onValueChange = { heightFeetText = it },
                        label = { Text("Feet") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightInchesText,
                        onValueChange = { heightInchesText = it },
                        label = { Text("Inches") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Age input
            OutlinedTextField(
                value = ageText,
                onValueChange = { ageText = it },
                label = { Text("Age (years)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Sex selection
            Text("Sex", style = MaterialTheme.typography.labelMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isMale,
                        onClick = { isMale = true }
                    )
                    Text("Male")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isMale,
                        onClick = { isMale = false }
                    )
                    Text("Female")
                }
            }

            // Activity selection
            Text("Activity level", style = MaterialTheme.typography.labelMedium)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                activityLabels.forEachIndexed { index, label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = activityIndex == index,
                            onClick = { activityIndex = index }
                        )
                        Text(label)
                    }
                }
            }

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val w = currentWeightKg()
                    val h = currentHeightCm()
                    val a = ageText.toIntOrNull()
                    if (w != null && h != null && a != null) {
                        val baseBmr = if (isMale) {
                            10 * w + 6.25 * h - 5 * a + 5
                        } else {
                            10 * w + 6.25 * h - 5 * a - 161
                        }
                        val baseTdee = baseBmr * activityMultipliers[activityIndex]
                        savedBmr = baseBmr
                        savedTdee = baseTdee
                        showResult = true
                        errorText = null
                    } else {
                        errorText = "Fill in weight, height, age, and sex to calculate your TDEE."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calculate TDEE")
            }
        } else {
            val bmr = savedBmr
            val tdee = savedTdee
            if (bmr != null && tdee != null) {
                Text(
                    text = "Your estimated daily calories",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Estimated BMR: ${bmr.toInt()} kcal/day")
                Text("Estimated TDEE: ${tdee.toInt()} kcal/day")

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Rough guidelines:", style = MaterialTheme.typography.labelMedium)
                Text("- Mild fat loss: TDEE - 250 to 400 kcal")
                Text("- Aggressive cut: TDEE - 500 to 700 kcal")
                Text("- Slow bulk: TDEE + 200 to 300 kcal")

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        showResult = false
                        weightText = ""
                        heightCmText = ""
                        heightFeetText = ""
                        heightInchesText = ""
                        ageText = ""
                        errorText = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("New calculation")
                }
            } else {
                // Fallback: reset to input mode if results were somehow cleared
                showResult = false
            }
        }
    }
}

// 1RM calculator using Epley formula
@Composable
fun OneRmCalculator() {
    val scrollState = rememberScrollState()

    // User input fields
    var weightText by remember { mutableStateOf("") }
    var repsText by remember { mutableStateOf("") }
    var useKg by remember { mutableStateOf(true) }

    // Result + error state
    var showResult by remember { mutableStateOf(false) }
    var savedOneRm by remember { mutableStateOf<Double?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!showResult) {
            Text(
                text = "1RM (One-Rep Max) Estimator",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Enter the weight you lifted and how many clean reps you did. " +
                        "This uses the Epley formula, which works best for sets of 1–12 reps. " +
                        "You can enter kg or lb – results will be in the same unit.",
                style = MaterialTheme.typography.bodySmall
            )

            // Units toggle
            Text("Units", style = MaterialTheme.typography.labelMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = useKg, onClick = { useKg = true })
                    Text("kg")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !useKg, onClick = { useKg = false })
                    Text("lb")
                }
            }

            // Weight input
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text(if (useKg) "Weight used (kg)" else "Weight used (lb)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Reps input
            OutlinedTextField(
                value = repsText,
                onValueChange = { repsText = it },
                label = { Text("Reps (1–12)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val weight = weightText.toDoubleOrNull()
                    val reps = repsText.toIntOrNull()
                    val oneRm = if (weight != null && reps != null && reps in 1..12) {
                        weight * (1.0 + reps.toDouble() / 30.0)
                    } else null

                    if (oneRm != null) {
                        savedOneRm = oneRm
                        showResult = true
                        errorText = null
                    } else {
                        errorText = "Enter a valid weight and reps (1–12) to estimate your 1RM."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Estimate 1RM")
            }
        } else {
            val oneRm = savedOneRm
            if (oneRm != null) {
                Text(
                    text = "Estimated 1RM",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("1RM ≈ ${String.format("%.1f", oneRm)} ${if (useKg) "kg" else "lb"}")

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Suggested training percentages:",
                    style = MaterialTheme.typography.labelMedium
                )

                val percents = listOf(0.6, 0.7, 0.8, 0.9)
                percents.forEach { p ->
                    val w = oneRm * p
                    Text(
                        text = "${(p * 100).toInt()}% ≈ ${String.format("%.1f", w)} ${if (useKg) "kg" else "lb"}"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        showResult = false
                        weightText = ""
                        repsText = ""
                        errorText = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("New calculation")
                }
            } else {
                showResult = false
            }
        }
    }
}

// Protein calculator (returns a range in grams/day)
@Composable
fun ProteinNeedsCalculator() {
    val scrollState = rememberScrollState()

    // User input + unit toggle (local to this calculator)
    var weightText by remember { mutableStateOf("") }
    var useKg by remember { mutableStateOf(true) }

    // User goal selection
    var goal by remember { mutableStateOf("Recomp / Maintain") }

    // Convert input to number
    val weight = weightText.toDoubleOrNull()

    // Convert to kg if user entered lb
    val weightKg = if (weight != null) {
        if (useKg) weight else weight * 0.45359237
    } else null

    // Protein range targets (g/kg) based on goal
    val (low, high) = when (goal) {
        "Cut / Fat loss" -> 2.0 to 2.7
        "Recomp / Maintain" -> 1.6 to 2.2
        "Bulk / Gain" -> 1.6 to 2.0
        else -> 1.6 to 2.2
    }

    // Compute grams/day range if weight is valid
    val gramsRange = weightKg?.let { kg ->
        val lowG = kg * low
        val highG = kg * high
        lowG to highG
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Protein Needs", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "General evidence-based ranges for lifters are around 1.6–2.2 g/kg of bodyweight per day, " +
                    "higher when cutting, slightly lower when bulking. You can enter weight in kg or lb.",
            style = MaterialTheme.typography.bodySmall
        )

        // Bodyweight input
        OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text(if (useKg) "Body weight (kg)" else "Body weight (lb)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Unit selection
        Text("Units", style = MaterialTheme.typography.labelMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = useKg, onClick = { useKg = true })
                Text("kg")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !useKg, onClick = { useKg = false })
                Text("lb")
            }
        }

        // Goal selection
        Text("Goal", style = MaterialTheme.typography.labelMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Cut / Fat loss", "Recomp / Maintain", "Bulk / Gain").forEach { g ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = goal == g,
                        onClick = { goal = g }
                    )
                    Text(g)
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Show results if valid
        if (gramsRange != null) {
            val (lowG, highG) = gramsRange

            Text(text = "Recommended daily protein:", style = MaterialTheme.typography.labelMedium)
            Text("${lowG.toInt()} – ${highG.toInt()} g per day")

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Example splits:", style = MaterialTheme.typography.labelMedium)

            // Example meal splits
            val meals = 3
            val mealsHigh = 4
            Text("- If you eat $meals meals: ${(lowG / meals).toInt()} – ${(highG / meals).toInt()} g per meal")
            Text("- If you eat $mealsHigh meals: ${(lowG / mealsHigh).toInt()} – ${(highG / mealsHigh).toInt()} g per meal")
        } else {
            Text(
                text = "Enter your bodyweight to get a recommended daily protein range.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Extra notes
        Text(text = "Notes:", style = MaterialTheme.typography.labelMedium)
        Text("- Aim for a good protein source in every meal (meat, eggs, dairy, whey, tofu, etc.).")
        Text("- More total calories > tiny differences in protein when bulking.")
        Text("- When cutting, higher protein helps keep muscle while losing fat.")
    }
}

// Body fat calculator with two methods: BMI estimate + Navy tape method
@Composable
fun BodyFatCalculator() {
    val scrollState = rememberScrollState()

    // Unit selection (metric or imperial)
    var useMetric by remember { mutableStateOf(true) }
    val heightUnit = if (useMetric) "cm" else "in"
    val weightUnit = if (useMetric) "kg" else "lb"

    // Sex selection
    var isMale by remember { mutableStateOf(true) }

    // BMI method inputs
    var bmiHeightText by remember { mutableStateOf("") }
    var bmiWeightText by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }

    // Navy method inputs
    var heightText by remember { mutableStateOf("") }
    var neckText by remember { mutableStateOf("") }
    var waistText by remember { mutableStateOf("") }
    var hipText by remember { mutableStateOf("") }

    // Convert string to Double? (helper)
    fun d(s: String) = s.toDoubleOrNull()

    // Unit conversions (imperial -> metric)
    fun toKg(v: Double) = if (useMetric) v else v * 0.45359237
    fun toCm(v: Double) = if (useMetric) v else v * 2.54

    // -------------------------
    // BMI METHOD CALC
    // -------------------------
    val bmiHeightCm = d(bmiHeightText)?.let { toCm(it) }
    val bmiWeightKg = d(bmiWeightText)?.let { toKg(it) }
    val age = ageText.toIntOrNull()

    // BMI = kg / m^2
    val bmi = if (bmiHeightCm != null && bmiWeightKg != null && bmiHeightCm > 0) {
        val m = bmiHeightCm / 100.0
        bmiWeightKg / (m * m)
    } else null

    // BMI body fat estimate formula
    val bmiBodyFat = if (bmi != null && age != null) {
        val sexFactor = if (isMale) 1.0 else 0.0
        1.20 * bmi + 0.23 * age - 10.8 * sexFactor - 5.4
    } else null

    // -------------------------
    // NAVY METHOD CALC
    // -------------------------
    val navyHeight = d(heightText)?.let { toCm(it) }
    val neck = d(neckText)?.let { toCm(it) }
    val waist = d(waistText)?.let { toCm(it) }
    val hip = d(hipText)?.let { toCm(it) }

    // Check required inputs based on sex
    val hasMaleInputs = isMale && navyHeight != null && neck != null && waist != null
    val hasFemaleInputs = !isMale && navyHeight != null && neck != null && waist != null && hip != null

    // Compute Navy method body fat % (different formula for male/female)
    val navyBodyFat: Double? = when {
        hasMaleInputs && (waist!! - neck!!) > 0 -> {
            val density =
                1.0324 -
                        0.19077 * log10(waist - neck) +
                        0.15456 * log10(navyHeight!!)
            (495.0 / density) - 450.0
        }

        hasFemaleInputs && (waist!! + hip!! - neck!!) > 0 -> {
            val density =
                1.29579 -
                        0.35004 * log10(waist + hip - neck) +
                        0.22100 * log10(navyHeight!!)
            (495.0 / density) - 450.0
        }

        else -> null
    }

    // Helpful error message if measurement math is invalid
    val invalidShape = when {
        hasMaleInputs && waist != null && neck != null && (waist - neck) <= 0 ->
            "Waist must be larger than neck for the Navy method."
        hasFemaleInputs && waist != null && hip != null && neck != null &&
                (waist + hip - neck) <= 0 ->
            "Waist + hip must be larger than neck."
        else -> null
    }

    // -------------------------
    // UI
    // -------------------------
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Body Fat Percentage", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "These are rough estimates using BMI and the US Navy tape method. " +
                    "You can use metric (kg / cm) or imperial (lb / in). For imperial, enter heights in total inches (e.g. 5'10\" = 70 in).",
            style = MaterialTheme.typography.bodySmall
        )

        // Unit toggle
        Text("Units", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = useMetric, onClick = { useMetric = true })
            Text("Metric (kg / cm)")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = !useMetric, onClick = { useMetric = false })
            Text("Imperial (lb / in)")
        }

        // Sex toggle
        Text("Sex", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isMale, onClick = { isMale = true })
            Text("Male")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = !isMale, onClick = { isMale = false })
            Text("Female")
        }

        // -------------------------
        // BMI UI
        // -------------------------
        Divider()
        Text("Quick Estimate (BMI Method)", style = MaterialTheme.typography.labelLarge)
        Text(
            "Fast estimate using BMI, age, and sex. Less precise than Navy method.",
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = bmiWeightText,
            onValueChange = { bmiWeightText = it },
            label = { Text("Body weight ($weightUnit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = bmiHeightText,
            onValueChange = { bmiHeightText = it },
            label = { Text("Height ($heightUnit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ageText,
            onValueChange = { ageText = it },
            label = { Text("Age (years)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Show BMI number if available
        bmi?.let { Text("BMI: ${String.format("%.1f", it)}") }

        // Show BMI bodyfat if available
        bmiBodyFat?.let {
            if (!it.isNaN() && !it.isInfinite()) {
                Text("Estimated BF% (BMI): ${String.format("%.1f", it)}%")
            }
        } ?: Text(
            "Enter weight, height, and age for BMI estimate.",
            style = MaterialTheme.typography.bodySmall
        )

        // -------------------------
        // NAVY UI
        // -------------------------
        Divider()
        Text("US Navy Tape-Measure Method", style = MaterialTheme.typography.labelLarge)
        Text("More accurate if measurements are correct.", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = heightText,
            onValueChange = { heightText = it },
            label = { Text("Height ($heightUnit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = neckText,
            onValueChange = { neckText = it },
            label = { Text("Neck ($heightUnit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = waistText,
            onValueChange = { waistText = it },
            label = { Text("Waist ($heightUnit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Hip is only required for female Navy method
        if (!isMale) {
            OutlinedTextField(
                value = hipText,
                onValueChange = { hipText = it },
                label = { Text("Hip ($heightUnit)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Show Navy body fat estimate if valid
        if (navyBodyFat != null && !navyBodyFat.isNaN() && !navyBodyFat.isInfinite()) {
            Text(
                "Estimated BF% (Navy): ${String.format("%.1f", navyBodyFat)}%",
                style = MaterialTheme.typography.headlineSmall
            )
        } else {
            Text(
                "Enter all tape measurements for Navy estimate.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Show shape/measurement error if present
        invalidShape?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

