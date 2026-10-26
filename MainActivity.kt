package com.leanlife.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val android.content.Context.dataStore by preferencesDataStore(name = "leanlife")
private val Green = Color(0xFF42E78B)
private val Background = Color(0xFF07130F)
private val Surface = Color(0xFF10251C)
private val Muted = Color(0xFFB6CFC1)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LeanLifeApp(this) }
    }
}

private enum class Page(val label: String) {
    Home("Главная"), Food("Питание"), Move("Активность"), Habits("Привычки"), Profile("Профиль")
}

@Composable
private fun LeanLifeApp(activity: ComponentActivity) {
    val scope = rememberCoroutineScope()
    val water by activity.dataStore.data.map { it[intPreferencesKey("water")] ?: 0 }.collectAsState(initial = 0)
    val mealLog by activity.dataStore.data.map { it[stringPreferencesKey("meals")] ?: "" }.collectAsState(initial = "")
    val checks by activity.dataStore.data.map { it[stringPreferencesKey("habits")] ?: "" }.collectAsState(initial = "")
    val checked = remember(checks) { checks.split("|").filter { it.isNotBlank() }.toSet() }
    var page by remember { mutableStateOf(Page.Home) }
    var selectedMeal by remember { mutableStateOf("Завтрак") }
    var mealText by remember { mutableStateOf("") }
    var activityChoice by remember { mutableStateOf("Прогулка") }
    var activityDone by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("Друг") }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Green, onPrimary = Color(0xFF052012), background = Background,
        surface = Surface, onSurface = Color.White, secondary = Muted
    )) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF0A1912), contentColor = Green) {
                    Page.entries.forEach { item ->
                        NavigationBarItem(
                            selected = page == item,
                            onClick = { page = item },
                            icon = {
                                Icon(when(item) {
                                    Page.Home -> Icons.Default.Home
                                    Page.Food -> Icons.Default.Restaurant
                                    Page.Move -> Icons.Default.DirectionsWalk
                                    Page.Habits -> Icons.Default.CheckCircle
                                    Page.Profile -> Icons.Default.Person
                                }, contentDescription = item.label)
                            },
                            label = { Text(item.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Green, selectedTextColor = Green,
                                indicatorColor = Surface, unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("🌿 LeanLife", color = Green, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            Text("Здоровые привычки — без крайностей", color = Muted, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.AccountCircle, null, tint = Green, modifier = Modifier.size(34.dp))
                    }
                }
                when(page) {
                    Page.Home -> {
                        item { Text("Привет, $name! 👋", fontSize = 27.sp, fontWeight = FontWeight.Bold) }
                        item { Text("Сегодня важны не цифры, а забота о себе.", color = Muted) }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard("Вода", "$water мл", Icons.Default.WaterDrop, Modifier.weight(1f))
                                StatCard("Привычки", "${checked.size}/4", Icons.Default.CheckCircle, Modifier.weight(1f))
                            }
                        }
                        item {
                            CardBlock("План на сегодня", "Отмечай пункты в комфортном темпе.") {
                                listOf("Вода в течение дня", "Разнообразное питание", "Движение в удовольствие", "Время на отдых").forEach { habit ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = habit in checked, onCheckedChange = { yes ->
                                            val updated = if (yes) checked + habit else checked - habit
                                            scope.launch { activity.dataStore.edit { it[stringPreferencesKey("habits")] = updated.joinToString("|") } }
                                        })
                                        Text(habit, color = Color.White)
                                    }
                                }
                            }
                        }
                        item {
                            CardBlock("Вода", "Записывай выпитую воду; потребность у всех разная.") {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = { scope.launch { activity.dataStore.edit { it[intPreferencesKey("water")] = (water - 250).coerceAtLeast(0) } } }) { Text("− 250 мл") }
                                    Text("$water мл", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    Button(onClick = { scope.launch { activity.dataStore.edit { it[intPreferencesKey("water")] = water + 250 } } }) { Text("+ 250 мл") }
                                }
                            }
                        }
                        item { CardBlock("Совет дня", "Регулярные приёмы пищи, сон и движение важнее строгих ограничений.") {} }
                    }
                    Page.Food -> {
                        item { Text("Питание", fontSize = 27.sp, fontWeight = FontWeight.Bold) }
                        item { Text("Записывай блюда без осуждения и строгих запретов.", color = Muted) }
                        item {
                            CardBlock("Добавить блюдо", "Выбери приём пищи и напиши, что ел(а).") {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Завтрак", "Обед", "Ужин", "Перекус").forEach {
                                        FilterChip(selected = selectedMeal == it, onClick = { selectedMeal = it }, label = { Text(it, fontSize = 11.sp) })
                                    }
                                }
                                OutlinedTextField(value = mealText, onValueChange = { mealText = it }, label = { Text("Например: каша и фрукты") }, modifier = Modifier.fillMaxWidth())
                                Button(onClick = {
                                    if (mealText.isNotBlank()) {
                                        scope.launch {
                                            activity.dataStore.edit { p ->
                                                val old = p[stringPreferencesKey("meals")] ?: ""
                                                p[stringPreferencesKey("meals")] = (old + "\n$selectedMeal: ${mealText.trim()}").trim()
                                            }
                                        }
                                        mealText = ""
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить блюдо") }
                            }
                        }
                        item {
                            CardBlock("Журнал питания", "Локальная запись на этом устройстве.") {
                                Text(if (mealLog.isBlank()) "Пока нет записей." else mealLog, color = Muted)
                                TextButton(onClick = { scope.launch { activity.dataStore.edit { it.remove(stringPreferencesKey("meals")) } } }) { Text("Очистить журнал") }
                            }
                        }
                        item { CardBlock("Простая идея", "Старайся сочетать разные продукты: овощи или фрукты, источник белка и привычные гарниры. Нет необходимости делить еду на «хорошую» и «плохую».") {} }
                    }
                    Page.Move -> {
                        item { Text("Активность", fontSize = 27.sp, fontWeight = FontWeight.Bold) }
                        item { Text("Выбирай движение по самочувствию. Отдых тоже считается заботой о себе.", color = Muted) }
                        item {
                            CardBlock("Выбери вариант", "Без обязательных норм и соревнования с собой.") {
                                listOf("Прогулка", "Танцы", "Растяжка", "Велосипед", "День отдыха").forEach {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = activityChoice == it, onClick = { activityChoice = it; activityDone = false })
                                        Text(it)
                                    }
                                }
                                Text(
                                    when(activityChoice) {
                                        "Прогулка" -> "Прогуляйся в комфортном темпе и в безопасном месте."
                                        "Танцы" -> "Включи музыку и двигайся так, как нравится."
                                        "Растяжка" -> "Мягкие движения без боли и рывков."
                                        "Велосипед" -> "Выбери безопасный маршрут и надень шлем."
                                        else -> "Отдохни, восстановись и займись любимым делом."
                                    }, color = Muted
                                )
                                Button(onClick = { activityDone = !activityDone }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (activityDone) "Отмечено ✓" else "Отметить")
                                }
                            }
                        }
                    }
                    Page.Habits -> {
                        item { Text("Привычки", fontSize = 27.sp, fontWeight = FontWeight.Bold) }
                        item { Text("Маленькие шаги без стремления к идеалу.", color = Muted) }
                        item {
                            CardBlock("Мой список", "Отмечай то, что получилось сегодня.") {
                                listOf("Регулярно есть", "Пить воду по жажде", "Двигаться в удовольствие", "Соблюдать режим сна").forEach { habit ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = habit in checked, onCheckedChange = { yes ->
                                            val updated = if (yes) checked + habit else checked - habit
                                            scope.launch { activity.dataStore.edit { it[stringPreferencesKey("habits")] = updated.joinToString("|") } }
                                        })
                                        Text(habit)
                                    }
                                }
                            }
                        }
                    }
                    Page.Profile -> {
                        item { Text("Профиль", fontSize = 27.sp, fontWeight = FontWeight.Bold) }
                        item {
                            CardBlock("Настройки", "Данные хранятся локально на устройстве.") {
                                OutlinedTextField(value = name, onValueChange = { name = it.take(24) }, label = { Text("Как к тебе обращаться") }, modifier = Modifier.fillMaxWidth())
                                Text("Версия 0.1.0 · бесплатно · без рекламы", color = Muted)
                                Text("LeanLife не ставит диагнозы и не заменяет врача. Не используй приложение для голодания или чрезмерных тренировок.", color = Muted, fontSize = 12.sp)
                                TextButton(onClick = { scope.launch { activity.dataStore.edit { it.clear() } }; name = "Друг" }) { Text("Сбросить локальные данные") }
                            }
                        }
                    }
                }
                item {
                    Text("LeanLife • Бесплатно • Без рекламы", color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(25.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CardBlock(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, color = Muted, fontSize = 12.sp)
            content()
        }
    }
}
