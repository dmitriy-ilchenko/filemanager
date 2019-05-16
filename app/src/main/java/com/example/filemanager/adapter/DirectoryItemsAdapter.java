package com.example.filemanager.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.filemanager.R;
import com.example.filemanager.databinding.ItemDirectoryItemBinding;
import com.example.filemanager.model.DirectoryItem;
import com.example.filemanager.model.DirectoryItemType;
import com.example.filemanager.util.DateFormatUtil;
import com.example.filemanager.util.FileSizeFormatUtil;

import java.util.Collections;
import java.util.List;

public class DirectoryItemsAdapter extends RecyclerView.Adapter<DirectoryItemsAdapter.ViewHolder> {

    public interface Listener {
        void onDirectoryItemClicked(@NonNull DirectoryItem item);
        void onDirectoryItemCutClicked(@NonNull DirectoryItem item);
        void onDirectoryItemCopyClicked(@NonNull DirectoryItem item);
        void onDirectoryItemRenameClicked(@NonNull DirectoryItem item);
        void onDirectoryItemDeleteClicked(@NonNull DirectoryItem item);
        void onDirectoryItemInfoClicked(@NonNull DirectoryItem item);
        void onDirectoryItemShareClicked(@NonNull DirectoryItem item);
        void onItemSelectionChanged(boolean isInSelectMode);
    }


    private List<DirectoryItem> data = Collections.emptyList();
    private String searchQuery = "";
    private Listener listener;
    private int selectedItemsCount;


    public DirectoryItemsAdapter(Listener listener) {
        this.listener = listener;
    }


    public void setSearchQuery(@NonNull String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public void setData(@NonNull List<DirectoryItem> data) {
        this.data = data;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemDirectoryItemBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.item_directory_item, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        DirectoryItem item = data.get(i);
        viewHolder.bind(item);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    private void notifyItemSelectionChanged() {
        boolean isInSelectMode = isInSelectMode();
        listener.onItemSelectionChanged(isInSelectMode);
    }

    private boolean isInSelectMode() {
        return selectedItemsCount > 0;
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        private boolean isItemSelected;
        private ItemDirectoryItemBinding binding;

        public ViewHolder(@NonNull ItemDirectoryItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(@NonNull DirectoryItem item) {
            initEventHandlers(item);

            showDirectoryItemName(item);
            showDirectoryItemTypeImage(item);
            showDateIfNeeded(item);
            showFileSizeIfNeeded(item);
        }

        private void initEventHandlers(@NonNull DirectoryItem item) {
            binding.imageViewMore.setOnClickListener(v -> showPopupMenu(v, item));

            binding.getRoot().setOnClickListener(v -> {
                handleItemClicked(item);
            });

            binding.getRoot().setOnLongClickListener(v -> {
                handleItemLongClicked();
                return true;
            });
        }

        private void handleItemClicked(@NonNull DirectoryItem item) {
            if (isInSelectMode()) {
                toggleItemSelected();
            } else {
                listener.onDirectoryItemClicked(item);
            }
        }

        private void handleItemLongClicked() {
            toggleItemSelected();
        }

        private void showDirectoryItemName(@NonNull DirectoryItem item) {
            CharSequence name;
            if (searchQuery.isEmpty()) {
                name = item.getName();
            } else {
                name = highlightText(item.getName(), searchQuery);
            }
            binding.textViewName.setText(name);
        }

        private void showDirectoryItemTypeImage(@NonNull DirectoryItem item) {
            int drawable = mapDirectoryTypeToDrawable(item.getType());
            binding.imageViewItemType.setBackgroundResource(drawable);
        }

        private void showDateIfNeeded(@NonNull DirectoryItem item) {
            if (!item.getType().equals(DirectoryItemType.DIRECTORY)) {
                return;
            }

            String dateText = DateFormatUtil.formatDate(item.getLastModificationDate());
            binding.textViewProperty.setText(dateText);
        }

        private void showFileSizeIfNeeded(@NonNull DirectoryItem item) {
            if (item.getType().equals(DirectoryItemType.DIRECTORY)) {
                return;
            }

            String formattedFileSize = FileSizeFormatUtil.formatFileSize(binding.getRoot().getContext(), item.getFileSizeInBytes());
            binding.textViewProperty.setText(formattedFileSize);
        }

        @DrawableRes
        private int mapDirectoryTypeToDrawable(@NonNull DirectoryItemType itemType) {
            switch (itemType) {
                case DIRECTORY:
                    return R.drawable.ic_directory;
                case IMAGE:
                    return R.drawable.ic_image;
                case AUDIO:
                    return R.drawable.ic_audio;
                case VIDEO:
                    return R.drawable.ic_video;
                case TEXT:
                    return R.drawable.ic_document;
                case OTHER:
                    return R.drawable.ic_other_file;
            }
            return -1;
        }

        private void toggleItemSelected() {
            isItemSelected = !isItemSelected;

            int backgroundColorId;
            int moreButtonVisibility;

            if (isItemSelected) {
                selectedItemsCount++;
                backgroundColorId = R.color.light_gray;
                moreButtonVisibility = View.INVISIBLE;
            } else {
                selectedItemsCount--;
                backgroundColorId = R.color.transparent;
                moreButtonVisibility = View.VISIBLE;
            }

            Resources resources = binding.getRoot().getContext().getResources();
            int backgroundColor = resources.getColor(backgroundColorId);

            binding.getRoot().setBackgroundColor(backgroundColor);
            binding.imageViewMore.setVisibility(moreButtonVisibility);

            notifyItemSelectionChanged();
        }

        @SuppressLint("RestrictedApi")
        private void showPopupMenu(@NonNull View view, @NonNull DirectoryItem item) {
            Context context = itemView.getContext();

            PopupMenu popup = new PopupMenu(context, view);
            MenuInflater inflater = popup.getMenuInflater();
            Menu popupMenu = popup.getMenu();
            inflater.inflate(R.menu.menu_directory_item, popupMenu);

            popup.setOnMenuItemClickListener(menuItem -> {
                handlePopupMenuItemClicked(menuItem.getItemId(), item);
                return true;
            });

            if (item.getType().equals(DirectoryItemType.DIRECTORY)) {
                popupMenu.removeItem(R.id.item_share);
            }

            MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) popupMenu, view);
            menuHelper.setForceShowIcon(true);
            menuHelper.show();
        }

        private void handlePopupMenuItemClicked(@IdRes int itemId, @NonNull DirectoryItem item) {
            switch (itemId) {
                case R.id.item_cut: {
                    listener.onDirectoryItemCutClicked(item);
                    break;
                }
                case R.id.item_copy: {
                    listener.onDirectoryItemCopyClicked(item);
                    break;
                }
                case R.id.item_rename: {
                    listener.onDirectoryItemRenameClicked(item);
                    break;
                }
                case R.id.item_delete: {
                    listener.onDirectoryItemDeleteClicked(item);
                    break;
                }
                case R.id.item_info: {
                    listener.onDirectoryItemInfoClicked(item);
                    break;
                }
                case R.id.item_share: {
                    listener.onDirectoryItemShareClicked(item);
                    break;
                }
            }
        }

        private SpannableString highlightText(@NonNull String text, @NonNull String searchText) {
            SpannableString result = new SpannableString(text);

            // Remove previous spans
            BackgroundColorSpan[] backgroundSpans = result.getSpans(0, result.length(), BackgroundColorSpan.class);
            for (BackgroundColorSpan span : backgroundSpans) {
                result.removeSpan(span);
            }

            searchText = searchText.toLowerCase();
            text = text.toLowerCase();

            // Highlight all searchText occurrences
            int indexOfKeyword = text.indexOf(searchText, 0);
            while (indexOfKeyword >= 0) {
                //Create a background color span on the keyword
                result.setSpan(new BackgroundColorSpan(Color.YELLOW), indexOfKeyword, indexOfKeyword + searchText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                //Get the next index of the keyword
                indexOfKeyword = text.indexOf(searchText, indexOfKeyword + searchText.length());
            }

            return result;
        }
    }
}
