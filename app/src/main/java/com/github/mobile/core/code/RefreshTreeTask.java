/*
 * Copyright 2012 GitHub Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.mobile.core.code;

import android.accounts.Account;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.github.mobile.accounts.AuthenticatedUserTask;
import com.google.inject.Inject;

import java.io.IOException;
import java.util.concurrent.Executor;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.Reference;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;

/**
 * Task to load the tree for a repository's default branch
 */
public class RefreshTreeTask extends AuthenticatedUserTask<FullTree> {

    private static final String TAG = "RefreshTreeTask";

    private final Repository repository;

    @Inject
    private RepositoryService repoService;

    @Inject
    private DataService dataService;

    /**
     * Create task to refresh repository's tree
     *
     * @param repository
     * @param context
     * @param executor
     */
    public RefreshTreeTask(final Repository repository, final Context context,
            final Executor executor) {
        super(context, executor);

        this.repository = repository;
    }

    /**
     * Create task to refresh repository's tree
     *
     * @param repository
     * @param context
     */
    public RefreshTreeTask(final Repository repository, final Context context) {
        super(context);

        this.repository = repository;
    }

    @Override
    protected FullTree run(Account account) throws Exception {
        String branch = repository.getMasterBranch();
        if (TextUtils.isEmpty(branch)) {
            branch = repoService.getRepository(repository).getMasterBranch();
            if (TextUtils.isEmpty(branch))
                throw new IOException("Repository does not have master branch");
        }

        Reference ref = dataService.getReference(repository, "heads/" + branch);
        if (ref == null || ref.getObject() == null
                || TextUtils.isEmpty(ref.getObject().getSha()))
            throw new IOException(
                    "Reference does not have associated commit SHA-1");

        Commit commit = dataService.getCommit(repository, ref.getObject()
                .getSha());
        if (commit == null || commit.getTree() == null
                || TextUtils.isEmpty(commit.getTree().getSha()))
            throw new IOException("Commit does not have associated tree SHA-1");

        Tree tree = dataService.getTree(repository, commit.getTree().getSha(),
                true);
        return new FullTree(tree, branch);
    }

    @Override
    protected void onException(Exception e) throws RuntimeException {
        super.onException(e);

        Log.d(TAG, "Exception loading tree", e);
    }
}