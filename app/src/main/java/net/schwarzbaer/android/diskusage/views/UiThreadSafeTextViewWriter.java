package net.schwarzbaer.android.diskusage.views;

import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.function.Consumer;

public class UiThreadSafeTextViewWriter
{
    private final TextView output;
    private final Consumer<Runnable> runOnUiThread;
    private String completeText;

    public UiThreadSafeTextViewWriter(@NonNull TextView output, @NonNull Consumer<Runnable> runOnUiThread) {
        this.output = output;
        this.runOnUiThread = runOnUiThread;
        completeText = "";
    }

    public void setText(String format, Object... args) {
        completeText = String.format(Locale.ENGLISH, format, args);
        runOnUiThread.accept(() -> {
            output.setText(completeText);
        });
    }

    public void addLine(String format, Object... args) {
        String line = String.format(Locale.ENGLISH, format, args);
        completeText = String.format("%s%n%s", completeText, line);
        runOnUiThread.accept(() -> {
            output.setText(completeText);
        });
    }

    public String getCompleteText() {
        return completeText;
    }
}
