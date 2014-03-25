package org.hive2hive.core.file.watcher;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hive2hive.core.api.interfaces.IFileManager;
import org.hive2hive.core.exceptions.NoPeerConnectionException;
import org.hive2hive.core.exceptions.NoSessionException;
import org.hive2hive.core.file.FileUtil;
import org.hive2hive.core.log.H2HLogger;
import org.hive2hive.core.log.H2HLoggerFactory;
import org.hive2hive.core.processes.framework.exceptions.InvalidProcessStateException;
import org.hive2hive.core.processes.framework.interfaces.IProcessComponent;

public class DeleteFileBuffer extends BaseFileBuffer {

	private static final H2HLogger logger = H2HLoggerFactory.getLogger(DeleteFileBuffer.class);
	private static final long MAX_DELETION_PROCESS_DURATION_MS = 30000; // timeout to omit blocks

	public DeleteFileBuffer(IFileManager fileManager, File root) {
		super(fileManager, root);
	}

	/**
	 * Process the files in the buffer after the buffering time exceeded.
	 * 
	 * @param bufferedFiles
	 */
	protected void processBuffer(IFileBufferHolder buffer) {
		List<File> bufferedFiles = buffer.getFileBuffer();
		Set<File> syncFiles = buffer.getSyncFiles();

		Set<File> toRemove = new HashSet<File>();
		for (File file : bufferedFiles) {
			if (!syncFiles.contains(file)) {
				// has already been deleted
				toRemove.add(file);
			}
		}
		bufferedFiles.removeAll(toRemove);

		// sort first
		FileUtil.sortPreorder(bufferedFiles);
		// reverse the sorting
		Collections.reverse(bufferedFiles);

		// delete individual files
		for (File toDelete : bufferedFiles) {
			try {
				logger.debug("Starting to delete buffered file " + toDelete);
				IProcessComponent delete = fileManager.delete(toDelete);
				if (!fileManager.isAutostart())
					delete.start();
				delete.await();
			} catch (NoSessionException | NoPeerConnectionException | InvalidProcessStateException
					| InterruptedException e) {
				logger.error(e.getMessage());
			}
		}

		logger.debug("Buffer with " + bufferedFiles.size() + " files processed.");
	}
}