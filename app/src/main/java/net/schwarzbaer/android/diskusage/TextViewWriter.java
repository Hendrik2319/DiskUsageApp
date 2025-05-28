package net.schwarzbaer.android.diskusage;

import android.widget.TextView;

import java.util.Locale;

public class TextViewWriter {
    private final TextView output;
    private String completeText;

    public TextViewWriter(TextView output) {
        this.output = output;
        completeText = "";
    }

    public void setText(String format, Object... args) {
        completeText = String.format(Locale.ENGLISH, format, args);
        output.setText(completeText);
    }

    public void addLine(String format, Object... args) {
        String line = String.format(Locale.ENGLISH, format, args);
        completeText = String.format("%s%n%s", completeText, line);
        output.setText(completeText);
    }

    public String getCompleteText() {
        return completeText;
    }
}
