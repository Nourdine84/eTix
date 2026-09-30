package com.etix.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.data.BudgetStore
import com.etix.databinding.FragmentHomeV2Binding
import com.etix.databinding.ItemHomeBudgetLineBinding
import com.etix.features.budget.BudgetRules
import com.etix.features.budget.BudgetSummaryEngine
import com.etix.features.budget.HomeBudgetState
import com.etix.features.budget.HomeBudgetSummary
import com.etix.features.home.FinancialStateEngine
import com.etix.features.home.FinancialTone
import com.etix.features.home.HomeCopy
import com.etix.features.home.HomeSnapshot
import com.etix.features.home.MonthlyTrendPoint
import com.etix.features.home.TrendEngine
import com.etix.features.store.TimeRange
import com.etix.model.Ticket
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

/**
 * Accueil — référence iOS HomeView (feature/home-hero-v2). Conformité aux maquettes validées
 * non confirmée. Logique portée dans features/home/HomeStats.kt.
 */
class HomeFragmentV2 : Fragment() {

    private var _binding: FragmentHomeV2Binding? = null
    private val binding get() = _binding!!

    private val range = MutableStateFlow(TimeRange.DEFAULT)

    private data class HomeUi(
        val snap: HomeSnapshot,
        val trend: List<MonthlyTrendPoint>,
        /** Lot 8 : carte Budget (mois courant, null sans budget). */
        val budget: HomeBudgetSummary?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(KEY_RANGE)
            ?.let { runCatching { TimeRange.valueOf(it) }.getOrNull() }
            ?.let { range.value = it }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val main = activity as? MainActivityV2

        // iOS : RadialGradient bleu 18 % → transparent, rayon 180 pt, statique
        binding.heroGlow.background = android.graphics.drawable.GradientDrawable().apply {
            gradientType = android.graphics.drawable.GradientDrawable.RADIAL_GRADIENT
            colors = intArrayOf(ContextCompat.getColor(requireContext(), R.color.v2_primary_glow), 0x00007BFF)
            gradientRadius = 180f * resources.displayMetrics.density
        }
        // Rayon borné à la zone : sinon bords nets visibles sur écran étroit (constaté API 21, 320 dp)
        binding.heroGlow.post {
            val g = binding.heroGlow
            (g.background as? android.graphics.drawable.GradientDrawable)?.gradientRadius =
                minOf(180f * resources.displayMetrics.density, minOf(g.width, g.height) / 2f).coerceAtLeast(1f)
        }

        binding.btnSettings.setOnClickListener { main?.openSettings() }
        binding.btnAddTicket.setOnClickListener { main?.goToPage(MainActivityV2.PAGE_ADD) }
        binding.btnHistory.setOnClickListener { main?.goToPage(MainActivityV2.PAGE_HISTORY) }
        // btnScanTicket : désactivé dans le layout (scanner non branché sur Android)

        binding.homeTogglePeriod.check(buttonFor(range.value))
        binding.homeTogglePeriod.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            range.value = when (id) {
                R.id.btnHomeToday -> TimeRange.TODAY
                R.id.btnHomeYear -> TimeRange.YEAR
                else -> TimeRange.MONTH
            }
        }

        val repository = TicketRepository(AppDatabase.getInstance(requireContext()).ticketDao())
        val budgetStore = BudgetStore(requireContext())
        val ui = combine(repository.getAllFlow(), range, BudgetStore.version) { tickets: List<Ticket>, r, _ ->
            // iOS HomeView : BudgetSummaryEngine seulement sur « Ce mois »
            val budget = if (r == TimeRange.MONTH) BudgetSummaryEngine.compute(tickets, budgetStore.load()) else null
            HomeUi(HomeSnapshot.of(tickets, r), TrendEngine.monthlyTrend(tickets), budget)
        }.flowOn(Dispatchers.Default)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ui.collect { render(it) }
            }
        }
    }

    private fun render(ui: HomeUi) {
        val ctx = requireContext()
        val r = range.value
        val snap = ui.snap
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        binding.tvGreeting.text = HomeCopy.greeting(hour)
        binding.tvWish.text = HomeCopy.wish(hour)
        binding.tvTicketCount.text = HomeCopy.countLabel(snap.allTimeTicketCount)

        binding.tvHeroLabel.text = HomeCopy.heroLabel(r)
        binding.tvHeroAmount.text = euro(snap.periodTotal)

        // Chip delta toujours présent : hausse = rouge, baisse = vert, sinon neutre (iOS)
        val delta = snap.deltaPercent
        val (chipText, fg, bg) = if (delta != null) {
            val up = delta >= 0
            Triple(
                String.format(Locale.FRANCE, "%s %+.1f %%  %s", if (up) "↗" else "↘", delta, HomeCopy.deltaLabel(r)),
                if (up) R.color.v2_negative else R.color.v2_positive,
                if (up) R.color.v2_negative_12 else R.color.v2_positive_12
            )
        } else {
            Triple("—  Pas de comparaison", R.color.v2_text_secondary, R.color.v2_neutral_12)
        }
        binding.tvDeltaChip.text = chipText
        binding.tvDeltaChip.setTextColor(ContextCompat.getColor(ctx, fg))
        binding.tvDeltaChip.backgroundTintList = ContextCompat.getColorStateList(ctx, bg)

        // iOS : budgetTense = carte Budget critique ou dépassée → phrase « Ton rythme de dépenses augmente »
        val state = FinancialStateEngine.evaluate(snap, budgetTense = ui.budget?.isTense == true)
        binding.tvNarration.text = HomeCopy.narration(state, r)
        binding.tvNarration.setTextColor(ContextCompat.getColor(ctx, when (state.tone) {
            FinancialTone.POSITIVE -> R.color.v2_positive
            FinancialTone.NEUTRAL -> R.color.v2_text_secondary
            FinancialTone.ATTENTION -> R.color.v2_attention
        }))
        binding.heroContent.contentDescription =
            "${HomeCopy.heroLabel(r)} : ${euro(snap.periodTotal)}. ${chipText.drop(2)}. ${binding.tvNarration.text}"

        renderBudget(ui.budget)

        binding.cardTrend.visibility = if (snap.allTimeTicketCount > 0) View.VISIBLE else View.GONE
        renderTrend(ui.trend)
        binding.tvAverageBasket.text = euro(snap.averageBasket)
    }

    /** iOS BudgetSummaryCardView : titre, « X dépensés sur Y prévus », barre globale, jours restants, 3 lignes max. */
    private fun renderBudget(s: HomeBudgetSummary?) {
        val b = binding
        if (s == null) { b.cardBudget.visibility = View.GONE; return }
        val ctx = requireContext()
        b.cardBudget.visibility = View.VISIBLE
        fun color(st: HomeBudgetState) = ContextCompat.getColor(ctx, when (st) {
            HomeBudgetState.COMFORTABLE -> R.color.v2_primary
            HomeBudgetState.CAUTION -> R.color.v2_attention
            HomeBudgetState.CRITICAL, HomeBudgetState.EXCEEDED -> R.color.v2_negative
        })
        b.cardBudget.setCardBackgroundColor(ContextCompat.getColor(ctx, when (s.state) {
            HomeBudgetState.COMFORTABLE -> R.color.v2_surface_home
            HomeBudgetState.CAUTION -> R.color.v2_attention_08
            HomeBudgetState.CRITICAL, HomeBudgetState.EXCEEDED -> R.color.v2_negative_08
        }))
        b.tvBudgetHeadline.text = BudgetSummaryEngine.headline(s)
        b.tvBudgetHeadline.setTextColor(if (s.state == HomeBudgetState.EXCEEDED) color(s.state)
            else ContextCompat.getColor(ctx, R.color.v2_text_primary))
        b.tvBudgetCaption.text = BudgetSummaryEngine.caption(s)
        b.budgetGlobalBar.setIndicatorColor(color(s.state))
        b.budgetGlobalBar.progress = (s.globalRatio.coerceIn(0.0, 1.0) * 1000).toInt()
        b.tvBudgetPercent.text = BudgetSummaryEngine.percent(s.globalRatio)
        b.tvBudgetPercent.setTextColor(color(s.state))
        b.tvBudgetDaysLeft.text = BudgetSummaryEngine.daysLeft(s.daysLeftInMonth)

        b.budgetDivider.visibility = if (s.lines.isEmpty()) View.GONE else View.VISIBLE
        b.budgetLines.removeAllViews()
        val inf = LayoutInflater.from(ctx)
        for (line in s.lines) {
            val row = ItemHomeBudgetLineBinding.inflate(inf, b.budgetLines, true)
            row.tvLineName.text = line.categoryName
            row.lineBar.setIndicatorColor(color(line.state))
            row.lineBar.progress = (line.ratio.coerceIn(0.0, 1.0) * 1000).toInt()
            row.tvLinePercent.text = BudgetSummaryEngine.percent(line.ratio)
            row.tvLinePercent.setTextColor(color(line.state))
            row.root.contentDescription = "${line.categoryName} : ${BudgetRules.formatEuro(line.spent)} sur " +
                "${BudgetRules.formatEuro(line.limit)}, ${BudgetSummaryEngine.percent(line.ratio).replace("%", " pour cent")}"
        }
        b.tvBudgetMore.visibility = if (s.extraCount > 0) View.VISIBLE else View.GONE
        b.tvBudgetMore.text = BudgetSummaryEngine.more(s.extraCount)
        b.budgetCardContent.contentDescription = "Budgets du mois. ${b.tvBudgetHeadline.text}. ${b.tvBudgetCaption.text}. " +
            "${BudgetSummaryEngine.percent(s.globalRatio).replace("%", " pour cent")}. ${b.tvBudgetDaysLeft.text}."
    }

    /** iOS trendBars : barre du mois courant en bleu, autres à 30 %, hauteur min 6, max 70. */
    private fun renderTrend(points: List<MonthlyTrendPoint>) {
        val ctx = requireContext()
        val container = binding.trendBars
        container.removeAllViews()
        val maxValue = max(points.maxOfOrNull { it.total } ?: 1.0, 1.0)
        val density = resources.displayMetrics.density
        points.forEachIndexed { i, p ->
            val column = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (i > 0) marginStart = (8 * density).toInt()
                }
            }
            val bar = View(ctx).apply {
                setBackgroundResource(R.drawable.bg_v2_bar)
                backgroundTintList = ContextCompat.getColorStateList(
                    ctx, if (i == points.lastIndex) R.color.v2_primary else R.color.v2_primary_30
                )
                val h = max(6.0, p.total / maxValue * 70).toFloat() * density
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h.toInt())
            }
            val label = TextView(ctx).apply {
                text = p.month
                textSize = 9f
                setTextColor(ContextCompat.getColor(ctx, R.color.v2_text_secondary))
                gravity = android.view.Gravity.CENTER
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = (6 * density).toInt() }
            }
            column.addView(bar)
            column.addView(label)
            column.contentDescription = "${p.month} : ${euro(p.total)}"
            container.addView(column)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_RANGE, range.value.name)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buttonFor(r: TimeRange) = when (r) {
        TimeRange.TODAY -> R.id.btnHomeToday
        TimeRange.MONTH -> R.id.btnHomeMonth
        TimeRange.YEAR -> R.id.btnHomeYear
    }

    private fun euro(v: Double) = String.format(Locale.FRANCE, "%.2f €", v)

    companion object {
        private const val KEY_RANGE = "home_range"
    }
}
