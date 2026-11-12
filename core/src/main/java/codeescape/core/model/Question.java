package codeescape.core.model;

public class Question {
    public String text;
    public String[] answers;
    public int correct;
    public boolean syntax;
    public String expected;

    public Question(String text, String[] answers, int correct, boolean syntax, String expected) {
        this.text = text;
        this.answers = answers;
        this.correct = correct;
        this.syntax = syntax;
        this.expected = expected;
    }
}
