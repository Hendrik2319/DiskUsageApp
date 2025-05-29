package net.schwarzbaer.android.diskusage.views;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import net.schwarzbaer.android.diskusage.databinding.ActivityLocalFileListViewBinding;
import net.schwarzbaer.android.diskusage.models.FileCategory;
import net.schwarzbaer.android.diskusage.models.FileSystemScanner;
import net.schwarzbaer.android.diskusage.models.Storage;

import java.io.File;
import java.util.List;

public class LocalFileListViewActivity extends AppCompatActivity
{

    public static final String activityParam_StorageIndex = "StorageIndex";
    public static final String activityParam_FileCategory = "FileCategory";
    public static final String activityParam_FolderID = "FolderID";
    public static final String activityParam_SortOrder = "SortOrder";

    private ActivityLocalFileListViewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityLocalFileListViewBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        final int storageIndex = intent.getIntExtra(activityParam_StorageIndex, 0);
        final FileCategory fileCat = FileCategory.valueOf_checked(intent.getStringExtra(activityParam_FileCategory));
        final int folderID = intent.getIntExtra(activityParam_FolderID, Storage.FolderID_Root);
        final String defaultSortOrderStr = intent.getStringExtra(activityParam_SortOrder);
        Storage.SortOrder defaultSortOrder = Storage.SortOrder.valueOf_checked(defaultSortOrderStr);
        if (defaultSortOrder==null) defaultSortOrder = Storage.SortOrder.Original;

        Storage storage = FileSystemScanner.getInstance().getStorage(storageIndex);
        Storage.ScannedFolder scannedFolder =
                storage==null || fileCat==null
                    ? null
                    : folderID == Storage.FolderID_Root
                        ? storage.getRootFolder(fileCat)
                        : storage.getFolder(folderID);

        binding.txtViewOutput.setText(String.format("Local Files in Folder: %s", scannedFolder == null ? "<no folder>" : scannedFolder.getPath()));

        binding.listFiles.setLayoutManager(new LinearLayoutManager(this));

        ArrayAdapter<Storage.SortOrder> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Storage.SortOrder.values());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spnSelectOrder.setAdapter(spinnerAdapter);

        if (scannedFolder != null) {
            MyAdapter listAdapter = new MyAdapter(this, storageIndex, fileCat, scannedFolder, defaultSortOrder);
            binding.listFiles.setAdapter(listAdapter);
            binding.spnSelectOrder.setSelection( spinnerAdapter.getPosition(defaultSortOrder));
            binding.spnSelectOrder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    listAdapter.setOrder(spinnerAdapter.getItem(position));
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent)
                {
                    listAdapter.setOrder(Storage.SortOrder.Original);
                }
            });
        }
        else
            binding.spnSelectOrder.setEnabled(false);
    }

    public void clickBackBtn(View view) {
        finish();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView txtName;
        private final TextView txtSize;

        private MyViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtFileName);
            txtSize = itemView.findViewById(R.id.txtFileSize);
        }
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyViewHolder>
    {
        private final Context context;
        private final int storageIndex;
        @NonNull
        private final FileCategory fileCat;
        @NonNull
        private final Storage.ScannedFolder scannedFolder;
        private List<File> data;

        private MyAdapter(Context context, int storageIndex, @NonNull FileCategory fileCat, @NonNull Storage.ScannedFolder scannedFolder, @NonNull Storage.SortOrder sortOrder)
        {
            this.context = context;
            this.storageIndex = storageIndex;
            this.fileCat = fileCat;
            this.scannedFolder = scannedFolder;
            data = this.scannedFolder.getLocalFiles(sortOrder);
        }

        public void setOrder(@NonNull Storage.SortOrder sortOrder)
        {
            data = scannedFolder.getLocalFiles(sortOrder);
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount()
        {
            return data.size();
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_local_file_list_view, parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position)
        {
            String strName, strSize;
            File localFile;

            if (0 <= position && position < data.size())
                localFile = data.get(position);
            else
                localFile = null;

            if (localFile!=null)
            {
                long fileSize_Byte = localFile.length();
                strSize = fileSize_Byte == 0 ? "----" : Storage.getFormatedSize(fileSize_Byte);
                strName = localFile.getName();
            }
            else
            {
                strSize = "----";
                strName = "<No File>";
            }

            holder.txtName.setText(strName);
            holder.txtSize.setText(strSize);
        }
    }
}