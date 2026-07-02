package com.manoli.moodmate.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String
)

val onboardingPages = listOf(
    OnboardingPage(
        emoji = "🔒",
        title = "Privacidad total",
        description = "Tus datos se guardan solo en tu dispositivo. Nada se comparte ni se envía a ningún servidor."
    ),
    OnboardingPage(
        emoji = "🧠",
        title = "Registra cómo te sientes",
        description = "Cada día puedes anotar tu estado de ánimo, energía y estrés. También puedes añadir notas o las horas de sueño."
    ),
    OnboardingPage(
        emoji = "📊",
        title = "Descubre patrones",
        description = "Visualiza tu evolución en gráficos y recibe consejos sobre cómo el sueño afecta tu bienestar."
    ),
    OnboardingPage(
        emoji = "🏆",
        title = "Crea hábitos positivos",
        description = "Completa ejercicios de respiración, gratitud y afirmaciones. ¡Mantén tu racha!"
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val item = onboardingPages[page]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = item.emoji,
                    fontSize = 72.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // Indicadores de página
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(onboardingPages.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                )
            }
        }

        // Botón "Empezar" / "Siguiente"
        Button(
            onClick = {
                if (pagerState.currentPage < onboardingPages.size - 1) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    onFinish()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (pagerState.currentPage == onboardingPages.size - 1) "Empezar" else "Siguiente"
            )
        }
    }
}