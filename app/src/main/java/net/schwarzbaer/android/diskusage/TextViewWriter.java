package net.schwarzbaer.android.diskusage;

import android.widget.TextView;

public class TextViewWriter {
    private final TextView output;
    private String completeText;

    public TextViewWriter(TextView output) {
        this.output = output;
        completeText = "";
    }

    public void setText(String str) {
        completeText = str;
        output.setText(completeText);
    }

    public void addLine(String line) {
        completeText = String.format("%s%n%s", completeText, line);
        output.setText(completeText);
    }

    public String getCompleteText() {
        return completeText;
    }
}
