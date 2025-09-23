package ioannapergamali.savejoannepink.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class GameFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val questionTextView = TextView(context).apply {
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
        }

        val optionViews = List(3) {
            TextView(context).apply {
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
            }
        }

        rootLayout.addView(questionTextView)
        optionViews.forEach(rootLayout::addView)

        val question = Question(
            text = "Ποιο είναι το αποτέλεσμα του 2 + 2;",
            correctAnswer = "4",
            wrongAnswer1 = "3",
            wrongAnswer2 = "5"
        )

        bindQuestion(question, questionTextView, optionViews)

        return rootLayout
    }

    private fun bindQuestion(
        question: Question,
        questionView: TextView,
        optionViews: List<TextView>
    ) {
        val (text, correctAnswer, wrongAnswer1, wrongAnswer2) = question

        questionView.text = text
        val shuffledOptions = listOf(correctAnswer, wrongAnswer1, wrongAnswer2).shuffled()

        optionViews.zip(shuffledOptions).forEach { (view, option) ->
            view.text = option
        }
    }
}

data class Question(
    val text: String,
    val correctAnswer: String,
    val wrongAnswer1: String,
    val wrongAnswer2: String
)
