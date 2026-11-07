package com.example.mylittlefinancontrol.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.mylittlefinancontrol.DashboardCache
import com.example.mylittlefinancontrol.DetallePeriodoActivity
import com.example.mylittlefinancontrol.GeminiAdvisorDialogFragment
import com.example.mylittlefinancontrol.MainActivity
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.Modelos.ResumenFinanciero
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class FragmentInicio : Fragment() {

    private lateinit var mContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MyLittleFinanControlTheme {
                    val mainActivity = activity as? MainActivity
                    InicioStatefulScreen(
                        onNavigateMovimientos = { mainActivity?.verFragmentoMovimientos() },
                        onNavigateGastosFijos = { mainActivity?.verFragmentoGastosFijos() },
                        onNavigateDeudas = { mainActivity?.verFragmentoDeudas() },
                        onNavigateAhorros = { mainActivity?.verFragmentoAhorros() },
                        onOpenGeminiAdvisor = {
                            GeminiAdvisorDialogFragment().show(parentFragmentManager, "GeminiAdvisor")
                        },
                        onOpenDetallePeriodo = { tipoPeriodo ->
                            val intent = Intent(mContext, DetallePeriodoActivity::class.java).apply {
                                putExtra("TIPO_PERIODO", tipoPeriodo)
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun calcularFechasPeriodo(tipo: String): Pair<Pair<String, String>, Pair<String, String>> {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance()
            val hoy = sdf.format(cal.time)

            return when (tipo) {
                "SEMANAL" -> {
                    val endActual = hoy
                    val calStartAct = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
                    val startActual = sdf.format(calStartAct.time)

                    val calEndAnt = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                    val endAnt = sdf.format(calEndAnt.time)
                    val calStartAnt = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -13) }
                    val startAnt = sdf.format(calStartAnt.time)

                    Pair(Pair(startActual, endActual), Pair(startAnt, endAnt))
                }
                "MENSUAL" -> {
                    val calActIni = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
                    val startActual = sdf.format(calActIni.time)
                    val endActual = hoy

                    val calAntFin = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_YEAR, -1) }
                    val endAnt = sdf.format(calAntFin.time)
                    val calAntIni = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, -1) }
                    val startAnt = sdf.format(calAntIni.time)

                    Pair(Pair(startActual, endActual), Pair(startAnt, endAnt))
                }
                "SEMESTRAL" -> {
                    val endActual = hoy
                    val calStartAct = Calendar.getInstance().apply { add(Calendar.MONTH, -5); set(Calendar.DAY_OF_MONTH, 1) }
                    val startActual = sdf.format(calStartAct.time)

                    val calEndAnt = Calendar.getInstance().apply { time = calStartAct.time; add(Calendar.DAY_OF_YEAR, -1) }
                    val endAnt = sdf.format(calEndAnt.time)
                    val calStartAnt = Calendar.getInstance().apply { time = calEndAnt.time; add(Calendar.MONTH, -5); set(Calendar.DAY_OF_MONTH, 1) }
                    val startAnt = sdf.format(calStartAnt.time)

                    Pair(Pair(startActual, endActual), Pair(startAnt, endAnt))
                }
                else -> {
                    val calActIni = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1) }
                    val startActual = sdf.format(calActIni.time)
                    val endActual = hoy

                    val calAntFin = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1); add(Calendar.DAY_OF_YEAR, -1) }
                    val endAnt = sdf.format(calAntFin.time)
                    val calAntIni = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1); add(Calendar.YEAR, -1) }
                    val startAnt = sdf.format(calAntIni.time)

                    Pair(Pair(startActual, endActual), Pair(startAnt, endAnt))
                }
            }
        }
    }
}

@Composable
fun InicioStatefulScreen(
    onNavigateMovimientos: () -> Unit,
    onNavigateGastosFijos: () -> Unit,
    onNavigateDeudas: () -> Unit,
    onNavigateAhorros: () -> Unit,
    onOpenGeminiAdvisor: () -> Unit,
    onOpenDetallePeriodo: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { NotasDatabaseHelper.getInstance(context) }

    var dashboardCache by remember { mutableStateOf(db.getCachedDashboardImmediate()) }
    var resumenFinanciero by remember { mutableStateOf(ResumenFinanciero(0.0, 0.0, 0.0, 0.0)) }
    var esIncognito by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refreshTrigger) {
        val immediate = db.getCachedDashboardImmediate()
        if (immediate != null) {
            dashboardCache = immediate
        }
        val (freshCache, freshResumen) = withContext(Dispatchers.IO) {
            val c = db.getDashboardDataCached { p -> FragmentInicio.calcularFechasPeriodo(p) }
            val r = db.getResumenFinanciero()
            c to r
        }
        dashboardCache = freshCache
        resumenFinanciero = freshResumen
    }

    InicioScreen(
        cache = dashboardCache,
        resumenFinanciero = resumenFinanciero,
        esIncognito = esIncognito,
        onToggleIncognito = { esIncognito = !esIncognito },
        onNavigateMovimientos = onNavigateMovimientos,
        onNavigateGastosFijos = onNavigateGastosFijos,
        onNavigateDeudas = onNavigateDeudas,
        onNavigateAhorros = onNavigateAhorros,
        onOpenGeminiAdvisor = onOpenGeminiAdvisor,
        onOpenDetallePeriodo = onOpenDetallePeriodo
    )
}

@Composable
fun InicioScreen(
    cache: DashboardCache?,
    resumenFinanciero: ResumenFinanciero,
    esIncognito: Boolean,
    onToggleIncognito: () -> Unit,
    onNavigateMovimientos: () -> Unit,
    onNavigateGastosFijos: () -> Unit,
    onNavigateDeudas: () -> Unit,
    onNavigateAhorros: () -> Unit,
    onOpenGeminiAdvisor: () -> Unit,
    onOpenDetallePeriodo: (String) -> Unit
) {
    val colors = DabaTheme.colors
    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    val balanceTotal = resumenFinanciero.balanceTotal
    val totalAhorros = resumenFinanciero.totalAhorros
    val ahorroCuenta = resumenFinanciero.ahorroEnCuenta
    val ahorroAlcancia = resumenFinanciero.ahorroEnAlcancia

    val ingresosMes = cache?.ingresosMes ?: 0.0
    val gastosMes = cache?.gastosMes ?: 0.0
    val totalGastosFijos = cache?.totalGastosFijos ?: 0.0
    val totalDeudasRestante = cache?.totalDeudasRestante ?: 0.0
    val countGastosMes = cache?.countGastosMes ?: 0
    val countGF = cache?.countGF ?: 0
    val countDeudas = cache?.countDeudas ?: 0

    val signo = if (balanceTotal < 0) "-$ " else if (balanceTotal > 0) "+$ " else "$ "
    val valorFormateado = format.format(abs(balanceTotal)).replace("$", "").trim()
    val balanceGeneralText = if (esIncognito) "••••••••" else "$signo$valorFormateado"
    val balanceColor = if (balanceTotal < 0) colors.textPrimary else colors.primaryEmerald

    val periodoSemanal = cache?.periodosResumen?.get("SEMANAL") ?: Pair(0.0, "0% vs ant.")
    val periodoMensual = cache?.periodosResumen?.get("MENSUAL") ?: Pair(0.0, "0% vs ant.")
    val periodoSemestral = cache?.periodosResumen?.get("SEMESTRAL") ?: Pair(0.0, "0% vs ant.")
    val periodoAnual = cache?.periodosResumen?.get("ANUAL") ?: Pair(0.0, "0% vs ant.")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .padding(bottom = 84.dp)
    ) {
        // 1. Hero Balance Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clickable { onNavigateMovimientos() },
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceCard,
            border = BorderStroke(1.dp, colors.borderSubtle),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_balance),
                            contentDescription = null,
                            tint = colors.textVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BALANCE DISPONIBLE",
                            color = colors.textVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    }
                    IconButton(
                        onClick = onToggleIncognito,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (esIncognito) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                            ),
                            contentDescription = "Alternar visibilidad del balance",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = balanceGeneralText,
                    color = balanceColor,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.expenseRedBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_trending_down),
                                contentDescription = null,
                                tint = colors.expenseRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "-2.4% este mes",
                                color = colors.expenseRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cierre proyectado sereno",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4 Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickActionBtn(
                        modifier = Modifier.weight(1f),
                        label = "Gasto",
                        iconRes = R.drawable.ic_remove,
                        iconBg = colors.expenseRedBg,
                        iconTint = colors.expenseRed,
                        onClick = onNavigateMovimientos
                    )
                    QuickActionBtn(
                        modifier = Modifier.weight(1f),
                        label = "Ingreso",
                        iconRes = R.drawable.ic_add,
                        iconBg = colors.ghostBg,
                        iconTint = colors.primaryEmerald,
                        onClick = onNavigateMovimientos
                    )
                    QuickActionBtn(
                        modifier = Modifier.weight(1f),
                        label = "Transferir",
                        iconRes = R.drawable.ic_sync_alt,
                        iconBg = colors.surfaceContainerHigh,
                        iconTint = colors.textPrimary,
                        onClick = onNavigateMovimientos
                    )
                    QuickActionBtn(
                        modifier = Modifier.weight(1f),
                        label = "Analítica",
                        iconRes = R.drawable.ic_insights,
                        iconBg = colors.primaryEmerald,
                        iconTint = colors.surfaceCard,
                        onClick = onOpenGeminiAdvisor
                    )
                }
            }
        }

        // 2. Mis Ahorros (Intocable) Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp)
                .clickable { onNavigateAhorros() },
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceBg,
            border = BorderStroke(1.dp, colors.borderSubtle),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_savings),
                        contentDescription = null,
                        tint = colors.textVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MIS AHORROS (INTOCABLE)",
                        color = colors.textVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (esIncognito) "••••••••" else format.format(totalAhorros),
                    color = colors.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (esIncognito) {
                        "Cuentas: •••• • Alcancía/Efectivo: ••••"
                    } else {
                        "Cuentas: ${format.format(ahorroCuenta)} • Alcancía/Efectivo: ${format.format(ahorroAlcancia)}"
                    },
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // 3. 2x2 Bento Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BentoMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "INGRESOS MES",
                    amountText = if (esIncognito) "••••••" else format.format(ingresosMes),
                    amountColor = colors.primaryEmerald,
                    subtext = if (ingresosMes > 0) "1 depósito activo" else "Sin depósitos aún",
                    iconRes = R.drawable.ic_arrow_downward,
                    iconBg = colors.ghostBg,
                    iconTint = colors.primaryEmerald,
                    onClick = onNavigateMovimientos
                )
                BentoMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "GASTOS MES",
                    amountText = if (esIncognito) "••••••" else format.format(gastosMes),
                    amountColor = colors.textPrimary,
                    subtext = if (countGastosMes == 1) "1 registro activo" else "$countGastosMes registros activos",
                    iconRes = R.drawable.ic_local_fire_department,
                    iconBg = colors.expenseRedBg,
                    iconTint = colors.expenseRed,
                    onClick = onNavigateMovimientos
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BentoMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "GASTOS FIJOS",
                    amountText = if (esIncognito) "••••••" else format.format(totalGastosFijos),
                    amountColor = colors.textPrimary,
                    subtext = "$countGF recurrentes",
                    iconRes = R.drawable.ic_autorenew,
                    iconBg = colors.ghostBg,
                    iconTint = colors.primaryEmerald,
                    onClick = onNavigateGastosFijos
                )
                BentoMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "DEUDAS TOTALES",
                    amountText = if (esIncognito) "••••••" else format.format(totalDeudasRestante),
                    amountColor = colors.textPrimary,
                    subtext = if (countDeudas > 0) "$countDeudas activas" else "Bajo control",
                    iconRes = R.drawable.ic_shield,
                    iconBg = colors.ghostBg,
                    iconTint = colors.primaryEmerald,
                    onClick = onNavigateDeudas
                )
            }
        }

        // 4. Resúmenes Financieros 2x2 Grid
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Resúmenes Financieros",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Toca para ver detalle",
                color = colors.textMuted,
                fontSize = 12.sp
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodoSummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "SEMANAL",
                    balanceText = if (esIncognito) "••••••" else format.format(periodoSemanal.first),
                    trendText = periodoSemanal.second,
                    onClick = { onOpenDetallePeriodo("SEMANAL") }
                )
                PeriodoSummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "MENSUAL",
                    balanceText = if (esIncognito) "••••••" else format.format(periodoMensual.first),
                    trendText = periodoMensual.second,
                    onClick = { onOpenDetallePeriodo("MENSUAL") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodoSummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "SEMESTRAL",
                    balanceText = if (esIncognito) "••••••" else format.format(periodoSemestral.first),
                    trendText = periodoSemestral.second,
                    onClick = { onOpenDetallePeriodo("SEMESTRAL") }
                )
                PeriodoSummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "ANUAL",
                    balanceText = if (esIncognito) "••••••" else format.format(periodoAnual.first),
                    trendText = periodoAnual.second,
                    onClick = { onOpenDetallePeriodo("ANUAL") }
                )
            }
        }
    }
}

@Composable
private fun QuickActionBtn(
    modifier: Modifier = Modifier,
    label: String,
    @DrawableRes iconRes: Int,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    val colors = DabaTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = colors.textPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BentoMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    amountText: String,
    amountColor: Color,
    subtext: String,
    @DrawableRes iconRes: Int,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    val colors = DabaTheme.colors
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = amountText,
                color = amountColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PeriodoSummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    balanceText: String,
    trendText: String,
    onClick: () -> Unit
) {
    val colors = DabaTheme.colors
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = colors.primaryEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = "Ir",
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = balanceText,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = colors.surfaceContainer
            ) {
                Text(
                    text = trendText,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InicioScreenPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        InicioScreen(
            cache = null,
            resumenFinanciero = ResumenFinanciero(
                balanceTotal = 3153541.0,
                totalGastosFijos = 200000.0,
                totalDeudasPendientes = 100000.0,
                totalAhorros = 1250000.0,
                ahorroEnCuenta = 950000.0,
                ahorroEnAlcancia = 300000.0
            ),
            esIncognito = false,
            onToggleIncognito = {},
            onNavigateMovimientos = {},
            onNavigateGastosFijos = {},
            onNavigateDeudas = {},
            onNavigateAhorros = {},
            onOpenGeminiAdvisor = {},
            onOpenDetallePeriodo = {}
        )
    }
}
