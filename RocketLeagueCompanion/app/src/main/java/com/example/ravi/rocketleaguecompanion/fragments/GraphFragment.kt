package com.example.ravi.rocketleaguecompanion.fragments


import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.ravi.rocketleaguecompanion.custom.Timestamp
import com.example.ravi.rocketleaguecompanion.R
import com.example.ravi.rocketleaguecompanion.custom.DateAxisFormatter
import com.example.ravi.rocketleaguecompanion.custom.Player
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import kotlinx.android.synthetic.main.fragment_graph.*
import org.jetbrains.anko.textColor
import kotlin.math.sign

class GraphFragment : Fragment() {

    private val currentShotPercColor = "#5Fa161"
    private val totalShotPercColor = "#F42A3B"
    private val duelGraphColor = "#FFFF00"
    private val duoGraphColor = "#5Fa1FF"
    private val standardGraphColor = "#F0a161"
    private val soloGraphColor = "#5F0061"
    private lateinit var totalShotPercentageDataSet: LineDataSet
    private lateinit var currentShotPercentageDataSet: LineDataSet
    private lateinit var skillDataSetList: Array<LineDataSet>
    private lateinit var balanceDataSet: PieDataSet
    private var initStart = true
    private lateinit var player: Player


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_graph, container, false)
    }

    companion object {
        fun newInstance(): GraphFragment = GraphFragment()
    }

    /**
     * puts rolling shot percentage on the line chart
     */
    @SuppressLint("SimpleDateFormat")
    //Warning suggests to use local time instead, this could cause problem when a users locale time changes to yesterday when travelling
    private fun addTotalShotPercentage(recentStamp: Timestamp) {
        if (player.season.size == totalShotPercentageDataSet.entryCount)
        //if last entry was on the same day, delete it
            totalShotPercentageDataSet.removeLast()

        totalShotPercentageDataSet.addEntry(
                Entry(recentStamp.time.toFloat(),
                        recentStamp.getTotalShotPercentage()
                ))
    }

    /**
     * puts daily shot percentage on the line chart
     */
    @SuppressLint("SimpleDateFormat")
    //Warning suggests to use local time instead, this could cause problem when a users locale time changes to yesterday when travelling
    private fun addCurrentShotPercentage(recentStamp: Timestamp, oldStamp: Timestamp) {
        if (player.season.size == currentShotPercentageDataSet.entryCount)
        //if last entry was on the same day, delete it
            currentShotPercentageDataSet.removeLast()

        if (recentStamp.getShotPerc(oldStamp) >= 0) {
            currentShotPercentageDataSet.addEntry(
                    Entry(recentStamp.time.toFloat(),
                            recentStamp.getShotPerc(oldStamp)
                    ))
        } else {
            currentShotPercentageDataSet.addEntry(
                    Entry(recentStamp.time.toFloat(),
                            0f
                    ))
        }
    }

    /**
     *updates daily textfields with the current daily stats
     */
    @SuppressLint("SetTextI18n")   // NUMBER% doesn't need to be extracted
    private fun updateDailyCard(recentStamp: Timestamp, oldStamp: Timestamp) {
        val rankingDiff = recentStamp.getRankingDifferential(oldStamp)
        dailyShotPercTxt.text = "${recentStamp.getShotPerc(oldStamp).toInt()}%"
        dailyGoalsTxt.text = "${recentStamp.goals - oldStamp.goals}"
        dailyShotsTxt.text = "${recentStamp.shots - oldStamp.shots}"

        //update difference of the fields with +-DIFF and recolor it in red(-) or green(+) or black(0)
        when {
            rankingDiff[0] < 0 -> {
                dailyDuelTxt.text = "${rankingDiff[0]}"
                dailyDuelTxt.textColor = ColorTemplate.rgb("#AA0000")
            }
            rankingDiff[0] > 0 -> {
                dailyDuelTxt.text = "+ ${rankingDiff[0]}"
                dailyDuelTxt.textColor = ColorTemplate.rgb("#008800")
            }
            else -> {
                dailyDuelTxt.text = "${rankingDiff[0]}"
                dailyDuelTxt.textColor = ColorTemplate.rgb("#00005f")
            }
        }

        when {
            rankingDiff[1] < 0 -> {
                dailyDuoTxt.text = "${rankingDiff[1]}"
                dailyDuoTxt.textColor = ColorTemplate.rgb("#AA0000")
            }
            rankingDiff[1] > 0 -> {
                dailyDuoTxt.text = "+ ${rankingDiff[1]}"
                dailyDuoTxt.textColor = ColorTemplate.rgb("#008800")
            }
            else -> {
                dailyDuoTxt.text = "${rankingDiff[1]}"
                dailyDuelTxt.textColor = ColorTemplate.rgb("#00005f")
            }
        }

        when {
            rankingDiff[2] < 0 -> {
                dailySoloTxt.text = "${rankingDiff[2]}"
                dailySoloTxt.textColor = ColorTemplate.rgb("#AA0000")
            }
            rankingDiff[2] > 0 -> {
                dailySoloTxt.text = "+ ${rankingDiff[2]}"
                dailySoloTxt.textColor = ColorTemplate.rgb("#008800")
            }
            else -> {
                dailySoloTxt.text = "${rankingDiff[2]}"
                dailySoloTxt.textColor = ColorTemplate.rgb("#00005f")
            }
        }

        when {
            rankingDiff[3] < 0 -> {
                dailyStandardTxt.text = "${rankingDiff[3]}"
                dailyStandardTxt.textColor = ColorTemplate.rgb("#AA0000")
            }
            rankingDiff[3] > 0 -> {
                dailyStandardTxt.text = "+ ${rankingDiff[3]}"
                dailyStandardTxt.textColor = ColorTemplate.rgb("#008800")
            }
            else -> {
                dailyStandardTxt.text = "${rankingDiff[3]}"
                dailyStandardTxt.textColor = ColorTemplate.rgb("#00005f")
            }
        }
    }

    /**
     * puts mmr on the line chart
     */
    @SuppressLint("SimpleDateFormat")
    //Warning suggests to use local time instead, this could cause problem when a users locale time changes to yesterday when travelling
    private fun addMmr(recentStamp: Timestamp) {
        for (i in recentStamp.rankingList.indices) {
            if (player.season.size == skillDataSetList[i].entryCount)
            //if last entry was on the same day, delete it
                skillDataSetList[i].removeLast()

            skillDataSetList[i].addEntry(
                    Entry(recentStamp.time.toFloat(),
                            recentStamp.rankingList[i].mmr.toFloat()
                    ))
        }
    }

    /**
     * puts all relevant data to the line charts
     */
    fun addChartEntries(player: Player, recentStamp: Timestamp, oldStamp: Timestamp) {
        this.player = player
        //recalculate Axis scale
        shotpercChart.xAxis.axisMaximum = recentStamp.time.toFloat() + (86400000L * 1L).toFloat()
        skillChart.xAxis.axisMaximum = recentStamp.time.toFloat() + (86400000L * 1L).toFloat()
        skillChart.axisLeft.axisMinimum = minOf(minOf(recentStamp.rankingList[0].mmr, recentStamp.rankingList[1].mmr, recentStamp.rankingList[2].mmr), recentStamp.rankingList[3].mmr).toFloat() - 200
        skillChart.axisLeft.axisMaximum = maxOf(maxOf(recentStamp.rankingList[0].mmr, recentStamp.rankingList[1].mmr, recentStamp.rankingList[2].mmr), recentStamp.rankingList[3].mmr).toFloat() + 200
        //init if necessary, or just fill in data
        if (initStart) {
            initCharts(recentStamp)
            initStart = false
        } else {
            addTotalShotPercentage(recentStamp)
            addCurrentShotPercentage(recentStamp, oldStamp)
            addMmr(recentStamp)
            updateDailyCard(recentStamp, oldStamp)
        }
        //refresh chart to draw updates
        shotpercChart.notifyDataSetChanged()
        shotpercChart.invalidate()
        balanceChart.notifyDataSetChanged()
        balanceChart.invalidate()
        skillChart.notifyDataSetChanged()
        skillChart.invalidate()
    }

    /**
     * initializes all charts and puts the first stamp on it
     */
    private fun initCharts(recentStamp: Timestamp) {
        //initalize shotPercChart
        //create each graph and set colors
        val x = recentStamp.time.toFloat()
        val xAxisStart = x - (86400000L * 1L).toFloat()
        val xAxisEnd = x + (86400000L * 1L).toFloat()


        totalShotPercentageDataSet = LineDataSet(arrayListOf(Entry(
                x, recentStamp.getTotalShotPercentage())), "Total Shot Percentage")
        totalShotPercentageDataSet.color = ColorTemplate.rgb(totalShotPercColor)
        totalShotPercentageDataSet.setCircleColor(ColorTemplate.rgb(totalShotPercColor))

        currentShotPercentageDataSet = LineDataSet(arrayListOf(Entry(
                x, 0f)), "Current Shot Percentage")
        currentShotPercentageDataSet.color = ColorTemplate.rgb(currentShotPercColor)
        currentShotPercentageDataSet.setCircleColor(ColorTemplate.rgb(currentShotPercColor))


        totalShotPercentageDataSet.valueFormatter = PercentFormatter()

        currentShotPercentageDataSet.valueFormatter = PercentFormatter()

        //add graphs to the chart
        shotpercChart.data = LineData()
        shotpercChart.data.addDataSet(totalShotPercentageDataSet)
        shotpercChart.data.addDataSet(currentShotPercentageDataSet)

        //alter axises
        shotpercChart.xAxis.axisMinimum = xAxisStart
        shotpercChart.xAxis.setDrawAxisLine(true)
        shotpercChart.xAxis.setDrawLabels(true)
        shotpercChart.xAxis.axisMaximum = xAxisEnd
        shotpercChart.xAxis.valueFormatter = DateAxisFormatter()

        shotpercChart.axisRight.setDrawLabels(false)
        shotpercChart.axisRight.setDrawGridLines(false)

        shotpercChart.axisLeft.axisMinimum = 0f
        shotpercChart.axisLeft.axisMaximum = 100f
        shotpercChart.axisLeft.valueFormatter = PercentFormatter()

        shotpercChart.description.text = ""

        //initalize balance chart
        val sum = (recentStamp.goals + recentStamp.saves + recentStamp.assists).toFloat()
        //balanceChart.centerText = "Playstyle"
        balanceDataSet = PieDataSet(arrayListOf(PieEntry(recentStamp.goals / sum, "Goal%"),
                PieEntry(recentStamp.saves / sum, "Save%"),
                PieEntry(recentStamp.assists / sum, "Assist%")
        ), "")
        balanceChart.setUsePercentValues(true)
        balanceDataSet.colors = arrayListOf(ColorTemplate.rgb("#0000AA"), ColorTemplate.rgb("#00AA00"), ColorTemplate.rgb("#AA0000"))
        balanceChart.data = PieData(balanceDataSet)
        balanceChart.holeRadius = 0f
        balanceChart.setTransparentCircleAlpha(0)
        balanceChart.description.text = ""
        balanceDataSet.valueFormatter = PercentFormatter()

        balanceChart.legend.isEnabled = false
        balanceDataSet.valueTextColor = ColorTemplate.rgb("#FFFFFF")
        balanceDataSet.valueTextSize = 13f


        //initalize ranking chart

        //init mmr datasets
        val duelDataSet = LineDataSet(arrayListOf(Entry(
                x, recentStamp.rankingList[0].mmr.toFloat()
        )), "Duel MMR")
        val duoDataSet = LineDataSet(arrayListOf(Entry(
                x, recentStamp.rankingList[1].mmr.toFloat()
        )), "Duo MMR")
        val standardDataSet = LineDataSet(arrayListOf(Entry(
                x, recentStamp.rankingList[2].mmr.toFloat()
        )), "Standard MMR")
        val soloDataSet = LineDataSet(arrayListOf(Entry(
                x, recentStamp.rankingList[3].mmr.toFloat()
        )), "Solo MMR")

        //set colors
        duelDataSet.color = ColorTemplate.rgb(duelGraphColor)
        duoDataSet.color = ColorTemplate.rgb(duoGraphColor)
        standardDataSet.color = ColorTemplate.rgb(standardGraphColor)
        soloDataSet.color = ColorTemplate.rgb(soloGraphColor)
        duelDataSet.setCircleColor(ColorTemplate.rgb(duelGraphColor))
        duoDataSet.setCircleColor(ColorTemplate.rgb(duoGraphColor))
        standardDataSet.setCircleColor(ColorTemplate.rgb(standardGraphColor))
        soloDataSet.setCircleColor(ColorTemplate.rgb(soloGraphColor))

        //set axis lable
        skillChart.xAxis.valueFormatter = DateAxisFormatter()

        skillDataSetList = arrayOf(duelDataSet, duoDataSet, standardDataSet, soloDataSet)

        //set skilChart data
        skillChart.data = LineData()
        skillChart.data.addDataSet(duelDataSet)
        skillChart.data.addDataSet(duoDataSet)
        skillChart.data.addDataSet(standardDataSet)
        skillChart.data.addDataSet(soloDataSet)

        //alter skillChart axis
        skillChart.xAxis.axisMinimum = xAxisStart
        skillChart.xAxis.setDrawAxisLine(true)
        skillChart.xAxis.setDrawLabels(true)
        skillChart.xAxis.axisMaximum = xAxisEnd

        skillChart.axisRight.setDrawLabels(false)
        skillChart.axisRight.setDrawGridLines(false)

        skillChart.axisLeft.axisMinimum = minOf(minOf(recentStamp.rankingList[0].mmr, recentStamp.rankingList[1].mmr, recentStamp.rankingList[2].mmr), recentStamp.rankingList[3].mmr).toFloat() - 200
        skillChart.axisLeft.axisMaximum = maxOf(maxOf(recentStamp.rankingList[0].mmr, recentStamp.rankingList[1].mmr, recentStamp.rankingList[2].mmr), recentStamp.rankingList[3].mmr).toFloat() + 200

        skillChart.description.text = ""

    }
}
