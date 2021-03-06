package com.example.filemanager.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.example.filemanager.model.DirectoryItem;
import com.example.filemanager.model.DirectoryItemType;
import com.example.filemanager.model.SortType;
import com.example.filemanager.repository.directory.DirectoryRepository;
import com.example.filemanager.repository.settings.SettingsRepository;
import com.example.filemanager.util.FilterHiddenDirectoryItemsUtil;
import com.example.filemanager.util.SearchDirectoryItemsUtil;
import com.example.filemanager.util.SortDirectoryItemsUtil;
import com.example.filemanager.util.Unit;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class DirectoryViewModel extends ViewModel {
    private DirectoryRepository directoryRepository;
    private SettingsRepository settingsRepository;
    private CompositeDisposable disposable = new CompositeDisposable();

    private Stack<String> directories = new Stack<>();
    private List<DirectoryItem> cachedDirectoryContent;

    private List<DirectoryItem> itemsToMove = new ArrayList<>();
    private List<DirectoryItem> itemsToCopy = new ArrayList<>();

    public Subject<Boolean> isLoading = BehaviorSubject.createDefault(true);
    public Subject<String> currentDirectory = BehaviorSubject.create();
    public Subject<List<DirectoryItem>> directoryContent = BehaviorSubject.create();
    public Subject<Throwable> error = PublishSubject.create();

    public BehaviorSubject<Boolean> isCopyModeEnabled = BehaviorSubject.create();
    public Subject<Boolean> isCopyDialogVisible = BehaviorSubject.create();
    public Subject<String> searchQuery = BehaviorSubject.create();

    public Subject<Unit> showSortTypeDialogEvent = PublishSubject.create();
    public Subject<Unit> showCreateDirectoryDialogEvent = PublishSubject.create();
    public Subject<DirectoryItem> showRenameItemDialogEvent = PublishSubject.create();
    public Subject<DirectoryItem> showDeleteItemDialogEvent = PublishSubject.create();
    public Subject<List<DirectoryItem>> showDeleteItemsDialogEvent = PublishSubject.create();
    public Subject<DirectoryItem> showInfoItemDialogEvent = PublishSubject.create();
    public Subject<DirectoryItem> shareFileEvent = PublishSubject.create();
    public Subject<DirectoryItem> openFileEvent = PublishSubject.create();
    public Subject<Unit> closeScreenEvent = PublishSubject.create();


    public DirectoryViewModel(@NonNull String directory, @NonNull DirectoryRepository directoryRepository, @NonNull SettingsRepository settingsRepository) {
        this.directoryRepository = directoryRepository;
        this.settingsRepository = settingsRepository;
        goToDirectory(directory);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposable.dispose();
    }


    public void handleBackPressed() {
        if (isRootDirectory() && isCopyModeEnabled()) {
            disableCopyMode();
        } else {
            goToParentDirectory();
        }
    }

    public void handleActionBarBackPressed() {
        if (isCopyModeEnabled()) {
            disableCopyMode();
        } else {
            goToParentDirectory();
        }
    }

    public void handleItemClicked(@NonNull DirectoryItem item) {
        if (item.getType() == DirectoryItemType.DIRECTORY) {
            goToDirectory(item.getFilePath());
        } else {
            openFileEvent.onNext(item);
        }
    }

    public void handleCutClicked(@NonNull DirectoryItem item) {
        itemsToMove.add(item);
        enableCopyMode();
    }

    public void handleCutClicked(@NonNull List<DirectoryItem> items) {
        itemsToMove.addAll(items);
        enableCopyMode();
    }

    public void handleCopyClicked(@NonNull DirectoryItem item) {
        itemsToCopy.add(item);
        enableCopyMode();
    }

    public void handleCopyClicked(@NonNull List<DirectoryItem> items) {
        itemsToCopy.addAll(items);
        enableCopyMode();
    }

    public void handlePasteClicked() {
        isCopyDialogVisible.onNext(true);

        String currentDirectory = directories.peek();

        Disposable subscription = directoryRepository
                .moveAndCopy(currentDirectory, itemsToMove, itemsToCopy)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            isCopyDialogVisible.onNext(false);
                            refreshCurrentDirectory();
                            disableCopyMode();
                        },
                        error -> {
                            isCopyDialogVisible.onNext(false);
                            this.error.onNext(error);
                            disableCopyMode();
                            error.printStackTrace();
                        }
                );

        disposable.add(subscription);
    }

    public void handleRenameClicked(@NonNull DirectoryItem item) {
        showRenameItemDialogEvent.onNext(item);
    }

    public void handleRenameConfirmed(@NonNull String newName, @NonNull DirectoryItem item) {
        if (item.getName().equals(newName)) {
            return;
        }

        isLoading.onNext(true);

        Disposable subscription = directoryRepository
                .rename(newName, item)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::refreshCurrentDirectory,
                        error -> {
                            isLoading.onNext(false);
                            this.error.onNext(error);
                            error.printStackTrace();
                        }
                );

        disposable.add(subscription);
    }

    public void handleDeleteClicked(@NonNull DirectoryItem item) {
        showDeleteItemDialogEvent.onNext(item);
    }

    public void handleDeleteClicked(@NonNull List<DirectoryItem> items) {
        showDeleteItemsDialogEvent.onNext(items);
    }

    public void handleDeleteConfirmed(@NonNull DirectoryItem item) {
        isLoading.onNext(true);

        Disposable subscription = directoryRepository
                .delete(item)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::refreshCurrentDirectory,
                        error -> {
                            isLoading.onNext(false);
                            this.error.onNext(error);
                            error.printStackTrace();
                        }
                );

        disposable.add(subscription);
    }

    public void handleDeleteConfirmed(@NonNull List<DirectoryItem> items) {
        isLoading.onNext(true);

        Disposable subscription = directoryRepository
                .delete(items)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::refreshCurrentDirectory,
                        error -> {
                            isLoading.onNext(false);
                            this.error.onNext(error);
                            error.printStackTrace();
                        }
                );

        disposable.add(subscription);
    }

    public void handleInfoClicked(@NonNull DirectoryItem item) {
        showInfoItemDialogEvent.onNext(item);
    }

    public void handleShareClicked(@NonNull DirectoryItem item) {
        shareFileEvent.onNext(item);
    }

    public void handleChangeSortTypeClicked() {
        showSortTypeDialogEvent.onNext(Unit.get());
    }

    public void handleSortTypeChanged() {
        refreshCachedDirectoryContent();
    }

    public void handleShowOrHideHiddenFilesClicked() {
        boolean areHiddenFilesVisible = settingsRepository.areHiddenFilesVisible();
        areHiddenFilesVisible = !areHiddenFilesVisible;
        settingsRepository.setHiddenFilesVisible(areHiddenFilesVisible);
        refreshCachedDirectoryContent();
    }

    public void handleCreateDirectoryClicked() {
        showCreateDirectoryDialogEvent.onNext(Unit.get());
    }

    public void handleCreateDirectoryConfirmed(@NonNull String newDirectoryName) {
        isLoading.onNext(true);

        String currentDirectory = directories.peek();
        Disposable subscription = directoryRepository
                .createDirectory(currentDirectory, newDirectoryName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::refreshCurrentDirectory,
                        error -> {
                            isLoading.onNext(false);
                            this.error.onNext(error);
                            error.printStackTrace();
                        }
                );

        disposable.add(subscription);
    }

    public void handleSearchQueryChanged(@NonNull String query) {
        if (cachedDirectoryContent == null)  {
            return;
        }

        isLoading.onNext(true);

        Disposable subscription = Single.just(cachedDirectoryContent)
                .map(items -> searchDirectoryItems(items, query))
                .map(this::filterHiddenFiles)
                .map(this::sortDirectoryItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            isLoading.onNext(false);
                            searchQuery.onNext(query);
                            directoryContent.onNext(result);
                        },
                        error -> {
                            isLoading.onNext(false);
                            this.error.onNext(error);
                            error.printStackTrace();
                        }
                );

        disposable.add(subscription);
    }


    private boolean isRootDirectory() {
        return directories.size() == 1;
    }

    private void goToDirectory(@NonNull String directory) {
        directories.push(directory);
        currentDirectory.onNext(directory);
        loadDirectoryContent(directory);
    }

    private void goToParentDirectory() {
        String parentDirectory;

        try {
            directories.pop();
            parentDirectory = directories.pop();
        } catch (EmptyStackException ex) {
            parentDirectory = null;
        }

        if (parentDirectory == null) {
            closeScreenEvent.onNext(Unit.get());
            return;
        }

        goToDirectory(parentDirectory);
    }

    private void refreshCurrentDirectory() {
        String currentDirectory = directories.peek();
        loadDirectoryContent(currentDirectory);
    }

    private void refreshCachedDirectoryContent() {
        if (cachedDirectoryContent == null)  {
            return;
        }

        isLoading.onNext(true);

        Disposable subscription = Single.just(cachedDirectoryContent)
                .map(this::filterHiddenFiles)
                .map(this::sortDirectoryItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            isLoading.onNext(false);
                            directoryContent.onNext(result);
                        },
                        error -> {
                            isLoading.onNext(false);
                            this.error.onNext(error);
                            error.printStackTrace();
                        }
                );

        disposable.add(subscription);
    }

    private void loadDirectoryContent(@NonNull String directory) {
        isLoading.onNext(true);

        Disposable subscription = directoryRepository
                .getDirectoryContent(directory)
                .map(items -> new Pair<>(items, filterHiddenFiles(items)))
                .map(p -> new Pair<>(p.first, sortDirectoryItems(p.second)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            List<DirectoryItem> originalResult = result.first;
                            List<DirectoryItem> filteredResult = result.second;
                            cachedDirectoryContent = originalResult;
                            isLoading.onNext(false);
                            directoryContent.onNext(filteredResult);
                        },
                        error -> {
                            isLoading.onNext(false);
                            this.error.onNext(error);
                            error.printStackTrace();
                        }
                );

        disposable.add(subscription);
    }


    @NonNull
    private List<DirectoryItem> filterHiddenFiles(@NonNull List<DirectoryItem> items) {
        boolean showHiddenFiles = settingsRepository.areHiddenFilesVisible();
        return FilterHiddenDirectoryItemsUtil.filterHiddenFiles(items, showHiddenFiles);
    }

    @NonNull
    private List<DirectoryItem> sortDirectoryItems(@NonNull List<DirectoryItem> items) {
        SortType sortType = settingsRepository.getSortType();
        return SortDirectoryItemsUtil.sort(items, sortType);
    }

    @NonNull
    private List<DirectoryItem> searchDirectoryItems(@NonNull List<DirectoryItem> items, @NonNull String query) {
        return SearchDirectoryItemsUtil.searchDirectoryItems(items, query);
    }


    private boolean isCopyModeEnabled() {
        return !itemsToMove.isEmpty() || !itemsToCopy.isEmpty();
    }

    private void enableCopyMode() {
        Boolean enabled = isCopyModeEnabled.getValue();
        if (enabled == null || !enabled) {
            isCopyModeEnabled.onNext(true);
        }
    }

    private void disableCopyMode() {
        Boolean enabled = isCopyModeEnabled.getValue();
        if (enabled == null || enabled) {
            isCopyModeEnabled.onNext(false);
        }

        itemsToMove.clear();
        itemsToCopy.clear();
    }


    public static class Factory implements ViewModelProvider.Factory {
        private String directory;
        private DirectoryRepository directoryRepository;
        private SettingsRepository settingsRepository;

        public Factory(@NonNull String directory, @NonNull DirectoryRepository directoryRepository, @NonNull SettingsRepository settingsRepository) {
            this.directory = directory;
            this.directoryRepository = directoryRepository;
            this.settingsRepository = settingsRepository;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new DirectoryViewModel(directory, directoryRepository, settingsRepository);
        }
    }
}
