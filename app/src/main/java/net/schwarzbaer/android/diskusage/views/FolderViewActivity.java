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
import net.schwarzbaer.android.diskusage.databinding.ActivityFolderViewBinding;
import net.schwarzbaer.android.diskusage.models.FileCategory;
import net.schwarzbaer.android.diskusage.models.FileSystemScanner;
import net.schwarzbaer.android.diskusage.models.Storage;

import java.util.List;
import java.util.Locale;

public class FolderViewActivity extends AppCompatActivity
{
    public static final String activityParam_StorageIndex = "StorageIndex";
    public static final String activityParam_FileCategory = "FileCategory";
    public static final String activityParam_FolderID = "FolderID";
    public static final String activityParam_SortOrder = "SortOrder";

    private ActivityFolderViewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityFolderViewBinding.inflate(getLayoutInflater());
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
        final String initialSortOrderStr = intent.getStringExtra(activityParam_SortOrder);
        Storage.SortOrder initialSortOrder = Storage.SortOrder.valueOf_checked(initialSortOrderStr);
        if (initialSortOrder==null) initialSortOrder = Storage.SortOrder.Original;

        Storage storage = FileSystemScanner.getInstance().getStorage(storageIndex);
        Storage.ScannedFolder scannedFolder =
                storage==null || fileCat==null
                    ? null
                    : folderID == Storage.FolderID_Root
                        ? storage.getRootFolder(fileCat)
                        : storage.getFolder(folderID);

        binding.txtFolderViewOutput.setText(String.format("Folder: %s", scannedFolder == null ? "<no folder>" : scannedFolder.getPath()));

        binding.listFolders.setLayoutManager(new LinearLayoutManager(this));

        ArrayAdapter<Storage.SortOrder> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Storage.SortOrder.values());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spnFolderOrder.setAdapter(spinnerAdapter);

        if (scannedFolder != null) {
            MyAdapter listAdapter = new MyAdapter(this, storageIndex, fileCat, scannedFolder, folderID, initialSortOrder);
            binding.listFolders.setAdapter(listAdapter);
            binding.spnFolderOrder.setSelection( spinnerAdapter.getPosition(initialSortOrder));
            binding.spnFolderOrder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    Storage.SortOrder sortOrder = spinnerAdapter.getItem(position);
                    listAdapter.setOrder(sortOrder==null ? Storage.SortOrder.Original : sortOrder);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent)
                {
                    listAdapter.setOrder(Storage.SortOrder.Original);
                }
            });
        }
        else
            binding.spnFolderOrder.setEnabled(false);
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
            txtTitle  = itemView.findViewById(R.id.txtFolderTitle);
            txtFiles  = itemView.findViewById(R.id.txtFolderFiles);
            txtSize   = itemView.findViewById(R.id.txtFolderSize);
            txtMarker = itemView.findViewById(R.id.txtFolderMarker);
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
        private final int folderID;
        @NonNull
        private Storage.SortOrder sortOrder;
        private List<Storage.ScannedFolder> subFolders;

        private MyAdapter(Context context, int storageIndex, @NonNull FileCategory fileCat, @NonNull Storage.ScannedFolder scannedFolder, int folderID, @NonNull Storage.SortOrder sortOrder)
        {
            this.context = context;
            this.storageIndex = storageIndex;
            this.fileCat = fileCat;
            this.scannedFolder = scannedFolder;
            this.folderID = folderID;
            this.sortOrder = sortOrder;
            subFolders = this.scannedFolder.getSubFolders(this.sortOrder);
        }

        public void setOrder(Storage.SortOrder sortOrder)
        {
            this.sortOrder = sortOrder;
            subFolders = scannedFolder.getSubFolders(this.sortOrder);
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount()
        {
            int fileCount = scannedFolder.getLocalFileCount();
            int folderCount = subFolders.size();
            return folderCount + (fileCount==0 ? 0 : 1);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_folder_view, parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position)
        {
            final String strTitle, strFiles, strSize;
            final View.OnClickListener onClickListener;

            int localFileCount = scannedFolder.getLocalFileCount();
            if (localFileCount!=0 && position==0)
            {
                strTitle = "[ Local Files ]";
                strFiles = localFileCount == 1 ? "1 file" : String.format(Locale.ENGLISH, "%d files", localFileCount);
                strSize = Storage.getFormatedSize(scannedFolder.getLocalFilesSize_Byte());
                onClickListener = view -> {
                    Intent intent = new Intent(context, LocalFileListViewActivity.class);
                    intent.putExtra(LocalFileListViewActivity.activityParam_StorageIndex, storageIndex);
                    intent.putExtra(LocalFileListViewActivity.activityParam_FileCategory, fileCat.name());
                    intent.putExtra(LocalFileListViewActivity.activityParam_FolderID, folderID);
                    intent.putExtra(LocalFileListViewActivity.activityParam_SortOrder, sortOrder.name());
                    context.startActivity(intent);
                };
            }
            else
            {
                int folderIndex = position - (localFileCount!=0 ? 1 : 0);
                Storage.ScannedFolder scannedSubFolder;

                if (0 <= folderIndex && folderIndex < subFolders.size())
                    scannedSubFolder = subFolders.get(folderIndex);
                else
                    scannedSubFolder = null;

                if (scannedSubFolder != null) {
                    long fileCount     = scannedSubFolder.getTotalFileCount();
                    long fileSize_Byte = scannedSubFolder.getTotalSize_Byte();
                    int subFolderID    = scannedSubFolder.getFolderID();
                    strTitle = scannedSubFolder.getName();
                    strFiles = fileCount == 0 ? "no files" : fileCount == 1 ? "1 file" : String.format(Locale.ENGLISH, "%d files", fileCount);
                    strSize = fileSize_Byte == 0 ? "----" : Storage.getFormatedSize(fileSize_Byte);
                    onClickListener = view -> {
                        Intent intent = new Intent(context, FolderViewActivity.class);
                        intent.putExtra(FolderViewActivity.activityParam_StorageIndex, storageIndex);
                        intent.putExtra(FolderViewActivity.activityParam_FileCategory, fileCat.name());
                        intent.putExtra(FolderViewActivity.activityParam_FolderID, subFolderID);
                        intent.putExtra(FolderViewActivity.activityParam_SortOrder, sortOrder.name());
                        context.startActivity(intent);
                    };
                }
                else
                {
                    strTitle = "<No Folder>";
                    strFiles = "<No Files>";
                    strSize = "----";
                    onClickListener = null;
                }
            }

            holder.txtTitle.setText(strTitle);
            holder.txtFiles.setText(strFiles);
            holder.txtSize.setText(strSize);

            if (onClickListener == null)
                holder.txtMarker.setText("X");
            else
            {
                holder.txtMarker.setText(">>");
                holder.itemView.setOnClickListener(onClickListener);
            }
        }
    }
}