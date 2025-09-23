package ioannapergamali.savejoannepink.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.ioannapergamali.mysmartroute.R
import kotlin.math.roundToInt

class GameFragment : Fragment() {

    private lateinit var scoreIndicator: LinearProgressIndicator
    private lateinit var scoreTextView: TextView
    private var score = STARTING_SCORE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = requireContext()

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                24.dp(context),
                24.dp(context),
                24.dp(context),
                24.dp(context),
            )
        }

        val progressTitleView = TextView(context).apply {
            text = getString(R.string.game_progress_title)
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = 8.dp(context)
            }
        }

        scoreIndicator = LinearProgressIndicator(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = 12.dp(context)
            }
            min = MIN_SCORE
            max = MAX_SCORE
            isIndeterminate = false
            trackThickness = 12.dp(context)
            trackCornerRadius = 6.dp(context)
            setTrackColor(ContextCompat.getColor(context, R.color.progress_track))
        }

        scoreTextView = TextView(context).apply {
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = 24.dp(context)
            }
        }

        rootLayout.addView(progressTitleView)
        rootLayout.addView(scoreIndicator)
        rootLayout.addView(scoreTextView)

        val questionTextView = TextView(context).apply {
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = 24.dp(context)
            }
        }

        val optionViews = List(3) {
            TextView(context).apply {
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = 16.dp(context)
                }
                setPadding(
                    16.dp(context),
                    12.dp(context),
                    16.dp(context),
                    12.dp(context),
                )
                isClickable = true
            }
        }

        rootLayout.addView(questionTextView)
        optionViews.forEach(rootLayout::addView)

        val question = Question(
            text = "Ποιο είναι το αποτέλεσμα του 2 + 2;",
            correctAnswer = "4",
            wrongAnswer1 = "3",
            wrongAnswer2 = "5",
        )

        bindQuestion(question, questionTextView, optionViews)

        updateScore(score, animate = false)

        return rootLayout
    }

    fun onWisdomItemTouched() {
        adjustScoreBy(SCORE_STEP)
    }

    fun onDamageItemTouched() {
        adjustScoreBy(-SCORE_STEP)
    }

    fun registerItemCollision(item: GameItem) {
        when (item) {
            GameItem.WISDOM -> onWisdomItemTouched()
            GameItem.DAMAGE -> onDamageItemTouched()
        }
    }

    private fun bindQuestion(
        question: Question,
        questionView: TextView,
        optionViews: List<TextView>,
    ) {
        val (text, correctAnswer, wrongAnswer1, wrongAnswer2) = question

        questionView.text = text
        val shuffledOptions = listOf(correctAnswer, wrongAnswer1, wrongAnswer2).shuffled()

        optionViews.zip(shuffledOptions).forEach { (view, option) ->
            view.text = option
            view.setOnClickListener {
                if (option == correctAnswer) {
                    onWisdomItemTouched()
                } else {
                    onDamageItemTouched()
                }
            }
        }
    }

    private fun adjustScoreBy(delta: Int, animate: Boolean = true) {
        updateScore(score + delta, animate)
    }

    private fun updateScore(newScore: Int, animate: Boolean) {
        score = newScore.coerceIn(MIN_SCORE, MAX_SCORE)
        scoreTextView.text = getString(R.string.game_score_format, score)
        scoreIndicator.setProgressCompat(score, animate)
        updateIndicatorColor()
    }

    private fun updateIndicatorColor() {
        val colorRes = when {
            score >= HIGH_SCORE_THRESHOLD -> R.color.progress_high
            score >= MEDIUM_SCORE_THRESHOLD -> R.color.progress_medium
            else -> R.color.progress_low
        }

        val context = scoreIndicator.context
        scoreIndicator.setIndicatorColor(ContextCompat.getColor(context, colorRes))
    }

    private fun Int.dp(context: Context): Int =
        (this * context.resources.displayMetrics.density).roundToInt()

    private companion object {
        const val STARTING_SCORE = 100
        const val SCORE_STEP = 10
        const val MIN_SCORE = 0
        const val MAX_SCORE = 200
        const val MEDIUM_SCORE_THRESHOLD = 90
        const val HIGH_SCORE_THRESHOLD = 140
    }
}

data class Question(
    val text: String,
    val correctAnswer: String,
    val wrongAnswer1: String,
    val wrongAnswer2: String,
)

enum class GameItem {
    WISDOM,
    DAMAGE,
}
