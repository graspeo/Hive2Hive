package org.hive2hive.core.processes.implementations.files.delete;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.hive2hive.core.H2HConstants;
import org.hive2hive.core.processes.framework.RollbackReason;
import org.hive2hive.core.processes.framework.abstracts.ProcessStep;
import org.hive2hive.core.processes.framework.exceptions.InvalidProcessStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes a file on the disk (if it exists)
 * 
 * @author Nico
 * 
 */
public class DeleteFileOnDiskStep extends ProcessStep {

	private static final Logger logger = LoggerFactory.getLogger(DeleteFileOnDiskStep.class);

	private final File file;

	public DeleteFileOnDiskStep(File file) {
		this.file = file;
	}

	@Override
	protected void doExecute() throws InvalidProcessStateException {
		if (file.exists()) {
			logger.debug("Deleting file '{}' on disk.", file.getAbsolutePath());

			try {
				FileUtils.moveFileToDirectory(file, H2HConstants.TRASH_DIRECTORY, true);
			} catch (IOException e) {
				logger.warn("File '{}' could not be moved to the trash directory and gets deleted.",
						file.getAbsolutePath());
				FileUtils.deleteQuietly(file);
			}
		} else {
			logger.warn("File '{}' cannot be deleted as it does not exist.", file.getAbsolutePath());
		}
	}

	@Override
	protected void doRollback(RollbackReason reason) throws InvalidProcessStateException {
		File trashFile = new File(H2HConstants.TRASH_DIRECTORY, file.getName());

		if (trashFile.exists()) {
			try {
				FileUtils.moveFileToDirectory(trashFile, file.getParentFile(), true);
			} catch (IOException e) {
				logger.warn("File '{}' could not be moved to the original folder.",
						trashFile.getAbsolutePath());
			}
		} else {
			logger.warn("File '{}' cannot be recovered from trash as it does not exist.",
					trashFile.getAbsolutePath());
		}
	}

}
