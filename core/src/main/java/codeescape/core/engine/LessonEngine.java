package codeescape.core.engine;

import codeescape.core.content.GameConfig;
import codeescape.core.content.QuestionBank;
import codeescape.core.model.GameSession;
import codeescape.core.model.Question;

import java.util.*;

/**
 * Owns lesson/question progression and attempt state for a single run.
 * {@link GameSession} is the single source of truth for attempt state.
 */
public class LessonEngine {
    public enum AnswerResult { CORRECT, WRONG, TIMEOUT_WRONG, LEVEL_COMPLETE, GAME_COMPLETE, GAME_OVER }

    private final Question[][] canonical = QuestionBank.canonical();
    private Question[][] sessionQuestions = new Question[0][];
    private final GameSession session = new GameSession();

    public void startNewLesson() { startNewLesson(0); }

    public void startNewLesson(int savedXpBeforeRun) {
        session.resetForNewLesson(savedXpBeforeRun);
        sessionQuestions = new Question[canonical.length][];
        Random rnd = new Random();
        for (int i = 0; i < canonical.length; i++) {
            List<Question> list = new ArrayList<Question>(Arrays.asList(canonical[i]));
            Collections.shuffle(list, rnd);
            for (int qi = 0; qi < list.size(); qi++) {
                Question original = list.get(qi);
                if (!original.syntax && original.answers != null && original.answers.length > 1) {
                    List<Integer> order = new ArrayList<Integer>();
                    for (int a = 0; a < original.answers.length; a++) order.add(a);
                    Collections.shuffle(order, rnd);
                    String[] shuffledAnswers = new String[original.answers.length];
                    int shuffledCorrect = -1;
                    for (int a = 0; a < order.size(); a++) {
                        int oldIndex = order.get(a);
                        shuffledAnswers[a] = original.answers[oldIndex];
                        if (oldIndex == original.correct) shuffledCorrect = a;
                    }
                    list.set(qi, new Question(original.text, shuffledAnswers, shuffledCorrect, false, original.expected));
                } else {
                    list.set(qi, new Question(original.text,
                            original.answers == null ? new String[0] : original.answers.clone(),
                            original.correct, original.syntax, original.expected));
                }
            }
            sessionQuestions[i] = list.toArray(new Question[0]);
        }
    }

    public GameSession getSession() { return session; }

    public void cancelRun(int savedXp) {
        session.resetAfterCancel(savedXp);
    }

    /** Marks the current run as committed (XP persistence handled by caller/repository). */
    public void markSessionCommitted() {
        session.markCommitted();
    }

    public Question currentQuestion() {
        int li = Math.max(0, Math.min(canonical.length - 1, session.level - 1));
        int qi = Math.max(0, Math.min(GameConfig.QUESTIONS_PER_LEVEL - 1, session.questionIndex));
        Question[][] active = (sessionQuestions != null && sessionQuestions.length == canonical.length) ? sessionQuestions : canonical;
        return active[li][qi];
    }

    public boolean isCurrentSyntax() { return currentQuestion().syntax; }
    public int questionTimeSeconds() { return isCurrentSyntax() ? GameConfig.SYNTAX_SECONDS : GameConfig.MCQ_SECONDS; }

    public AnswerResult submitChoice(int choice) {
        Question q = currentQuestion();
        boolean ok = choice == q.correct;
        if (ok) return onCorrect(); else return onWrong();
    }

    public AnswerResult submitSyntax(String typed) {
        Question q = currentQuestion();
        String t = typed == null ? "" : typed.trim().replaceAll("\\s+", " ");
        String e = q.expected == null ? "" : q.expected.trim().replaceAll("\\s+", " ");
        if (t.equalsIgnoreCase(e)) return onCorrect(); else return onWrong();
    }

    public AnswerResult timeout() { return onWrong(true); }

    private AnswerResult onCorrect() {
        session.xp += GameConfig.XP_PER_CORRECT;
        session.correctAnswers++;
        session.questionIndex++;
        if (session.questionIndex >= GameConfig.QUESTIONS_PER_LEVEL) {
            if (session.level >= GameConfig.LEVEL_COUNT) return AnswerResult.GAME_COMPLETE;
            session.level++;
            session.questionIndex = 0;
            return AnswerResult.LEVEL_COMPLETE;
        }
        return AnswerResult.CORRECT;
    }

    private AnswerResult onWrong() { return onWrong(false); }
    private AnswerResult onWrong(boolean timeout) {
        session.hearts--;
        if (session.hearts <= 0) return AnswerResult.GAME_OVER;
        return timeout ? AnswerResult.TIMEOUT_WRONG : AnswerResult.WRONG;
    }

    public int getLevel() { return session.level; }
    public int getQuestionIndex() { return session.questionIndex; }
    public int getHearts() { return session.hearts; }
    public int getXp() { return session.xp; }
    public int getCorrectAnswers() { return session.correctAnswers; }
    public void setHearts(int h) { session.hearts = h; }
    public void setXp(int x) { session.xp = x; }
    public void setLevel(int l) { session.level = l; }
    public void setQuestionIndex(int q) { session.questionIndex = q; }
    public void setCorrectAnswers(int c) { session.correctAnswers = c; }
}
