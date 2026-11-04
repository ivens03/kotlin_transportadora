package br.com.mba.fametro.kotlintransportadora.core.ui.tema

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Paleta inspirada no Mercado Livre (a mesma do front Angular): amarelo no topo, azul nas ações, fundo cinza. */
object Cores {
    val Amarelo = Color(0xFFFFE600)
    val Azul = Color(0xFF3483FA)
    val AzulEscuro = Color(0xFF2968C8)
    val AzulClaro = Color(0xFFE3EDFB)
    val Marinho = Color(0xFF2D3277)
    val Verde = Color(0xFF00A650)
    val Vermelho = Color(0xFFF23D4F)
    val Laranja = Color(0xFFFF7733)
    val Cinza = Color(0xFF737373)
    val Fundo = Color(0xFFEDEDED)
    val Texto = Color(0xFF333333)
    val TextoSuave = Color(0xFF666666)
    val Borda = Color(0xFFE5E5E5)
}

private val esquema = lightColorScheme(
    primary = Cores.Azul,
    onPrimary = Color.White,
    primaryContainer = Cores.AzulClaro,
    onPrimaryContainer = Cores.AzulEscuro,
    secondary = Cores.Amarelo,
    onSecondary = Cores.Marinho,
    // aba/filtro selecionado (SegmentedButton, FilterChip): azul claro em vez do lilás padrão do Material
    secondaryContainer = Cores.AzulClaro,
    onSecondaryContainer = Cores.AzulEscuro,
    background = Cores.Fundo,
    onBackground = Cores.Texto,
    surface = Color.White,
    onSurface = Cores.Texto,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Cores.TextoSuave,
    error = Cores.Vermelho,
    outline = Cores.Borda,
)

private val tipografia = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
)

private val formas = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
)

@Composable
fun RotaCertaTema(conteudo: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, typography = tipografia, shapes = formas, content = conteudo)
}
