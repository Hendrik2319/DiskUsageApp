package net.schwarzbaer.android.diskusage.views;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.schwarzbaer.android.diskusage.R;
import net.schwarzbaer.android.diskusage.models.FileCategory;
import net.schwarzbaer.android.diskusage.models.FileSystemScanner;
import net.schwarzbaer.android.diskusage.models.Storage;

import java.util.Locale;

public class StorageViewActivity extends AppCompatActivity
{
    public static String activityParam_StorageIndex = "StorageIndex";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_storage_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        int storageIndex = getIntent().getIntExtra(activityParam_StorageIndex, 0);
        Storage storage = FileSystemScanner.getInstance().getStorage(storageIndex);

        TextView txtStorageViewOutput = findViewById(R.id.txtStorageViewOutput);
        txtStorageViewOutput.setText(String.format("Storage: %s", storage == null ? "<no storage>" : storage.getPath()));

        RecyclerView recyclerView = findViewById(R.id.listFileCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (storage != null)
            recyclerView.setAdapter(new MyAdapter(this, storage));
    }

    public void clickBackBtn(View view) {
        finish();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView txtTitle;
        private final TextView txtFiles;
        private final TextView txtMarker;
        private final TextView txtSize;

        private MyViewHolder(View itemView) {
            super(itemView);
            txtTitle  = itemView.findViewById(R.id.txtFileCategoryTitle);
            txtFiles  = itemView.findViewById(R.id.txtFileCategoryFiles);
            txtSize   = itemView.findViewById(R.id.txtFileCategorySize);
            txtMarker = itemView.findViewById(R.id.txtFileCategoryMarker);
        }
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyViewHolder>
    {
        private final static FileCategory[] categories = FileCategory.values();
        private final Context context;
        private final Storage storage;

        private MyAdapter(Context context, @NonNull Storage storage) {
            this.context = context;
            this.storage = storage;
        }

        @Override
        public int getItemCount() {
            return categories.length;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_storage_view, parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            FileCategory fileCat = categories[position];
            holder.txtTitle.setText(fileCat.label);

            long fileCount = storage.getFileCount(fileCat);
            holder.txtFiles.setText(fileCount == 0 ? "no files" : String.format(Locale.ENGLISH, "%d files", fileCount));

            long fileSize_Byte = storage.getFileSize_Byte(fileCat);
            holder.txtSize.setText(fileSize_Byte == 0 ? "----" : getFormatedSize(fileSize_Byte));

            if (fileCount==0)
                holder.txtMarker.setText(" X ");
            else {
                holder.txtMarker.setText(" >>");
                holder.itemView.setOnClickListener(view -> {
                    // TODO: FolderViewActivity
//                    Intent intent = new Intent(context, StorageViewActivity.class);
//                    intent.putExtra(StorageViewActivity.activityParam_StorageIndex, position);
//                    context.startActivity(intent);
                });
            }
        }

        private static String getFormatedSize(long size_Byte)
        {
            double value = size_Byte;
            if (value < 1200) return String.format(Locale.ENGLISH, "%d B", size_Byte);

            value /= 1024;
            if (value < 10  ) return String.format(Locale.ENGLISH, "%1.2f kB", value);
            if (value < 100 ) return String.format(Locale.ENGLISH, "%1.1f kB", value);
            if (value < 1000) return String.format(Locale.ENGLISH, "%1.0f kB", value);

            value /= 1024;
            if (value < 10  ) return String.format(Locale.ENGLISH, "%1.2f MB", value);
            if (value < 100 ) return String.format(Locale.ENGLISH, "%1.1f MB", value);
            if (value < 1000) return String.format(Locale.ENGLISH, "%1.0f MB", value);

            value /= 1024;
            if (value < 10  ) return String.format(Locale.ENGLISH, "%1.2f GB", value);
            if (value < 100 ) return String.format(Locale.ENGLISH, "%1.1f GB", value);
            if (value < 1000) return String.format(Locale.ENGLISH, "%1.0f GB", value);

            value /= 1024;
            if (value < 10  ) return String.format(Locale.ENGLISH, "%1.2f TB", value);
            if (value < 100 ) return String.format(Locale.ENGLISH, "%1.1f TB", value);
            return                   String.format(Locale.ENGLISH, "%1.0f TB", value);
        }
    }
}