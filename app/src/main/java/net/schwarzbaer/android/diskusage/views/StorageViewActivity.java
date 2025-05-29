package net.schwarzbaer.android.diskusage.views;

import android.content.Context;
import android.content.Intent;
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
import net.schwarzbaer.android.diskusage.databinding.ActivityStorageViewBinding;
import net.schwarzbaer.android.diskusage.models.FileCategory;
import net.schwarzbaer.android.diskusage.models.FileSystemScanner;
import net.schwarzbaer.android.diskusage.models.Storage;

import java.util.Locale;

public class StorageViewActivity extends AppCompatActivity
{
    public static final String activityParam_StorageIndex = "StorageIndex";

    private ActivityStorageViewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStorageViewBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        int storageIndex = getIntent().getIntExtra(activityParam_StorageIndex, 0);
        Storage storage = FileSystemScanner.getInstance().getStorage(storageIndex);

        binding.txtStorageViewOutput.setText(String.format("Storage: %s", storage == null ? "<no storage>" : storage.getPath()));

        binding.listFileCategories.setLayoutManager(new LinearLayoutManager(this));
        if (storage != null)
            binding.listFileCategories.setAdapter(new MyAdapter(this, storage, storageIndex));
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
        private final int storageIndex;

        private MyAdapter(Context context, @NonNull Storage storage, int storageIndex) {
            this.context = context;
            this.storage = storage;
            this.storageIndex = storageIndex;
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
            holder.txtSize.setText(fileSize_Byte == 0 ? "----" : Storage.getFormatedSize(fileSize_Byte));

            if (fileCount==0)
                holder.txtMarker.setText(" X ");
            else {
                holder.txtMarker.setText(" >>");
                holder.itemView.setOnClickListener(view -> {
                    Intent intent = new Intent(context, FolderViewActivity.class);
                    intent.putExtra(FolderViewActivity.activityParam_StorageIndex, storageIndex);
                    intent.putExtra(FolderViewActivity.activityParam_FileCategory, fileCat.name());
                    intent.putExtra(FolderViewActivity.activityParam_FolderID, Storage.FolderID_Root);
                    context.startActivity(intent);
                });
            }
        }
    }
}