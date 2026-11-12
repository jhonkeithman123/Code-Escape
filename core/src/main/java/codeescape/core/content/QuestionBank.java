package codeescape.core.content;

import codeescape.core.model.Question;
import java.util.*;

public final class QuestionBank {
    private QuestionBank() {}

    private static Question q(String text, String a, String b, String c, String d, int correct) {
        return new Question(text, new String[]{a, b, c, d}, correct, false, null);
    }
    private static Question qs(String text, String expected) { return new Question(text, new String[0], -1, true, expected); }

    /**
     * Returns the canonical level/question content as a defensive copy: a new
     * outer array and fresh per-level arrays on every call, safe to hand to
     * consumers that may shuffle or annotate questions in place.
     */
    public static Question[][] all() {
        Question[][] canonical = canonical();
        Question[][] copy = new Question[canonical.length][];
        for (int i = 0; i < canonical.length; i++) {
            copy[i] = canonical[i].clone();
        }
        return copy;
    }

    public static Question[][] canonical() {
        return new Question[][]{
            {
                q("Which HTML tag creates a paragraph?","<p>","<h1>","<br>","<div>",0),
                q("Which HTML tag creates the largest heading?","<h6>","<head>","<h1>","<title>",2),
                q("Which HTML tag displays an image?","<image>","<pic>","<img>","<src>",2),
                qs("Complete the HTML syntax to create a paragraph containing Hello World:","<p>Hello World</p>"),
                qs("Complete the HTML syntax to display an image named photo.jpg:","<img src=\"photo.jpg\">"),
                q("Which attribute provides alternate text for an HTML image?", "href", "alt", "srcset", "class", 1),
                q("Which tag creates an ordered list?", "<ul>", "<ol>", "<li>", "<dl>", 1),
                q("Which HTML attribute links a label to an input id?", "for", "name", "target", "rel", 0),
                qs("Complete the HTML syntax for a line break:", "<br>"),
                qs("Complete the HTML syntax for a link to index.html: <a ______>Home</a>", "href=\"index.html\""),
            },
            {
                q("Which CSS property changes text color?","font-color","text-color","color","foreground",2),
                q("Which selector targets an element by id?",".id","#id","*id","@id",1),
                q("Which CSS property changes the background color?","background-color","bg-color","color-background","background",0),
                qs("Complete the CSS syntax to make paragraph text blue: p { ______ }","color: blue;"),
                qs("Complete the CSS syntax to add a 10px margin: .box { ______: 10px; }","margin"),
                q("Which CSS property rounds an element's corners?", "border-style", "corner-radius", "border-radius", "round", 2),
                q("Which selector targets every paragraph?", "#p", ".p", "p", "@p", 2),
                q("Which CSS property controls space inside an element's border?", "margin", "padding", "gap", "outline", 1),
                qs("Complete the CSS syntax to select an element with class card: ______ { color: white; }", ".card"),
                qs("Complete the CSS declaration for a 2px solid border: .box { ______ }", "border: 2px solid;"),
            },
            {
                q("Which keyword declares a variable that can be reassigned in JavaScript?","const","let","static","fixed",1),
                q("Which symbol is used for strict equality in JavaScript?","=","==","===","!=",2),
                q("Which method prints a message in the browser console?","console.log()","print()","writeLine()","echo()",0),
                qs("Complete the JavaScript syntax to declare score with the value 10:","let score = 10;"),
                qs("Complete the JavaScript condition: if (score ______ 10) { console.log(\"Good\"); }",">="),
                q("Which JavaScript structure repeats code while a condition is true?", "if", "while", "switch", "try", 1),
                q("Which operator adds one to a number?", "--", "**", "++", "==", 2),
                q("Which keyword returns a value from a function?", "give", "send", "return", "yielded", 2),
                qs("Complete the JavaScript statement to show an alert: ______(\"Hello\");", "alert"),
                qs("Complete the function call to print name: console.______(name);", "log"),
            },
            {
                q("What should you check first when a webpage does not display correctly?","Only the volume","The HTML structure and tags","The keyboard","The wallpaper",1),
                q("Which character commonly ends a CSS declaration?",";","?","#","@",0),
                q("If JavaScript says a function is not defined, what should you check?","The screen size","The function name and spelling","The mouse","The image size",1),
                qs("Fix the missing closing brace: function greet() { console.log(\"Hello\"); ______","}"),
                qs("Fix the missing HTML closing tag: <p>Hello World______","</p>"),
                q("What does a syntax error usually mean?", "The code breaks language rules", "The monitor is off", "The file is too colorful", "The mouse is disconnected", 0),
                q("Which browser tool helps inspect console errors?", "Calculator", "Developer Tools", "File Explorer", "Paint", 1),
                q("What is a common cause of a missing-variable error?", "Variable name is misspelled", "Screen brightness", "Image format", "Speaker volume", 0),
                qs("Complete the JavaScript comment for one line: ______ this line", "//"),
                qs("Complete the HTML closing tag for a div: ______", "</div>"),
            },
            {
                q("Which HTML element contains the main visible webpage content?","<head>","<body>","<meta>","<title>",1),
                q("Which CSS property changes the size of text?","text-style","font-size","text-size","font-height",1),
                q("Which JavaScript method prints to the console?","alert()","console.log()","printLine()","display()",1),
                qs("Complete the HTML button syntax: <button ______>Click</button>","type=\"button\""),
                qs("Complete the CSS syntax to add 10px padding: .card { ______: 10px; }","padding"),
                q("Which HTML tag connects an external CSS file?", "<style src>", "<css>", "<link>", "<script>", 2),
                q("Which CSS layout system is useful for arranging items in rows or columns?", "Flexbox", "Console", "HTML", "Prompt", 0),
                q("Which JavaScript keyword checks a condition?", "when", "if", "check", "caseif", 1),
                qs("Complete the HTML script tag opening for app.js: <script ______>", "src=\"app.js\""),
                qs("Complete the CSS syntax to center text: .title { text-align: ______; }", "center"),
            }
        };
    }
}
